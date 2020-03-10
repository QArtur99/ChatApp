package com.artf.chatapp.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.graphics.drawable.toBitmap
import com.artf.chatapp.R
import com.artf.chatapp.view.MainActivity

class NotificationUtils {

    companion object {
        private val VERBOSE_CHANNEL_NAME: CharSequence = "Verbose WorkManager Notifications"
        private const val VERBOSE_CHANNEL_DESCRIPTION = "Shows notifications whenever work starts"
        private const val CHANNEL_ID = "VERBOSE_NOTIFICATION"

        fun makeStatusNotification(
            context: Context,
            notificationId: Int,
            userString: String,
            senderName: String?,
            senderPhotoUrl: String?,
            message: String?

        ) {

            val bitmapDrawable =
                AppCompatResources.getDrawable(context, R.drawable.ic_account_circle_black_24dp)
                    ?.toBitmap()

            // Make a channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name = VERBOSE_CHANNEL_NAME
                val description = VERBOSE_CHANNEL_DESCRIPTION
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance)
                channel.description = description

                // Add the channel
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                notificationManager?.createNotificationChannel(channel)
            }

            // Create an Intent for the activity you want to start
            val resultIntent = Intent(context, MainActivity::class.java)
            resultIntent.putExtra("userString", userString)

            val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(resultIntent)
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            // Create the notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentIntent(resultPendingIntent)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(senderName)
                .setContentText(message)
                .setLargeIcon(bitmapDrawable)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(LongArray(0))

            // Show the notification
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }
}
