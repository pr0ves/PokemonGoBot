package ink.abb.pogo.scraper.util.directions

import com.pokegoapi.google.common.geometry.S1Angle
import com.pokegoapi.google.common.geometry.S2LatLng
import ink.abb.pogo.scraper.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

var routeProvider = "http://valhalla.mapzen.com/route"


fun getRoutefile(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    val connection = URL(createURLString(olat, olng, dlat, dlng)).openConnection() as HttpURLConnection
    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    connection.setRequestProperty("Accept-Language", "en")
    connection.setRequestProperty("Cache-Control", "max=0")
    connection.setRequestProperty("Connection", "keep-alive")
    connection.setRequestProperty("DNT", "1")
    connection.setRequestProperty("Host", "valhalla.mapzen.com")
    connection.setRequestProperty("Upgrade-Insecure-Requests", "1")
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36")
    var routeFile = String()
    try {
        connection.inputStream.bufferedReader().lines().forEach {
            routeFile += "$it\n"
        }
    } catch (e: Exception) {
        Log.red("Error fetching route from provider: " + e.message)
    }

    return routeFile
}


fun createURLString(olat: Double, olng: Double, dlat: Double, dlng: Double): String {
    return "$routeProvider?json={\"locations\":[{\"lat\":$olat,\"lon\":$olng,\"type\":\"break\"},{\"lat\":$dlat,\"lon\":$dlng,\"type\":\"break\"}]," +
            "\"costing\":\"pedestrian\"}&api_key=valhalla-fcJdEyF"
}

fun getRouteCoordinates(olat: Double, olng: Double, dlat: Double, dlng: Double): ArrayList<S2LatLng> {
    val route = getRoutefile(olat, olng, dlat, dlng)
    if (route.length > 0 && route.contains("\"status\":0")) {
        return decodePolyline(route.split("\"shape\":\"")[1].split("\"")[0])
    } else {
        return ArrayList()
    }
}

fun decodePolyline(encoded: String): ArrayList<S2LatLng> {

    println(encoded)
    val poly = ArrayList<S2LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {

        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].toInt() - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        println(index)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = S2LatLng(S1Angle.degrees(lat.toDouble() / 1E6), S1Angle.degrees(lng.toDouble() / 1E6))
        poly.add(p)
    }

    return poly
}

fun getRouteCoordinates(start: S2LatLng, end: S2LatLng): ArrayList<S2LatLng> {
    return getRouteCoordinates(start.latDegrees(), start.lngDegrees(), end.latDegrees(), end.lngDegrees())
}
