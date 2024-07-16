package com.platform.feature.utils

import android.util.Log

class LogUtil {
    companion object {
        private const val TAG = "LogUtils"

        fun d(msg: String?) {
            Log.d(TAG, msg!!)
        }

        fun d(tag: String?, msg: String?) {
            Log.d(tag, msg!!)
        }

        fun e(msg: String?) {
            Log.e(TAG, msg!!)
        }

        fun e(tag: String?, msg: String?) {
            Log.e(tag, msg!!)
        }

        fun e(tag: String?, msg: String?, tr: Throwable) {
            Log.e(tag, msg!!, tr)
        }

        fun i(msg: String?) {
            Log.i(TAG, msg!!)
        }

        fun i(tag: String?, msg: String?) {
            Log.i(tag, msg!!)
        }

        fun w(tag: String?, msg: String?) {
            Log.w(tag, msg!!)
        }

        fun v(tag: String?, msg: String?) {
            Log.v(tag, msg!!)
        }
    }
}