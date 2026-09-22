package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.BusinessProfileEntity
import com.example.data.local.entity.ImportHistoryEntity
import com.example.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessAndStaffDao {
    // Business Profile
    @Query("SELECT * FROM business_profile WHERE id = 'primary_business' LIMIT 1")
    fun getBusinessProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profile WHERE id = 'primary_business' LIMIT 1")
    suspend fun getBusinessProfileDirect(): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: BusinessProfileEntity)

    // Staff
    @Query("SELECT * FROM staff WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE pin = :pin AND isActive = 1 LIMIT 1")
    suspend fun authenticateStaffByPin(pin: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffList(staffList: List<StaffEntity>)

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    // Audit Log
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 200): Flow<List<AuditLogEntity>>

    // Import History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportHistory(history: ImportHistoryEntity)

    @Query("SELECT * FROM import_history ORDER BY timestamp DESC")
    fun getAllImportHistory(): Flow<List<ImportHistoryEntity>>

    @Query("SELECT * FROM import_history WHERE id = :id LIMIT 1")
    suspend fun getImportHistoryById(id: String): ImportHistoryEntity?

    @Update
    suspend fun updateImportHistory(history: ImportHistoryEntity)
}
