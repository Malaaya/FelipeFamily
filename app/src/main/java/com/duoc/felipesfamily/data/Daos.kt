package com.duoc.felipesfamily.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/** Resultado de unir usuario + perfil de prestador. */
data class ProviderInfo(
    /** Id de la prestación (un prestador puede tener varias). */
    val profileId: Long,
    val userId: Long,
    val alias: String,
    /** Nombre amigable de la prestación (o el alias si no tiene). */
    val name: String,
    val zone: String,
    val services: String,
    val coverageKm: Int,
    val capacityHaDay: Int,
    val experienceYears: Int,
    val availability: String,
    val rating: Double
)

/** Resultado de unir interés + necesidad + productor. */
data class InterestInfo(
    val interestId: Long,
    val status: String,
    val needId: Long,
    val producerAlias: String,
    val service: String,
    val crop: String,
    val surfaceHa: Double,
    val zone: String,
    val period: String,
    val product: String
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE alias = :alias LIMIT 1")
    suspend fun byAlias(alias: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): UserEntity?

    @Query("SELECT * FROM users ORDER BY id DESC")
    suspend fun all(): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(session: SessionEntity)

    @Query("SELECT * FROM session WHERE id = 1")
    suspend fun get(): SessionEntity?

    @Query("DELETE FROM session")
    suspend fun clear()
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(n: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE userId = :userId AND seen = 0 ORDER BY id DESC")
    suspend fun unseen(userId: Long): List<NotificationEntity>

    @Query("UPDATE notifications SET seen = 1 WHERE userId = :userId")
    suspend fun markAllSeen(userId: Long)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)
}

@Dao
interface NeedDao {
    @Insert
    suspend fun insert(need: NeedEntity): Long

    @Update
    suspend fun update(need: NeedEntity)

    @Query("SELECT * FROM needs WHERE status IN (:statuses)")
    suspend fun byStatuses(statuses: List<String>): List<NeedEntity>

    @Query("DELETE FROM needs WHERE producerId = :producerId")
    suspend fun deleteByProducer(producerId: Long)

    @Query("DELETE FROM needs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM needs WHERE producerId = :producerId ORDER BY id DESC")
    suspend fun byProducer(producerId: Long): List<NeedEntity>
}

@Dao
interface ProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: ProviderProfileEntity): Long

    @Query("SELECT * FROM provider_profiles WHERE userId = :userId ORDER BY id DESC")
    suspend fun profiles(userId: Long): List<ProviderProfileEntity>

    @Query("SELECT COUNT(*) FROM provider_profiles WHERE userId = :userId")
    suspend fun countByUser(userId: Long): Int

    @Query(
        "SELECT p.id AS profileId, u.id AS userId, u.alias AS alias, COALESCE(NULLIF(p.displayName, ''), u.alias) AS name, u.zone AS zone, p.services AS services, " +
            "p.coverageKm AS coverageKm, p.capacityHaDay AS capacityHaDay, " +
            "p.experienceYears AS experienceYears, p.availability AS availability, p.rating AS rating " +
            "FROM users u INNER JOIN provider_profiles p ON p.userId = u.id WHERE p.active = 1"
    )
    suspend fun all(): List<ProviderInfo>

    @Query("UPDATE provider_profiles SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM provider_profiles WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("DELETE FROM provider_profiles WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM provider_profiles")
    suspend fun count(): Int
}

@Dao
interface ServiceDao {
    @Insert
    suspend fun insert(service: ServiceEntity): Long

    @Query("SELECT * FROM services ORDER BY id")
    suspend fun all(): List<ServiceEntity>

    @Query("SELECT COUNT(*) FROM services")
    suspend fun count(): Int
}

@Dao
interface InterestDao {
    @Insert
    suspend fun insert(interest: InterestEntity): Long

    @Query("DELETE FROM interests WHERE needId IN (SELECT id FROM needs WHERE producerId = :producerId)")
    suspend fun deleteByProducer(producerId: Long)

    @Query("DELETE FROM interests WHERE needId = :needId")
    suspend fun deleteByNeed(needId: Long)

    @Query("DELETE FROM interests WHERE providerId = :providerId")
    suspend fun deleteByProvider(providerId: Long)

    @Query("UPDATE interests SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query(
        "SELECT i.id AS interestId, i.status AS status, n.id AS needId, u.alias AS producerAlias, " +
            "n.service AS service, n.crop AS crop, n.surfaceHa AS surfaceHa, n.zone AS zone, " +
            "n.period AS period, n.product AS product " +
            "FROM interests i INNER JOIN needs n ON n.id = i.needId " +
            "INNER JOIN users u ON u.id = n.producerId " +
            "WHERE i.providerId = :providerId ORDER BY i.id DESC"
    )
    suspend fun byProvider(providerId: Long): List<InterestInfo>
}
