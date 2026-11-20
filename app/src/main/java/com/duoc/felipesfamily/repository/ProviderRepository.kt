package com.duoc.felipesfamily.repository

import android.content.Context
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.InterestInfo
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.NeedStatus
import com.duoc.felipesfamily.data.NotificationEntity
import com.duoc.felipesfamily.data.ProviderProfileEntity
import com.duoc.felipesfamily.network.ProviderServiceDto
import com.duoc.felipesfamily.network.RetrofitClient

class ProviderRepository(context: Context) {
    private val database = AppDatabase.get(context)
    private val api = RetrofitClient.backendApi

    suspend fun profilesForUser(userId: Long): List<ProviderProfileEntity> =
        database.providerDao().profiles(userId)

    suspend fun interestsForProvider(providerId: Long): List<InterestInfo> =
        database.interestDao().byProvider(providerId)

    suspend fun saveProfile(profile: ProviderProfileEntity): Long {
        val id = database.providerDao().save(profile)
        runCatching {
            api.saveProviderProfile(
                ProviderServiceDto(
                    id = id,
                    userId = profile.userId,
                    displayName = profile.displayName,
                    services = profile.services,
                    coverageKm = profile.coverageKm,
                    capacityHaDay = profile.capacityHaDay,
                    experienceYears = profile.experienceYears,
                    availability = profile.availability,
                    active = profile.active
                )
            )
        }
        return id
    }

    suspend fun openNeeds(): List<NeedEntity> =
        database.needDao().byStatuses(listOf(NeedStatus.NEW, NeedStatus.MATCHED))

    suspend fun insertNotification(notification: NotificationEntity): Long =
        database.notificationDao().insert(notification)

    suspend fun setProfileActive(profileId: Long, active: Boolean) =
        database.providerDao().setActive(profileId, active)

    suspend fun deleteProfile(profileId: Long) =
        database.providerDao().delete(profileId)

    suspend fun profileCountForUser(userId: Long): Int =
        database.providerDao().countByUser(userId)

    suspend fun deleteInterestsForProvider(providerId: Long) =
        database.interestDao().deleteByProvider(providerId)

    suspend fun updateInterestStatus(interestId: Long, status: String) {
        database.interestDao().setStatus(interestId, status)
        runCatching {
            api.updateInterestStatus(interestId, mapOf("status" to status))
        }
    }
}
