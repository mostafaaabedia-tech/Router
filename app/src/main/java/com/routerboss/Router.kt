package com.routerboss

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL

data class Resp(val code: Int, val body: String)
data class Client(val mac: String, val ip: String)

object Router {
    // Guesses for old TP-Link ADSL firmware. The Setup tab probes these so we learn which ones are real.
    val PATHS = listOf(
        "/", "/index.htm", "/basic/home.htm", "/basic/home_wan.htm", "/basic/home_lan.htm",
        "/basic/home_dhcp.htm", "/basic/home_status.htm", "/advanced/home_qos.htm",
        "/advanced/qos.htm", "/basic/status.htm", "/cgi-bin/status"
    )

    fun fetch(ip: String, path: String, user: String, pass: String): Resp {
        val u = URL("http://$ip${if (path.startsWith("/")) path else "/$path"}")
        val c = u.openConnection() as HttpURLConnection
        val b64 = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
        c.connectTimeout = 3000
        c.readTimeout = 4000
        c.setRequestProperty("Authorization", "Basic $b64")
        c.setRequestProperty("Cookie", "Authorization=Basic%20$b64")
        c.setRequestProperty("Referer", "http://$ip/")
        return try {
            val code = c.responseCode
            val stream = if (code in 200..399) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText().take(300_000) } ?: ""
            Resp(code, text)
        } finally {
            c.disconnect()
        }
    }

    private val macRx = Regex("(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}")
    private val ipRx = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")

    /** Generic: finds every MAC on the page and the nearest IP next to it. */
    fun parseClients(h: String): List<Client> {
        val seen = HashSet<String>()
        val out = ArrayList<Client>()
        for (m in macRx.findAll(h)) {
            val mac = m.value.lowercase().replace('-', ':')
            if (!seen.add(mac)) continue
            val a = maxOf(0, m.range.first - 150)
            val z = minOf(h.length, m.range.last + 150)
            val ip = ipRx.find(h.substring(a, z))?.value ?: "?"
            out.add(Client(mac, ip))
        }
        return out
    }
}
