package com.routerboss

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object Telnet {
    fun open(host: String): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(host, 23), 800) }
        true
    } catch (e: Exception) { false }

    fun run(host: String, user: String, pass: String, cmds: List<String>, waitMs: Long = 1500): String {
        return Socket().use { s ->
            s.connect(InetSocketAddress(host, 23), 3000)
            s.soTimeout = 300
            val inp = s.getInputStream()
            val out = s.getOutputStream()
            val sb = StringBuilder()
            fun reply(cmd: Int, opt: Int) { out.write(byteArrayOf(255.toByte(), cmd.toByte(), opt.toByte())); out.flush() }
            fun send(x: String) { out.write((x + "\r\n").toByteArray()); out.flush() }
            fun pump(ms: Long) {
                val end = System.currentTimeMillis() + ms
                while (System.currentTimeMillis() < end) {
                    val b = try { inp.read() } catch (e: SocketTimeoutException) { continue }
                    if (b < 0) return
                    if (b == 255) {
                        val cmd = inp.read()
                        val opt = inp.read()
                        if (cmd == 251) reply(254, opt) else if (cmd == 253) reply(252, opt)
                        else if (cmd == 250) {
                            var prev = 0
                            while (true) { val x = inp.read(); if (x < 0 || (prev == 255 && x == 240)) break; prev = x }
                        }
                    } else if (b != 0 && b != 13) sb.append(b.toChar())
                }
            }
            pump(1200)
            var t = sb.toString().lowercase()
            if ("login" in t || "user name" in t || "username" in t) { send(user); pump(800); t = sb.toString().lowercase() }
            if ("password" in t) { send(pass); pump(1200) }
            for (cm in cmds) { send(cm); pump(waitMs) }
            sb.toString()
        }
    }
}
