package com.example.bsm_management.ui.intro

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class GuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val web = WebView(this)
        setContentView(web)

        web.settings.javaScriptEnabled = true
        web.loadUrl("file:///android_asset/huongdan_bsm.html")
    }
}
