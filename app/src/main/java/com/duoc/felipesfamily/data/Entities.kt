package com.duoc.felipesfamily.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Perfiles de usuario del caso TECMUNDO. */
object Roles {
    const val PRODUCER = "PRODUCTOR"
    const val PROVIDER = "PRESTADOR"
    const val ADMIN = "ADMIN"
}

object NeedStatus {
    const val NEW = "Sin revisar"
    const val MATCHED = "Con coincidencias"
    const val INTEREST = "Interés enviado"
}

object InterestStatus {
    const val SENT = "Interés recibido"
    const val ACCEPTED = "Vinculación aceptada (simulada)"
    const val REJECTED = "Rechazado"
}

/** Usuarios ficticios registrados en el dispositivo. */
@Entity(tableName = "users", indices = [Index(value = ["alias"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val passwordHash: String,
    val role: String,
    val zone: String
)

/** Sesión iniciada (una sola fila). */
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: Int = 1,
    val userId: Long
)

/** Necesidad de servicio registrada por un productor. */
@Entity(tableName = "needs")
data class NeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val producerId: Long,
    val service: String,
    val crop: String,
    val surfaceHa: Double,
    val zone: String,
    val period: String,
    val product: String,
    val status: String = NeedStatus.NEW
)

/** Perfil demostrativo de un prestador de servicios. */
@Entity(tableName = "provider_profiles")
data class ProviderProfileEntity(
    @PrimaryKey val userId: Long,
    val services: String,
    val coverageKm: Int,
    val capacityHaDay: Int,
    val experienceYears: Int,
    val availability: String,
    val rating: Double = 4.5,
    /** false = prestación desactivada: no aparece en el matching. */
    @ColumnInfo(defaultValue = "1") val active: Boolean = true
)

/** Catálogo de servicios y criterios de matching (lo administra el admin). */
@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val criteria: String,
    val radiusKm: Int
)

/** Interés (vinculación simulada) de un productor hacia un prestador. */
@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val needId: Long,
    val providerId: Long,
    val status: String = InterestStatus.SENT
)
