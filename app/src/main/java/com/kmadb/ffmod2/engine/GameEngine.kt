package com.kmadb.ffmod2.engine

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.kmadb.ffmod2.IMemService
import rikka.shizuku.Shizuku
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Player(
    val id: Int,
    var x: Float, var y: Float, var z: Float,
    var hp: Int, var maxHp: Int,
    var name: String,
    var dist: Float = 0f,
    var screenX: Float = 0f,
    var screenY: Float = 0f,
    var boxTop: Float = 0f,
    var boxBot: Float = 0f
)

object GameEngine {
    private const val TAG = "GameEngine"
    private const val IL2CPP_LIB   = "libil2cpp.so"
    private const val OFF_ENT_MGR  = 0x05A3B210L
    private const val OFF_ENT_LIST = 0x18L
    private const val OFF_ENT_COUNT= 0x20L
    private const val ENT_STRIDE   = 0xE0L
    private const val OFF_ID       = 0x00L
    private const val OFF_POS      = 0x10L
    private const val OFF_HP       = 0x40L
    private const val OFF_MAXHP    = 0x44L
    private const val OFF_NAME     = 0x80L

    val AC_DOMAINS = listOf(
        "log.ff.garena.com","bigdata.garena.com",
        "accheck.freefireth.com","report.freefireth.com","anticheat.freefireth.com"
    )

    private val players = mutableListOf<Player>()
    private val lock = Any()
    @Volatile var running = false
    @Volatile var pid = -1
    private var mem: IMemService? = null
    private var il2cppBase = 0L

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.kmadb.ffmod2", MemService::class.java.name)
    ).daemon(false).processNameSuffix("mem").version(1)

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            mem = IMemService.Stub.asInterface(b)
            start()
        }
        override fun onServiceDisconnected(n: ComponentName) {
            mem = null; running = false
        }
    }

    fun bind() { Shizuku.bindUserService(serviceArgs, conn) }
    fun unbind() { running = false; Shizuku.unbindUserService(serviceArgs, conn, true) }

    private fun start() {
        running = true
        Thread({
            blockAc()
            resolveProcess()
            while (running) {
                try { tick(); Thread.sleep(50) }
                catch (_: InterruptedException) { break }
                catch (e: Exception) { Log.w(TAG, e.message) }
            }
        }, "game-engine").also { it.isDaemon = true; it.start() }
    }

    private fun blockAc() { AC_DOMAINS.forEach { mem?.blockDomain(it) } }

    private fun resolveProcess() {
        repeat(10) {
            pid = mem?.getPid(Settings.selectedPackage) ?: -1
            if (pid > 0) {
                il2cppBase = mem?.getMapBase(pid, IL2CPP_LIB)?.toLongOrNull(16) ?: 0L
                return
            }
            Thread.sleep(1000)
        }
    }

    private fun tick() {
        val m = mem ?: return
        if (pid <= 0 || il2cppBase == 0L) return
        val mgrPtr = m.readMem(pid, il2cppBase + OFF_ENT_MGR, 8)?.toLong() ?: return
        if (mgrPtr == 0L) return
        val listPtr = m.readMem(pid, mgrPtr + OFF_ENT_LIST, 8)?.toLong() ?: return
        val count = m.readMem(pid, mgrPtr + OFF_ENT_COUNT, 4)?.toInt()?.coerceIn(0,60) ?: return
        val fresh = mutableListOf<Player>()
        for (i in 0 until count) {
            val base = listPtr + i * ENT_STRIDE
            val id   = m.readMem(pid, base + OFF_ID,    4)?.toInt()   ?: continue
            val posB = m.readMem(pid, base + OFF_POS,  12)            ?: continue
            val hp   = m.readMem(pid, base + OFF_HP,    4)?.toInt()   ?: continue
            val maxHp= m.readMem(pid, base + OFF_MAXHP, 4)?.toInt()?.coerceAtLeast(1) ?: continue
            val nameB= m.readMem(pid, base + OFF_NAME,  32)           ?: continue
            val bb   = ByteBuffer.wrap(posB).order(ByteOrder.LITTLE_ENDIAN)
            fresh.add(Player(id=id, x=bb.float, y=bb.float, z=bb.float,
                hp=hp, maxHp=maxHp, name=nameB.toString(Charsets.UTF_8).trimEnd('\u0000')))
        }
        synchronized(lock) { players.clear(); players.addAll(fresh) }
    }

    fun getPlayers(): List<Player> = synchronized(lock) { players.toList() }

    private fun ByteArray.toLong(): Long = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).long
    private fun ByteArray.toInt(): Int = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).int
}
