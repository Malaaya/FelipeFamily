package com.duoc.felipesfamily.repository

import android.content.Context
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.InterestEntity
import com.duoc.felipesfamily.data.InterestStatus
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.NotificationEntity
import com.duoc.felipesfamily.data.ProviderInfo
import com.duoc.felipesfamily.data.ServiceEntity
import com.duoc.felipesfamily.network.InterestRequestDto
import com.duoc.felipesfamily.network.NeedDto
import com.duoc.felipesfamily.network.RetrofitClient
import com.duoc.felipesfamily.util.ZoneCoords
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProducerRepository(context: Context) {
    private val database = AppDatabase.get(context)
    private val api = RetrofitClient.backendApi
    private val weatherApi = RetrofitClient.weatherApi
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun unseenNotifications(userId: Long): List<NotificationEntity> =
        database.notificationDao().unseen(userId)

    suspend fun markNotificationsSeen(userId: Long) =
        database.notificationDao().markAllSeen(userId)

    suspend fun needsForProducer(producerId: Long): List<NeedEntity> {
        syncScope.launch {
            runCatching { api.getNeedsByProducer(producerId) }
        }
        return database.needDao().byProducer(producerId)
    }

    suspend fun allServices(): List<ServiceEntity> {
        syncScope.launch {
            runCatching { api.getServices() }
        }
        return database.serviceDao().all()
    }

    suspend fun insertNeed(need: NeedEntity): Long {
        val localId = database.needDao().insert(need)
        syncScope.launch {
            runCatching {
                api.createNeed(
                    NeedDto(
                        id = localId,
                        producerId = need.producerId,
                        service = need.service,
                        crop = need.crop,
                        surfaceHa = need.surfaceHa,
                        zone = need.zone,
                        period = need.period,
                        product = need.product,
                        status = need.status
                    )
                )
            }
        }
        return localId
    }

    suspend fun allActiveProviders(): List<ProviderInfo> {
        syncScope.launch {
            runCatching { api.getActiveProviders() }
        }
        return database.providerDao().all()
    }

    suspend fun updateNeed(need: NeedEntity) =
        database.needDao().update(need)

    suspend fun sendInterest(needId: Long, providerId: Long) {
        database.interestDao().insert(InterestEntity(needId = needId, providerId = providerId))
        syncScope.launch {
            runCatching {
                api.sendInterest(
                    InterestRequestDto(
                        needId = needId,
                        providerId = providerId,
                        status = InterestStatus.SENT
                    )
                )
            }
        }
    }

    suspend fun fetchZoneWeatherSummary(zone: String): String? {
        val point = ZoneCoords.of(zone) ?: return null
        return runCatching {
            val response = weatherApi.getCurrentWeather(point.latitude, point.longitude)
            val w = response.currentWeather ?: return null
            val flightOk = if (w.windspeed <= 20.0) "Viento apto para dron" else "Viento alto para vuelo"
            "Clima actual en $zone: ${w.temperature}°C · Viento ${w.windspeed} km/h ($flightOk)"
        }.getOrNull()
    }
}