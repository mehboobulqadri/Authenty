package com.milkaholic.authenty.domain

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.*

class ClipboardHelper(private val context: Context) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var clearJob: Job? = null

    fun copyToClipboard(text: String, label: String, clearAfterMs: Long = 30000) {
        try {
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            
            // Cancel previous clear job
            clearJob?.cancel()
            
            // Schedule new clear job
            clearJob = scope.launch {
                delay(clearAfterMs)
                // Only clear if the current content is what we copied
                // Note: On Android 10+ we can't read clipboard in background, so this check might fail if app is backgrounded.
                // However, if the app is in foreground, it works.
                // For security, we might just want to clear it blindly or use a service.
                // But for now, let's try to clear it.
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    } else {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                    // Only show toast if we are in foreground (hard to check here, so maybe skip toast or use try-catch)
                    // Toast.makeText(context, "Clipboard cleared", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // Ignore errors accessing clipboard
                }
            }
            
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Fallback or log error
            e.printStackTrace()
        }
    }
}
