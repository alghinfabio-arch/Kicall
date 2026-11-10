package com.kmadb.ffmod2

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.kmadb.ffmod2.engine.GameEngine
import com.kmadb.ffmod2.service.OverlayService
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private val OVR_REQ = 101
    private val SHZ_REQ = 102

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        Shizuku.addRequestPermissionResultListener { code, _ -> if (code == SHZ_REQ) refresh() }
        listOf("com.dts.freefireth","com.dena.a91476360","com.garena.game.ff").forEach { pkg ->
            try {
                packageManager.getPackageInfo(pkg, 0)
                com.kmadb.ffmod2.engine.Settings.selectedPackage = pkg
                return@forEach
            } catch (_: Exception) {}
        }
        findViewById<Button>(R.id.btn_shizuku).setOnClickListener {
            if (!Shizuku.pingBinder()) { toast("Buka Shizuku dulu!"); return@setOnClickListener }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(SHZ_REQ)
            else toast("Shizuku udah granted ✓")
        }
        findViewById<Button>(R.id.btn_overlay).setOnClickListener {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")), OVR_REQ)
        }
        findViewById<Button>(R.id.btn_launch).setOnClickListener {
            when {
                !Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                    toast("Grant Shizuku dulu")
                !Settings.canDrawOverlays(this) -> toast("Grant overlay dulu")
                else -> {
                    startService(Intent(this, OverlayService::class.java))
                    moveTaskToBack(true)
                }
            }
        }
        refresh()
    }

    private fun refresh() {
        val shz = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        val ovr = Settings.canDrawOverlays(this)
        findViewById<TextView>(R.id.tv_shizuku).apply {
            text = if (shz) "Shizuku ✓" else "Shizuku ✗"
            setTextColor(if (shz) 0xFF00FF66.toInt() else 0xFFFF4444.toInt())
        }
        findViewById<TextView>(R.id.tv_overlay).apply {
            text = if (ovr) "Overlay ✓" else "Overlay ✗"
            setTextColor(if (ovr) 0xFF00FF66.toInt() else 0xFFFF4444.toInt())
        }
        findViewById<TextView>(R.id.tv_pkg).text = "📦 ${com.kmadb.ffmod2.engine.Settings.selectedPackage}"
        findViewById<Button>(R.id.btn_launch).isEnabled = shz && ovr
    }

    override fun onResume() { super.onResume(); refresh() }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    override fun onActivityResult(req: Int, res: Int, d: Intent?) { super.onActivityResult(req,res,d); refresh() }
}
