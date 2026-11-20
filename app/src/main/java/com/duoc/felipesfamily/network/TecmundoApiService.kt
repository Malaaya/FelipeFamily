package com.duoc.felipesfamily.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Endpoints de los microservicios propios desarrollados en Spring Boot. */
interface TecmundoApiService {

    // 1. GET: Obtener catálogo de servicios agrícolas
    @GET("api/services")
    suspend fun getServices(): List<ServiceCatalogDto>

    // 2. POST: Crear un nuevo servicio en el catálogo (Administrador)
    @POST("api/services")
    suspend fun createService(@Body service: ServiceCatalogDto): ServiceCatalogDto

    // 3. GET: Listar necesidades de un productor
    @GET("api/needs/producer/{producerId}")
    suspend fun getNeedsByProducer(@Path("producerId") producerId: Long): List<NeedDto>

    // 4. POST: Registrar una nueva necesidad agrícola (Productor)
    @POST("api/needs")
    suspend fun createNeed(@Body need: NeedDto): NeedDto

    // 5. GET: Obtener prestaciones activas para el motor de matching
    @GET("api/providers")
    suspend fun getActiveProviders(): List<ProviderServiceDto>

    // 6. POST: Registrar o actualizar prestación de servicio (Prestador)
    @POST("api/providers")
    suspend fun saveProviderProfile(@Body profile: ProviderServiceDto): ProviderServiceDto

    // 7. POST: Enviar manifestación de interés de un productor a un prestador
    @POST("api/interests")
    suspend fun sendInterest(@Body interest: InterestRequestDto): InterestRequestDto

    // 8. PUT: Aceptar o rechazar vinculación simulada (Prestador)
    @PUT("api/interests/{id}/status")
    suspend fun updateInterestStatus(
        @Path("id") interestId: Long,
        @Body statusBody: Map<String, String>
    ): InterestRequestDto

    // 9. DELETE: Eliminar cuenta de usuario (Administrador)
    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") userId: Long)
}