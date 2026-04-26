package com.example.ridebuddy.data.offline

import android.content.Context
import com.example.ridebuddy.util.AssetExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _mapExtractionReady = MutableStateFlow(false)
    val mapExtractionReady: StateFlow<Boolean> = _mapExtractionReady.asStateFlow()

    /**
     * Extracts a given file from assets to the offline data directory.
     * Emits true to mapExtractionReady StateFlow on success.
     */
    suspend fun extractMapAsset(assetFileName: String) {
        val success = AssetExtractor.extractAssetIfNeeded(context, assetFileName, offlineDataDir)
        if (success && assetFileName == "germany.map") {
            _mapExtractionReady.value = true
        }
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
