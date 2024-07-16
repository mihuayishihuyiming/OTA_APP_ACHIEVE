package com.platform.feature.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import com.platform.feature.settings.SettingsManager.Companion.getSystemString
import com.platform.feature.settings.SettingsManager.Companion.putSystemString
import java.io.File
import java.io.FileWriter
import java.io.IOException

object KeyUtil {
    const val TAG = "KeyUtil"

    const val KEY_SIDE = "KEY_SIDE"
    private const val FILE_SIDE = "/sys/devices/platform/device_info/KEY_SIDE"
    private const val KEY_SIDE_MAP = "side_map"
    private const val PROPERTY_SIDE = "debug.map.keyside"

    private const val KEY_P1 = "KEY_P1"
    private const val KEY_P1_MAP = "p1_map"
    private const val PROPERTY_P1 = "debug.map.keyp1"
    private const val FILE_P1 = "/sys/devices/platform/device_info/KEY_P1"

    const val KEY_P2 = "KEY_P2"
    private const val KEY_P2_MAP = "p2_map"
    private const val PROPERTY_P2 = "debug.map.keyp2"
    private const val FILE_P2 = "/sys/devices/platform/device_info/KEY_P2"

    const val KEY_F1 = "KEY_F1"
    private const val KEY_F1_MAP = "f1_map"
    private const val PROPERTY_F1 = "debug.map.keyf1"
    private const val FILE_F1 = "/sys/devices/platform/device_info/KEY_F1"

    private const val KEYCODE_PREFIX = "KEYCODE_"
    private const val ERROR = "ERROR"

    val customKeyMappings = mapOf(
        "305" to "KEYCODE_P1",
        "306" to "KEYCODE_P2",
        "307" to "KEYCODE_F13",
        "308" to "KEYCODE_SWITCH_ABC_123",
        "309" to "KEYCODE_POGO",
        "310" to "KEYCODE_FN"
    )

    fun setKeyCode(mContext: Context, key: String, Keyvalue: Int) {
        val scanCode = getScanCode(Keyvalue.toString())
        if (!TextUtils.isEmpty(scanCode)) {
            if (key == KEY_SIDE) {
                putSystemString(mContext.contentResolver, PROPERTY_SIDE, scanCode)
                writeFile(KEY_SIDE_MAP, scanCode)
            } else if (key == KEY_P1) {
                putSystemString(mContext.contentResolver, PROPERTY_P1, scanCode)
                writeFile(KEY_P1_MAP, scanCode)
            } else if (key == KEY_P2) {
                putSystemString(mContext.contentResolver, PROPERTY_P2, scanCode)
                writeFile(KEY_P2_MAP, scanCode)
            } else if (key == KEY_F1) {
                putSystemString(mContext.contentResolver, PROPERTY_F1, scanCode)
                writeFile(KEY_F1_MAP, scanCode)
            }
        }
    }

    fun getKeyCode(mContext: Context, key: String): Int {
        var keyvalue = "-1"
        if (key == KEY_SIDE) {
            keyvalue = getSystemString(mContext.contentResolver, PROPERTY_SIDE, "-1")
        } else if (key == KEY_P1) {
            keyvalue = getSystemString(mContext.contentResolver, PROPERTY_P1, "-1")
        } else if (key == KEY_P2) {
            keyvalue = getSystemString(mContext.contentResolver, PROPERTY_P2, "-1")
        } else if (key == KEY_F1) {
            keyvalue = getSystemString(mContext.contentResolver, PROPERTY_F1, "-1")
        }
        return getKeyCode(Integer.parseInt(keyvalue))
    }

    fun queryKeyCode(Keyvalue: Int):Boolean {
        val scanCode = getScanCode(Keyvalue.toString())
        if (!TextUtils.isEmpty(scanCode)) {
            return true
        }
        return false
    }

    fun getScanCode(value: String): String {
        var scanCode = ""
        val keyCodePrefix = "KEYCODE_"
        var keyCodeName = KeyEvent.keyCodeToString(value.toInt())
        if (customKeyMappings.containsKey(value)) {
            // If the key value is customized, the driver key value is directly obtained
            keyCodeName = customKeyMappings[value]!!
        }
        keyCodeName = keyCodeName.replace(keyCodePrefix, "")
        LogUtil.d(TAG, "getCode = " + getCode(keyCodeName))
        if ("ERROR" != getCode(keyCodeName)) {
            var temp: Array<String>? = null
            temp = getCode(keyCodeName).split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            scanCode = temp[1]
            var valid = false
            if (TextUtils.isDigitsOnly(scanCode)) {
                val scanValue = scanCode.toInt()
                if (scanValue == 580 || scanValue >= 0 && scanValue < 500) {
                    valid = true
                }
            }
            if (!valid) {
                scanCode = ""
            }
        } else {
            scanCode = ""
        }
        LogUtil.d(
            TAG,
            "scanCode = $scanCode"
        )
        return scanCode
    }

    private fun getCode(cmd: String): String {
        var result: String? = null
        result = try {
            val cmdx = arrayOf(
                "/system/bin/sh", "-c",
                "cat system/usr/keylayout/Generic.kl | grep -w ' $cmd'"
            )
            val ret = ShellExe.execCommand(cmdx)
            if (0 == ret) {
                ShellExe.output
            } else {
                ShellExe.output
            }
        } catch (e: IOException) {
            "ERROR"
        }
        return result!!.trim { it <= ' ' }
    }

    fun getKeyCode(scanCode: Int): Int {
        val scancode = scanCode.toString()
        val keyName = getKeyName(scancode)
        return if (keyName.contains(scancode)) {
            val keycodename =
                keyName.substring(keyName.indexOf(scancode) + scancode.length).trim { it <= ' ' }
            getKeyCodeValue(KEYCODE_PREFIX + keycodename)
        } else {
            -1
        }
    }

    fun getKeyName(scanCode: String): String {
        var keyName = ""
        try {
            val cmdx = arrayOf(
                "/system/bin/sh", "-c",
                "cat system/usr/keylayout/Generic.kl | grep -w $scanCode"
            )
            val ret = ShellExe.execCommand(cmdx)
            if (ret == 0) {
                val result: String = ShellExe.output
                // Assuming the result contains the key name
                // You may need to parse the result based on your file format
                keyName = result.trim { it <= ' ' }
                LogUtil.d(TAG, "Key name retrieved: $keyName")
            }
        } catch (e: IOException) {
            keyName = ERROR
            LogUtil.e(
                TAG, "Error getting key name for scan code $scanCode", e
            )
        }
        return keyName
    }

    private fun getKeyCodeValue(keyCodeName: String): Int {
        return if (ERROR == keyCodeName) {
            -1
        } else try {
            val field = KeyEvent::class.java.getField(keyCodeName)
            field.getInt(null)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
            -1
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            -1
        }
    }

    @Synchronized
    fun writeFile(prefKey: String, value: String?) {
        var path = FILE_SIDE
        if (KEY_SIDE_MAP == prefKey) {
            path = FILE_SIDE
        } else if (KEY_P1_MAP == prefKey) {
            path = FILE_P1
        } else if (KEY_P2_MAP == prefKey) {
            path = FILE_P2
        } else if (KEY_F1_MAP == prefKey) {
            path = FILE_F1
        }
        try {
            val file = File(path)
            val fw = FileWriter(file)
            fw.write(value)
            fw.flush()
            fw.close()
        } catch (e: Exception) {
            LogUtil.d(TAG, "writeFile error = $e")
            e.printStackTrace()
        }
    }
}