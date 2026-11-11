package com.duoc.felipesfamily.repository

import android.content.Context
import com.duoc.felipesfamily.data.AppDatabase
import com.duoc.felipesfamily.data.SessionEntity
import com.duoc.felipesfamily.data.UserEntity

class AuthRepository(context: Context) {
    private val database = AppDatabase.get(context)

    suspend fun seedIfEmpty() = database.seedIfEmpty()

    suspend fun userForSavedSession(): UserEntity? {
        val session = database.sessionDao().get() ?: return null
        return database.userDao().byId(session.userId)
    }

    suspend fun findUserByAlias(alias: String): UserEntity? =
        database.userDao().byAlias(alias)

    suspend fun createUser(user: UserEntity): Long =
        database.userDao().insert(user)

    suspend fun saveSession(userId: Long) =
        database.sessionDao().save(SessionEntity(userId = userId))

    suspend fun clearSession() = database.sessionDao().clear()
}
