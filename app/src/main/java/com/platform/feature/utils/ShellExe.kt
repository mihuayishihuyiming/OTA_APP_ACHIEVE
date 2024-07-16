package com.platform.feature.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset

object ShellExe {
    private const val TAG = "Shellexe"
    const val ERROR = "ERROR"
    const val RESULT_SUCCESS = 0
    const val RESULT_FAIL = -1
    const val RESULT_EXCEPTION = -2
    private val sResultBuilder = StringBuilder("")
    val output: String
        /**
         * Get shell command output
         *
         * @return Shell command output
         */
        get() = sResultBuilder.toString()

    /**
     * Execute shell command
     * @param command Command string need to execute
     * @return Result
     * @throws IOException Throws when occurs #IOException
     */
    @Throws(IOException::class)
    fun execCommand(command: String): Int {
        return execCommand(arrayOf("/system/bin/sh", "-c", command))
    }

    /**
     * Execute shell command
     * @param command Shell command array
     * @return Result
     * @throws IOException Throws when occurs #IOException
     */
    @Throws(IOException::class)
    fun execCommand(command: Array<String>?): Int {
        var result = RESULT_FAIL
        val runtime = Runtime.getRuntime()
        val proc = runtime.exec(command)
        var bufferedReader: BufferedReader? = null
        sResultBuilder.delete(0, sResultBuilder.length)
        try {
            bufferedReader = BufferedReader(
                InputStreamReader(
                    proc
                        .inputStream, Charset.defaultCharset()
                )
            )
            result = if (proc.waitFor() == 0) {
                val line = bufferedReader.readLine()
                if (line != null) {
                    sResultBuilder.append(line)
                }
                RESULT_SUCCESS
            } else {
                sResultBuilder.append(ERROR)
                RESULT_FAIL
            }
        } catch (e: InterruptedException) {
            sResultBuilder.append(ERROR)
            result = RESULT_EXCEPTION
        } finally {
            if (null != bufferedReader) {
                try {
                    bufferedReader.close()
                } catch (e: IOException) {
                }
            }
        }
        return result
    }
}