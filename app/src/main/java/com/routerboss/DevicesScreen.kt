package com.routerboss

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress

/* ============ DEVICES ============ */
@Composable
fun Devices(c: Cfg) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val me = remember { myIp(ctx) }
    var found by remember { mutableStateOf<List<String>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }
    var editIp by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) }
    var adv by remember { mutableStateOf(false) }
    var path by remember { mutableStateOf("/basic/home_dhcp.htm") }
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var msg by remember { mutableStateOf("") }
    @Suppress("UNUSED_VARIABLE") val t = tick

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Who’s on my wifi", "Find every phone and laptop, and spot strangers",
            if (scanned) "${found.size} devices online" else null)
        Spacer(Modifier.height(10.dp))
        Soft {
            Hint("Tap scan, then name your own devices. Anything still called “Unknown” might be a neighbour.")
            Spacer(Modifier.height(12.dp))
            Button(enabled = !scanning, shape = RoundedCornerShape(14.dp), onClick = {
                scope.launch {
                    scanning = true
                    val base = c.ip.substringBeforeLast(".")
                    val sem = Semaphore(40)
                    found = withContext(Dispatchers.IO) {
                        (1..254).map { n ->
                            async {
                                sem.withPermit {
                                    val h = "$base.$n"
                                    if (try { InetAddress.getByName(h).isReachable(500) } catch (e: Exception) { false }) h else null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    c.online = found.size
                    scanned = true; scanning = false
                }
            }) { Text(if (scanning) "Scanning… about 10 seconds" else if (scanned) "Scan again" else "Scan my wifi") }
            if (scanning) { Spacer(Modifier.height(10.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        found.forEach { ip ->
            val n = c.name(ip)
            val role = when (ip) { c.ip -> "Your router"; me -> "This phone"; else -> null }
            Soft {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(if (ip == c.ip) "📡" else "📱", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(role ?: n.ifEmpty { "Unknown device" }, fontWeight = FontWeight.SemiBold)
                        Hint(ip)
                        if (role == null && n.isEmpty()) Hint("Not named yet. Is this yours?", MaterialTheme.colorScheme.tertiary)
                    }
                    if (role == null) TextButton(onClick = { editIp = ip; editName = n }) { Text(if (n.isEmpty()) "Name" else "Rename") }
                }
            }
        }
        TextButton(onClick = { adv = !adv }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(if (adv) "Hide advanced" else "Advanced: read the router’s own list") }
        if (adv) Soft {
            Hint("Reads device names and MAC addresses from your router’s web page.")
            OutlinedTextField(path, { path = it }, label = { Text("Page address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(shape = RoundedCornerShape(14.dp), onClick = {
                scope.launch {
                    msg = "Reading…"
                    val r = withContext(Dispatchers.IO) { runCatching { Router.fetch(c.ip, path, c.user, c.pass) } }
                    r.onSuccess { clients = Router.parseClients(it.body); msg = "Router answered (${it.code}). ${clients.size} devices found." }
                        .onFailure { msg = "Couldn’t read it: ${it.message}" }
                }
            }) { Text("Read list") }
            Hint(msg)
            clients.forEach { Text("${it.ip}   ${it.mac}", fontSize = 13.sp) }
        }
        Spacer(Modifier.height(12.dp))
    }
    editIp?.let { ip ->
        AlertDialog(
            onDismissRequest = { editIp = null },
            confirmButton = { TextButton(onClick = { c.setName(ip, editName.trim()); editIp = null; tick++ }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editIp = null }) { Text("Cancel") } },
            title = { Text("Who is this?") },
            text = { OutlinedTextField(editName, { editName = it }, label = { Text("e.g. Mom’s phone") }, singleLine = true) }
        )
    }
}
