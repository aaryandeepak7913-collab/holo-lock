package com.holo.lock

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.webkit.*
import androidx.webkit.WebViewAssetLoader

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var unlocked = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val need = checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        if (need)
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1)
        if (!Settings.canDrawOverlays(this))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

        web = WebView(this)
        setContentView(web)
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest) = loader.shouldInterceptRequest(r.url)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(r: PermissionRequest) { runOnUiThread { r.grant(r.resources) } }
        }
        web.addJavascriptInterface(object {
            @JavascriptInterface fun unlock() = runOnUiThread {
                unlocked = true
                try { stopLockTask() } catch (e: Exception) {}
                finishAndRemoveTask()
            }
        }, "Android")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        startForegroundService(Intent(this, UnlockService::class.java))
        if (!need) pin()
    }

    private fun pin() { try { startLockTask() } catch (e: Exception) {} }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rc, p, g)
        pin()
    }

    @Deprecated("Back is disabled while locked")
    override fun onBackPressed() {}

    override fun onStop() {
        super.onStop()
        // If someone escapes before unlocking, bring the lock straight back.
        if (!unlocked && !isFinishing && Settings.canDrawOverlays(this))
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
