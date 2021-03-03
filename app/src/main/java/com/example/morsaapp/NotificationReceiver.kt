package com.example.morsaapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Run", "Is Runnning")
        Toast.makeText(context, "Receiver is Running", Toast.LENGTH_LONG).show()

            val intent = Intent(context, ReStockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent : PendingIntent= PendingIntent.getActivity( context,0, intent, 0)

        val deferred: Deferred<String> = GlobalScope.async { reStock(context,"admin") }

        runBlocking {
            Log.d("ReStockResult",deferred.await())
            if(deferred.await() == "[]"){
                Log.d("Notification Result", "No ReStock Available")
            }
            else{
                val builder = NotificationCompat.Builder(context, "1")
                    .setSmallIcon(R.drawable.morsa_login_img)
                    .setContentTitle("Movimientos")
                    .setContentText("Faltan movimientos por realizar")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(1, builder.build())
            }
        }
    }

    private fun reStock( context : Context,username : String) : String
    {
        val odooConn = OdooConn("contacto@exinnotech.com","1411",context)
        val prefs = context.getSharedPreferences("startupPreferences", 0)
        odooConn.authenticateOdoo()
        return odooConn.reStock(username,prefs.getInt("activeUser",0))
    }
}
