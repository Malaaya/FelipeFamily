package com.duoc.felipesfamily.repository

import android.content.Context
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.ProfileSummary
import com.duoc.felipesfamily.data.ProviderProfileEntity
import com.duoc.felipesfamily.data.ServiceEntity
import com.duoc.felipesfamily.data.UserEntity
import com.duoc.felipesfamily.network.RetrofitClient
import com.duoc.felipesfamily.network.ServiceCatalogDto

class AdminRepository(context: Context) {
    private val database = AppDatabase.get(context)
    private val api = RetrofitClient.backendApi

    suspend fun allUsers(): List<UserEntity> = database.userDao().all()

    suspend fun providerCount(): Int = database.providerDao().count()

    suspend fun allProviderSummaries(): List<ProfileSummary> =
        database.providerDao().allSummaries()

    suspend fun allServices(): List<ServiceEntity> = database.serviceDao().all()

    suspend fun needsForProducer(producerId: Long): List<NeedEntity> =
        database.needDao().byProducer(producerId)

    suspend fun profilesForProvider(providerId: Long): List<ProviderProfileEntity> =
        database.providerDao().profiles(providerId)

    suspend fun deleteUser(userId: Long) {
        database.interestDao().deleteByProducer(userId)
        database.needDao().deleteByProducer(userId)
        database.interestDao().deleteByProvider(userId)
        database.providerDao().deleteByUser(userId)
        database.notificationDao().deleteByUser(userId)
        database.userDao().deleteById(userId)
        runCatching { api.deleteUser(userId) }
    }

    suspend fun deleteNeed(needId: Long) {
        database.interestDao().deleteByNeed(needId)
        database.needDao().deleteById(needId)
    }

    suspend fun deleteProfile(profile: ProviderProfileEntity) {
        database.providerDao().delete(profile.id)
        if (database.providerDao().countByUser(profile.userId) == 0) {
            database.interestDao().deleteByProvider(profile.userId)
        }
    }

    suspend fun insertService(service: ServiceEntity): Long {
        val id = database.serviceDao().insert(service)
        runCatching {
            api.createService(
                ServiceCatalogDto(
                    id = id,
                    name = service.name,
                    criteria = service.criteria,
                    radiusKm = service.radiusKm
                )
            )
        }
        return id
    }
}
