package com.example.ridebuddy.data.offline

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineDataDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        object Completed : DownloadState()
        data class Error(val exception: Throwable) : DownloadState()
    }

    /**
     * Download a file from the given [urlStr] into the [targetFile].
     * Supports resuming via Range header.
     */
    fun downloadFile(urlStr: String, targetFile: File): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection

            // Check existing file for resuming
            var downloadedBytes = 0L
            if (targetFile.exists()) {
                downloadedBytes = targetFile.length()
                connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
            }

            connection.connect()

            val responseCode = connection.responseCode
            // HTTP_PARTIAL indicates resuming is supported
            val isResuming = responseCode == HttpURLConnection.HTTP_PARTIAL

            // Check if download is already complete or server doesn't support resuming
            if (!isResuming && responseCode != HttpURLConnection.HTTP_OK) {
                // If it's 416 Requested Range Not Satisfiable, maybe we already downloaded the whole thing?
                if (responseCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT || responseCode >= 400) {
                     // Check total content length if possible to see if it's already fully downloaded
                     throw Exception("Server returned HTTP response code: $responseCode")
                }
            }

            val contentLength = connection.contentLengthCompat()
            val totalBytes = if (isResuming) contentLength + downloadedBytes else contentLength

            if (isResuming && totalBytes == downloadedBytes) {
                emit(DownloadState.Completed)
                return@flow
            }

            // If server doesn't support resuming, start from scratch
            if (!isResuming && targetFile.exists()) {
                targetFile.delete()
                downloadedBytes = 0L
            }

            inputStream = connection.inputStream
            outputStream = FileOutputStream(targetFile, isResuming)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int

            var lastProgress = -1

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                if (totalBytes > 0) {
                    val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                    if (progress != lastProgress) {
                        emit(DownloadState.Downloading(progress))
                        lastProgress = progress
                    }
                }
            }

            outputStream.flush()
            emit(DownloadState.Completed)
        } catch (e: Exception) {
            emit(DownloadState.Error(e))
        } finally {
            inputStream?.close()
            outputStream?.close()
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    // Helper to get content length compatibly
    private fun HttpURLConnection.contentLengthCompat(): Long {
        return try {
            getHeaderField("Content-Length")?.toLong() ?: -1L
        } catch (e: NumberFormatException) {
            -1L
        }
    }
}
