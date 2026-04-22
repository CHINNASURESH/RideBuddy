package com.example.ridebuddy.util

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

object GpxParser {
    suspend fun parse(inputStream: InputStream): List<LatLong> = withContext(Dispatchers.IO) {
        val points = mutableListOf<LatLong>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val name = parser.name
                    if (name == "trkpt" || name == "rtept") {
                        val latStr = parser.getAttributeValue(null, "lat")
                        val lonStr = parser.getAttributeValue(null, "lon")
                        if (latStr != null && lonStr != null) {
                            val lat = latStr.toDoubleOrNull()
                            val lon = lonStr.toDoubleOrNull()
                            if (lat != null && lon != null) {
                                points.add(LatLong(lat, lon))
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream.close()
        }
        points
    }
}
