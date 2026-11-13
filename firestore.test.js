const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_nexa_123";
const BOB_UID = "bob_nexa_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTimestamp() {
  return new Date(Date.now() - 1000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write agents, automations, or memories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/agents`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/automations`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/memories`).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated owner can create and read their own profile and agent", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      bossName: "BOSS",
      wakeWords: ["hey nexa", "hello nexa"],
      activeProvider: "FIREBASE_GEMINI",
      energyPoints: 10,
      moodStatus: "Neural Core Ready",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/agents`).doc("agent_whatsapp").set({
      userId: ALICE_UID,
      name: "WhatsApp Pro",
      role: "Auto-reply smart assistant",
      systemPrompt: "Reply politely in Hinglish.",
      triggerType: "WHATSAPP_INCOMING",
      allowedTools: ["AUTO_REPLY", "OPEN_APP"],
      isEnabled: true,
      autoExecute: false,
      runCount: 0,
      accentColorHex: "#38BDF8",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/agents`).get());
});

test("Shadow update with ghost field or invalid enum is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/agents`).doc("bad_agent").set({
      userId: ALICE_UID,
      name: "Ghost Agent",
      role: "Role",
      systemPrompt: "Prompt",
      triggerType: "MANUAL",
      allowedTools: ["AUTO_REPLY"],
      isEnabled: true,
      autoExecute: false,
      runCount: 0,
      accentColorHex: "#38BDF8",
      isAdmin: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/automations`).doc("bad_rule").set({
      userId: ALICE_UID,
      title: "Bad Rule",
      targetApp: "UNKNOWN_APP",
      triggerPattern: "*",
      replyTemplateOrGoal: "Hi",
      useAiReply: true,
      isActive: true,
      requireConfirmation: true,
      triggeredCount: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Cross-user access is strictly denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/memories`).doc("mem_1").set({
      userId: ALICE_UID,
      title: "OTP Rule",
      content: "Forward bank OTP only to verified mail",
      category: "OTP_RULE",
      agentName: "Gmail Agent",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/memories`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/memories`).doc("mem_1").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/memories`).doc("mem_2").set({
      userId: ALICE_UID,
      title: "Spoof",
      content: "Spoof",
      category: "MEMORY",
      agentName: "Spoof",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});
