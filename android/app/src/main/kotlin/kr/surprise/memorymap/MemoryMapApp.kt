package kr.surprise.memorymap

import android.app.Application

class MemoryMapApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
