package com.routerboss

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

/* ============ building blocks ============ */
@Composable
fun Header(title: String, sub: String, chip: String? = null) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0B8F84), Color(0xFF0E4D64))))
            .padding(22.dp, 26.dp, 22.dp, 28.dp)
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = Color(0xCCFFFFFF), fontSize = 14.sp)
            if (chip != null) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(50), color = Color(0x33FFFFFF)) {
                    Text(chip, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(12.dp, 5.dp))
                }
            }
        }
    }
}

@Composable
fun Soft(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
fun Title(t: String) = Text(t, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

@Composable
fun Hint(t: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) = Text(t, fontSize = 13.sp, color = color, lineHeight = 19.sp)

@Composable
fun Tile(modifier: Modifier, emoji: String, big: String, small: String) {
    Card(
        modifier, shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(emoji, fontSize = 22.sp)
            Text(big, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Hint(small)
        }
    }
}

@Composable
fun Gauge(frac: Float, value: String, unit: String, label: String, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val s = 16.dp.toPx()
                val sz = Size(size.width - s, size.height - s)
                val tl = Offset(s / 2, s / 2)
                drawArc(track, 135f, 270f, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
                if (frac > 0.01f) drawArc(color, 135f, 270f * frac.coerceIn(0f, 1f), false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Hint(unit)
            }
        }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Chart(a: List<Float>, b: List<Float>, ca: Color, cb: Color) {
    Canvas(Modifier.fillMaxWidth().height(90.dp).padding(top = 10.dp)) {
        val mx = maxOf(1f, (a + b).maxOrNull() ?: 1f)
        fun line(v: List<Float>, col: Color) {
            if (v.size < 2) return
            val path = Path()
            v.forEachIndexed { i, y ->
                val px = size.width * i / 59f
                val py = size.height * (1 - y / mx)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(path, col, style = Stroke(5f, cap = StrokeCap.Round))
        }
        line(a, ca); line(b, cb)
    }
}
