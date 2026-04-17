package com.example.ridebuddy.data.offline

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Define a dedicated app-specific directory for offline data
    private val offlineDataDir: File by lazy {
        val dir = File(context.getExternalFilesDir(null), "offline_data")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * Gets the path to the offline data directory.
     * @return The directory where Mapsforge and BRouter offline files are stored.
     */
    fun getOfflineDataDirectory(): File {
        return offlineDataDir
    }

    /**
     * Checks if the required .map and .rd5 files for a specific region exist.
     * @param mapFileName The name of the map file (e.g., "germany.map")
     * @param routingFileName The name of the routing segment file (e.g., "E5_N45.rd5")
     * @return True if both files exist, false otherwise.
     */
    fun checkRegionFilesExist(mapFileName: String, routingFileName: String): Boolean {
        return checkFileExists(mapFileName) && checkFileExists(routingFileName)
    }

    /**
     * Checks if a specific file exists in the offline data directory.
     * @param fileName The name of the file to check.
     * @return True if the file exists, false otherwise.
     */
    fun checkFileExists(fileName: String): Boolean {
        val file = File(offlineDataDir, fileName)
        return file.exists() && file.isFile
    }

    /**
     * Returns a File object pointing to a file in the offline data directory.
     * @param fileName The name of the file.
     * @return The File object.
     */
    fun getOfflineFile(fileName: String): File {
        return File(offlineDataDir, fileName)
    }
}
