package com.artf.chatapp.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.graphics.drawable.toBitmap
import com.artf.chatapp.R
import com.artf.chatapp.view.MainActivity
import com.artf.chatapp.notification.data.NewMessageNotification

object NotificationUtils {

    private val VERBOSE_CHANNEL_NAME: CharSequence = "Verbose WorkManager Notifications"
    private const val VERBOSE_CHANNEL_DESCRIPTION = "Shows notifications whenever work starts"
    private const val CHANNEL_ID = "VERBOSE_NOTIFICATION"

    fun makeStatusNotification(
        context: Context,
        newNotification: NewMessageNotification
    ) {
        createNotificationChannel(context)
        val notification = getNotification(context, newNotification)
        NotificationManagerCompat.from(context).notify(newNotification.notificationId, notification)
    }

    private fun getNotification(
        context: Context,
        notification: NewMessageNotification
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentIntent(getPendingIntent(context, notification.userString))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(notification.senderName)
            .setContentText(notification.message)
            .setLargeIcon(getBitmapDrawable(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(LongArray(0))
            .build()
    }

    private fun getPendingIntent(context: Context, userString: String): PendingIntent? {
        val resultIntent = Intent(context, MainActivity::class.java)
        resultIntent.putExtra("userString", userString)
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun getBitmapDrawable(context: Context): Bitmap? {
        return AppCompatResources.getDrawable(context, R.drawable.ic_account_circle_black_24dp)
            ?.toBitmap()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = VERBOSE_CHANNEL_NAME
            val description = VERBOSE_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            getNotificationManager(context)?.createNotificationChannel(channel)
        }
    }

    private fun getNotificationManager(context: Context): NotificationManager? {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    }
}
