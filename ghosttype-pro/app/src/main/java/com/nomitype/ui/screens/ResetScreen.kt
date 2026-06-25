package com.nomitype.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomitype.ime.AutoTypeEngine
import com.nomitype.ime.AutoTypeForegroundService
import com.nomitype.ime.FloatingPointerService
import com.nomitype.utils.SettingsStore

private val RedReset = Color(0xFFFF3B30)
private val Orange   = Color(0xFFFF8C00)

@Composable
fun ResetScreen() {
    val ctx  = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var resetDone  by remember { mutableStateOf(false) }

    // Confirmation dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(18.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(RedReset.copy(alpha = 0.12f))
                        .border(1.5.dp, RedReset.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("⚠️", fontSize = 28.sp) }
            },
            title = {
                Text(
                    "Reset All Settings?",
                    color = RedReset,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "This will factory reset the entire app:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedReset.copy(alpha = 0.07f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "⌨️ Keyboard size, font, haptic, sound",
                            "🤖 Auto-Type settings & target name",
                            "🎯 Pointer position & settings",
                            "👤 Plans, name, active plan",
                            "🔧 All other app preferences"
                        ).forEach { item ->
                            Text(item, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                    Text(
                        "This action cannot be undone!",
                        color = RedReset,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try { AutoTypeEngine.stop() } catch (_: Throwable) {}
                        try { AutoTypeForegroundService.stop(ctx) } catch (_: Throwable) {}
                        try { FloatingPointerService.stop(ctx) } catch (_: Throwable) {}
                        SettingsStore.resetAll(ctx)
                        showDialog = false
                        resetDone  = true
                    },
                    shape  = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedReset,
                        contentColor   = Color.White
                    )
                ) { Text("Yes, Reset Now", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Reset Settings", color = RedReset, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(
            "Restore the app to its original factory defaults. All your customisations will be erased.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        // What will be reset list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("What will be reset", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            listOf(
                "⌨️" to "Keyboard size, font, haptic, sound",
                "🤖" to "Auto-Type settings & target name",
                "🎯" to "Pointer position & settings",
                "👤" to "Plans, name, active plan",
                "🔧" to "All other app preferences"
            ).forEach { (emoji, label) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        }

        // Success banner
        if (resetDone) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "✅ App has been reset! Re-open the keyboard — default settings will be applied.",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }

        // Reset button
        Button(
            onClick = { showDialog = true },
            enabled = !resetDone,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RedReset,
                contentColor = Color.White,
                disabledContainerColor = RedReset.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("🔄  Reset All Settings", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(RedReset.copy(alpha = 0.07f))
                .padding(12.dp)
        ) {
            Text(
                "⚠  All data will be permanently deleted. This cannot be undone.",
                color = RedReset.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
