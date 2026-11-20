package com.duoc.felipesfamily

import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.ProviderInfo
import com.duoc.felipesfamily.util.Matching
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingTest {

    private fun sampleNeed(
        service: String = "Fumigación con dron",
        zone: String = "Zona Sur-2",
        surfaceHa: Double = 20.0,
        period: String = "20/11 - 25/11"
    ) = NeedEntity(
        id = 1L,
        producerId = 10L,
        service = service,
        crop = "Trigo",
        surfaceHa = surfaceHa,
        zone = zone,
        period = period,
        product = "Líquido"
    )

    private fun sampleProvider(
        profileId: Long = 1L,
        name: String = "Drones Valle Sur",
        services: String = "Fumigación con dron",
        zone: String = "Zona Sur-2",
        capacityHaDay: Int = 30,
        availability: String = "15/11 - 30/11"
    ) = ProviderInfo(
        profileId = profileId,
        userId = 20L,
        alias = "prestador_demo01",
        name = name,
        zone = zone,
        services = services,
        coverageKm = 60,
        capacityHaDay = capacityHaDay,
        experienceYears = 5,
        availability = availability,
        rating = 4.8
    )

    @Test
    fun `compute otorga 100 por ciento cuando coinciden servicio zona capacidad y fechas`() {
        val need = sampleNeed()
        val provider = sampleProvider()

        val results = Matching.compute(need, listOf(provider))

        assertEquals(1, results.size)
        assertEquals(100, results.first().score)
        assertTrue(results.first().sameZone)
        assertTrue(results.first().capacityOk)
        assertTrue(results.first().available)
    }

    @Test
    fun `compute filtra prestadores que no ofrecen el servicio solicitado`() {
        val need = sampleNeed(service = "Fumigación con dron")
        val wrongService = sampleProvider(profileId = 2L, services = "Aplicación foliar")

        val results = Matching.compute(need, listOf(wrongService))

        assertTrue(results.isEmpty())
    }

    @Test
    fun `compute ordena resultados de mayor a menor compatibilidad`() {
        val need = sampleNeed(zone = "Zona Sur-2", surfaceHa = 20.0, period = "20/11 - 25/11")
        val partialMatch = sampleProvider(profileId = 1L, name = "Parcial", zone = "Zona Norte-1", capacityHaDay = 5)
        val fullMatch = sampleProvider(profileId = 2L, name = "Completo", zone = "Zona Sur-2", capacityHaDay = 40)

        val results = Matching.compute(need, listOf(partialMatch, fullMatch))

        assertEquals("Completo", results.first().provider.name)
        assertTrue(results[0].score > results[1].score)
    }
}