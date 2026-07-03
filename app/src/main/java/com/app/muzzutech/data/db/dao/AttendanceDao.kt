package com.app.muzzutech.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.muzzutech.data.model.Attendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attendance: Attendance)

    @Update
    suspend fun update(attendance: Attendance)

    @Delete
    suspend fun delete(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE servicemanId = :smId ORDER BY date DESC")
    fun getByServiceMan(smId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE servicemanId = :smId AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getByServiceManInRange(smId: Long, start: Long, end: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE servicemanId = :smId AND date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getByServiceManInRangeList(smId: Long, start: Long, end: Long): List<Attendance>

    @Query("SELECT * FROM attendance WHERE servicemanId = :smId AND date = :date LIMIT 1")
    suspend fun getEntry(smId: Long, date: Long): Attendance?

    @Query("SELECT COUNT(*) FROM attendance WHERE servicemanId = :smId AND date BETWEEN :start AND :end AND present = 1")
    suspend fun getPresentDays(smId: Long, start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE servicemanId = :smId AND date BETWEEN :start AND :end AND present = 1 AND halfDay = 0")
    suspend fun getFullPresentDays(smId: Long, start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE servicemanId = :smId AND date BETWEEN :start AND :end AND present = 1 AND halfDay = 1")
    suspend fun getHalfPresentDays(smId: Long, start: Long, end: Long): Int
}
