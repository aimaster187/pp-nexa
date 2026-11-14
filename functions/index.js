const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldPath } = require("firebase-admin/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");

const OWNER_EMAIL = "pranavji628@gmail.com";
const DATABASE_ID =
  "ai-studio-android-nexaai-74a37412-09eb-422f-8076-ca2691f1d973";

initializeApp();
const db = getFirestore(undefined, DATABASE_ID);

setGlobalOptions({
  region: "asia-south1",
  maxInstances: 5
});

function login(req) {
  if (!req.auth) {
    throw new HttpsError("unauthenticated", "Please sign in first.");
  }
  return req.auth;
}

function admin(req) {
  const a = login(req);
  if (a.token.admin !== true || a.token.owner !== true) {
    throw new HttpsError("permission-denied", "Owner admin access required.");
  }
  return a;
}

function pathCheck(value, collection = false) {
  if (typeof value !== "string" || value.length > 500) {
    throw new HttpsError("invalid-argument", "Invalid Firestore path.");
  }
  const p = value.split("/");
  if (p.some(x => !x || x === "." || x === ".." || x.length > 1500)) {
    throw new HttpsError("invalid-argument", "Invalid path.");
  }
  if (collection ? p.length % 2 !== 1 : p.length % 2 !== 0) {
    throw new HttpsError("invalid-argument", "Invalid collection/document path.");
  }
  return p.join("/");
}

const secretPattern =
  /(password|passwd|secret|private.?key|api.?key|access.?token|refresh.?token|otp|authorization)/i;

function redact(value) {
  if (Array.isArray(value)) return value.map(redact);
  if (value && typeof value === "object" && !value.toDate) {
    const out = {};
    for (const [k, v] of Object.entries(value)) {
      out[k] = secretPattern.test(k) ? "[REDACTED]" : redact(v);
    }
    return out;
  }
  return value;
}

function removeRedacted(value) {
  if (Array.isArray(value)) return value.map(removeRedacted);
  if (value && typeof value === "object") {
    const out = {};
    for (const [k, v] of Object.entries(value)) {
      if (v === "[REDACTED]") continue;
      out[k] = removeRedacted(v);
    }
    return out;
  }
  return value;
}

async function audit(a, action, path, extra = {}) {
  await db.collection("zed_admin_audit").add({
    adminUid: a.uid,
    adminEmail: a.token.email || "",
    action,
    path,
    details: redact(extra),
    createdAt: new Date()
  });
}

exports.bootstrapOwner = onCall(async req => {
  const a = login(req);
  const email = String(a.token.email || "").toLowerCase();

  if (email !== OWNER_EMAIL || a.token.email_verified !== true) {
    throw new HttpsError(
      "permission-denied",
      "Verified owner Gmail required: " + OWNER_EMAIL
    );
  }

  const auth = getAuth();
  const user = await auth.getUser(a.uid);
  await auth.setCustomUserClaims(a.uid, {
    ...(user.customClaims || {}),
    admin: true,
    owner: true
  });

  await audit(a, "OWNER_BOOTSTRAP", "auth/" + a.uid);
  return { success: true, message: "Owner claim set. Sign in again." };
});

exports.adminSummary = onCall(async req => {
  const a = admin(req);
  const [users, licenses] = await Promise.all([
    db.collection("zed_users").get(),
    db.collection("zed_licenses").get()
  ]);
  const u = users.docs.map(d => d.data());
  const l = licenses.docs.map(d => d.data());

  await audit(a, "DASHBOARD_READ", "/");

  return {
    totalUsers: users.size,
    vipUsers: u.filter(x => x.isVip === true).length,
    bannedUsers: u.filter(x => x.banned === true).length,
    totalLicenses: licenses.size,
    activeLicenses: l.filter(x => x.status === "active").length,
    usedLicenses: l.filter(x => x.status === "used").length,
    bannedLicenses: l.filter(x => x.status === "banned").length
  };
});

exports.adminListCollections = onCall(async req => {
  const a = admin(req);
  const parent = req.data?.parentPath;

  let refs;
  if (parent) {
    const p = pathCheck(parent);
    refs = await db.doc(p).listCollections();
  } else {
    refs = await db.listCollections();
  }

  await audit(a, "COLLECTIONS_READ", parent || "/");
  return { collections: refs.map(c => c.id).sort() };
});

exports.adminListDocuments = onCall(async req => {
  const a = admin(req);
  const path = pathCheck(req.data?.collectionPath, true);
  const limit = Math.min(Math.max(Number(req.data?.limit) || 30, 1), 100);

  let q = db.collection(path)
    .orderBy(FieldPath.documentId())
    .limit(limit + 1);

  if (req.data?.cursor) q = q.startAfter(String(req.data.cursor));

  const snap = await q.get();
  const docs = snap.docs.slice(0, limit);

  await audit(a, "DOCUMENTS_READ", path, { count: docs.length });

  return {
    documents: docs.map(d => ({
      id: d.id,
      path: d.ref.path,
      data: redact(d.data())
    })),
    nextCursor: snap.docs.length > limit
      ? docs[docs.length - 1].id
      : null
  };
});

exports.adminGetDocument = onCall(async req => {
  const a = admin(req);
  const path = pathCheck(req.data?.documentPath);
  const snap = await db.doc(path).get();

  await audit(a, "DOCUMENT_READ", path);
  return {
    exists: snap.exists,
    id: snap.id,
    path,
    data: snap.exists ? redact(snap.data()) : null
  };
});

exports.adminSetDocument = onCall(async req => {
  const a = admin(req);
  const path = pathCheck(req.data?.documentPath);
  const data = req.data?.data;

  if (!data || typeof data !== "object" || Array.isArray(data)) {
    throw new HttpsError("invalid-argument", "JSON object required.");
  }

  if (JSON.stringify(data).length > 90000) {
    throw new HttpsError("invalid-argument", "Document too large.");
  }

  const clean = removeRedacted(data);
  await db.doc(path).set(clean, { merge: true });
  await audit(a, "DOCUMENT_MERGE", path);

  return { success: true };
});

exports.adminDeleteDocument = onCall(async req => {
  const a = admin(req);
  const path = pathCheck(req.data?.documentPath);
  const id = path.split("/").pop();

  if (String(req.data?.confirmId || "") !== id) {
    throw new HttpsError("failed-precondition", "Exact document ID confirmation required.");
  }

  const ref = db.doc(path);
  const snap = await ref.get();

  if (!snap.exists) {
    throw new HttpsError("not-found", "Document does not exist.");
  }

  await audit(a, "DOCUMENT_DELETE", path, { oldData: redact(snap.data()) });
  await ref.delete();

  return { success: true };
});
