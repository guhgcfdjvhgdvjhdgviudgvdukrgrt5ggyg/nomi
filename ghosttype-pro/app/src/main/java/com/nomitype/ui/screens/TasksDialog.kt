package com.nomitype.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nomitype.security.Obf
import com.nomitype.security.ObfConstants
import com.nomitype.utils.SettingsStore

private val Orange      = Color(0xFFFF8C00)
private val GreenWa     = Color(0xFF25D366)
private val GreenWaDark = Color(0xFF128C7E)
private val PinkIg      = Color(0xFFE4405F)
private val PinkIgDark  = Color(0xFFC13584)

@Composable
fun TasksDialog(onUnlocked: () -> Unit) {
    val ctx   = LocalContext.current
    val prefs = SettingsStore.prefs(ctx)

    val waUrl   = remember { Obf.decode(ctx, ObfConstants.WA_CHANNEL_URL) }
    val instaUrl = remember { Obf.decode(ctx, ObfConstants.INSTAGRAM_URL) }

    var waJoined  by remember { mutableStateOf(false) }
    var igJoined  by remember { mutableStateOf(false) }

    val bothDone = waJoined && igJoined

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Hero icon ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Orange.copy(alpha = 0.25f), Orange.copy(alpha = 0.08f))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.verticalGradient(listOf(Orange.copy(alpha = 0.6f), Orange.copy(alpha = 0.2f))),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 36.sp)
                }

                // ── Title ──────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Unlock NomiType Pro",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Complete 2 quick tasks to get free access",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                // ── WhatsApp Task Card ─────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(GreenWa.copy(alpha = if (waJoined) 0.18f else 0.09f),
                                       GreenWaDark.copy(alpha = if (waJoined) 0.12f else 0.05f))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(GreenWa.copy(alpha = if (waJoined) 0.7f else 0.35f),
                                       GreenWaDark.copy(alpha = if (waJoined) 0.4f else 0.2f))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(GreenWa.copy(alpha = 0.30f), GreenWaDark.copy(alpha = 0.20f))
                                    )
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(listOf(GreenWa.copy(alpha = 0.7f), GreenWaDark.copy(alpha = 0.4f))),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (waJoined) "✅" else "📢", fontSize = 26.sp)
                        }

                        // Text
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Follow on WhatsApp",
                                color = GreenWa,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Join NomiType Pro official channel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            AnimatedVisibility(visible = waJoined) {
                                Text(
                                    "✓ Joined successfully",
                                    color = GreenWa,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Button
                        AnimatedContent(targetState = waJoined, label = "wa_btn") { joined ->
                            if (joined) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(GreenWa.copy(alpha = 0.15f))
                                        .border(1.dp, GreenWa.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Done ✓", color = GreenWa, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        try {
                                            ctx.startActivity(
                                                Intent(Intent.ACTION_VIEW,
                                                    Uri.parse(waUrl))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (_: Exception) {}
                                        waJoined = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GreenWa,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Join", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // ── Instagram Task Card ─────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(PinkIg.copy(alpha = if (igJoined) 0.18f else 0.09f),
                                       PinkIgDark.copy(alpha = if (igJoined) 0.12f else 0.05f))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(PinkIg.copy(alpha = if (igJoined) 0.7f else 0.35f),
                                       PinkIgDark.copy(alpha = if (igJoined) 0.4f else 0.2f))
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(PinkIg.copy(alpha = 0.30f), PinkIgDark.copy(alpha = 0.20f))
                                    )
                                )
                                .border(
                                    1.dp,
                                    Brush.verticalGradient(listOf(PinkIg.copy(alpha = 0.7f), PinkIgDark.copy(alpha = 0.4f))),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (igJoined) "✅" else "📸", fontSize = 26.sp)
                        }

                        // Text
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Follow on Instagram",
                                color = PinkIg,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "@syed_nomi_kashmiri",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            AnimatedVisibility(visible = igJoined) {
                                Text(
                                    "✓ Followed successfully",
                                    color = PinkIg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Button
                        AnimatedContent(targetState = igJoined, label = "ig_btn") { joined ->
                            if (joined) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PinkIg.copy(alpha = 0.15f))
                                        .border(1.dp, PinkIg.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Done ✓", color = PinkIg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        try {
                                            ctx.startActivity(
                                                Intent(Intent.ACTION_VIEW,
                                                    Uri.parse(instaUrl))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (_: Exception) {}
                                        igJoined = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PinkIg,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Follow", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                // ── Unlock button ──────────────────────────────
                AnimatedContent(targetState = bothDone, label = "unlock_btn") { ready ->
                    Button(
                        onClick = {
                            if (ready) {
                                prefs.edit().putBoolean(SettingsStore.KEY_TASKS_UNLOCKED, true).apply()
                                onUnlocked()
                            }
                        },
                        enabled = ready,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange,
                            contentColor = Color.Black,
                            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text(
                            if (ready) "🔓  Unlock NomiType Pro" else "Complete both tasks to unlock",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }

                AnimatedVisibility(visible = !bothDone) {
                    Text(
                        when {
                            !waJoined && !igJoined -> "Join WhatsApp and follow on Instagram to unlock"
                            !waJoined -> "Join WhatsApp channel to unlock"
                            else -> "Follow on Instagram to unlock"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
