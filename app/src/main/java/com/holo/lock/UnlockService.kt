package com.holo.lock

import android.app.*
import android.content.*
import android.os.IBinder

class UnlockService : Service() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            c.startActivity(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("hl", "Holo-Lock", NotificationManager.IMPORTANCE_MIN))
        startForeground(1, Notification.Builder(this, "hl")
            .setContentTitle("Holo-Lock is active").setSmallIcon(android.R.drawable.ic_lock_lock).build())
        registerReceiver(receiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { unregisterReceiver(receiver); super.onDestroy() }
}
