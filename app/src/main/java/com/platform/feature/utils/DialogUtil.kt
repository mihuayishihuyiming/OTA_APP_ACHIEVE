package com.platform.feature.utils

import android.app.AlertDialog
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import com.platform.feature.R

object DialogUtil {

    private const val TAG = "DialogUtil"
    fun createRebootDialog(rebootReason: String, context: Context) {
        val mOTAisRebootNowDialog: AlertDialog?
        val mPowerManager: PowerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.is_reboot_now_dialog_title)
        builder.setCancelable(false)
        builder.setMessage(R.string.is_reboot_now_dialog_message)
//        builder.setIconAttribute(android.R.attr.alertDialogIcon)
        builder.setIcon(R.drawable.icon_reboot)
        builder.setPositiveButton(
            com.android.internal.R.string.ok
        ) { dialog, _ ->
            LogUtil.i(TAG, "this is reboot ")
            dialog.cancel()
            mPowerManager.reboot(rebootReason)

        }
        builder.setNegativeButton(
            com.android.internal.R.string.cancel
        ) { dialog, _ ->
            LogUtil.i(TAG, "this is cancel ")
            dialog.cancel()
        }
        mOTAisRebootNowDialog = builder.create()
        mOTAisRebootNowDialog!!.window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        if (!mOTAisRebootNowDialog.isShowing) {
            LogUtil.i(TAG, "dialog will show ")
            mOTAisRebootNowDialog.show()
        }
    }
}