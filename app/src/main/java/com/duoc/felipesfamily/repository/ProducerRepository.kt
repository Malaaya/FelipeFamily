package com.duoc.felipesfamily.repository

import android.content.Context
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.InterestEntity
import com.duoc.felipesfamily.data.NeedEntity
import com.duoc.felipesfamily.data.NotificationEntity
import com.duoc.felipesfamily.data.ProviderInfo
import com.duoc.felipesfamily.data.ServiceEntity

class ProducerRepository(context: Context) {
    private val database = AppDatabase.get(context)

    suspend fun unseenNotifications(userId: Long): List<NotificationEntity> =
        database.notificationDao().unseen(userId)

    suspend fun markNotificationsSeen(userId: Long) =
        database.notificationDao().markAllSeen(userId)

    suspend fun needsForProducer(producerId: Long): List<NeedEntity> =
        database.needDao().byProducer(producerId)

    suspend fun allServices(): List<ServiceEntity> =
        database.serviceDao().all()

    suspend fun insertNeed(need: NeedEntity): Long =
        database.needDao().insert(need)

    suspend fun allActiveProviders(): List<ProviderInfo> =
        database.providerDao().all()

    suspend fun updateNeed(need: NeedEntity) =
        database.needDao().update(need)

    suspend fun sendInterest(needId: Long, providerId: Long) =
        database.interestDao().insert(InterestEntity(needId = needId, providerId = providerId))
}
