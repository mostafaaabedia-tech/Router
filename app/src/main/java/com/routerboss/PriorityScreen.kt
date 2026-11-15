package com.routerboss

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

/* ============ PRIORITY (QoS) ============ */
@Composable
fun Priority(c: Cfg) {
    val clip = LocalClipboardManager.current
    val nums = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    var calls by remember { mutableStateOf(40f) }
    var video by remember { mutableStateOf(35f) }
    var dl by remember { mutableStateOf(25f) }
    var profile by remember { mutableStateOf("Balanced") }
    val profiles = listOf("Balanced" to Triple(40f, 35f, 25f), "Work & calls" to Triple(60f, 25f, 15f), "Movie night" to Triple(20f, 60f, 20f))
    val d = (c.planDn.toDoubleOrNull() ?: 0.0) * 1000 * 0.85
    val u = (c.planUp.toDoubleOrNull() ?: 0.0) * 1000 * 0.85
    val sum = maxOf(1f, calls + video + dl)
    val rows = listOf(Triple("📞 Calls & work", calls, 0), Triple("🎬 Videos & social", video, 1), Triple("⬇️ Big downloads", dl, 2))
    val rank = rows.sortedByDescending { it.second }.map { it.third }
    fun prio(i: Int) = when (rank.indexOf(i)) { 0 -> "High"; 1 -> "Medium"; else -> "Low" }
    fun kb(v: Float, line: Double) = Math.round(line * v / sum)
    val text = rows.joinToString("\n") { "${it.first.drop(3)} (${prio(it.third)}): down ${kb(it.second, d)} kbps, up ${kb(it.second, u)} kbps" }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Smooth internet", "Decide who goes first, so calls never freeze")
        Spacer(Modifier.height(10.dp))
        Soft {
            Title("What is this?")
            Hint("When many people use the wifi, everything slows down. “Priority” (QoS) tells your router which things to protect first, like video calls.")
        }
        Soft {
            Title("1. Your internet plan")
            Hint("Check your bill or run a speed test. Numbers are in Mbps.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(c.planDn, { c.planDn = it; c.save() }, label = { Text("⬇ Download") }, keyboardOptions = nums, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(c.planUp, { c.planUp = it; c.save() }, label = { Text("⬆ Upload") }, keyboardOptions = nums, singleLine = true, modifier = Modifier.weight(1f))
            }
        }
        Soft {
            Title("2. Pick a style")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profiles.forEach { p ->
                    FilterChip(selected = profile == p.first, onClick = { profile = p.first; calls = p.second.first; video = p.second.second; dl = p.second.third },
                        label = { Text(p.first, fontSize = 12.sp) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Hint("Or fine-tune with the sliders:")
            Text("📞 Calls & work: ${Math.round(calls / sum * 100)}%")
            Slider(calls, { calls = it; profile = "" }, valueRange = 0f..100f)
            Text("🎬 Videos & social: ${Math.round(video / sum * 100)}%")
            Slider(video, { video = it; profile = "" }, valueRange = 0f..100f)
            Text("⬇️ Big downloads: ${Math.round(dl / sum * 100)}%")
            Slider(dl, { dl = it; profile = "" }, valueRange = 0f..100f)
        }
        Soft {
            Title("3. Type these into the router")
            rows.forEach {
                Text("${it.first}  ·  ${prio(it.third)} priority", fontWeight = FontWeight.SemiBold)
                Hint("⬇ ${kb(it.second, d)} kbps    ⬆ ${kb(it.second, u)} kbps")
                Spacer(Modifier.height(6.dp))
            }
            Hint("We keep 15% free so nothing gets stuck.")
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { clip.setText(AnnotatedString(text)) }, shape = RoundedCornerShape(14.dp)) { Text("Copy these numbers") }
            Spacer(Modifier.height(10.dp))
            Hint("How: open the Router tab, find QoS (often under Advanced), and add one rule per line above.")
        }
        Spacer(Modifier.height(12.dp))
    }
}
