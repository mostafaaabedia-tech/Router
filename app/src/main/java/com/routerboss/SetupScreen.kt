package com.routerboss

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

/* ============ SETUP ============ */
data class IfInfo(val n: Int, val name: String, val up: Boolean)

suspend fun readIfs(c: Cfg): Result<List<IfInfo>> = withContext(Dispatchers.IO) {
    runCatching {
        val o = mutableListOf<String>()
        for (i in 1..12) { o.add("1.3.6.1.2.1.2.2.1.2.$i"); o.add("1.3.6.1.2.1.2.2.1.8.$i") }
        val r = Snmp.get(c.ip, c.comm, o)
        val list = ArrayList<IfInfo>()
        for (i in 1..12) {
            val d = r["1.3.6.1.2.1.2.2.1.2.$i"]
            if (d != null && !d.missing) list.add(IfInfo(i, d.text(), r["1.3.6.1.2.1.2.2.1.8.$i"]?.long() == 1L))
        }
        list as List<IfInfo>
    }
}

@Composable
fun Setup(c: Cfg, go: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    var steps by remember { mutableStateOf<List<String>>(emptyList()) }
    var ifs by remember { mutableStateOf<List<IfInfo>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var adv by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Connect your router", "Takes about 20 seconds")
        Spacer(Modifier.height(10.dp))
        Soft {
            Title("1. Join your router’s wifi")
            Hint("Your phone must be on the same wifi as the router. We found this address automatically. You can change it.")
            OutlinedTextField(c.ip, { c.ip = it.trim(); c.save() }, label = { Text("Router address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { gateway(ctx)?.let { c.ip = it; c.save() } }) { Text("Find it again") }
        }
        Soft {
            Title("2. Router login")
            Hint("Usually admin and admin. It’s on the sticker under the router.")
            OutlinedTextField(c.user, { c.user = it; c.save() }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(c.pass, { c.pass = it; c.save() }, label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Button(enabled = !busy, shape = RoundedCornerShape(14.dp), onClick = {
                scope.launch {
                    busy = true; steps = emptyList(); ifs = emptyList()
                    val s = ArrayList<String>()
                    val r = withContext(Dispatchers.IO) { runCatching { Router.fetch(c.ip, "/", c.user, c.pass) } }.getOrNull()
                    var reached = false
                    if (r == null) s.add("❌ Can’t reach ${c.ip}. Are you on the router’s wifi?")
                    else if (r.code == 401 || r.code == 403) { s.add("🔑 Router found, but the username or password is wrong."); reached = true }
                    else { s.add("✅ Router found and login works"); reached = true }
                    steps = s.toList()
                    if (reached) {
                        val res = readIfs(c)
                        res.onSuccess { l ->
                            ifs = l
                            s.add("✅ Live speed is available")
                            val guess = l.firstOrNull { Regex("(?i)ppp|adsl|atm|wan|dsl").containsMatchIn(it.name) }
                            if (guess != null) { c.ifx = guess.n.toString() }
                        }.onFailure { s.add("⚠️ Live speed is off. Turn on SNMP in your router (look under Advanced or Management), then try again.") }
                        c.setupDone = true; c.save()
                    }
                    steps = s.toList()
                    busy = false
                }
            }) { Text(if (busy) "Checking…" else "Connect") }
            if (busy) { Spacer(Modifier.height(10.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            steps.forEach { Spacer(Modifier.height(6.dp)); Text(it, fontSize = 14.sp) }
            if (steps.any { it.startsWith("✅ Router") }) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { go(0) }) { Text("Go to Home") }
            }
        }
        if (ifs.isNotEmpty()) Soft {
            Title("3. Which connection is your internet?")
            Hint("We picked the most likely one. If the speed on Home looks wrong, try another.")
            ifs.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = c.ifx == f.n.toString(), onClick = { c.ifx = f.n.toString(); c.save() })
                    Text("${f.name}  ${if (f.up) "🟢" else "⚪"}", fontSize = 14.sp)
                }
            }
        }
        TextButton(onClick = { adv = !adv }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(if (adv) "Hide advanced" else "Advanced") }
        if (adv) Soft {
            OutlinedTextField(c.comm, { c.comm = it.trim(); c.save() }, label = { Text("SNMP community (usually public)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(shape = RoundedCornerShape(14.dp), onClick = {
                scope.launch {
                    val sb = StringBuilder()
                    for (p in Router.PATHS) {
                        val r = withContext(Dispatchers.IO) { runCatching { Router.fetch(c.ip, p, c.user, c.pass) } }
                        sb.append(p).append(" -> ").append(r.fold({ "${it.code}, ${it.body.length} chars" }, { "error ${it.message}" })).append("\n")
                        log = sb.toString()
                    }
                }
            }) { Text("Probe router pages") }
            OutlinedButton(onClick = { clip.setText(AnnotatedString(log)) }, shape = RoundedCornerShape(14.dp)) { Text("Copy log") }
            SelectionContainer { Text(log, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(12.dp))
    }
}
