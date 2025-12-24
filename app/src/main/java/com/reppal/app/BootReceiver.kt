package com.reppal.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val pendingResult = goAsync()
            val dao = RappelDatabase.getDatabase(context).rappelDao()
            val appContext = context.applicationContext

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val maintenant = System.currentTimeMillis()
                    val rappelsAVenir = dao.getAllRappelsSync().filter { it.timestamp > maintenant }

                    rappelsAVenir.forEach { rappel ->
                        val cal = Calendar.getInstance().apply { timeInMillis = rappel.timestamp }
                        AlarmUtils.programmerNotification(appContext, rappel, cal)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}