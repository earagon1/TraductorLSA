package com.example.traductorlsa

import android.app.Application
import com.clerk.api.Clerk

class SenarApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Clerk.initialize(
            context= this,
            
        )
    }
}
