package com.rakesh.dittomobile
import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class ClipboardOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private lateinit var clipboard: android.content.ClipboardManager
    private val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        try {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val value = clip.getItemAt(0).coerceToText(this)?.toString()
                if (!value.isNullOrBlank()) ClipStore(this).add(value)
            }
        } catch (_: SecurityException) { } catch (_: Exception) { }
    }
    override fun onCreate() {
        super.onCreate()
        clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.addPrimaryClipChangedListener(listener)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotification()
        showBubble()
    }
    private fun createNotification() {
        val channelId = "ditto_overlay"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(channelId, "Ditto floating button", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, channelId).setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Ditto Mobile is ready").setContentText("Tap the floating clipboard button to open history")
            .setContentIntent(open).setOngoing(true).build()
        startForeground(41, notification)
    }
    private fun showBubble() {
        if (!android.provider.Settings.canDrawOverlays(this)) { stopSelf(); return }
        val view = TextView(this).apply {
            text = "▤"; textSize = 25f; gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(0xFF4256C5.toInt()); elevation = 12f
            setOnClickListener { startActivity(Intent(this@ClipboardOverlayService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(56, 56, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = 16; y = 0
        }
        try { wm.addView(view, params); bubble = view } catch (_: Exception) { stopSelf() }
    }
    override fun onDestroy() {
        try { clipboard.removePrimaryClipChangedListener(listener) } catch (_: Exception) { }
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
