package com.example.traductorlsa

import android.app.Application
import com.clerk.api.Clerk

class SenarApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Clerk.initialize(
            context= this,
            publishableKey = "pk_test_am9pbnQtZ2VsZGluZy0xMi5jbGVyay5hY2NvdW50cy5kZXYk"
        )
    }
}
