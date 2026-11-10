package com.kmadb.ffmod2.overlay

import android.content.Context
import android.graphics.*
import android.view.View
import com.kmadb.ffmod2.engine.GameEngine
import com.kmadb.ffmod2.engine.Player
import com.kmadb.ffmod2.engine.Settings
import kotlin.math.sqrt

class EspOverlay(context: Context) : View(context) {
    private val box  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeWidth=2.5f }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeWidth=1.5f }
    private val hpBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.FILL; color=Color.argb(160,30,30,30) }
    private val hpFg = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.FILL }
    private val txt  = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign=Paint.Align.CENTER; typeface=Typeface.MONOSPACE }
    private val dot  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.FILL; color=Color.RED }
    private val self = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.FILL; color=Color.GREEN }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeWidth=1.5f; color=Color.argb(180,0,230,80) }
    private val cross= Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeWidth=1f; color=Color.argb(60,255,255,255) }

    override fun onDraw(c: Canvas) {
        val players = GameEngine.getPlayers()
        val sw = width.toFloat(); val sh = height.toFloat()
        for (p in players) project(p, sw, sh)
        if (Settings.playerCount) {
            txt.color=Color.argb(220,0,255,100); txt.textSize=34f; txt.textAlign=Paint.Align.LEFT
            c.drawText("● ${players.size} musuh", 24f, 56f, txt)
            txt.textAlign=Paint.Align.CENTER
        }
        if (Settings.espEnabled) for (p in players) drawPlayer(c, p)
        if (Settings.radarEnabled) drawRadar(c, players, sw, sh)
        postInvalidateDelayed(33)
    }

    private fun project(p: Player, sw: Float, sh: Float) {
        val scale = sh / (2f * Settings.radarRange)
        p.screenX = sw/2f + p.z*scale
        p.screenY = sh/2f - p.x*scale
        p.dist = sqrt((p.x*p.x+p.z*p.z).toDouble()).toFloat()
        val h = (sh*0.18f).coerceAtLeast(60f) / (p.dist.coerceAtLeast(1f)/20f)
        p.boxTop = p.screenY - h
        p.boxBot = p.screenY + h*0.1f
    }

    private fun drawPlayer(c: Canvas, p: Player) {
        val bw = (p.boxBot-p.boxTop)*0.35f
        val sx=p.screenX; val top=p.boxTop; val bot=p.boxBot
        val col = when {
            p.hp < p.maxHp*0.3f -> Color.argb((Settings.boxAlpha*255).toInt(),255,50,50)
            p.hp < p.maxHp*0.6f -> Color.argb((Settings.boxAlpha*255).toInt(),255,200,0)
            else -> Color.argb((Settings.boxAlpha*255).toInt(),0,230,80)
        }
        if (Settings.espBox) {
            box.color=col
            val cLen=bw*0.4f
            c.drawLine(sx-bw,top,sx-bw+cLen,top,box); c.drawLine(sx+bw,top,sx+bw-cLen,top,box)
            c.drawLine(sx-bw,bot,sx-bw+cLen,bot,box); c.drawLine(sx+bw,bot,sx+bw-cLen,bot,box)
            c.drawLine(sx-bw,top,sx-bw,top+(bot-top)*0.25f,box); c.drawLine(sx+bw,top,sx+bw,top+(bot-top)*0.25f,box)
            c.drawLine(sx-bw,bot,sx-bw,bot-(bot-top)*0.25f,box); c.drawLine(sx+bw,bot,sx+bw,bot-(bot-top)*0.25f,box)
        }
        if (Settings.espLine) {
            line.color=Color.argb((Settings.lineAlpha*180).toInt(),0,230,80)
            c.drawLine(sx,bot,width/2f,height.toFloat(),line)
        }
        if (Settings.espHp) {
            val barX=sx-bw-7f; val barH=bot-top
            c.drawRect(barX-3f,top,barX,bot,hpBg)
            val r=p.hp.toFloat()/p.maxHp.toFloat()
            hpFg.color=col; c.drawRect(barX-3f,top+barH*(1f-r),barX,bot,hpFg)
        }
        if (Settings.espName) {
            txt.color=Color.argb((Settings.nameAlpha*230).toInt(),255,255,255); txt.textSize=26f
            c.drawText(p.name.ifEmpty{"Enemy"},sx,top-28f,txt)
        }
        if (Settings.espDist) {
            txt.color=Color.argb((Settings.nameAlpha*180).toInt(),180,255,180); txt.textSize=22f
            c.drawText("${p.dist.toInt()}m  ${p.hp}HP",sx,top-8f,txt)
        }
    }

    private fun drawRadar(c: Canvas, players: List<Player>, sw: Float, sh: Float) {
        val cx=sw-130f; val cy=130f; val r=110f
        val bg=Paint().apply { color=Color.argb((Settings.radarAlpha*200).toInt(),0,0,0); style=Paint.Style.FILL }
        c.drawCircle(cx,cy,r,bg); c.drawCircle(cx,cy,r,ring)
        c.drawCircle(cx,cy,r*0.5f,cross)
        c.drawLine(cx-r,cy,cx+r,cy,cross); c.drawLine(cx,cy-r,cx,cy+r,cross)
        c.drawCircle(cx,cy,5f,self)
        for (p in players) {
            dot.color=if(p.hp<p.maxHp*0.3f) Color.RED else Color.argb(220,255,100,0)
            c.drawCircle(
                (cx+(p.z/Settings.radarRange)*r).coerceIn(cx-r+5f,cx+r-5f),
                (cy-(p.x/Settings.radarRange)*r).coerceIn(cy-r+5f,cy+r-5f),
                5f,dot)
        }
        txt.color=Color.argb(180,150,255,150); txt.textSize=20f; txt.textAlign=Paint.Align.CENTER
        c.drawText("${Settings.radarRange.toInt()}m · ${players.size}",cx,cy+r+18f,txt)
    }
}
