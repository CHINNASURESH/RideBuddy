package com.example.ridebuddy.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object AssetExtractor {

    /**
     * Copies a specified file from the assets directory to the given destination directory.
     * Skips copying if the file already exists.
     *
     * @param context The application context.
     * @param assetFileName The name of the file in the assets directory.
     * @param destinationDir The directory where the file should be copied.
     * @return A Boolean indicating whether the file is ready (either newly copied or already existed).
     */
    suspend fun extractAssetIfNeeded(
        context: Context,
        assetFileName: String,
        destinationDir: File
    ): Boolean = withContext(Dispatchers.IO) {
        val destinationFile = File(destinationDir, assetFileName)
        destinationFile.parentFile?.mkdirs()

        if (destinationFile.exists() && destinationFile.isFile) {
            // File already exists, no need to copy
            return@withContext true
        }

        val tmpFile = File(destinationDir, "$assetFileName.tmp")

        try {
            val inputStream: InputStream = context.assets.open(assetFileName)
            val outputStream: OutputStream = FileOutputStream(tmpFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val renameSuccess = tmpFile.renameTo(destinationFile)
            if (!renameSuccess) {
                tmpFile.delete()
                return@withContext false
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            return@withContext false
        }
    }
}
