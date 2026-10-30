package com.duoc.felipesfamily.util

import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.ProviderInfo

data class MatchResult(
    val provider: ProviderInfo,
    val score: Int,
    val sameZone: Boolean = false,
    val nearZone: Boolean = false,
    val capacityOk: Boolean = true,
    val available: Boolean = true
)

/**
 * Matching básico (0–100): servicio 40 + zona 30 + capacidad 15 + disponibilidad 15.
 * Solo aparecen prestadores que ofrecen el servicio pedido.
 */
object Matching {
    fun compute(need: NeedEntity, providers: List<ProviderInfo>): List<MatchResult> =
        providers
            .filter { p -> p.services.split(",").any { it.trim().equals(need.service, ignoreCase = true) } }
            .map {
                val zp = zonePoints(need.zone, it.zone)
                MatchResult(
                    it, score(need, it), sameZone = zp == 30, nearZone = zp == 15,
                    capacityOk = it.capacityHaDay >= need.surfaceHa,
                    available = Validators.overlaps(need.period, it.availability)
                )
            }
            .sortedByDescending { it.score }

    private fun score(need: NeedEntity, p: ProviderInfo): Int {
        var total = 40
        total += zonePoints(need.zone, p.zone)
        total += if (p.capacityHaDay >= need.surfaceHa) 15 else (15 * p.capacityHaDay / need.surfaceHa).toInt()
        if (Validators.overlaps(need.period, p.availability)) total += 15
        return total.coerceIn(0, 100)
    }

    private fun zonePoints(a: String, b: String): Int {
        if (a.trim().equals(b.trim(), ignoreCase = true)) return 30
        val sameArea = a.substringBeforeLast('-').trim().equals(b.substringBeforeLast('-').trim(), ignoreCase = true)
        return if (sameArea) 15 else 0
    }
}
