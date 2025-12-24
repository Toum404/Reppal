package com.reppal.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.core.app.NotificationCompat

class RappelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("ID", 0)
        val titre = intent.getStringExtra("TITRE") ?: context.getString(R.string.default_reminder_title)

        val rawDesc = intent.getStringExtra("DESC")

        val desc: CharSequence = if (rawDesc.isNullOrBlank()) {
            val noDescText = context.getString(R.string.no_description)
            SpannableString(noDescText).apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, 0)
            }
        } else {
            rawDesc
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "rappel_channel"

        val channelName = context.getString(R.string.channel_name)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            enableLights(true)
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        val intentApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_RAPPEL_ID", id)
        }
        val pendingApp = PendingIntent.getActivity(
            context,
            id,
            intentApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, RappelActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("ID", id)
        }
        val pendingSnooze = PendingIntent.getBroadcast(
            context,
            id + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, RappelActionReceiver::class.java).apply {
            action = "ACTION_DELETE"
            putExtra("ID", id)
        }
        val pendingDelete = PendingIntent.getBroadcast(
            context,
            id + 2000,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titre)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)

            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingApp)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_delete), pendingDelete)
            .addAction(0, context.getString(R.string.action_snooze), pendingSnooze)
            .build()

        notificationManager.notify(id, notification)
    }
}