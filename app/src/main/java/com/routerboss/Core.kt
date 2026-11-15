package com.routerboss

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.net.SocketTimeoutException

/* ============ helpers ============ */
fun ipOf(i: Int) = "${i and 255}.${i shr 8 and 255}.${i shr 16 and 255}.${i shr 24 and 255}"

@Suppress("DEPRECATION")
fun gateway(ctx: Context): String? = try {
    val g = (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).dhcpInfo.gateway
    if (g == 0) null else ipOf(g)
} catch (e: Exception) { null }

@Suppress("DEPRECATION")
fun myIp(ctx: Context): String = try {
    ipOf((ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).dhcpInfo.ipAddress)
} catch (e: Exception) { "" }

fun fmt(k: Double) = if (k >= 1000) "%.1f".format(k / 1000) else "%.0f".format(k)
fun unit(k: Double) = if (k >= 1000) "Mbps" else "kbps"
fun wrap(n: Long, p: Long) = if (n >= p) n - p else n + 4294967296L - p

class Cfg(ctx: Context) {
    private val p = ctx.getSharedPreferences("rb", Context.MODE_PRIVATE)
    var ip by mutableStateOf(p.getString("ip", "192.168.1.1")!!)
    var user by mutableStateOf(p.getString("user", "admin")!!)
    var pass by mutableStateOf(p.getString("pass", "admin")!!)
    var comm by mutableStateOf(p.getString("comm", "public")!!)
    var ifx by mutableStateOf(p.getString("ifx", "1")!!)
    var planDn by mutableStateOf(p.getString("pd", "8")!!)
    var planUp by mutableStateOf(p.getString("pu", "1")!!)
    var setupDone by mutableStateOf(p.getBoolean("done", false))
    var online by mutableIntStateOf(-1)
    init { if (!p.contains("ip")) gateway(ctx)?.let { ip = it } }
    fun save() {
        p.edit().putString("ip", ip).putString("user", user).putString("pass", pass).putString("comm", comm)
            .putString("ifx", ifx).putString("pd", planDn).putString("pu", planUp).putBoolean("done", setupDone).apply()
    }
    fun name(ip: String): String = p.getString("n_$ip", "") ?: ""
    fun setName(ip: String, n: String) { p.edit().putString("n_$ip", n).apply() }
}

/* ============ live monitor (keeps running on every tab) ============ */
class Mon {
    var down by mutableStateOf(0.0)
    var up by mutableStateOf(0.0)
    var totD by mutableStateOf(0L)
    var totU by mutableStateOf(0L)
    var err by mutableStateOf<String?>(null)
    var ok by mutableStateOf(false)
    val dh = mutableStateListOf<Float>()
    val uh = mutableStateListOf<Float>()

    suspend fun run(c: Cfg) {
        var lt = 0L; var pi = 0L; var po = 0L
        while (true) {
            try {
                val x = c.ifx.trim().ifEmpty { "1" }
                val kI = "1.3.6.1.2.1.2.2.1.10.$x"
                val kO = "1.3.6.1.2.1.2.2.1.16.$x"
                val r = withContext(Dispatchers.IO) { Snmp.get(c.ip, c.comm, listOf(kI, kO)) }
                val now = System.currentTimeMillis()
                val i = r[kI]; val o = r[kO]
                if (i == null || o == null || i.missing || o.missing) {
                    ok = false; down = 0.0; up = 0.0
                    err = "This connection has no traffic counters. Pick another one in Setup."
                } else {
                    val li = i.long(); val lo = o.long()
                    if (lt > 0) {
                        val dt = (now - lt) / 1000.0
                        val di = wrap(li, pi); val dOut = wrap(lo, po)
                        down = di * 8 / 1000 / dt; up = dOut * 8 / 1000 / dt
                        totD += di; totU += dOut
                        dh.add(down.toFloat()); uh.add(up.toFloat())
                        if (dh.size > 60) { dh.removeAt(0); uh.removeAt(0) }
                    }
                    lt = now; pi = li; po = lo; ok = true; err = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ok = false; down = 0.0; up = 0.0
                err = if (e is SocketTimeoutException) "Your router isn’t sharing live speed yet. Open Setup to turn it on."
                else "Can’t read the router right now. Are you on its wifi?"
            }
            delay(2000)
        }
    }
}
