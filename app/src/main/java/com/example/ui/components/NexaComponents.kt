package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NexaFloatingBottomBar(
    selectedTab: NexaTab,
    isListening: Boolean,
    onSelectTab: (NexaTab) -> Unit,
    onMicClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFFF4F8FF),
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth().height(68.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockTabItem("Home", Icons.Default.Home, selectedTab == NexaTab.HOME) { onSelectTab(NexaTab.HOME) }
                DockTabItem("Scan", Icons.Default.QrCodeScanner, selectedTab == NexaTab.AGENTS) { onSelectTab(NexaTab.AGENTS) }
                Spacer(modifier = Modifier.width(62.dp))
                DockTabItem("Yaadein", Icons.Default.Psychology, selectedTab == NexaTab.CONTROL) { onSelectTab(NexaTab.CONTROL) }
                DockTabItem("Chat", Icons.AutoMirrored.Filled.Chat, selectedTab == NexaTab.TRANSCRIBE) { onSelectTab(NexaTab.TRANSCRIBE) }
            }
        }

        // Elevated 4D Center Mic Orb
        val micBrush = if (isListening) {
            Brush.radialGradient(listOf(Color(0xFFFF4D8D), Color(0xFFE11D48)))
        } else {
            Brush.radialGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB)))
        }
        Surface(
            onClick = onMicClick,
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 16.dp,
            modifier = Modifier.size(66.dp).offset(y = (-6).dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(micBrush).padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = Color(0xFFF4F8FF), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = if (isListening) Color(0xFFE11D48) else Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockTabItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val pillBg = if (selected) {
        Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6)))
    } else {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Column(
        modifier = Modifier
            .width(62.dp)
            .height(54.dp)
            .background(pillBg, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else Color(0xFF334155),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF475569)
        )
    }
}
