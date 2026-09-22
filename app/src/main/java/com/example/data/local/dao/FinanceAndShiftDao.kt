package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CashShiftEntity
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceAndShiftDao {
    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Cash Shifts
    @Query("SELECT * FROM cash_shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<CashShiftEntity>>

    @Query("SELECT * FROM cash_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    fun getCurrentOpenShift(): Flow<CashShiftEntity?>

    @Query("SELECT * FROM cash_shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    suspend fun getCurrentOpenShiftDirect(): CashShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashShiftEntity)

    @Update
    suspend fun updateShift(shift: CashShiftEntity)
}
