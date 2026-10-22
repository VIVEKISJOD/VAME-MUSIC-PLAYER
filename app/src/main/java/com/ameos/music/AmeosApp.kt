package com.ameos.music

import android.app.Application
import org.schabi.newpipe.extractor.NewPipe

class AmeosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(DownloaderImpl())
    }
}
