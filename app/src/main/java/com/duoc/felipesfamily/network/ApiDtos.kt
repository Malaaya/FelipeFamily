package com.duoc.felipesfamily.network

import com.google.gson.annotations.SerializedName

/** DTO para comunicación con los microservicios Spring Boot de TECMUNDO. */
data class NeedDto(
    val id: Long? = null,
    val producerId: Long,
    val service: String,
    val crop: String,
    val surfaceHa: Double,
    val zone: String,
    val period: String,
    val product: String,
    val status: String
)

data class ProviderServiceDto(
    val id: Long? = null,
    val userId: Long,
    val displayName: String,
    val services: String,
    val coverageKm: Int,
    val capacityHaDay: Int,
    val experienceYears: Int,
    val availability: String,
    val active: Boolean
)

data class ServiceCatalogDto(
    val id: Long? = null,
    val name: String,
    val criteria: String,
    val radiusKm: Int
)

data class InterestRequestDto(
    val needId: Long,
    val providerId: Long,
    val status: String
)

/** DTO de respuesta de la API externa Open-Meteo (clima actual para fumigación con dron). */
data class WeatherResponseDto(
    @SerializedName("current_weather") val currentWeather: CurrentWeatherDto?
)

data class CurrentWeatherDto(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int
)