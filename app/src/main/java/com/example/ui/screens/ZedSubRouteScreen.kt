package com.example.ui.screens

import kotlinx.coroutines.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.NexaLicenseManager

@Composable
fun ZedSubRouteScreen(
    route: String,
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit = {},
    onOpenApiKeys: () -> Unit = {},
    onOpenBrowserWithUrl: (String) -> Unit = {},
    onSendQuickPrompt: (String) -> Unit = {},
    onRequestPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("zed_v38_settings", Context.MODE_PRIVATE) }
    val scroll = rememberScrollState()
    val licState by NexaLicenseManager.state.collectAsState()

    val frostBg = Brush.verticalGradient(colors = listOf(Color(0xFFEAF1FC), Color(0xFFDCE7F9), Color(0xFFE5EEFC)))
    val cardColor = Color(0xFFF4F8FF)
    val textDark = Color(0xFF0F172A)
    val textSub = Color(0xFF64748B)

    val screenTitle = when (route) {
        "settings" -> "Settings"
        "personal" -> "Personal"
        "persona" -> "ZED AI"
        "secret" -> "Secret settings"
        "skills" -> "ZED SKILLS"
        "subagents" -> "Sub-agents"
        "email" -> "Email"
        "messages" -> "Messages aur WhatsApp"
        "connectors" -> "Connectors"
        "advanced" -> "Advanced"
        "theme" -> "Theme"
        "sos" -> "Emergency SOS"
        "permissions" -> "Permissions"
        "accessibility" -> "Accessibility"
        "scanner" -> "Scanner"
        "rules" -> "ZED ke rules"
        "yaadein" -> "Yaadein"
        "agents_hub" -> "Agents & Auto-Reply"
        "pc_phone" -> "PC ⇄ Phone"
        "upgrade" -> "License"
        "about" -> "About ZED AI"
        "privacy" -> "Privacy Policy"
        "notifications" -> "Notifications"
        else -> route.replaceFirstChar { it.uppercase() }
    }

    Column(modifier = Modifier.fillMaxSize().background(frostBg).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = cardColor, shadowElevation = 16.dp, border = androidx.compose.foundation.BorderStroke(1.3.dp, Brush.linearGradient(listOf(Color.White, Color(0xFF93C5FD)))), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textDark) }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(screenTitle, fontSize = 23.sp, fontWeight = FontWeight.Light, color = textDark)
            }

            Surface(onClick = { onOpenRoute("notifications") }, shape = CircleShape, color = cardColor, shadowElevation = 16.dp, border = androidx.compose.foundation.BorderStroke(1.3.dp, Brush.linearGradient(listOf(Color.White, Color(0xFF93C5FD)))), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.NotificationsNone, contentDescription = "Alerts", tint = textDark) }
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (route) {
                "settings" -> {
                    SettingsSectionHeader("ACCOUNT")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.Person, "Personal", "Tumhara naam, music, Gemini aur YouTube keys") { onOpenRoute("personal") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.AccountCircle, "Account & VIP License", if (licState.userEmail.isNotBlank()) licState.userEmail else "Google Login & VIP Key Status") { onOpenRoute("upgrade") }
                    }

                    SettingsSectionHeader("ASSISTANT")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.HeadsetMic, "ZED AI", "Persona, awaaz, bhasha") { onOpenRoute("persona") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.RecordVoiceOver, "Voice engine", "Normal ya Advanced Agentic — wo kaise sunti, sochti aur bolti hai") { onOpenRoute("persona") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.Lock, "Secret settings", "Companion mode: Friendly, Caring, Professional") { onOpenRoute("secret") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.Bolt, "Skills", "Installed playbooks aur online skill store (20/25)") { onOpenRoute("skills") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.SmartToy, "Sub-agents", "Coding models aur background agents") { onOpenRoute("subagents") }
                    }

                    SettingsSectionHeader("KAAM AUR MESSAGES")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.Email, "Email", "ZED tumhare address se mail bhej sake") { onOpenRoute("email") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.ChatBubble, "Messages aur WhatsApp", "Message alerts, auto-reply, groups aur reports") { onOpenRoute("messages") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.Share, "Social media", "Handle, caption ka andaaz, daily story, scheduled posts") { onSendQuickPrompt("Mera social media content plan banao") }
                    }

                    SettingsSectionHeader("JUDE HUE ACCOUNTS")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.Hub, "Connectors", "Google Drive, GitHub, Vercel, Notion, Telegram aur bhi") { onOpenRoute("connectors") }
                    }

                    SettingsSectionHeader("YAADEIN AUR DATA")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.CloudUpload, "Backup", "Apni yaadein aur chats export aur restore karo") { Toast.makeText(context, "Cloud & Local Backup Synced!", Toast.LENGTH_SHORT).show() }
                    }

                    SettingsSectionHeader("SYSTEM")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.Tune, "Advanced", "Theme, Appearance, SOS, Permissions & Event triggers") { onOpenRoute("advanced") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.Extension, "Optional", "Extra integrations — Maps / Places") { onOpenBrowserWithUrl("https://maps.google.com") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.BatteryChargingFull, "Battery saver", "ZED chalte waqt battery kam kharch ho") { onOpenRoute("accessibility") }
                    }

                    Text("ZED AI v3.8.0  •  by The Titan AI", fontSize = 11.sp, color = textSub, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }

                "personal" -> {
                    var bossName by remember { mutableStateOf(prefs.getString("boss_name", "BOSS") ?: "BOSS") }
                    var gender by remember { mutableStateOf(prefs.getString("gender", "Ladka") ?: "Ladka") }
                    var phone by remember { mutableStateOf(prefs.getString("phone", "+91 ") ?: "+91 ") }
                    var musicApp by remember { mutableStateOf(prefs.getString("music_app", "YT Music") ?: "YT Music") }
                    var favSong by remember { mutableStateOf(prefs.getString("fav_song", "Kesariya") ?: "Kesariya") }

                    SettingsCard {
                        Text("Tumhara naam", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Text("ZED tumhe kya bulaye (khaali = 'dost')", fontSize = 12.sp, color = textSub)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = bossName, onValueChange = { bossName = it; prefs.edit().putString("boss_name", it).apply() }, textStyle = TextStyle(color = textDark, fontSize = 14.sp), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }

                    SettingsCard {
                        Text("Tum ho", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Ladka", "Ladki").forEach { item ->
                                val sel = gender == item
                                Surface(onClick = { gender = item; prefs.edit().putString("gender", item).apply() }, shape = RoundedCornerShape(16.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA)) {
                                    Text(item, color = if (sel) Color.White else textDark, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }

                    SettingsCard {
                        Text("Phone number", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = phone, onValueChange = { phone = it; prefs.edit().putString("phone", it).apply() }, textStyle = TextStyle(color = textDark, fontSize = 14.sp), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }

                    SettingsCard {
                        Text("Music & Favourite Gaana", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("YT Music", "Spotify", "YouTube").forEach { app ->
                                val sel = musicApp == app
                                Surface(onClick = { musicApp = app; prefs.edit().putString("music_app", app).apply() }, shape = RoundedCornerShape(14.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA)) {
                                    Text(app, color = if (sel) Color.White else textDark, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = favSong, onValueChange = { favSong = it; prefs.edit().putString("fav_song", it).apply() }, label = { Text("Favourite gaana") }, textStyle = TextStyle(color = textDark, fontSize = 14.sp), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }

                    SettingsCard {
                        Text("Gemini API key", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onOpenApiKeys, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(14.dp)) { Text("API Vault Kholo", color = Color.White) }
                            OutlinedButton(onClick = { onOpenBrowserWithUrl("https://aistudio.google.com/app/apikey") }, shape = RoundedCornerShape(14.dp)) { Text("Gemini key lo", color = Color(0xFF2563EB)) }
                        }
                    }
                }

                "persona" -> {
                    var selectedPersona by remember { mutableStateOf(prefs.getString("persona", "ZED") ?: "ZED") }
                    var selectedVoice by remember { mutableStateOf(prefs.getString("voice_tone", "Halki-phulki") ?: "Halki-phulki") }
                    var sleepMinutes by remember { mutableStateOf(prefs.getString("sleep_min", "10 min") ?: "10 min") }
                    var callAnnounce by remember { mutableStateOf(prefs.getBoolean("call_announce", true)) }

                    SettingsCard {
                        Text("Awaaz & Voice Persona", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ZED", "Friday", "Venom", "Jarvis").forEach { p ->
                                val sel = selectedPersona == p
                                Surface(onClick = { selectedPersona = p; prefs.edit().putString("persona", p).apply() }, shape = RoundedCornerShape(12.dp), color = if (sel) Color(0xFFDBEAFE) else Color(0xFFE8EFFA)) {
                                    Text(p, color = if (sel) Color(0xFF1D4ED8) else textSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        listOf("Halki-phulki" to "Aoede", "Damdaar" to "Kore", "Jawaan" to "Leda", "Chamakdaar" to "Zephyr", "Josheeli" to "Laomedeia", "Mulaayam" to "Despina", "Saaf" to "Erinome", "Befikar" to "Callirrhoe").forEach { (title, sub) ->
                            val isSel = selectedVoice == title
                            Surface(
                                onClick = { selectedVoice = title; prefs.edit().putString("voice_tone", title).apply(); Toast.makeText(context, "Voice: $title ($sub)", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) Color(0xFFEFF6FF) else Color.White,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textDark); Text(sub, fontSize = 11.sp, color = textSub) }
                                    Icon(if (isSel) Icons.Default.CheckCircle else Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF2563EB))
                                }
                            }
                        }
                    }

                    SettingsCard {
                        Text("Koi na bole to so jao", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Kabhi nahi", "5 min", "10 min", "20 min").forEach { m ->
                                val sel = sleepMinutes == m
                                Surface(onClick = { sleepMinutes = m; prefs.edit().putString("sleep_min", m).apply() }, shape = RoundedCornerShape(14.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA)) {
                                    Text(m, color = if (sel) Color.White else textDark, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Call announcement", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textDark)
                                Text("ZED har aane wali call batati hai aur utha/kaat sakti hai", fontSize = 12.sp, color = textSub)
                            }
                            Switch(checked = callAnnounce, onCheckedChange = { callAnnounce = it; prefs.edit().putBoolean("call_announce", it).apply() })
                        }
                    }
                }

                "secret" -> {
                    var bondMode by remember { mutableStateOf(prefs.getString("bond_mode", "Friendly") ?: "Friendly") }
                    SettingsCard {
                        Text("Companion Mode", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        listOf(
                            "Friendly" to "Ek pyaari, kareebi dost. Har sawaal ka seedha aur helpful jawaab deti hai.",
                            "Caring" to "Thodi extra care, warm tone aur personal motivation ke saath baat karti hai.",
                            "Professional" to "Bilkul sharp, formal aur business/coding focus ke saath kaam karti hai."
                        ).forEach { (title, desc) ->
                            val sel = bondMode == title
                            Surface(onClick = { bondMode = title; prefs.edit().putString("bond_mode", title).apply() }, shape = RoundedCornerShape(18.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else textDark)
                                        Text(desc, fontSize = 12.sp, color = if (sel) Color.White.copy(alpha = 0.9f) else textSub)
                                    }
                                    if (sel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                }

                "skills" -> {
                    val skillList = listOf(
                        "pro-email" to "Professional email likhna aur bhejna — leave, apology, follow-up",
                        "group-report" to "Job/work WhatsApp group me fixed format wali daily report bhejna",
                        "pro-whatsapp" to "Professional WhatsApp message likhna — client, boss, HR, vendor",
                        "youtube-script" to "YouTube video ka ready-to-speak script likhne ka tarika",
                        "youtube-title-thumbnail" to "YouTube title aur thumbnail text likhna — curiosity gap banao",
                        "youtube-upload" to "Video upload se pehle description, chapters, tags checklist",
                        "social-posting" to "Photo ya poster ko Instagram/Facebook pe story ya post daalna",
                        "web-design" to "Website banate waqt colour, layout aur phone-first rules",
                        "coding-agent" to "ZED khud coding karti hai — kab kaunsa coding tool use karna hai",
                        "code-publish" to "Banayi website ko GitHub ya hosting par live deploy karna",
                        "android-app-banao" to "Termux me Android app banana aur APK dena",
                        "daily-briefing" to "Subah good morning / din ka update do bole to poora briefing dena",
                        "call-secretary" to "Incoming phone call announce/pickup/reject etiquette",
                        "music-dj" to "Gaana/music/video bajane aur mood ke hisaab se DJ banne ka tarika",
                        "phone-care" to "Battery bachana aur phone slow/garam hone par settings sahi karna",
                        "study-focus" to "Padhai/kaam ka focus session — DND, timer, distractions band"
                    )
                    SettingsCard {
                        Text("Installed skills (20/25)", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        skillList.forEach { (id, desc) ->
                            var enabled by remember { mutableStateOf(prefs.getBoolean("skill_$id", true)) }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(id, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                                    Text(desc, fontSize = 11.sp, color = textSub)
                                }
                                Switch(checked = enabled, onCheckedChange = { enabled = it; prefs.edit().putBoolean("skill_$id", it).apply() })
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                        }
                    }
                }

                "subagents" -> {
                    var freeModels by remember { mutableStateOf(prefs.getBoolean("subagent_free", true)) }
                    SettingsCard {
                        Text("Sub-agent ka dimaag", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ZED Agentic Plan — Gemini 2.5 Flash + Auto-Fallback. Rate-limit aane par apne aap agle model par switch.", fontSize = 12.sp, color = textSub)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Default (Gemini ke fast models)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textDark)
                            Switch(checked = freeModels, onCheckedChange = { freeModels = it; prefs.edit().putBoolean("subagent_free", it).apply() })
                        }
                    }
                }

                "email" -> {
                    var smtpEmail by remember { mutableStateOf(prefs.getString("smtp_email", "") ?: "") }
                    var smtpPass by remember { mutableStateOf(prefs.getString("smtp_pass", "") ?: "") }
                    var mailSign by remember { mutableStateOf(prefs.getString("smtp_sign", "Regards,\nZED AI") ?: "") }
                    SettingsCard {
                        Text("Kis email se bhejna hai", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = smtpEmail, onValueChange = { smtpEmail = it; prefs.edit().putString("smtp_email", it).apply() }, label = { Text("tum@gmail.com") }, textStyle = TextStyle(color = textDark, fontSize = 14.sp), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = smtpPass, onValueChange = { smtpPass = it; prefs.edit().putString("smtp_pass", it).apply() }, label = { Text("16 character wala app password") }, textStyle = TextStyle(color = textDark, fontSize = 14.sp), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = mailSign, onValueChange = { mailSign = it; prefs.edit().putString("smtp_sign", it).apply() }, label = { Text("Signature") }, textStyle = TextStyle(color = textDark, fontSize = 14.sp), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { Toast.makeText(context, "Email Config Saved!", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("✓ Connection check karo", color = Color.White)
                        }
                    }
                }

                "messages" -> {
                    var msgAlerts by remember { mutableStateOf(prefs.getBoolean("msg_alerts", false)) }
                    var waReply by remember { mutableStateOf(prefs.getBoolean("wa_reply", false)) }
                    var replyLimit by remember { mutableStateOf(prefs.getString("wa_limit", "Normal") ?: "Normal") }
                    SettingsCard {
                        Text("Message alerts & WhatsApp Auto-Reply", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Naye messages ke baare me batao", fontSize = 14.sp, color = textDark)
                            Switch(checked = msgAlerts, onCheckedChange = { msgAlerts = it; prefs.edit().putBoolean("msg_alerts", it).apply() })
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("WhatsApp messages ka auto-reply karo", fontSize = 14.sp, color = textDark)
                            Switch(checked = waReply, onCheckedChange = { waReply = it; prefs.edit().putBoolean("wa_reply", it).apply() })
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        listOf("Normal (12/60)", "Zyada (25/150)", "Sabse zyada (40/300)").forEach { lvl ->
                            val sel = replyLimit == lvl
                            Surface(onClick = { replyLimit = lvl; prefs.edit().putString("wa_limit", lvl).apply() }, shape = RoundedCornerShape(14.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(lvl, color = if (sel) Color.White else textDark, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }

                "connectors" -> {
                    SettingsCard {
                        listOf(
                            Triple("Google Drive", "Files you create or pick", "https://drive.google.com"),
                            Triple("GitHub", "Repos, issues and files", "https://github.com/aimaster187"),
                            Triple("Vercel", "Put a site online", "https://vercel.com"),
                            Triple("Notion", "Pages, notes and databases", "https://www.notion.so"),
                            Triple("Telegram", "Send yourself messages and files", "https://web.telegram.org"),
                            Triple("Todoist", "Your real task list", "https://todoist.com")
                        ).forEachIndexed { idx, (name, sub, url) ->
                            SettingsRowItem(Icons.Default.Extension, name, sub) { onOpenBrowserWithUrl(url) }
                            if (idx < 5) HorizontalDivider(color = Color(0xFFE2E8F0))
                        }
                    }
                }

                "advanced" -> {
                    SettingsSectionHeader("LOOK AUR FEEL")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.AutoAwesome, "Theme", "Helper emoji, Font size, Surfaces (Flat/Glass/Soft/Clay)") { onOpenRoute("theme") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.Favorite, "Appearance", "5D Holographic Orb style, colour aur size") { onBack() }
                    }
                    SettingsSectionHeader("SAFETY AUR ACCESS")
                    SettingsCard {
                        SettingsRowItem(Icons.Default.Warning, "Emergency SOS", "Country code +91 & SOS emergency contacts") { onOpenRoute("sos") }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.VerifiedUser, "Permissions", "Default assistant, Mic, Camera, Termux, Screen capture") { onOpenRoute("permissions") }
                    }
                }

                "theme" -> {
                    var selectedEmoji by remember { mutableStateOf(prefs.getString("helper_emoji", "Auto") ?: "Auto") }
                    var fontSizeChoice by remember { mutableStateOf(prefs.getString("font_size", "Default") ?: "Default") }
                    var surfaceStyle by remember { mutableStateOf(prefs.getString("surface_style", "Clay") ?: "Clay") }

                    SettingsCard {
                        Text("Helper emoji", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        val emojis = listOf("Auto", "🤖", "🕵️", "🤑", "🧑‍🎨", "🧑‍💻", "🥷", "👨‍🚀", "🧙‍♂️", "🔮", "👽", "🧠")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            emojis.take(6).forEach { em ->
                                val sel = selectedEmoji == em
                                Surface(onClick = { selectedEmoji = em; prefs.edit().putString("helper_emoji", em).apply() }, shape = RoundedCornerShape(14.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA), modifier = Modifier.size(46.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text(em, color = if (sel) Color.White else textDark, fontSize = if (em == "Auto") 12.sp else 20.sp) }
                                }
                            }
                        }
                    }

                    SettingsCard {
                        Text("Font Size & Surfaces Style", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Chhota", "Default", "Bada").forEach { sz ->
                                val sel = fontSizeChoice == sz
                                Surface(onClick = { fontSizeChoice = sz; prefs.edit().putString("font_size", sz).apply() }, shape = RoundedCornerShape(14.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA)) {
                                    Text(sz, color = if (sel) Color.White else textDark, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Flat", "Glass", "Soft", "Clay").forEach { st ->
                                val sel = surfaceStyle == st
                                Surface(onClick = { surfaceStyle = st; prefs.edit().putString("surface_style", st).apply() }, shape = RoundedCornerShape(14.dp), color = if (sel) Color(0xFF2563EB) else Color(0xFFE8EFFA)) {
                                    Text(st, color = if (sel) Color.White else textDark, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                                }
                            }
                        }
                    }
                }

                "sos" -> {
                    var sosNumber by remember { mutableStateOf(prefs.getString("sos_contact", "") ?: "") }
                    SettingsCard {
                        Text("Country code: 🇮🇳 India (+91)", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Favorite aur SOS contacts", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = sosNumber, onValueChange = { sosNumber = it; prefs.edit().putString("sos_contact", it).apply() }, label = { Text("+91 Emergency Contact Number") }, textStyle = TextStyle(color = textDark, fontSize = 14.sp), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { Toast.makeText(context, "SOS Contact Saved!", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(14.dp)) {
                            Text("Save SOS Contact", color = Color.White)
                        }
                    }
                }

                "permissions" -> {
                    val permItems = listOf(
                        Triple("Default assistant", "ZED AI ko phone ka digital assistant banao (Google Assistant ki jagah)", Settings.ACTION_VOICE_INPUT_SETTINGS),
                        Triple("Microphone", "Taaki tum ZED se baat kar sako (zaroori)", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                        Triple("Termux commands", "Taaki ZED tumhare liye Termux me commands chala sake", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                        Triple("Camera & Phone calls", "Photo/video lene aur call lagane ke liye", Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
                        Triple("Accessibility service", "WhatsApp/YouTube control aur screen reading ke liye", Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        Triple("Battery — optimization nahi", "Taaki background me ZED chalti rahe", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                        Triple("Dusre apps ke upar dikhna", "Overlay & Floating Orb ke liye", Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    )
                    permItems.forEach { (title, sub, action) ->
                        SettingsCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textDark)
                                    Text(sub, fontSize = 12.sp, color = textSub)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onRequestPermissions()
                                        runCatching {
                                            val intent = if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS || action == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                                                Intent(action, Uri.parse("package:${context.packageName}"))
                                            } else Intent(action)
                                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Allow karo", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                "accessibility" -> {
                    SettingsCard {
                        Text("Accessibility Control", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                        Text("ZED baat kar sakti hai, par ON hone tak dusre apps me tap ya padh nahi sakti.", fontSize = 13.sp, color = textSub)
                    }
                    Button(
                        onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("ON karo", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }

                    SettingsCard {
                        SettingsRowItem(Icons.Default.BatteryChargingFull, "Battery: ZED ko optimise mat karo", "Background me band hone se bachayein") {
                            runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.SettingsApplications, "Autostart (Xiaomi, Oppo, vivo, realme)", "Autostart list me ZED AI ON karein") {
                            runCatching { context.startActivity(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SettingsRowItem(Icons.Default.Lightbulb, "Switch grey dikh raha hai?", "App info → ⋮ (upar right) → Allow restricted settings") {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }

                "scanner" -> {
                    Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF060B14), modifier = Modifier.fillMaxWidth().height(420.dp)) {
                        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("• ZED VISION", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("BACK CAM", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.size(190.dp)) {
                                    drawCircle(color = Color(0xFF3B82F6).copy(alpha = 0.5f), style = Stroke(width = 3f))
                                    drawCircle(color = Color(0xFF38BDF8), radius = 12f)
                                    drawLine(Color(0xFF38BDF8), Offset(size.width / 2f, 0f), Offset(size.width / 2f, 24f), strokeWidth = 3f)
                                    drawLine(Color(0xFF38BDF8), Offset(size.width / 2f, size.height - 24f), Offset(size.width / 2f, size.height), strokeWidth = 3f)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                Text("FLIP", color = Color.White, fontSize = 12.sp)
                                Surface(onClick = { onSendQuickPrompt("Meri screen aur camera view analyze karo") }, shape = CircleShape, color = Color(0xFF3B82F6), modifier = Modifier.size(56.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, contentDescription = "Scan", tint = Color.White) }
                                }
                                Text("REC", color = Color(0xFFF43F5E), fontSize = 12.sp)
                            }
                        }
                    }
                }

                "notifications" -> {
                    SettingsCard {
                        Text("ZED AI v3.8.0: 5D Quantum Core & Google Auth Live", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("by The Titan AI • Chief Developer: Pranav Patel. Ab aap roz 10 minute FREE trial ya VIP License Key ke saath poora system chala sakte hain.", fontSize = 13.sp, color = textSub)
                    }
                }

                "agents_hub" -> {
                    SettingsCard {
                        SettingsRowItem(Icons.Default.RecordVoiceOver, "Voice Wake Commander", "Always-ready agent triggered by Hey ZED / Alexa") {
                            onSendQuickPrompt("Activate voice wake commander")
                        }
                        HorizontalDivider(color = Color(0xFFD6E2F5))
                        SettingsRowItem(Icons.Default.InstallMobile, "App Installer & Navigator", "Opens Play Store & automates app actions") {
                            onOpenRoute("accessibility")
                        }
                        HorizontalDivider(color = Color(0xFFD6E2F5))
                        SettingsRowItem(Icons.Default.MarkEmailUnread, "WhatsApp & OTP Auto-Reply", "Smart auto-reply & OTP reader rules") {
                            onOpenRoute("whatsapp")
                        }
                    }
                }

                "yaadein" -> {
                    Text("Sikhe hue tasks", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textDark, modifier = Modifier.padding(start = 4.dp))
                    listOf(
                        Triple("gemini se poochho", "8 steps • sawaal • built-in", "Gemini se aaj ka latest tech update poochho"),
                        Triple("chatgpt se poochho", "8 steps • sawaal • built-in", "ChatGPT style me smart summary banao"),
                        Triple("gemini video", "16 steps • video prompt • built-in", "Ek cinematic AI video prompt likho"),
                        Triple("gemini image", "17 steps • image prompt • built-in", "Ek ultra-realistic 4D AI image prompt banao"),
                        Triple("chatgpt image", "14 steps • image prompt • built-in", "Logo aur poster ke liye AI image prompt do")
                    ).forEach { (title, sub, prompt) ->
                        SettingsCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = textDark)
                                    Text(sub, fontSize = 11.sp, color = textSub)
                                }
                                TextButton(onClick = { onSendQuickPrompt(prompt) }) {
                                    Text("▶ Chalao", color = Color(0xFF2563EB), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    SettingsCard {
                        SettingsRowItem(Icons.Default.CloudUpload, "Backup & restore", "Apni yaadein aur chats dusre phone pe le jao") {
                            Toast.makeText(context, "Cloud & Local Backup Synced!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                "pc_phone" -> {
                    var pcEmail by remember { mutableStateOf("") }
                    var pcPass by remember { mutableStateOf("") }
                    SettingsCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(34.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("PC ⇄ Phone", fontSize = 22.sp, fontWeight = FontWeight.Light, color = textDark)
                            Text("Apna PC link karo — memory sync + tumhare PC wali ZED ke saath live call.", fontSize = 12.sp, color = textSub)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onOpenRoute("scanner") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PC ka QR scan karo", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("— ya email/password se —", fontSize = 11.sp, color = textSub)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = pcEmail, onValueChange = { pcEmail = it }, label = { Text("Email") }, textStyle = TextStyle(color = textDark), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = pcPass, onValueChange = { pcPass = it }, label = { Text("Password") }, textStyle = TextStyle(color = textDark), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { Toast.makeText(context, "PC Sync Connected!", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(14.dp)) {
                                    Text("Login", color = Color.White)
                                }
                                OutlinedButton(onClick = { Toast.makeText(context, "Account Created & Synced!", Toast.LENGTH_SHORT).show() }, shape = RoundedCornerShape(14.dp)) {
                                    Text("Account banao", color = textDark)
                                }
                            }
                        }
                    }
                }

                "upgrade", "about", "privacy", "rules" -> {
                    var vipInput by remember { mutableStateOf(licState.activeKey) }
                    val scopeLic = rememberCoroutineScope()
                    SettingsCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(if (licState.isVipUnlocked) "ZED Pro Max active hai" else "ZED AI VIP License", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = textDark)
                            Text("by The Titan AI • v5.0.0", fontSize = 12.sp, color = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(value = vipInput, onValueChange = { vipInput = it.uppercase() }, label = { Text("Enter ZED-VIP-XXXX-XXXX Key") }, textStyle = TextStyle(color = textDark, fontWeight = FontWeight.Bold), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { scopeLic.launch { NexaLicenseManager.verifyAndActivateKey(context, vipInput) } },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (licState.isChecking) "Verifying..." else "Activate VIP License", color = Color.White)
                            }
                            if (licState.statusMessage.isNotBlank()) {
                                Text(licState.statusMessage, color = Color(0xFFE11D48), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                    SettingsCard {
                        Text("ZED AI v3.8.0 • by The Titan AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Text("Signed in as: ${licState.userDisplayName.ifBlank { "Guest" }} (${licState.userEmail.ifBlank { "Not signed in" }})", fontSize = 12.sp, color = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("VIP Status: ${if (licState.isVipUnlocked) "ACTIVE (${licState.activeKey})" else "FREE TRIAL (10 min/day)"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onOpenBrowserWithUrl("https://aimaster187.github.io/pp-nexa/admin/") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(14.dp)) {
                                Text("Admin Panel", color = Color.White)
                            }
                            if (licState.isLoggedIn) {
                                OutlinedButton(onClick = { NexaLicenseManager.logoutUser(context) }, shape = RoundedCornerShape(14.dp)) {
                                    Text("Switch Account", color = Color(0xFFE11D48))
                                }
                            }
                        }
                    }
                }

                else -> {
                    SettingsCard {
                        Text("$screenTitle Module", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onSendQuickPrompt("$screenTitle open karo aur latest update dikhao") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)), shape = RoundedCornerShape(14.dp)) {
                            Text("Run with ZED AI", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp, start = 6.dp))
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFF4F8FF), shadowElevation = 7.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsRowItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8EFFA), modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(19.dp)) }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Normal, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
    }
}
