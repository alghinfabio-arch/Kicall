package com.kmadb.ffmod2.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat
import com.kmadb.ffmod2.engine.GameEngine
import com.kmadb.ffmod2.overlay.EspOverlay
import com.kmadb.ffmod2.overlay.FloatingPanel

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var esp: EspOverlay
    private lateinit var panel: FloatingPanel
    private lateinit var bubble: View

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotif())
        GameEngine.bind()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addEsp(); addPanel(); addBubble()
    }

    private fun addEsp() {
        esp = EspOverlay(this)
        wm.addView(esp, WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT))
    }

    private fun addPanel() {
        panel = FloatingPanel(this) { stopSelf() }
        val lp = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.72f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = 20; y = 300 }
        wm.addView(panel.root, lp)
        setupDrag(panel.root, lp)
        panel.root.visibility = View.GONE
    }

    private fun addBubble() {
        bubble = View(this).apply { setBackgroundColor(0xFF00EE66.toInt()) }
        val lp = WindowManager.LayoutParams(52, 52,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = 20; y = 200 }
        bubble.setOnClickListener {
            panel.root.visibility =
                if (panel.root.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        wm.addView(bubble, lp)
        setupDrag(bubble, lp)
    }

    private fun setupDrag(v: View, lp: WindowManager.LayoutParams) {
        var ix=0; var iy=0; var tx=0f; var ty=0f; var drag=false
        v.setOnTouchListener { _, e ->
            when(e.action) {
                MotionEvent.ACTION_DOWN -> { ix=lp.x;iy=lp.y;tx=e.rawX;ty=e.rawY;drag=false;true }
                MotionEvent.ACTION_MOVE -> {
                    val dx=e.rawX-tx; val dy=e.rawY-ty
                    if (Math.abs(dx)>6||Math.abs(dy)>6) drag=true
                    if (drag) { lp.x=ix+dx.toInt();lp.y=iy+dy.toInt();wm.updateViewLayout(v,lp) }
                    true
                }
                MotionEvent.ACTION_UP -> { if(!drag) v.performClick(); true }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        GameEngine.unbind()
        runCatching { wm.removeView(esp) }
        runCatching { wm.removeView(panel.root) }
        runCatching { wm.removeView(bubble) }
        super.onDestroy()
    }

    override fun onBind(i: Intent?): IBinder? = null

    private fun buildNotif(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("kmadb","KMADB",NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this,"kmadb")
            .setContentTitle("KMADB aktif")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true).build()
    }
}
