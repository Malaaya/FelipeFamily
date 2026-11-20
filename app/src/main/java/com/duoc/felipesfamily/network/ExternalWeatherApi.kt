package com.duoc.felipesfamily.network

import retrofit2.http.GET
import retrofit2.http.Query

/** Endpoint 10 (API externa): Consulta condiciones meteorológicas reales para vuelo de drones. */
interface ExternalWeatherApi {
    @GET("v1/forecast?current_weather=true")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): WeatherResponseDto
}