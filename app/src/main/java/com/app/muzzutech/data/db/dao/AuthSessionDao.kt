package com.app.muzzutech.data.db.dao

import androidx.room.*
import com.app.muzzutech.data.model.AuthSession

@Dao
interface AuthSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: AuthSession): Long

    @Query("SELECT * FROM auth_sessions WHERE firebaseUid = :firebaseUid AND isActive = 1 ORDER BY loginTimestamp DESC LIMIT 1")
    suspend fun getActiveSession(firebaseUid: String): AuthSession?

    @Query("UPDATE auth_sessions SET isActive = 0 WHERE firebaseUid = :firebaseUid")
    suspend fun deactivateAllSessions(firebaseUid: String)

    @Query("UPDATE auth_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun deactivateSession(sessionId: Long)

    // Worker sessions are keyed by userId (= worker id) + userType, because a
    // worker's firebaseUid is empty. Terminate/validity checks use these.
    @Query("UPDATE auth_sessions SET isActive = 0 WHERE userId = :userId AND userType = :userType")
    suspend fun deactivateSessionsByUser(userId: String, userType: String)

    @Query("SELECT COUNT(*) FROM auth_sessions WHERE userId = :userId AND userType = :userType AND isActive = 1")
    suspend fun countActiveSessionsByUser(userId: String, userType: String): Int

    @Query("SELECT * FROM auth_sessions WHERE userId = :userId AND userType = :userType AND isActive = 1")
    suspend fun getActiveSessionsByUser(userId: String, userType: String): List<AuthSession>
}
