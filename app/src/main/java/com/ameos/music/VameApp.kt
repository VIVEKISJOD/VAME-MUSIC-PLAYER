package com.ameos.music

import android.app.Application
import org.schabi.newpipe.extractor.NewPipe

class VameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(DownloaderImpl())
    }
}
