package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ZedNavigationDrawerContent(
    activeRoute: String,
    userEmail: String = "",
    onSelectRoute: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val scroll = rememberScrollState()
    val frostBg = Brush.verticalGradient(colors = listOf(Color(0xFFEAF1FC), Color(0xFFDCE7F9)))

    ModalDrawerSheet(
        drawerContainerColor = Color(0xFFE5EEFC),
        modifier = Modifier.width(305.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().background(frostBg).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF090E1A), shadowElevation = 6.dp, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("ZED AI", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    Text(if (userEmail.isNotBlank()) userEmail else "by The Titan AI", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DrawerSectionLabel("HOME")
                DrawerPillButton("Home", Icons.Default.Home, activeRoute == "home") { onSelectRoute("home"); onCloseDrawer() }
                DrawerPillButton("ZED Home", Icons.Default.Home, activeRoute == "zed_home") { onSelectRoute("zed_home"); onCloseDrawer() }

                DrawerSectionLabel("KAAM-KAAJ")
                DrawerPillButton("Markets", Icons.AutoMirrored.Filled.ShowChart, activeRoute == "markets") { onSelectRoute("markets"); onCloseDrawer() }
                DrawerPillButton("Marketing", Icons.Default.Storefront, activeRoute == "marketing") { onSelectRoute("marketing"); onCloseDrawer() }
                DrawerPillButton("Documents", Icons.Default.Description, activeRoute == "documents") { onSelectRoute("documents"); onCloseDrawer() }
                DrawerPillButton("Meri photos", Icons.Default.PhotoLibrary, activeRoute == "photos") { onSelectRoute("photos"); onCloseDrawer() }
                DrawerPillButton("Website / Coding", Icons.Default.Code, activeRoute == "coding") { onSelectRoute("coding"); onCloseDrawer() }
                DrawerPillButton("Terminal", Icons.Default.Terminal, activeRoute == "terminal") { onSelectRoute("terminal"); onCloseDrawer() }
                DrawerPillButton("Browser", Icons.Default.Language, activeRoute == "browser") { onSelectRoute("browser"); onCloseDrawer() }
                DrawerPillButton("Accessibility", Icons.Default.AccessibilityNew, activeRoute == "accessibility") { onSelectRoute("accessibility"); onCloseDrawer() }

                DrawerSectionLabel("SYSTEM")
                DrawerPillButton("Settings", Icons.Default.Settings, activeRoute == "settings") { onSelectRoute("settings"); onCloseDrawer() }
                DrawerPillButton("ZED ke rules", Icons.Default.VerifiedUser, activeRoute == "rules") { onSelectRoute("rules"); onCloseDrawer() }
                DrawerPillButton("PC ⇄ Phone", Icons.Default.SyncAlt, activeRoute == "pc_phone") { onSelectRoute("pc_phone"); onCloseDrawer() }

                DrawerSectionLabel("AUR")
                DrawerPillButton("Privacy Policy", Icons.Default.Lock, activeRoute == "privacy") { onSelectRoute("privacy"); onCloseDrawer() }
                DrawerPillButton("About", Icons.Default.Info, activeRoute == "about") { onSelectRoute("about"); onCloseDrawer() }
                DrawerPillButton("Upgrade", Icons.Default.Star, activeRoute == "upgrade") { onSelectRoute("upgrade"); onCloseDrawer() }
                DrawerPillButton("Notifications", Icons.Default.Notifications, activeRoute == "notifications") { onSelectRoute("notifications"); onCloseDrawer() }
            }

            Text("v5.0.0  •  The Titan AI", fontSize = 11.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        }
    }
}

@Composable
private fun DrawerSectionLabel(title: String) {
    Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 6.dp))
}

@Composable
private fun DrawerPillButton(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bgBrush = if (selected) Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB)))
    else Brush.horizontalGradient(listOf(Color(0xFFF4F8FF), Color(0xFFEFF5FF)))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = if (selected) 14.dp else 9.dp,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color.White, Color(0xFF93C5FD)))),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().background(bgBrush).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else Color(0xFF1E3A8A), modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) Color.White else Color(0xFF1E293B))
        }
    }
}
