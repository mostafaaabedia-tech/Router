package com.routerboss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cfg = Cfg(this)
        setContent {
            val light = lightColorScheme(
                primary = Color(0xFF0B8F84), onPrimary = Color.White, tertiary = Color(0xFFE5622A),
                background = Color(0xFFF1F6F7), surface = Color.White, surfaceVariant = Color(0xFFE2ECEE),
                onSurface = Color(0xFF0F1E22), onSurfaceVariant = Color(0xFF5A7076), outlineVariant = Color(0xFFD5E2E5),
                error = Color(0xFFD9383E)
            )
            val dark = darkColorScheme(
                primary = Color(0xFF2FD3C3), onPrimary = Color(0xFF00201D), tertiary = Color(0xFFFF9560),
                background = Color(0xFF0A1316), surface = Color(0xFF132227), surfaceVariant = Color(0xFF1E343A),
                onSurface = Color(0xFFE8F3F4), onSurfaceVariant = Color(0xFF93AAB0), outlineVariant = Color(0xFF263E45),
                error = Color(0xFFFF6B70)
            )
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) dark else light) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App(cfg) }
            }
        }
    }
}

@Composable
fun App(c: Cfg) {
    var tab by remember { mutableIntStateOf(if (c.setupDone) 0 else 4) }
    val m = remember { Mon() }
    LaunchedEffect(c.ip, c.comm, c.ifx) { m.run(c) }
    val tabs = listOf("🏠" to "Home", "📱" to "Devices", "✅" to "Tasks", "📶" to "Line", "🔧" to "Setup")
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEachIndexed { i, t ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i },
                    icon = { Text(t.first, fontSize = 20.sp) }, label = { Text(t.second, fontSize = 11.sp) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> Home(c, m) { tab = it }
                1 -> Devices(c)
                2 -> Tasks(c) { tab = it }
                3 -> Line(c) { tab = it }
                4 -> Setup(c) { tab = it }
                5 -> Priority(c)
                else -> Web(c)
            }
        }
    }
}
