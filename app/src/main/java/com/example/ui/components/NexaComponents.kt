package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NexaTab {
    HOME, AGENTS, CONTROL, TRANSCRIBE, CHAT
}

@Composable
fun NexaTechAtmosphereBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Clay5DBaseTop, Clay5DBaseMid, Clay5DBaseBot))
        )
    )
}

@Composable
fun NexaGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF2563EB),
    content: @Composable ColumnScope.() -> Unit
) {
    Clay5DSurface(
        modifier = modifier,
        cornerRadius = 26.dp,
        elevation = 14.dp,
        glowColor = accentColor,
        content = content
    )
}

@Composable
fun NexaTopBar(
    title: String,
    showDrawerButton: Boolean = true,
    onOpenDrawer: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTranscribe: () -> Unit = {},
    onOpenApiKeys: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onToggleWakeWord: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAgents: () -> Unit = {},
    onOpenBrowser: () -> Unit = {},
    onOpenUpgrade: () -> Unit = {},
    wakeActive: Boolean = false,
    hasAlertBadge: Boolean = false
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showDrawerButton) {
                Clay5DOrbButton(onClick = onOpenDrawer, size = 44.dp) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF0F172A), modifier = Modifier.size(21.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Clay5DOrbButton(onClick = onOpenNotifications, size = 44.dp) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = if (hasAlertBadge) Color(0xFF2563EB) else Color(0xFF0F172A),
                    modifier = Modifier.size(21.dp)
                )
            }

            Box {
                Clay5DOrbButton(onClick = { menuExpanded = true }, size = 44.dp) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color(0xFF0F172A), modifier = Modifier.size(21.dp))
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Clay5DCardTop, Clay5DCardBot)), RoundedCornerShape(24.dp))
                        .width(265.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings & Advanced Hub", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onOpenSettings() }
                    )
                    DropdownMenuItem(
                        text = { Text("Personal & Gemini API Key", color = Color(0xFF0F172A)) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onOpenApiKeys() }
                    )
                    DropdownMenuItem(
                        text = { Text("Agents & Auto-Reply", color = Color(0xFF0F172A)) },
                        leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onOpenAgents() }
                    )
                    DropdownMenuItem(
                        text = { Text("Transcribe Audio (AI)", color = Color(0xFF0F172A)) },
                        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onOpenTranscribe() }
                    )
                    DropdownMenuItem(
                        text = { Text("Permissions & Phone Control", color = Color(0xFF0F172A)) },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onOpenPermissions() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (wakeActive) "Stop Wake-Word Core" else "Start Wake-Word Core", color = Color(0xFF0F172A)) },
                        leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onToggleWakeWord() }
                    )
                    DropdownMenuItem(
                        text = { Text("ZED AI Web Browser", color = Color(0xFF0F172A)) },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onOpenBrowser() }
                    )
                    DropdownMenuItem(
                        text = { Text("VIP License & Upgrade", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF2563EB)) },
                        onClick = { menuExpanded = false; onOpenUpgrade() }
                    )
                    HorizontalDivider(color = Color(0xFFD6E2F5))
                    DropdownMenuItem(
                        text = { Text("Sign Out / Reset Account", color = Color(0xFFE11D48), fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFE11D48)) },
                        onClick = { menuExpanded = false; onSignOut() }
                    )
                }
            }
        }
    }
}

@Composable
fun NexaFloatingBottomBar(
    selectedTab: NexaTab,
    isListening: Boolean,
    onSelectTab: (NexaTab) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = Color.Transparent,
            shadowElevation = 18.dp,
            border = BorderStroke(1.4.dp, Brush.linearGradient(listOf(Color.White, Color(0xFF93C5FD)))),
            modifier = Modifier.fillMaxWidth().height(74.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Clay5DCardTop, Clay5DCardBot)))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockTab5D("Home", Icons.Default.Home, selectedTab == NexaTab.HOME) { onSelectTab(NexaTab.HOME) }
                DockTab5D("Scan", Icons.Default.QrCodeScanner, selectedTab == NexaTab.CONTROL) { onSelectTab(NexaTab.CONTROL) }
                Spacer(modifier = Modifier.width(64.dp))
                DockTab5D("Yaadein", Icons.Default.Psychology, selectedTab == NexaTab.AGENTS) { onSelectTab(NexaTab.AGENTS) }
                DockTab5D("Chat", Icons.Default.ChatBubbleOutline, selectedTab == NexaTab.CHAT) { onSelectTab(NexaTab.CHAT) }
            }
        }

        Surface(
            onClick = onMicClick,
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 18.dp,
            border = BorderStroke(3.dp, if (isListening) Color(0xFFE11D48) else Color(0xFF2563EB)),
            modifier = Modifier.size(66.dp).offset(y = (-8).dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color.White, Color(0xFFDBEAFE)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice Mic",
                    tint = if (isListening) Color(0xFFE11D48) else Color(0xFF1E3A8A),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun DockTab5D(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) Brush.verticalGradient(listOf(Color(0xFF4F94FF), Color(0xFF2563EB)))
                else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Color.White else Color(0xFF475569), modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) Color.White else Color(0xFF475569))
    }
}
