package com.exemple.blockingapps.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView

object BlockOverlay {

    private var composeView: ComposeView? = null

    fun show(context: Context) {
        if (composeView != null) return

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or     // Không chiếm focus
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,    // Tràn màn hình


            PixelFormat.OPAQUE
        )

        Handler(Looper.getMainLooper()).postDelayed({
            composeView = ComposeView(context).apply {
                setContent {
                    BlockOverlayUI()
                }
            }

            try {
                windowManager.addView(composeView, params)
                Log.e("OVERLAY", "Overlay shown successfully with DELAY.")
            } catch (e: Exception) {
                Log.e("OVERLAY", "FATAL ERROR adding view to WindowManager: ${e.message}", e)
                composeView = null
            }
        }, 100L)
    }


    fun hide(context: Context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        composeView?.let {
            try {
                wm.removeView(it)
                Log.e("OVERLAY", "Overlay hidden.")
            } catch (e: Exception) {
                Log.e("OVERLAY", "Error removing overlay: ${e.message}")
            }
        }
        composeView = null
    }
}

@Composable
fun BlockOverlayUI() {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = true) { Log.d("BLOCKER", "Overlay clicked! Input blocked.") }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "This app is blocked",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}