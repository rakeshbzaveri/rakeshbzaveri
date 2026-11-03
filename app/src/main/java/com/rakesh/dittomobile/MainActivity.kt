package com.rakesh.dittomobile
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private lateinit var store: ClipStore
    private var resumeRefresh by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ClipStore(this)
        setContent { DittoScreen(store, resumeRefresh) }
    }

    override fun onResume() {
        super.onResume()
        try {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (cm.hasPrimaryClip()) {
                cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.let { store.add(it) }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
        // Force the Compose list to reload after the latest clipboard read.
        resumeRefresh++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DittoScreen(store: ClipStore, resumeRefresh: Int) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val clips = remember(query, refresh, resumeRefresh) { store.list(query) }
    MaterialTheme {
        Scaffold(topBar = {
            TopAppBar(title = { Column { Text("Ditto Mobile", fontWeight = FontWeight.Bold); Text("Your searchable clipboard", style = MaterialTheme.typography.labelSmall) } }, actions = {
                TextButton(onClick = { store.clearUnpinned(); refresh++ }) { Text("Clear unpinned") }
            })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search clipboard history") }, leadingIcon = { Text("⌕") })
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${clips.size} entries", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        if (!Settings.canDrawOverlays(context)) context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        else context.startForegroundService(Intent(context, ClipboardOverlayService::class.java))
                    }) { Text("Enable floating button") }
                }
                Text("Tip: tap Copy on an entry, then paste normally in your other app.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            if (cm.hasPrimaryClip()) {
                                cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { store.add(it) }
                            }
                        } catch (_: SecurityException) {
                        } catch (_: Exception) {
                        }
                        refresh++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Import current clipboard") }
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clips, key = { it.id }) { clip ->
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(clip.text, maxLines = 5, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(clip.created)), style = MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { store.pin(clip.id, !clip.pinned); refresh++ }) { Text(if (clip.pinned) "★ Pinned" else "☆ Pin") }
                                    TextButton(onClick = { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("Ditto clip", clip.text)) }) { Text("Copy") }
                                    TextButton(onClick = { store.delete(clip.id); refresh++ }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
