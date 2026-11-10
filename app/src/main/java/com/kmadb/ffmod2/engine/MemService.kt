package com.kmadb.ffmod2.engine

import android.util.Log
import com.kmadb.ffmod2.IMemService
import java.io.File
import java.io.RandomAccessFile
import kotlin.system.exitProcess

class MemService : IMemService.Stub() {
    companion object { private const val TAG = "KMADBMem" }
    override fun destroy() = exitProcess(0)
    override fun getPid(packageName: String): Int {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("pidof", packageName))
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            out.split(" ").firstOrNull()?.trim()?.toIntOrNull() ?: -1
        } catch (e: Exception) { -1 }
    }
    override fun readMem(pid: Int, address: Long, size: Int): ByteArray? {
        return try {
            val f = RandomAccessFile("/proc/$pid/mem", "r")
            f.seek(address)
            val buf = ByteArray(size)
            f.readFully(buf)
            f.close()
            buf
        } catch (e: Exception) { null }
    }
    override fun writeMem(pid: Int, address: Long, data: ByteArray): Int {
        return try {
            val f = RandomAccessFile("/proc/$pid/mem", "rw")
            f.seek(address); f.write(data); f.close(); 0
        } catch (e: Exception) { -1 }
    }
    override fun findPattern(pid: Int, pattern: String, mask: String): Long {
        return try {
            val pat = pattern.split(" ").map { it.toInt(16).toByte() }.toByteArray()
            val msk = mask.toByteArray()
            File("/proc/$pid/maps").readLines().forEach { line ->
                if (!line.contains("r-xp") && !line.contains("r--p")) return@forEach
                val parts = line.split(" ").first().split("-")
                if (parts.size < 2) return@forEach
                val start = parts[0].toLongOrNull(16) ?: return@forEach
                val end = parts[1].toLongOrNull(16) ?: return@forEach
                val size = (end - start).coerceAtMost(0x1000000L).toInt()
                if (size <= 0) return@forEach
                val mem = readMem(pid, start, size) ?: return@forEach
                var i = 0
                outer@ while (i <= mem.size - pat.size) {
                    for (j in pat.indices) {
                        if (msk[j] == 'x'.code.toByte() && mem[i+j] != pat[j]) { i++; continue@outer }
                    }
                    return start + i
                }
            }
            -1L
        } catch (e: Exception) { -1L }
    }
    override fun getMapBase(pid: Int, libName: String): String {
        return try {
            File("/proc/$pid/maps").readLines()
                .firstOrNull { it.contains(libName) }
                ?.split("-")?.first() ?: "-1"
        } catch (e: Exception) { "-1" }
    }
    override fun blockDomain(domain: String): Int {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c",
                "nslookup $domain | grep Address | tail -1 | awk '{print \$2}'"))
            val ip = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            if (ip.isNotEmpty()) {
                Runtime.getRuntime().exec(arrayOf("iptables","-I","OUTPUT","-d",ip,"-j","DROP")).waitFor()
            }
            0
        } catch (e: Exception) { -1 }
    }
}
