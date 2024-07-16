package com.platform.feature.otaupdate

import android.content.Context
import android.os.Handler
import android.os.PowerManager
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.provider.Settings
import android.widget.Toast
import com.platform.feature.R
import com.platform.feature.utils.LogUtil
import java.io.File
import java.lang.Float.valueOf

/**
 * OTA update manager
 * @param mContext context
 * @param mHandler handler
 * @param updateFilePath file path of the update file
 */
class ROTAUpdateManager(
    private val mContext: Context,
    private val mHandler: Handler,
    updateFilePath: String?
) {

    private var mUpdateEngine: UpdateEngine? = null
    private val parsedUpdate: UpdateParser.ParsedUpdate
    private var mWakeLock: PowerManager.WakeLock? = null
    private val otaDir: File
    private var mPowerManager: PowerManager? = null

    private var mUpdateEngineCallback: UpdateEngineCallback? = null
    private var isCallbackHandled = false
    private var mCallback: ValueCallBack? = null
    val isDeleteFiles = false

    init {
        if (updateFilePath == null) {
            throw RuntimeException("updateFilePath is null")
        }
        parsedUpdate = UpdateParser.parse(File(updateFilePath))
        LogUtil.i(TAG, "onStatusUpdate status = $parsedUpdate")
        otaDir = File(updateFilePath)
        mPowerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    /**
     * Start the OTA update task.
     * @throws Exception
     */
    @Throws(Exception::class)
    fun startUpdateSystem() {
        if (mUpdateEngine == null) {
            LogUtil.d(TAG, "new  mUpdateEngine")
            mUpdateEngine = UpdateEngine()
        }

        if (isUpdating) {
            // Cancel a previous update task
            cancelUpdate()
        }
        try {
            mWakeLock =
                mPowerManager!!.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OTA:CpuKeepRunning")
            if (mWakeLock != null) {
                //The upgrade time is uncertain, and the wake-up time is not determined
                mWakeLock!!.acquire()
                LogUtil.d(TAG, "otaIsUpgrading wakeLock.acquire()")
            }
            if (mUpdateEngineCallback != null) {
                mUpdateEngineCallback = null
            }
            mUpdateEngineCallback = object : UpdateEngineCallback() {

                /**
                 * Called when the status of the update is updated.
                 *
                 * @param status The status of the update.
                 * @param percent The progress percentage of the update.
                 */
                override fun onStatusUpdate(status: Int, percent: Float) {
                    otaIsUpgrading = status
                    LogUtil.d(TAG, "onStatusUpdate status = $status; percent = $percent")
                    val valueProgress = valueOf(percent * 100).toInt()
                    mCallback!!.refreshOTAStatus(status, valueProgress)
                    handleStatusUpdate(status, percent)
                }

                /**
                 * Called when the payload application is complete.
                 *
                 * @param errorCode The error code if there is an error, or zero if successful.
                 */
                override fun onPayloadApplicationComplete(errorCode: Int) {
                    if (!isCallbackHandled) {
                        LogUtil.e(
                            TAG,
                            "onPayloadApplicationComplete errorCode = $errorCode"
                        )
                        // An error message is displayed
                        handlePayloadApplicationComplete(errorCode)
                        mUpdateEngine!!.unbind()
                        Settings.System.putInt(
                            mContext.contentResolver,
                            XC_OTA_UPDATE_START_STATE,
                            NO_UPDATING
                        )
                        if (isDeleteFiles) {
                            deleteFiles(otaDir)
                        }
                        if (mWakeLock != null) {
                            mWakeLock!!.release()
                            LogUtil.d(TAG, "otaIsUpgraded wakeLock.release()")
                        }
                        isCallbackHandled = true
                    }
                }
            }

            mUpdateEngine!!.bind(mUpdateEngineCallback, mHandler)
            // Apply the payload
            mUpdateEngine!!.applyPayload(
                parsedUpdate.mUrl,
                parsedUpdate.mOffset,
                parsedUpdate.mSize,
                parsedUpdate.mProps
            )
            LogUtil.d(TAG, "otaIsUpgrading mUpdateEngine.applyPayload()")
            isCallbackHandled = false
        } catch (e: Exception) {
            LogUtil.e(TAG, "Ota upgrade failed")
            mUpdateEngine!!.unbind()
            showToast(mContext.resources.getString(R.string.ota_isok))
            mCallback!!.refreshOTAStatus(-1, 100)
            e.printStackTrace()
        }
    }


    fun otaSuspend() {
        LogUtil.d(TAG, "otaSuspend")
        mUpdateEngine!!.suspend()
    }

    fun otaResume() {
        LogUtil.d(TAG, "otaResume")
        mUpdateEngine!!.resume()
    }

    private fun deleteFiles(file: File) {
        if (file.exists() && file.length() == 0L) {
            LogUtil.d(
                TAG,
                "The OTA file directory has not yet been created, please click to get the OTA upgrade path"
            )
        }
        LogUtil.d(TAG, "Delete the OTA path and the files in it")
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (f in files) {
                    deleteFiles(f)
                }
            }
        }
        file.delete()
    }

    private val isUpdating: Boolean
        get() = otaIsUpgrading != -1

    fun cancelUpdate() {
        try {
            if (mUpdateEngine != null) {
                mUpdateEngine!!.cancel()
            }
            LogUtil.d(TAG, "cancel update:")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unbindUpdate() {
        if (mUpdateEngine != null) {
            mUpdateEngine!!.unbind()
        }
    }

    private fun showToast(msg: String) {
        mHandler.post {
            Toast.makeText(
                mContext,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleStatusUpdate(status: Int, percent: Float) {
        when (status) {
            UpdateStatusConstants.UPDATE_AVAILABLE -> {
                showToast(mContext.resources.getString(R.string.ota_begin))
                Settings.System.putInt(
                    mContext.contentResolver,
                    XC_OTA_UPDATE_START_STATE,
                    UPDATING
                )
            }

            UpdateStatusConstants.VERIFYING -> {

            }

            UpdateStatusConstants.DOWNLOADING -> {

            }

            UpdateStatusConstants.FINALIZING -> {
            }

            UpdateStatusConstants.UPDATED_NEED_REBOOT -> {
            }
        }
    }

    interface ValueCallBack {
        fun refreshOTAStatus(otaStatus: Int, progress: Int)
    }

    fun setCallBack(callback: ValueCallBack?) {
        if (mCallback == null) {
            mCallback = callback
        }
    }

    private fun handlePayloadApplicationComplete(errorCode: Int) {
        if (mWakeLock != null) {
            mWakeLock!!.release()
            mWakeLock = null
        }
        when (errorCode) {
            ErrorCodeConstants.SUCCESS -> {
                otaIsUpgrading = -1
            }
        }
        if (errorCode != ErrorCodeConstants.SUCCESS) {
            showToast(mContext.resources.getString(R.string.ota_fail))
        }
    }

    object UpdateStatusConstants {
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

    object ErrorCodeConstants {
        /**
         * Error code: a request finished successfully.
         */
        const val SUCCESS = 0

        /**
         * Error code: a request failed due to a generic error.
         */
        const val ERROR = 1

        /**
         * Error code: an update failed to apply due to filesystem copier
         * error.
         */
        const val FILESYSTEM_COPIER_ERROR = 4

        /**
         * Error code: an update failed to apply due to an error in running
         * post-install hooks.
         */
        const val POST_INSTALL_RUNNER_ERROR = 5

        /**
         * Error code: an update failed to apply due to a mismatching payload.
         */
        const val PAYLOAD_MISMATCHED_TYPE_ERROR = 6

        /**
         * Error code: an update failed to apply due to an error in opening devices.
         */
        const val INSTALL_DEVICE_OPEN_ERROR = 7

        /**
         * Error code: an update failed to apply due to an error in opening kernel device.
         */
        const val KERNEL_DEVICE_OPEN_ERROR = 8

        /**
         * Error code: an update failed to apply due to an error in fetching the payload.
         */
        const val DOWNLOAD_TRANSFER_ERROR = 9

        /**
         * Error code: an update failed to apply due to a mismatch in payload hash.
         */
        const val PAYLOAD_HASH_MISMATCH_ERROR = 10

        /**
         * Error code: an update failed to apply due to a mismatch in payload size.
         */
        const val PAYLOAD_SIZE_MISMATCH_ERROR = 11

        /**
         * Error code: an update failed to apply due to failing to verify payload signatures.
         */
        const val DOWNLOAD_PAYLOAD_VERIFICATION_ERROR = 12

        /**
         * Error code: an update failed to apply due to a downgrade in payload timestamp.
         */
        const val PAYLOAD_TIMESTAMP_ERROR = 51

        /**
         * Error code: an update has been applied successfully but the new slot
         * hasn't been set to active.
         */
        const val UPDATED_BUT_NOT_ACTIVE = 52

        /**
         * Error code: there is not enough space on the device to apply the update. User should
         * be prompted to free up space and re-try the update.
         */
        const val NOT_ENOUGH_SPACE = 60

        /**
         * Error code: the device is corrupted and no further updates may be applied.
         */
        const val DEVICE_CORRUPTED = 61

        /**
         * Error code: User cancellation Last time update
         */
        const val OTHER_SITUATION = 48
    }

    companion object {
        private const val TAG = "ROTAUpdateManager"
        private var otaIsUpgrading = -1
        private const val XC_OTA_UPDATE_START_STATE = "xcheng.ota.update.starting"
        private const val UPDATING = 1
        private const val NO_UPDATING = 0
    }
}