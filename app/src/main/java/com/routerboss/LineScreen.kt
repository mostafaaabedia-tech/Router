package com.routerboss

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

fun num(text: String, key: String): Double? =
    Regex("(?i)${key}[^\\n]*?(-?\\d+(?:\\.\\d+)?)").find(text)?.groupValues?.get(1)?.toDoubleOrNull()

fun noiseV(v: Double) = if (v >= 10) "🟢 Great" else if (v >= 6) "🟡 OK" else "🔴 Weak"
fun attV(v: Double) = if (v <= 30) "🟢 Great" else if (v <= 50) "🟡 OK" else "🔴 Weak"

@Composable
fun Line(c: Cfg, go: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    var raw by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var nm by remember { mutableStateOf<Double?>(null) }
    var at by remember { mutableStateOf<Double?>(null) }
    var ask by remember { mutableStateOf(false) }
    var cmd by remember { mutableStateOf("") }
    fun doTelnet(cmds: List<String>, parse: Boolean) {
        scope.launch {
            busy = true
            val r = withContext(Dispatchers.IO) { runCatching { Telnet.run(c.ip, c.user, c.pass, cmds) } }
            raw = r.getOrElse { "Telnet failed: ${it.message}\nTelnet may be off on your router." }
            if (parse) { nm = num(raw, "noise margin"); at = num(raw, "attenuation") }
            busy = false
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Line quality", "How healthy is your phone line?")
        Spacer(Modifier.height(10.dp))
        Soft {
            Hint("Your internet travels on your phone line. A weak line means slow or dropping internet.")
            Spacer(Modifier.height(10.dp))
            Button(enabled = !busy, shape = RoundedCornerShape(14.dp), onClick = {
                doTelnet(listOf("wan adsl status", "wan adsl linedata near", "wan adsl linedata far"), true)
            }) { Text(if (busy) "Asking router…" else "Check my line") }
            if (busy) { Spacer(Modifier.height(10.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        if (nm != null || at != null) Soft {
            Title("Your line")
            nm?.let { Text("📉 Noise margin: $it dB   ${noiseV(it)}"); Hint("Higher is better. Above 10 is great.") }
            Spacer(Modifier.height(8.dp))
            at?.let { Text("📏 Attenuation: $it dB   ${attV(it)}"); Hint("Lower is better. Under 30 is great.") }
        }
        Soft {
            Title("Restart router")
            Hint("Internet drops for about a minute. Uses the router’s Telnet. If nothing happens, restart from the router settings page.")
            Spacer(Modifier.height(10.dp))
            Button(onClick = { ask = true }, shape = RoundedCornerShape(14.dp)) { Text("Restart router") }
            TextButton(onClick = { go(6) }) { Text("Open router settings") }
        }
        Soft {
            Title("Advanced: type a command")
            OutlinedTextField(cmd, { cmd = it }, label = { Text("Telnet command") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(enabled = !busy && cmd.isNotBlank(), shape = RoundedCornerShape(14.dp), onClick = { doTelnet(listOf(cmd.trim()), false) }) { Text("Send") }
        }
        if (raw.isNotEmpty()) Soft {
            Title("Router said")
            SelectionContainer { Text(raw, fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { clip.setText(AnnotatedString(raw)) }, shape = RoundedCornerShape(14.dp)) { Text("Copy") }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        confirmButton = { TextButton(onClick = { ask = false; doTelnet(listOf("sys reboot"), false) }) { Text("Restart now") } },
        dismissButton = { TextButton(onClick = { ask = false }) { Text("Cancel") } },
        title = { Text("Restart your router?") },
        text = { Text("Everyone on your wifi loses internet for about a minute.") }
    )
}package com.routerboss

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

fun num(text: String, key: String): Double? =
    Regex("(?i)${key}[^\\n]*?(-?\\d+(?:\\.\\d+)?)").find(text)?.groupValues?.get(1)?.toDoubleOrNull()

fun noiseV(v: Double) = if (v >= 10) "🟢 Great" else if (v >= 6) "🟡 OK" else "🔴 Weak"
fun attV(v: Double) = if (v <= 30) "🟢 Great" else if (v <= 50) "🟡 OK" else "🔴 Weak"

@Composable
fun Line(c: Cfg, go: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    val clip = LocalClipboardManager.current
    var raw by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var nm by remember { mutableStateOf<Double?>(null) }
    var at by remember { mutableStateOf<Double?>(null) }
    var ask by remember { mutableStateOf(false) }
    var cmd by remember { mutableStateOf("") }
    fun doTelnet(cmds: List<String>, parse: Boolean) {
        scope.launch {
            busy = true
            val r = withContext(Dispatchers.IO) { runCatching { Telnet.run(c.ip, c.user, c.pass, cmds) } }
            raw = r.getOrElse { "Telnet failed: ${it.message}\nTelnet may be off on your router." }
            if (parse) { nm = num(raw, "noise margin"); at = num(raw, "attenuation") }
            busy = false
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Line quality", "How healthy is your phone line?")
        Spacer(Modifier.height(10.dp))
        Soft {
            Hint("Your internet travels on your phone line. A weak line means slow or dropping internet.")
            Spacer(Modifier.height(10.dp))
            Button(enabled = !busy, shape = RoundedCornerShape(14.dp), onClick = {
                doTelnet(listOf("wan adsl status", "wan adsl linedata near", "wan adsl linedata far"), true)
            }) { Text(if (busy) "Asking router…" else "Check my line") }
            if (busy) { Spacer(Modifier.height(10.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        if (nm != null || at != null) Soft {
            Title("Your line")
            nm?.let { Text("📉 Noise margin: $it dB   ${noiseV(it)}"); Hint("Higher is better. Above 10 is great.") }
            Spacer(Modifier.height(8.dp))
            at?.let { Text("📏 Attenuation: $it dB   ${attV(it)}"); Hint("Lower is better. Under 30 is great.") }
        }
        Soft {
            Title("Restart router")
            Hint("Internet drops for about a minute. Uses the router’s Telnet. If nothing happens, restart from the router settings page.")
            Spacer(Modifier.height(10.dp))
            Button(onClick = { ask = true }, shape = RoundedCornerShape(14.dp)) { Text("Restart router") }
            TextButton(onClick = { go(6) }) { Text("Open router settings") }
        }
        Soft {
            Title("Advanced: type a command")
            OutlinedTextField(cmd, { cmd = it }, label = { Text("Telnet command") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(enabled = !busy && cmd.isNotBlank(), shape = RoundedCornerShape(14.dp), onClick = { doTelnet(listOf(cmd.trim()), false) }) { Text("Send") }
        }
        if (raw.isNotEmpty()) Soft {
            Title("Router said")
            SelectionContainer { Text(raw, fontSize = 12.sp) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { clip.setText(AnnotatedString(raw)) }, shape = RoundedCornerShape(14.dp)) { Text("Copy") }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        confirmButton = { TextButton(onClick = { ask = false; doTelnet(listOf("sys reboot"), false) }) { Text("Restart now") } },
        dismissButton = { TextButton(onClick = { ask = false }) { Text("Cancel") } },
        title = { Text("Restart your router?") },
        text = { Text("Everyone on your wifi loses internet for about a minute.") }
    )
}
