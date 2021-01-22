package com.example.morsaapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BackLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val intent = Intent(context, ReStockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent : PendingIntent = PendingIntent.getActivity( context,0, intent, 0)

        val builder = NotificationCompat.Builder(context, "1")
            .setSmallIcon(R.drawable.morsa_login_img)
            .setContentTitle("Movimientos")
            .setContentText("Faltan movimientos por realizar")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.search_icon_img, "Terminar Pendientes", pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1, builder.build())
    }
}