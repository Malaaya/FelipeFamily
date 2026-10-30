package com.duoc.felipesfamily.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duoc.felipesfamily.util.Validators

@Database(
    entities = [
        UserEntity::class, SessionEntity::class, NeedEntity::class,
        ProviderProfileEntity::class, ServiceEntity::class, InterestEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun needDao(): NeedDao
    abstract fun providerDao(): ProviderDao
    abstract fun serviceDao(): ServiceDao
    abstract fun interestDao(): InterestDao

    /** Carga datos ficticios de prueba la primera vez que se abre la app. */
    suspend fun seedIfEmpty() {
        if (serviceDao().count() == 0) {
            serviceDao().insert(ServiceEntity(name = "Fumigación con dron", criteria = "zona, cultivo, capacidad, disponibilidad", radiusKm = 40))
            serviceDao().insert(ServiceEntity(name = "Aplicación foliar", criteria = "zona, cultivo, disponibilidad", radiusKm = 60))
        }
        if (userDao().count() > 0) return
        val clave = Validators.hash("123456")
        userDao().insert(UserEntity(alias = "productor_demo01", passwordHash = clave, role = Roles.PRODUCER, zone = "Zona Sur-2"))
        userDao().insert(UserEntity(alias = "admin_demo", passwordHash = clave, role = Roles.ADMIN, zone = "Zona Centro-1"))
        val a = userDao().insert(UserEntity(alias = "prestador_demo01", passwordHash = clave, role = Roles.PROVIDER, zone = "Zona Sur-2"))
        val b = userDao().insert(UserEntity(alias = "prestador_demo02", passwordHash = clave, role = Roles.PROVIDER, zone = "Zona Sur-1"))
        val c = userDao().insert(UserEntity(alias = "prestador_demo03", passwordHash = clave, role = Roles.PROVIDER, zone = "Zona Centro-4"))
        providerDao().save(ProviderProfileEntity(userId = a, services = "Fumigación con dron", coverageKm = 40, capacityHaDay = 30, experienceYears = 5, availability = "15/11 - 30/11", rating = 4.6, displayName = "Drones Valle Sur"))
        providerDao().save(ProviderProfileEntity(userId = b, services = "Fumigación con dron", coverageKm = 25, capacityHaDay = 20, experienceYears = 3, availability = "20/11 - 25/11", rating = 4.2, displayName = "AeroAgro Araucanía"))
        providerDao().save(ProviderProfileEntity(userId = c, services = "Aplicación foliar", coverageKm = 60, capacityHaDay = 40, experienceYears = 8, availability = "10/11 - 30/11", rating = 4.8, displayName = "FoliarTec Centro"))
    }

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "felipe_family.db"
                ).fallbackToDestructiveMigration(true).build().also { instance = it }
            }
    }
}
