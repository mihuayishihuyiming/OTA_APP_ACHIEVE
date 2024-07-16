package com.platform.feature.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtil {

    /**
     * Generate a notification with the given parameters.
     *
     * @param context The application context.
     * @param channelId The ID of the notification channel.
     * @param notificationId The ID of the notification.
     * @param name The name of the notification channel.
     * @param description The description of the notification channel.
     * @param contentTitle The title of the notification content.
     * @param smallIcon The resource ID of the small icon for the notification.
     * @return The notification builder if the notification is successfully created, null otherwise.
     */
    fun createNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        name: CharSequence,
        description: String,
        contentTitle: String,
        smallIcon: Int
    ): NotificationCompat.Builder? {
        val builder: NotificationCompat.Builder?
        val channel: NotificationChannel?
        val notification: NotificationManagerCompat?
        builder = NotificationCompat.Builder(context, channelId)
        builder.setContentTitle(contentTitle).setSmallIcon(smallIcon)
        builder.setOnlyAlertOnce(true)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        channel = NotificationChannel(channelId, name, importance)
        channel.description = description

        val notificationManager: NotificationManager? =
            context.getSystemService(NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
        // Show the notification
        notification = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null
        }
        builder.build().flags = Notification.FLAG_FOREGROUND_SERVICE
        builder.setOngoing(true)
        notification.notify(notificationId, builder.build())
        return builder
    }


}