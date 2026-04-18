package com.example.ridebuddy.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.ridebuddy.data.local.RidePoint
import com.example.ridebuddy.data.local.RideSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxExporter {
    suspend fun exportRideSession(
        context: Context,
        session: RideSession,
        points: List<RidePoint>
    ): Boolean = withContext(Dispatchers.IO) {
        if (points.isEmpty()) return@withContext false

        val gpxContent = buildGpxContent(session, points)
        val fileName = "RideBuddy_Session_${session.id}_${System.currentTimeMillis()}.gpx"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/gpx+xml")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(gpxContent.toByteArray(Charsets.UTF_8))
                    }
                    true
                } else {
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = java.io.File(downloadsDir, fileName)
                file.outputStream().use { outputStream ->
                    outputStream.write(gpxContent.toByteArray(Charsets.UTF_8))
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildGpxContent(session: RideSession, points: List<RidePoint>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val stringBuilder = java.lang.StringBuilder()
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        stringBuilder.append("<gpx version=\"1.1\" creator=\"RideBuddy\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

        stringBuilder.append("  <metadata>\n")
        stringBuilder.append("    <time>${dateFormat.format(Date(session.startTime))}</time>\n")
        stringBuilder.append("  </metadata>\n")

        stringBuilder.append("  <trk>\n")
        stringBuilder.append("    <name>RideBuddy Session ${session.id}</name>\n")
        stringBuilder.append("    <trkseg>\n")

        for (point in points) {
            stringBuilder.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
            if (point.elevation != 0.0) {
                stringBuilder.append("        <ele>${point.elevation}</ele>\n")
            }
            stringBuilder.append("        <time>${dateFormat.format(Date(point.timestamp))}</time>\n")
            stringBuilder.append("      </trkpt>\n")
        }

        stringBuilder.append("    </trkseg>\n")
        stringBuilder.append("  </trk>\n")
        stringBuilder.append("</gpx>\n")

        return stringBuilder.toString()
    }
}
