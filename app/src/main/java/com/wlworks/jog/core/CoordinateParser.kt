package com.wlworks.jog.core

/**
 * 解析使用者輸入是否為座標。支援：
 *   25.0330, 121.5654
 *   25.0330 121.5654
 *   25°2'0"N 121°33'0"E
 * 解析不出來就回 null，交給 Geocoder 當地名處理。
 */
object CoordinateParser {

    /** 十進位格式：兩個數字，中間以逗號或空白分隔。 */
    private val DECIMAL = Regex(
        """^\s*([+-]?\d{1,3}(?:\.\d+)?)\s*[,\s]\s*([+-]?\d{1,3}(?:\.\d+)?)\s*$"""
    )

    /** 度分秒格式：度、分、秒加上半球字母，緯度在前、經度在後。 */
    private val DMS = Regex(
        """^\s*(\d{1,3})[°:\s]\s*(\d{1,2})['′:\s]\s*([\d.]+)["″]?\s*([NSns])\s*[,\s]\s*""" +
            """(\d{1,3})[°:\s]\s*(\d{1,2})['′:\s]\s*([\d.]+)["″]?\s*([EWew])\s*$"""
    )

    /** 把度／分／秒三段併成十進位度數，南半球與西半球取負值。 */
    private fun dms(d: String, m: String, s: String, negative: Boolean): Double? {
        val deg = d.toDoubleOrNull() ?: return null
        val min = m.toDoubleOrNull() ?: return null
        val sec = s.toDoubleOrNull() ?: return null
        val value = deg + min / 60.0 + sec / 3600.0
        return if (negative) -value else value
    }

    /** 依序試十進位與 DMS 兩種格式，都不符或超出經緯度範圍就回 null。 */
    fun parse(input: String): LatLng? {
        DECIMAL.find(input)?.let { m ->
            val lat = m.groupValues[1].toDoubleOrNull() ?: return@let
            val lng = m.groupValues[2].toDoubleOrNull() ?: return@let
            return LatLng(lat, lng).takeIf { it.isValid() }
        }
        DMS.find(input)?.let { m ->
            val g = m.groupValues
            val lat = dms(g[1], g[2], g[3], g[4].equals("S", true))
            val lng = dms(g[5], g[6], g[7], g[8].equals("W", true))
            if (lat != null && lng != null) return LatLng(lat, lng).takeIf { it.isValid() }
        }
        return null
    }
}
