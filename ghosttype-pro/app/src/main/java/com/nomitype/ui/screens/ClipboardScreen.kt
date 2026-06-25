package com.nomitype.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nomitype.data.db.AppDatabase
import com.nomitype.data.db.ClipboardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val Orange = Color(0xFFFF8C00)

@Composable
fun ClipboardScreen() {
    val ctx   = LocalContext.current
    val dao   = remember { AppDatabase.get(ctx).clipboardDao() }
    val items by dao.all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var query        by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Clipboard Manager",
                    color = Orange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Clip", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // ── Search ─────────────────────────────────────────
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search clips") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Orange,
                    unfocusedBorderColor = Color(0xFF444444),
                    cursorColor = Orange,
                    focusedLabelColor = Orange
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // ── List ───────────────────────────────────────────
            val filtered = items.filter { query.isBlank() || it.text.contains(query, ignoreCase = true) }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📋", fontSize = 40.sp)
                        Text(
                            if (query.isNotBlank()) "No clips match your search"
                            else "No clips yet — tap Add Clip to save one",
                            color = Color(0xFF888888),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { c ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(c.text, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                                    if (c.pinned) {
                                        Text("📌 Pinned", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.update(c.copy(pinned = !c.pinned)) } }) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = "Pin",
                                        tint = if (c.pinned) Orange else Color(0xFF888888)
                                    )
                                }
                                IconButton(onClick = {
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    com.nomitype.utils.ClipboardWatcher.suppressNext(c.text)
                                    cm.setPrimaryClip(ClipData.newPlainText("clip", c.text))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Orange)
                                }
                                IconButton(onClick = { scope.launch(Dispatchers.IO) { dao.delete(c) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6A6A))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add Clip Dialog ────────────────────────────────────────
    if (showAddDialog) {
        AddClipDialog(
            onDismiss = { showAddDialog = false },
            onSave = { text ->
                scope.launch(Dispatchers.IO) {
                    dao.insert(ClipboardItem(text = text.trim()))
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddClipDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val isValid = text.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📋", fontSize = 22.sp)
                Text(
                    "Add New Clip",
                    color = Orange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Type or paste any text to save it to your clipboard history.",
                    color = Color(0xFF999999),
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Enter text here…", color = Color(0xFF666666)) },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Orange,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                if (text.isNotEmpty()) {
                    Text(
                        "${text.length} characters",
                        color = Color(0xFF666666),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onSave(text) },
                enabled = isValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF444444),
                    disabledContentColor = Color(0xFF888888)
                )
            ) {
                Text("Save Clip", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF888888))
            }
        }
    )
}
