package com.routerboss

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import java.net.InetAddress

suspend fun doctor(c: Cfg): List<String> = withContext(Dispatchers.IO) {
    val o = ArrayList<String>()
    val rt = try { InetAddress.getByName(c.ip).isReachable(1500) } catch (e: Exception) { false }
    o.add(if (rt) "✅ Router answers" else "❌ Router does not answer. Are you on its wifi?")
    o.add(if (internetOk()) "✅ Internet is reachable" else "❌ No internet. Restart the router, or call your provider.")
    val dns = try { InetAddress.getByName("google.com"); true } catch (e: Exception) { false }
    o.add(if (dns) "✅ Website names work (DNS)" else "⚠️ Website names fail (DNS). Try restarting the router.")
    o.add("💡 Next: open the Line tab to check your phone line.")
    o
}

suspend fun checkup(c: Cfg): List<String> = withContext(Dispatchers.IO) {
    val o = ArrayList<String>()
    fun ok(u: String, p: String) = runCatching { Router.fetch(c.ip, "/", u, p).code in 200..399 }.getOrDefault(false)
    if (ok("nope", "nope")) o.add("ℹ️ Can’t test your router password (the page opens without a login).")
    else {
        val weak = listOf("admin" to "admin", "admin" to "password", "admin" to "1234", "admin" to "").firstOrNull { ok(it.first, it.second) }
        o.add(if (weak != null) "⚠️ Your router uses a common login (${weak.first} / ${weak.second}). Change it under System Tools, Password."
        else "✅ Router login is not a common default")
    }
    val pub = runCatching { Snmp.get(c.ip, "public", listOf("1.3.6.1.2.1.1.1.0")); true }.getOrDefault(false)
    o.add(if (pub) "⚠️ Router shares stats with the word “public”. Anyone on your wifi can read them. Change the SNMP community in the router, then update it in Setup."
    else "✅ SNMP is not using “public”")
    o.add(if (Telnet.open(c.ip)) "⚠️ Telnet is open (old and not encrypted). Only share your wifi with people you trust." else "✅ Telnet is closed")
    o.add("ℹ️ Wi-Fi security and WPS can’t be checked by the app yet. Open router settings, Wireless: choose WPA2 and turn WPS off.")
    o
}

@Composable
fun TaskRow(emoji: String, title: String, sub: String, btn: String, onClick: () -> Unit) {
    Soft {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Hint(sub) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onClick, shape = RoundedCornerShape(14.dp)) { Text(btn) }
        }
    }
}

@Composable
fun Tasks(c: Cfg, go: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var head by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<String>>(emptyList()) }
    var help by remember { mutableStateOf<Pair<String, String>?>(null) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("What do you want to do?", "Pick a task. We guide you step by step.")
        Spacer(Modifier.height(10.dp))
        AlertsCard()
        TaskRow("👀", "Who is on my Wi-Fi?", "See every device and spot strangers", "Open") { go(1) }
        TaskRow("🐢", "Fix slow internet", "We run quick checks", "Check") {
            scope.launch { busy = true; head = "Slow internet check"; res = doctor(c); busy = false }
        }
        TaskRow("🛡️", "Security checkup", "Find weak spots in your router", "Check") {
            scope.launch { busy = true; head = "Security checkup"; res = checkup(c); busy = false }
        }
        if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
        if (res.isNotEmpty()) Soft {
            Title(head)
            res.forEach { Text(it, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp)) }
        }
        TaskRow("🎮", "Smoother games and calls", "Choose who goes first", "Open") { go(5) }
        TaskRow("🔑", "Change my Wi-Fi password", "Step by step", "Show") {
            help = "Change my Wi-Fi password" to "1. Tap Open router settings.\n2. In the menu find Wireless (or WLAN, or Wi-Fi).\n3. Open Security.\n4. Pick WPA2-PSK, type a new password (8 or more characters), then Save.\n\nMenu names can differ a little on your router."
        }
        TaskRow("🚫", "Block a device", "Step by step", "Show") {
            help = "Block a device" to "1. Devices tab, tap Advanced, then Read list. Copy the device’s MAC address.\n2. Tap Open router settings.\n3. Find Access Control or MAC Filter (often under Wireless or Security).\n4. Add that MAC to the block list, then Save."
        }
        Spacer(Modifier.height(12.dp))
    }
    help?.let { h ->
        AlertDialog(
            onDismissRequest = { help = null },
            confirmButton = { TextButton(onClick = { help = null; go(6) }) { Text("Open router settings") } },
            dismissButton = { TextButton(onClick = { help = null }) { Text("Close") } },
            title = { Text(h.first) },
            text = { Text(h.second) }
        )
    }
}
