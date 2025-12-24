package com.reppal.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class RappelActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("ID", -1)
        if (id == -1) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dao = RappelDatabase.getDatabase(context).rappelDao()
        val scope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            "ACTION_DELETE" -> {
                scope.launch {
                    dao.getRappelById(id)?.let { dao.deleteRappel(it) }
                    notificationManager.cancel(id)
                }
            }

            "ACTION_SNOOZE" -> {
                scope.launch {
                    dao.getRappelById(id)?.let { rappel ->
                        val nouveauTimestamp = System.currentTimeMillis() + (60 * 60 * 1000)
                        val newCalendar = Calendar.getInstance().apply { timeInMillis = nouveauTimestamp }

                        val rappelMisAJour = rappel.copy(timestamp = nouveauTimestamp)

                        dao.insertRappel(rappelMisAJour)

                        AlarmUtils.programmerNotification(context, rappelMisAJour, newCalendar)

                        notificationManager.cancel(id)
                    }
                }
            }
        }
    }
}