package com.platform.feature.otaupdate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.platform.feature.R
import com.platform.feature.utils.LogUtil


class StartOTAUpdateActivity : Activity() {
    private var mHandler: Handler? = null
    private var otaIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_otaupdate)
        mHandler = Handler(Looper.getMainLooper())
        findViewById<View>(R.id.btn_choose_ota).setOnClickListener {
            val updateStatus: Int =
                Settings.System.getInt(contentResolver, OTA_UPDATE_START_STATE, 0)
            if (updateStatus == 1) {
                Toast.makeText(
                    this,
                    R.string.update_is_running,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Create an intent object with the action of getting content
                val intent = Intent(Intent.ACTION_GET_CONTENT);

                // Set the type of the intent to "application/zip"
                intent.type = "application/zip";

                // Add the category of being openable to the intent
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a chooser intent and start the activity with the chosen intent and request code 1
                startActivityForResult(Intent.createChooser(intent, ""), 1);

            }
        }

        findViewById<View>(R.id.reset_update_state).setOnClickListener {
            Settings.System.putInt(contentResolver, OTA_UPDATE_START_STATE, 0)
            Toast.makeText(
                this,
                R.string.update_reset,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /**
     * This method is called when an activity result is returned from another activity or fragment.
     *
     * @param requestCode     The request code associated with the result.
     * @param resultCode      The result code returned by the activity or fragment.
     * @param data            An Intent containing any data passed back.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            assert(data != null)
            val uri = data!!.data
            val pathString: String = UriUtil.getPath(this, uri!!)!!
            LogUtil.d(TAG, "pathString: $pathString")
            if (otaIntent != null) {
                stopService(otaIntent)
            }
            otaIntent = Intent(this, OTAUpdateService::class.java)
            otaIntent!!.putExtra("path", pathString)
            startForegroundService(otaIntent)
        }
    }

    companion object {
        private const val TAG = "SoftwareUpdateActivity"
        private const val OTA_UPDATE_START_STATE = "ota.update.starting"
    }
}