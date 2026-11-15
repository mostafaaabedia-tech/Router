package com.routerboss

import android.annotation.SuppressLint
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.*

/* ============ ROUTER (settings page inside the app) ============ */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Web(c: Cfg) {
    Column(Modifier.fillMaxSize()) {
        Text("Your router’s own settings. You’re logged in automatically.", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp, 10.dp))
        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    var tried = false
                    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
                        if (!tried) { tried = true; handler?.proceed(c.user, c.pass) } else handler?.cancel()
                    }
                }
                loadUrl("http://${c.ip}/")
            }
        })
    }
}
