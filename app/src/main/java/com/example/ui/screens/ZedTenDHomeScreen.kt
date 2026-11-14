package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalConfiguration

private val ZBg = Color(0xFFF5F7FC)
private val ZBlue = Color(0xFF246BFD)
private val ZInk = Color(0xFF17243B)
private val ZMuted = Color(0xFF77849A)
private val ZLine = Color(0xFFE5EAF3)

@Composable
fun ZedTenDHomeScreen(
    userName: String,
    onFeatureClick: (String) -> Unit,
    onVoiceClick: () -> Unit
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 760

    val features = listOf(
        Triple("🤖", "AI Agents", "agents"),
        Triple("💬", "AI Chat", "memory"),
        Triple("📱", "Phone Control", "phone"),
        Triple("🌐", "AI Browser", "browser"),
        Triple("🧠", "Neural Memory", "memory"),
        Triple("🎙️", "Voice Studio", "voice"),
        Triple("📷", "AI Scanner", "scanner"),
        Triple("⚡", "Automations", "automations"),
        Triple("🔐", "API Vault", "vault"),
        Triple("👑", "VIP & License", "upgrade")
    )

    Row(
        Modifier.fillMaxSize().background(ZBg)
    ) {
        if (wide) {
            Column(
                Modifier.width(245.dp).fillMaxHeight()
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Text("✦ ZED AI", fontSize = 25.sp,
                    fontWeight = FontWeight.Black, color = ZBlue)
                Text("10D INTELLIGENCE", color = ZMuted, fontSize = 10.sp)
                Spacer(Modifier.height(35.dp))
                listOf(
                    "⌂  Command Center" to "home",
                    "🤖  AI Agents" to "agents",
                    "💬  AI Chat" to "memory",
                    "📱  Phone Control" to "phone",
                    "🌐  Browser" to "browser",
                    "🧠  Memory" to "memory",
                    "⚡  Automations" to "automations",
                    "🔐  API Vault" to "vault",
                    "⚙  Settings" to "settings"
                ).forEach { item ->
                    Surface(
                        onClick = { if (item.second != "home") onFeatureClick(item.second) },
                        color = if (item.second == "home") Color(0xFFEAF1FF) else Color.Transparent,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(item.first, Modifier.padding(14.dp),
                            color = if (item.second == "home") ZBlue else ZInk,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("10D NEURAL ENGINE  •  READY",
                    color = Color(0xFF159B72), fontSize = 10.sp)
            }
        }

        Column(
            Modifier.weight(1f).fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(if (wide) 30.dp else 17.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ZED AI  /  10D WORKSPACE",
                        color = ZBlue, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold)
                    Text("Good to see you, $userName",
                        color = ZInk, fontSize = if (wide) 29.sp else 23.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("Your intelligent operating space",
                        color = ZMuted, fontSize = 13.sp)
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, ZLine)
                ) {
                    Text("✦", Modifier.padding(13.dp),
                        color = ZBlue, fontSize = 22.sp)
                }
            }

            Spacer(Modifier.height(22.dp))

            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF101E42), Color(0xFF174A9C), Color(0xFF287BDB))
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Text("✧  ZED 10D NEURAL CORE",
                        color = Color(0xFF9EDBFF),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(13.dp))
                    Text("Intelligence without boundaries.",
                        color = Color.White,
                        fontSize = if (wide) 27.sp else 22.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("Think • Create • Execute • Automate",
                        color = Color(0xFFD5E7FF), fontSize = 12.sp)
                    Spacer(Modifier.height(19.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0x332FFFFFF),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("●  NEURAL SYSTEM READY",
                                Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = onVoiceClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = ZBlue
                            )) {
                            Text("🎙  Activate AI")
                        }
                    }
                }
            }

            Spacer(Modifier.height(25.dp))
            Text("YOUR AI COMMAND CENTER",
                color = ZInk, fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold)
            Text("Everything connected in one intelligent workspace",
                color = ZMuted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (wide) 205.dp else 145.dp),
                modifier = Modifier.fillMaxWidth().height(
                    if (wide) 520.dp else 620.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(features.size) { index ->
                    val feature = features[index]
                    Surface(
                        onClick = { onFeatureClick(feature.third) },
                        color = Color.White,
                        shape = RoundedCornerShape(19.dp),
                        border = BorderStroke(1.dp, ZLine),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().height(112.dp)
                    ) {
                        Column(Modifier.padding(15.dp),
                            verticalArrangement = Arrangement.Center) {
                            Text(feature.first, fontSize = 25.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(feature.second, color = ZInk,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Open workspace  →",
                                color = ZBlue, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("10D EXPERIENCE", color = ZInk,
                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ZLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("◈  MULTI-AGENT INTELLIGENCE",
                        color = ZBlue, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text("Coordinate up to 5 AI agents, delegate tasks and monitor execution from one workspace.",
                        color = ZMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onFeatureClick("agents") }) {
                        Text("Open Agent Workspace")
                    }
                }
            }
            Spacer(Modifier.height(25.dp))
        }
    }
}
