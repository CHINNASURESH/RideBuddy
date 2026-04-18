package com.example.ridebuddy.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object SolarMath {
    /**
     * Calculates the sunset time for a given latitude, longitude, and date.
     * Uses the NOAA Solar Calculator equations.
     *
     * @param latitude The latitude in degrees.
     * @param longitude The longitude in degrees.
     * @param altitude The altitude in meters (optional, slightly affects visible sunset but primarily lat/lon are used here).
     * @param date The current date (or target date).
     * @return The sunset time as a Unix timestamp in milliseconds, or null if there is no sunset on that day (e.g. polar day/night).
     */
    fun calculateSunset(latitude: Double, longitude: Double, altitude: Double = 0.0, date: Calendar = Calendar.getInstance()): Long? {
        val timeZoneOffset = date.timeZone.getOffset(date.timeInMillis) / 3600000.0 // in hours
        val julianDay = getJulianDay(date)
        val julianCentury = (julianDay - 2451545.0) / 36525.0

        val geomMeanLongSun = (280.46646 + julianCentury * (36000.76983 + julianCentury * 0.0003032)) % 360.0
        val geomMeanAnomSun = 357.52911 + julianCentury * (35999.05029 - 0.0001537 * julianCentury)

        val eccentEarthOrbit = 0.016708634 - julianCentury * (0.000042037 + 0.0000001267 * julianCentury)

        val sunEqOfCtr = sin(Math.toRadians(geomMeanAnomSun)) * (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury)) +
                         sin(Math.toRadians(2 * geomMeanAnomSun)) * (0.019993 - 0.000101 * julianCentury) +
                         sin(Math.toRadians(3 * geomMeanAnomSun)) * 0.000289

        val sunTrueLong = geomMeanLongSun + sunEqOfCtr
        val sunAppLong = sunTrueLong - 0.00569 - 0.00478 * sin(Math.toRadians(125.04 - 1934.136 * julianCentury))

        val meanObliqEcliptic = 23.0 + (26.0 + (21.448 - julianCentury * (46.815 + julianCentury * (0.00059 - julianCentury * 0.001813))) / 60.0) / 60.0
        val obliqCorr = meanObliqEcliptic + 0.00256 * cos(Math.toRadians(125.04 - 1934.136 * julianCentury))

        val sunDeclination = Math.toDegrees(asin(sin(Math.toRadians(obliqCorr)) * sin(Math.toRadians(sunAppLong))))

        val varY = tan(Math.toRadians(obliqCorr / 2.0)).pow(2.0)

        val eqOfTime = 4.0 * Math.toDegrees(varY * sin(2.0 * Math.toRadians(geomMeanLongSun)) -
                       2.0 * eccentEarthOrbit * sin(Math.toRadians(geomMeanAnomSun)) +
                       4.0 * eccentEarthOrbit * varY * sin(Math.toRadians(geomMeanAnomSun)) * cos(2.0 * Math.toRadians(geomMeanLongSun)) -
                       0.5 * varY * varY * sin(4.0 * Math.toRadians(geomMeanLongSun)) -
                       1.25 * eccentEarthOrbit * eccentEarthOrbit * sin(2.0 * Math.toRadians(geomMeanAnomSun)))

        // Standard atmospheric refraction and semi-diameter of the sun = 0.833 degrees
        // Altitude correction approximation (roughly 1.92 * sqrt(altitude_in_meters) arcminutes) -> Convert to degrees
        val altitudeCorrectionDegrees = if (altitude > 0) 1.92 * sqrt(altitude) / 60.0 else 0.0
        val solarDepression = 90.833 + altitudeCorrectionDegrees

        val haArg = cos(Math.toRadians(solarDepression)) / (cos(Math.toRadians(latitude)) * cos(Math.toRadians(sunDeclination))) -
                    tan(Math.toRadians(latitude)) * tan(Math.toRadians(sunDeclination))

        if (haArg < -1.0 || haArg > 1.0) {
            return null // No sunset/sunrise
        }

        val haSunset = Math.toDegrees(acos(haArg))
        val solarNoon = (720.0 - 4.0 * longitude - eqOfTime + timeZoneOffset * 60.0) / 1440.0
        val sunsetTime = solarNoon + haSunset * 4.0 / 1440.0

        // sunsetTime is a fraction of a day (0 to 1). Convert to absolute milliseconds.
        // We use the local timezone of the input date, find its midnight, and add the fraction
        val targetDayStart = Calendar.getInstance(date.timeZone).apply {
            timeInMillis = date.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return targetDayStart.timeInMillis + (sunsetTime * 24.0 * 60.0 * 60.0 * 1000.0).toLong()
    }

    private fun getJulianDay(date: Calendar): Double {
        var year = date.get(Calendar.YEAR)
        var month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH) +
                  date.get(Calendar.HOUR_OF_DAY) / 24.0 +
                  date.get(Calendar.MINUTE) / 1440.0 +
                  date.get(Calendar.SECOND) / 86400.0

        if (month <= 2) {
            year -= 1
            month += 12
        }

        val a = year / 100
        val b = 2 - a + a / 4

        return (365.25 * (year + 4716)).toInt() + (30.6001 * (month + 1)).toInt() + day + b - 1524.5
    }
}
