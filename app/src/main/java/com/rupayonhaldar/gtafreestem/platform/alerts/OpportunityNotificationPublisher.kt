package com.rupayonhaldar.gtafreestem.platform.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rupayonhaldar.gtafreestem.MainActivity
import com.rupayonhaldar.gtafreestem.R
import com.rupayonhaldar.gtafreestem.platform.navigation.AppDeepLink
import com.rupayonhaldar.gtafreestem.ui.shell.PrimaryDestination

object OpportunityNotificationPublisher {
    const val CHANNEL_ID = "new_opportunity_matches"

    fun ensureChannel(context: Context, localizedName: String? = null) {
        val appContext = context.applicationContext
        val channel = NotificationChannel(
            CHANNEL_ID,
            localizedName?.trim()?.takeIf(String::isNotEmpty)
                ?: appContext.getString(R.string.opportunity_alerts_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.opportunity_alerts_channel_description)
        }
        appContext.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun hasRuntimePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun canPost(context: Context): Boolean {
        if (!hasRuntimePermission(context) ||
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = context.getSystemService(NotificationManager::class.java)
            ?.getNotificationChannel(CHANNEL_ID)
        return channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Removes alert notifications after opt-out or local-data deletion. */
    fun clearPostedNotifications(context: Context): Boolean = runCatching {
        NotificationManagerCompat.from(context.applicationContext).cancelAll()
        true
    }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun post(
        context: Context,
        copy: OpportunityNotificationCopy,
        newCount: Int,
        epochMillis: Long,
    ): Boolean {
        val appContext = context.applicationContext
        if (!canPost(appContext)) return false
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            AppDeepLink.uri(PrimaryDestination.OPPORTUNITIES),
            appContext,
            MainActivity::class.java,
        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_REQUEST_CODE,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_search)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setNumber(newCount)
            .build()
        val notificationId = NOTIFICATION_ID_BASE +
            ((epochMillis / 1_000L) % NOTIFICATION_ID_RANGE).toInt()
        return runCatching {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        }.isSuccess
    }

    private const val NOTIFICATION_REQUEST_CODE = 8_101
    private const val NOTIFICATION_ID_BASE = 10_000
    private const val NOTIFICATION_ID_RANGE = 1_000_000
}
