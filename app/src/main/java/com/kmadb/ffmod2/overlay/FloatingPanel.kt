package com.kmadb.ffmod2.overlay

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import com.kmadb.ffmod2.engine.GameEngine
import com.kmadb.ffmod2.engine.Settings

class FloatingPanel(context: Context, private val onClose: () -> Unit) {
    private val ctx = context
    val root: LinearLayout
    private val tabContents = mutableListOf<View>()
    private val tabButtons  = mutableListOf<TextView>()

    init { root = build() }

    private fun build(): LinearLayout {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(240,8,8,8))
        }
        val header = LinearLayout(ctx).apply {
            orientation=LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(255,15,25,15))
            setPadding(dp(10),dp(8),dp(10),dp(8))
            gravity=Gravity.CENTER_VERTICAL
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(44))
        }
        val title = TextView(ctx).apply {
            text="KMADB"; textSize=15f; setTextColor(Color.argb(255,0,240,100))
            typeface=Typeface.MONOSPACE
            layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)
        }
        val closeBtn = TextView(ctx).apply {
            text="✕"; textSize=16f; setTextColor(Color.GRAY)
            setPadding(dp(12),dp(4),dp(4),dp(4))
            setOnClickListener { onClose() }
        }
        header.addView(title); header.addView(closeBtn); root.addView(header)
        val tabBar = LinearLayout(ctx).apply {
            orientation=LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(255,12,12,12))
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(38))
        }
        listOf("ESP","AIM","MISC","INFO").forEachIndexed { i, name ->
            val btn = TextView(ctx).apply {
                text=name; textSize=11f; gravity=Gravity.CENTER; typeface=Typeface.MONOSPACE
                setTextColor(if(i==0) Color.argb(255,0,240,100) else Color.GRAY)
                layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1f)
                setOnClickListener { selectTab(i) }
            }
            tabBar.addView(btn); tabButtons.add(btn)
        }
        root.addView(tabBar)
        root.addView(View(ctx).apply {
            setBackgroundColor(Color.argb(80,0,200,80))
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,1)
        })
        val scroll = ScrollView(ctx).apply {
            layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(280))
        }
        val holder = FrameLayout(ctx)
        listOf(buildEsp(),buildAim(),buildMisc(),buildInfo()).forEach { v ->
            v.visibility=View.GONE; holder.addView(v); tabContents.add(v)
        }
        tabContents[0].visibility=View.VISIBLE
        scroll.addView(holder); root.addView(scroll)
        return root
    }

    private fun selectTab(i: Int) {
        tabButtons.forEachIndexed { idx,btn -> btn.setTextColor(if(idx==i) Color.argb(255,0,240,100) else Color.GRAY) }
        tabContents.forEachIndexed { idx,v -> v.visibility=if(idx==i) View.VISIBLE else View.GONE }
    }

    private fun buildEsp() = vList(
        row("ESP",         Settings.espEnabled)  { Settings.espEnabled=it },
        row("Box",         Settings.espBox)       { Settings.espBox=it },
        row("Line",        Settings.espLine)      { Settings.espLine=it },
        row("HP Bar",      Settings.espHp)        { Settings.espHp=it },
        row("Nama",        Settings.espName)      { Settings.espName=it },
        row("Jarak",       Settings.espDist)      { Settings.espDist=it },
        row("Radar",       Settings.radarEnabled) { Settings.radarEnabled=it },
        row("Player count",Settings.playerCount)  { Settings.playerCount=it },
        slider("Box alpha",  (Settings.boxAlpha*100).toInt())     { Settings.boxAlpha=it/100f },
        slider("Line alpha", (Settings.lineAlpha*100).toInt())    { Settings.lineAlpha=it/100f },
        slider("Radar range",(Settings.radarRange/2).toInt())     { Settings.radarRange=it*2f }
    )

    private fun buildAim() = vList(
        row("Aimbot",       Settings.aimEnabled)   { Settings.aimEnabled=it },
        row("Auto shoot",   Settings.aimAutoShoot) { Settings.aimAutoShoot=it },
        row("Visible only", Settings.aimVisible)   { Settings.aimVisible=it },
        slider("FOV",    Settings.aimFov.toInt())   { Settings.aimFov=it.toFloat() },
        slider("Smooth", Settings.aimSmooth.toInt()){ Settings.aimSmooth=it.toFloat() },
        boneSelector()
    )

    private fun buildMisc(): LinearLayout {
        val et = EditText(ctx).apply {
            hint="Domain / IP"; setTextColor(Color.LTGRAY); setHintTextColor(Color.DKGRAY)
            textSize=12f; setBackgroundColor(Color.argb(80,255,255,255))
            setPadding(dp(8),dp(6),dp(8),dp(6))
        }
        val addBtn = TextView(ctx).apply {
            text="BLOCK"; setTextColor(Color.argb(255,0,230,80))
            setPadding(dp(12),dp(8),dp(12),dp(8)); textSize=12f
            setOnClickListener { val d=et.text.toString().trim(); if(d.isNotEmpty()){Settings.blockedList.add(d);et.setText("")} }
        }
        val inputRow = LinearLayout(ctx).apply {
            orientation=LinearLayout.HORIZONTAL
            addView(et,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
            addView(addBtn); setPadding(dp(10),dp(6),dp(10),dp(6))
        }
        return vList(
            row("No recoil", Settings.noRecoil)  { Settings.noRecoil=it },
            row("Speed hack",Settings.speedHack) { Settings.speedHack=it },
            sectionLabel("Block domain / IP"), inputRow
        )
    }

    private fun buildInfo(): LinearLayout {
        val tv = TextView(ctx).apply {
            setTextColor(Color.LTGRAY); textSize=12f; typeface=Typeface.MONOSPACE
            setPadding(dp(14),dp(10),dp(14),dp(10))
        }
        fun refresh() { tv.text="KMADB v1.0\nAndroid 11-16\nShizuku memory engine\n\nPaket: ${Settings.selectedPackage}\nPID: ${GameEngine.pid}\nPlayer: ${GameEngine.getPlayers().size}" }
        refresh()
        Handler(Looper.getMainLooper()).postDelayed({ refresh() }, 1000)
        return vList(tv)
    }

    private fun row(label: String, init: Boolean, onChange: (Boolean)->Unit): LinearLayout {
        val sw = Switch(ctx).apply { isChecked=init; setOnCheckedChangeListener{_,v->onChange(v)} }
        val lbl = TextView(ctx).apply { text=label; setTextColor(Color.LTGRAY); textSize=13f
            layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f) }
        return LinearLayout(ctx).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(14),dp(2),dp(14),dp(2)); addView(lbl); addView(sw)
        }
    }

    private fun slider(label: String, init: Int, onChange: (Int)->Unit): LinearLayout {
        val tv = TextView(ctx).apply { text="$label   $init"; setTextColor(Color.LTGRAY); textSize=12f }
        val sb = SeekBar(ctx).apply {
            max=100; progress=init
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(s:SeekBar,p:Int,u:Boolean){tv.text="$label   $p";onChange(p)}
                override fun onStartTrackingTouch(s:SeekBar){}
                override fun onStopTrackingTouch(s:SeekBar){}
            })
        }
        return LinearLayout(ctx).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(4),dp(14),0); addView(tv); addView(sb)
        }
    }

    private fun boneSelector(): LinearLayout {
        val bones = arrayOf("Pinggang","Tulang belakang","Dada","Leher","Kepala")
        val sp = Spinner(ctx).apply {
            adapter=ArrayAdapter(ctx,android.R.layout.simple_spinner_item,bones)
                .also{it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)}
            setSelection(Settings.aimBone)
            onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{
                override fun onItemSelected(a:android.widget.AdapterView<*>,v:View?,p:Int,i:Long){Settings.aimBone=p}
                override fun onNothingSelected(a:android.widget.AdapterView<*>){}
            }
        }
        val lbl = TextView(ctx).apply { text="Target bone"; setTextColor(Color.LTGRAY); textSize=12f
            layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f) }
        return LinearLayout(ctx).apply {
            orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(14),dp(4),dp(14),dp(4)); addView(lbl); addView(sp)
        }
    }

    private fun sectionLabel(text: String) = TextView(ctx).apply {
        this.text=text; textSize=11f; setTextColor(Color.GRAY); setPadding(dp(14),dp(10),dp(14),dp(2))
    }

    private fun vList(vararg views: View) = LinearLayout(ctx).apply {
        orientation=LinearLayout.VERTICAL; views.forEach { addView(it) }
    }

    private fun dp(v: Int) = (v*ctx.resources.displayMetrics.density).toInt()
}
