package com.duoc.felipesfamily.util

import org.osmdroid.util.GeoPoint
import kotlin.math.cos
import kotlin.math.sin

/**
 * Coordenadas REFERENCIALES por zona (no son ubicaciones exactas).
 * "Zona Sur-2" -> centro del área "Sur" + un desplazamiento fijo según el número,
 * así zonas de la misma área quedan cerca entre sí (igual que en el matching).
 */
object ZoneCoords {
    private val areas = mapOf(
        "norte" to GeoPoint(-29.95, -71.25),
        "centro" to GeoPoint(-34.98, -71.23),
        "sur" to GeoPoint(-38.74, -72.59)
    )

    /** Devuelve null si el área no se reconoce (no se dibuja nada en el mapa). */
    fun of(zone: String): GeoPoint? {
        val clean = zone.trim().removePrefix("Zona").removePrefix("zona").trim()
        val area = clean.substringBeforeLast('-').trim().lowercase()
        val base = areas[area] ?: return null
        val n = clean.substringAfterLast('-', "").trim().toIntOrNull() ?: return base
        val angle = Math.toRadians(n * 72.0)
        return GeoPoint(base.latitude + 0.15 * sin(angle), base.longitude + 0.15 * cos(angle))
    }
}
