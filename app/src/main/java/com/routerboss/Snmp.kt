package com.routerboss

import java.math.BigInteger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

class SnmpValue(val type: Int, val raw: ByteArray) {
    val missing get() = type in 0x80..0x82
    fun long(): Long = if (raw.isEmpty()) 0L else BigInteger(1, raw).toLong()
    fun text(): String = when (type) {
        0x04 -> String(raw, Charsets.ISO_8859_1)
        0x43 -> "${long() / 100}s"
        in 0x80..0x82 -> "n/a"
        else -> long().toString()
    }
}

/** Tiny SNMP v2c GET client. No libraries, just UDP + BER. */
object Snmp {
    private fun len(n: Int): ByteArray = when {
        n < 128 -> byteArrayOf(n.toByte())
        n < 256 -> byteArrayOf(0x81.toByte(), n.toByte())
        else -> byteArrayOf(0x82.toByte(), (n shr 8).toByte(), n.toByte())
    }

    private fun tlv(tag: Int, v: ByteArray): ByteArray = byteArrayOf(tag.toByte()) + len(v.size) + v
    private fun i32(v: Int): ByteArray = tlv(2, BigInteger.valueOf(v.toLong()).toByteArray())

    private fun encOid(s: String): ByteArray {
        val p = s.trim('.').split('.').map { it.toInt() }
        val out = java.io.ByteArrayOutputStream()
        out.write(p[0] * 40 + p[1])
        for (k in 2 until p.size) {
            var n = p[k]
            val tmp = ArrayList<Int>()
            tmp.add(n and 0x7f); n = n shr 7
            while (n > 0) { tmp.add((n and 0x7f) or 0x80); n = n shr 7 }
            tmp.reversed().forEach { out.write(it) }
        }
        return tlv(6, out.toByteArray())
    }

    private fun decOid(b: ByteArray): String {
        val sb = StringBuilder()
        val f = b[0].toInt() and 0xff
        sb.append(f / 40).append('.').append(f % 40)
        var n = 0L
        for (k in 1 until b.size) {
            val x = b[k].toInt() and 0xff
            n = (n shl 7) or (x and 0x7f).toLong()
            if (x and 0x80 == 0) { sb.append('.').append(n); n = 0 }
        }
        return sb.toString()
    }

    private class Rd(val b: ByteArray) {
        var p = 0
        fun read(): Pair<Int, ByteArray> {
            val t = b[p++].toInt() and 0xff
            var l = b[p++].toInt() and 0xff
            if (l >= 0x80) {
                val n = l and 0x7f
                l = 0
                repeat(n) { l = (l shl 8) or (b[p++].toInt() and 0xff) }
            }
            val v = b.copyOfRange(p, p + l)
            p += l
            return t to v
        }
    }

    fun get(host: String, community: String, oids: List<String>, timeout: Int = 2000): Map<String, SnmpValue> {
        val vbs = oids.map { tlv(0x30, encOid(it) + byteArrayOf(5, 0)) }.fold(ByteArray(0)) { a, b -> a + b }
        val pdu = tlv(0xA0, i32(Random.nextInt(1, Int.MAX_VALUE)) + i32(0) + i32(0) + tlv(0x30, vbs))
        val msg = tlv(0x30, i32(1) + tlv(4, community.toByteArray()) + pdu)
        DatagramSocket().use { s ->
            s.soTimeout = timeout
            s.send(DatagramPacket(msg, msg.size, InetAddress.getByName(host), 161))
            val buf = ByteArray(4096)
            val pk = DatagramPacket(buf, buf.size)
            s.receive(pk)
            return parse(buf.copyOf(pk.length))
        }
    }

    private fun parse(b: ByteArray): Map<String, SnmpValue> {
        val top = Rd(b).read()
        val r = Rd(top.second)
        r.read(); r.read()
        val p = Rd(r.read().second)
        p.read()
        val err = BigInteger(p.read().second).toInt()
        p.read()
        if (err != 0) throw RuntimeException("SNMP error status $err")
        val vl = Rd(p.read().second)
        val out = LinkedHashMap<String, SnmpValue>()
        while (vl.p < vl.b.size) {
            val vb = Rd(vl.read().second)
            val o = decOid(vb.read().second)
            val v = vb.read()
            out[o] = SnmpValue(v.first, v.second)
        }
        return out
    }
}
