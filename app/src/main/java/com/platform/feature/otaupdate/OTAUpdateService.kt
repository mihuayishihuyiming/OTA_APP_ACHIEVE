package com.platform.feature.otaupdate

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.platform.feature.R
import com.platform.feature.otaupdate.ROTAUpdateManager.ValueCallBack
import com.platform.feature.utils.DialogUtil
import com.platform.feature.utils.LogUtil
import com.platform.feature.utils.NotificationUtil
import java.io.File


class OTAUpdateService : Service(), ValueCallBack {
    private var mROTAUpdateManager: ROTAUpdateManager? = null
    private var mHandler: Handler? = null

    private val channelId = "ota_channel_id"
    private val name: CharSequence = "OTA Update"
    private val description: String = "OTA Update"
    private var mBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? =
        null
    private val notificationId = 1001
    private var mPathString: String? = null
    private var waitTime = 1
    private val maxWaitTime = 10


    private val checkOtaFileRunnable = object : Runnable {
        override fun run() {
            if (waitTime >= maxWaitTime) {
                notificationManager!!.cancel(notificationId)
                return
            }
            waitTime++
            LogUtil.d(TAG, "check ota file is exists!")
            val otaFile = mPathString?.let { File(it) }
            if (otaFile!!.exists()) {
                startOTAUpdate(applicationContext, mHandler, mPathString)
            } else {
                LogUtil.d(TAG, "ota file isn't exists! waiting....")
                mFileHandler.removeCallbacks(this)
                mFileHandler.postDelayed(this, MESSAGE_DELAY_WAITING_OTA_FILE.toLong())
            }
        }
    }

    val mFileHandler: Handler = Handler()

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {

        mHandler = Handler(Looper.getMainLooper())
        startForeground(notificationId, buildNotification())
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        super.onCreate()
    }


    private fun buildNotification(): Notification {
        mBuilder = NotificationUtil.createNotification(
            this,
            channelId,
            notificationId,
            name,
            description,
            getString(R.string.update_title),
            R.drawable.icon_update
        )
        return mBuilder!!.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            //The service is restarted after being killed by the system
            return START_NOT_STICKY
        }
        LogUtil.d(TAG, "onStartCommand")
        mPathString = intent.getStringExtra("path")
        if (mROTAUpdateManager == null) {
            mPathString?.let { fileExists(it) }
        } else {
            mROTAUpdateManager!!.unbindUpdate()
            mROTAUpdateManager!!.cancelUpdate()
            mPathString?.let { fileExists(it) }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun fileExists(path: String) {
        val toFile = File(path)
        if (!toFile.exists()) {
            LogUtil.d(TAG, "no-exists path:$path")
            mFileHandler.postDelayed(
                checkOtaFileRunnable,
                MESSAGE_DELAY_WAITING_OTA_FILE.toLong()
            )
        } else {
            startOTAUpdate(applicationContext, mHandler, path)
        }
    }

    override fun onDestroy() {
        notificationManager!!.cancel(notificationId)
        super.onDestroy()
    }

    /**
     * Start the OTA upgrade.
     *
     * @param context The context.
     * @param handler The handler.
     * @param path    The path of the OTA file.
     */
    fun startOTAUpdate(
        context: Context?,
        handler: Handler?,
        path: String?
    ) {
        try {
            if (mROTAUpdateManager != null) {
                mROTAUpdateManager = null
            }
            mROTAUpdateManager = ROTAUpdateManager(context!!, handler!!, path)
            mROTAUpdateManager!!.setCallBack(this)
            mROTAUpdateManager!!.startUpdateSystem()
            LogUtil.d(TAG, "start ota upgrade")
        } catch (e: Exception) {
            notificationManager!!.cancel(notificationId)
            throw RuntimeException(e)
        }
    }


    /**
     * Refresh the OTA status.
     *
     * @param otaStatus The OTA status code.
     * @param progress  The progress of the OTA update.
     */
    override fun refreshOTAStatus(otaStatus: Int, progress: Int) {
        LogUtil.i(TAG, "refreshOTAStatusOTAStatus:$otaStatus,progress:$progress")
        when (otaStatus) {
            UpdateStatusConstants.FAIL ,UpdateStatusConstants.IDLE-> {
                notificationManager!!.cancel(notificationId)
            }

            UpdateStatusConstants.UPDATED_NEED_REBOOT -> {
                mHandler!!.postDelayed({ DialogUtil.createRebootDialog(REBOOT_REASON, this) }, 2000)
                LogUtil.i(TAG, "go to reboot")
            }

            UpdateStatusConstants.DOWNLOADING, UpdateStatusConstants.VERIFYING -> {
                mBuilder!!.setProgress(100, progress, false)
                mBuilder!!.setContentText(getString(R.string.ota_downloading, progress))
                notificationManager!!.notify(notificationId, mBuilder!!.build())
            }

            UpdateStatusConstants.FINALIZING -> {
                mBuilder!!.setProgress(100, progress, false)
                mBuilder!!.setContentText(getString(R.string.ota_finalizing, progress))
                notificationManager!!.notify(notificationId, mBuilder!!.build())
            }

            else -> {
                mBuilder!!.setContentText(getString(R.string.ota_runing))
                notificationManager!!.notify(notificationId, mBuilder!!.build())
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        mROTAUpdateManager!!.otaSuspend()
        mBuilder = NotificationUtil.createNotification(
            this,
            channelId,
            notificationId,
            name,
            description,
            getString(R.string.update_title),
            R.drawable.icon_update
        )
        mROTAUpdateManager!!.otaResume()
        mBuilder!!.setContentText(getString(R.string.ota_runing))
        notificationManager!!.notify(notificationId, mBuilder!!.build())
        LogUtil.d(TAG, "onConfigurationChanged")
//        startOTAUpdate(applicationContext, mHandler, mPathString)
    }


    object UpdateStatusConstants {
        /**
         * Update status code: update  is failed.
         */
        const val FAIL = -1

        /**
         * Update status code: update engine is in idle state.
         */
        const val IDLE = 0

        /**
         * Update status code: update engine is checking for update.
         */
        const val CHECKING_FOR_UPDATE = 1

        /**
         * Update status code: an update is available.
         */
        const val UPDATE_AVAILABLE = 2

        /**
         * Update status code: update engine is downloading an update.
         */
        const val DOWNLOADING = 3

        /**
         * Update status code: update engine is verifying an update.
         */
        const val VERIFYING = 4

        /**
         * Update status code: update engine is finalizing an update.
         */
        const val FINALIZING = 5

        /**
         * Update status code: an update has been applied and is pending for
         * reboot.
         */
        const val UPDATED_NEED_REBOOT = 6

        /**
         * Update status code: update engine is reporting an error event.
         */
        const val REPORTING_ERROR_EVENT = 7

        /**
         * Update status code: update engine is attempting to rollback an
         * update.
         */
        const val ATTEMPTING_ROLLBACK = 8

        /**
         * Update status code: update engine is in disabled state.
         */
        const val DISABLED = 9
    }

    companion object {
        private const val TAG = "OTAUpdateService"
        private const val REBOOT_REASON = "reboot-ab-update"
        private const val MSG_WAITING_OTA_FILE_READY = 1
        private const val MESSAGE_DELAY_WAITING_OTA_FILE = 300 // wait time (ms)

    }
}