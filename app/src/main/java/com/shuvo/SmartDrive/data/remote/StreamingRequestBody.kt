package com.shuvo.SmartDrive.data.remote

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException

/**
 * Custom OkHttp RequestBody that streams an InputStream directly from Android's ContentResolver
 * to the network socket in real-time, emitting progress percentages.
 *
 * CRITICAL ZERO-STORAGE REQUIREMENT:
 * This completely avoids buffering to disk (no cacheDir or filesDir copies).
 * It reads in small chunks (64 KB) from the content provider directly into the network sink.
 */
class StreamingRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mimeType: String,
    private val totalLength: Long,
    private val onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? {
        return mimeType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return totalLength
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Failed to open InputStream for URI: $uri")

        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesWritten = 0L

        inputStream.use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                sink.write(buffer, 0, bytesRead)
                bytesWritten += bytesRead
                onProgress(bytesWritten, totalLength)
            }
            sink.flush()
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024 // 64 KB chunk
    }
}

