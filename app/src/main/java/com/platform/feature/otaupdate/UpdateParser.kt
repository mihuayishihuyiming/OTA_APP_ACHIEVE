package com.platform.feature.otaupdate

import com.platform.feature.utils.LogUtil
import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.Locale
import java.util.zip.ZipFile
import kotlin.streams.toList

/** Parse an A/B update zip file.  */
object UpdateParser {
    private const val TAG = "ROTAUpdateManager"
    private const val PAYLOAD_BIN_FILE = "payload.bin"
    private const val PAYLOAD_PROPERTIES = "payload_properties.txt"
    private const val FILE_URL_PREFIX = "file://"
    private const val ZIP_FILE_HEADER = 30

    /**
     * Parse a zip file containing a system update and return a non null ParsedUpdate.
     */
    @Throws(IOException::class)
    fun parse(file: File): ParsedUpdate {
        var payloadOffset: Long = 0
        var payloadSize: Long = 0
        var payloadFound = false
        var props: Array<String>? = null
        ZipFile(file).use { zipFile ->
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val fileSize = entry.compressedSize
                if (!payloadFound) {
                    payloadOffset += (ZIP_FILE_HEADER + entry.name.length).toLong()
                    if (entry.extra != null) {
                        payloadOffset += entry.extra.size.toLong()
                    }
                }
                if (entry.isDirectory) {
                    continue
                } else if (entry.name == PAYLOAD_BIN_FILE) {
                    payloadSize = fileSize
                    payloadFound = true
                } else if (entry.name == PAYLOAD_PROPERTIES) {
                    zipFile.getInputStream(entry).bufferedReader().use { bufferedReader ->
                        props = bufferedReader.lines().toList().toTypedArray()
                    }
                }
                if (!payloadFound) {
                    payloadOffset += fileSize
                }
                LogUtil.d(
                    TAG,
                    String.format("Entry %s", entry.name)
                )
            }
        }
        return ParsedUpdate(file, payloadOffset, payloadSize, props)
    }

    /** Information parsed from an update file.  */
    class ParsedUpdate(
        file: File,
        val mOffset: Long,
        val mSize: Long,
        val mProps: Array<String>?
    ) {
        val mUrl: String

        init {
            mUrl = FILE_URL_PREFIX + file.absolutePath
        }

        val isValid: Boolean
            /** Verify the update information is correct.  */
            get() = mOffset >= 0 && mSize > 0 && mProps != null

        override fun toString(): String {
            return String.format(
                Locale.getDefault(),
                "ParsedUpdate: URL=%s, offset=%d, size=%s, props=%s",
                mUrl, mOffset, mSize, Arrays.toString(mProps)
            )
        }
    }
}