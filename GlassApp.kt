package com.neo.glassplayer

import android.app.Application
import com.neo.glassplayer.data.MusicRepository

class GlassApp : Application() {
    val repository: MusicRepository by lazy { MusicRepository(this) }
}
