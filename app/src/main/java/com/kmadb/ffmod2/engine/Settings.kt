package com.kmadb.ffmod2.engine

object Settings {
    var selectedPackage = "com.dts.freefireth"
    var aimEnabled   = true
    var aimBone      = 4
    var aimFov       = 180f
    var aimSmooth    = 5f
    var aimAutoShoot = false
    var aimVisible   = true
    var espEnabled   = true
    var espBox       = true
    var espLine      = true
    var espHp        = true
    var espName      = true
    var espDist      = true
    var espSmooth    = true
    var radarEnabled = true
    var radarRange   = 100f
    var playerCount  = true
    var boxAlpha     = 1f
    var lineAlpha    = 1f
    var nameAlpha    = 1f
    var radarAlpha   = 0.85f
    var noRecoil     = false
    var speedHack    = false
    var menuAlpha    = 0.95f
    val blockedList  = mutableListOf<String>()
}
