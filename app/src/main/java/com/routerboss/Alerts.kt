package com.routerboss

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.TimeUnit

fun internetOk(): Boolean {
    for (u in listOf("http://connectivitycheck.gstatic.com/generate_204", "http://clients3.google.com/generate_204")) {
        try {
            val h = URL(u).openConnection() as HttpURLConnection
            h.connectTimeout = 3000; h.readTimeout = 3000
            val ok = h.responseCode in 200..399
            h.disconnect()
            if (ok) return true
        } catch (e: Exception) { }
    }
    return false
}

suspend fun scanNet(base: String): List<String> = coroutineScope {
    val sem = Semaphore(40)
    (1..254).map { n ->
        async(Dispatchers.IO) {
            sem.withPermit {
                val h = "$base.$n"
                if (try { InetAddress.getByName(h).isReachable(400) } catch (e: Exception) { false }) h else null
            }
        }
    }.awaitAll().filterNotNull()
}

fun pushNote(ctx: Context, id: Int, title: String, text: String) {
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("rb_alerts", "Router alerts", NotificationManager.IMPORTANCE_HIGH))
    val n = NotificationCompat.Builder(ctx, "rb_alerts").setSmallIcon(android.R.drawable.stat_notify_error)
        .setContentTitle(title).setContentText(text).setAutoCancel(true).build()
    nm.notify(id, n)
}

class AlertWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): ListenableWorker.Result {
        val app = applicationContext
        val sp = app.getSharedPreferences("rb", Context.MODE_PRIVATE)
        val ip = sp.getString("ip", "192.168.1.1") ?: "192.168.1.1"
        val up = withContext(Dispatchers.IO) { try { InetAddress.getByName(ip).isReachable(1500) } catch (e: Exception) { false } }
        if (!up) return ListenableWorker.Result.success()
        val net = withContext(Dispatchers.IO) { internetOk() }
        val wasDown = sp.getBoolean("al_down", false)
        if (!net && !wasDown) pushNote(app, 1, "Internet is down", "Your router is on, but the internet is not reachable.")
        if (net && wasDown) pushNote(app, 2, "Internet is back", "Everything works again.")
        sp.edit().putBoolean("al_down", !net).apply()
        val live = scanNet(ip.substringBeforeLast("."))
        val seen = sp.getStringSet("al_seen", null)
        if (seen == null) {
            sp.edit().putStringSet("al_seen", live.toSet()).apply()
        } else {
            val fresh = live.filter { it !in seen && it != ip }
            if (fresh.isNotEmpty()) {
                pushNote(app, 3, "New device on your wifi", "${fresh.size} new: ${fresh.joinToString()}")
                sp.edit().putStringSet("al_seen", seen + fresh).apply()
            }
        }
        return ListenableWorker.Result.success()
    }
}

fun setAlerts(ctx: Context, on: Boolean) {
    ctx.getSharedPreferences("rb", Context.MODE_PRIVATE).edit()
        .putBoolean("alerts", on).remove("al_seen").remove("al_down").apply()
    val wm = WorkManager.getInstance(ctx)
    if (on) wm.enqueueUniquePeriodicWork("rb_alerts", ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES).build())
    else wm.cancelUniqueWork("rb_alerts")
}

@Composable
fun AlertsCard() {
    val ctx = LocalContext.current
    val sp = remember { ctx.getSharedPreferences("rb", Context.MODE_PRIVATE) }
    var on by remember { mutableStateOf(sp.getBoolean("alerts", false)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { setAlerts(ctx, true); on = true }
    }
    Soft {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Title("🔔 Alerts")
                Hint("Get a notification when a new device joins or the internet goes down. Checks about every 15 minutes while you are on your home wifi.")
            }
            Switch(checked = on, onCheckedChange = { want ->
                if (!want) { setAlerts(ctx, false); on = false }
                else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                else { setAlerts(ctx, true); on = true }
            })
        }
        Spacer(Modifier.height(6.dp))
        Hint("New device alerts go by IP address, so a phone that gets a new address can trigger one.")
    }
}
