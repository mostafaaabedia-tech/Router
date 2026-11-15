package com.routerboss

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

/* ============ HOME ============ */
@Composable
fun Home(c: Cfg, m: Mon, go: (Int) -> Unit) {
    val pd = (c.planDn.toDoubleOrNull() ?: 8.0) * 1000
    val pu = (c.planUp.toDoubleOrNull() ?: 1.0) * 1000
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header(
            if (m.ok) "Your internet, live" else "Welcome 👋",
            if (m.ok) "Updating every 2 seconds" else "Connect your router to see your speed",
            if (m.ok) "● Live" else "● Not connected"
        )
        Spacer(Modifier.height(10.dp))
        Soft {
            Title("Speed right now")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Gauge((m.down / pd).toFloat(), fmt(m.down), unit(m.down), "⬇ Download", MaterialTheme.colorScheme.primary)
                Gauge((m.up / pu).toFloat(), fmt(m.up), unit(m.up), "⬆ Upload", MaterialTheme.colorScheme.tertiary)
            }
            if (m.ok) Chart(m.dh.toList(), m.uh.toList(), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
        }
        if (!m.ok) Soft {
            Title("Not connected yet")
            Hint(m.err ?: "Waiting for your router…", MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { go(4) }, shape = RoundedCornerShape(14.dp)) { Text("Connect my router") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(Modifier.weight(1f), "📦", "${(m.totD + m.totU) / 1_000_000} MB", "Data used since you opened the app")
            Tile(Modifier.weight(1f), "📱", if (c.online < 0) "?" else "${c.online}", "Devices on your wifi (open Devices to scan)")
        }
        Soft {
            Title("💡 Quick guide")
            Hint("⬇ Download is what you get: videos, websites, apps.\n⬆ Upload is what you send: calls, photos, backups.\nMbps is speed. A bigger number means faster.")
        }
        Spacer(Modifier.height(12.dp))
    }
}
