package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.system.NexaLicenseManager
import kotlinx.coroutines.launch

@Composable
fun ZedSubRouteScreen(
    route: String,
    onBack: () -> Unit,
    onOpenBrowserUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val licState by NexaLicenseManager.state.collectAsState()
    val scroll = rememberScrollState()

    val frostBg = Brush.verticalGradient(
        colors = listOf(Color(0xFFEAF1FC), Color(0xFFDCE7F9), Color(0xFFE5EEFC))
    )
    val cardColor = Color(0xFFF4F8FF)
    val textDark = Color(0xFF1E293B)
    val textSub = Color(0xFF64748B)

    val title = when (route) {
        "upgrade" -> "License & App Update"
        "rules" -> "ZED ke Rules"
        "coding" -> "Website / Coding Lab"
        "terminal" -> "ZED Quantum Terminal"
        "pc_phone" -> "PC ⇄ Phone Bridge"
        "markets" -> "Markets Live"
        "marketing" -> "Marketing Studio"
        "documents" -> "Documents Vault"
        "photos" -> "Meri Photos"
        "privacy" -> "Privacy Policy"
        "about" -> "About ZED AI"
        else -> "ZED AI Studio"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(frostBg)
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = cardColor,
                shadowElevation = 8.dp,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textDark)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Light, color = textDark)
        }

        Spacer(modifier = Modifier.height(18.dp))

        when (route) {
            "upgrade" -> {
                Surface(shape = RoundedCornerShape(28.dp), color = cardColor, shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("ZED Pro Max active hai", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tumhara lifetime, ek-baar wala license is phone par verify ho gaya hai", fontSize = 13.sp, color = textSub)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (licState.activeKey.length >= 8) licState.activeKey.take(8) + "-••••-••••" else "ZED-VIP-••••-••••",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            letterSpacing = 2.sp
                        )
                        if (licState.isVipUnlocked) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Logout Key from this Phone",
                                color = Color(0xFFE11D48),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { scope.launch { NexaLicenseManager.logoutKey(context) } }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(28.dp), color = cardColor, shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text("App update", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = textDark, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Abhi: v3.5.0 • The Titan AI", fontSize = 13.sp, color = textSub, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(12.dp))
                        listOf(
                            "- 4D Frost-Neumorphic UI + 64-Tick Glowing Orb active hai.",
                            "- Bade gol ghere par tap karne se turant Mute/Unmute hota hai.",
                            "- Niche wale blue Mic se 1-Tap Continuous SpeechRecognizer chalta hai.",
                            "- ZED ka khud ka Chrome Browser (Gmail login support) jud gaya hai.",
                            "- 24x7 Background Service aur Hard VIP License Lock active hai."
                        ).forEach { line ->
                            Text(line, fontSize = 13.sp, color = textDark, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }
            }

            "rules" -> {
                var newRule by remember { mutableStateOf("") }
                val rulesList = remember {
                    mutableStateListOf(
                        "1. Mera naam ZED AI hai aur mujhe The Titan AI ne banaya hai.",
                        "2. The Titan AI ko Chief Developer Pranav Patel ne banaya aur design kiya hai.",
                        "3. Main hamesha Hinglish/Hindi me ek pakke dost aur smart assistant ki tarah baat karunga.",
                        "4. Background me 24x7 active reh kar har command ko turant poora karunga."
                    )
                }
                Surface(shape = RoundedCornerShape(24.dp), color = cardColor, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("ZED Core Rules & Identity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        rulesList.forEach { r ->
                            Text(r, fontSize = 13.sp, color = textDark, modifier = Modifier.padding(vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newRule,
                            onValueChange = { newRule = it },
                            label = { Text("Naya rule add karein...") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { if (newRule.isNotBlank()) { rulesList.add("${rulesList.size + 1}. $newRule"); newRule = "" } },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Rule", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "coding", "terminal" -> {
                var codeText by remember { mutableStateOf("<h1>ZED AI by The Titan AI</h1>\n<p>Engineered by Chief Developer Pranav Patel</p>") }
                Surface(shape = RoundedCornerShape(24.dp), color = cardColor, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(if (route == "terminal") "ZED Quantum Shell" else "Live HTML/Code Workspace", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = codeText,
                            onValueChange = { codeText = it },
                            minLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onOpenBrowserUrl("data:text/html," + codeText) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Run in ZED Browser", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            else -> {
                Surface(shape = RoundedCornerShape(24.dp), color = cardColor, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (route) {
                                "about" -> "ZED AI v3.5.0\nCreated by: The Titan AI\nChief Developer: Pranav Patel (pranavji628@gmail.com)\nArchitecture: 4D Frost-Neumorphic Autonomous OS"
                                "pc_phone" -> "PC ⇄ Phone Bridge Ready.\nApne PC browser me ZED Web Console khol kar same VIP Key se sync karein."
                                "markets" -> "Live Markets & Finance Dashboard ZED Browser me kholne ke liye niche button dabayein."
                                "privacy" -> "ZED AI Privacy Policy: Aapka data, VIP Key aur memories aapke private Firebase Vault me encrypted rehti hain."
                                else -> "Ye module ZED AI v3.5.0 (by The Titan AI) me 4D Frost-Neumorphic mode ke saath active hai."
                            },
                            fontSize = 14.sp,
                            color = textDark
                        )
                        if (route == "markets" || route == "marketing" || route == "documents" || route == "photos") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val target = when (route) {
                                        "markets" -> "https://www.google.com/finance"
                                        "documents" -> "https://docs.google.com"
                                        "photos" -> "https://photos.google.com"
                                        else -> "https://www.google.com"
                                    }
                                    onOpenBrowserUrl(target)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open in ZED Chrome Browser", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(90.dp))
    }
}
