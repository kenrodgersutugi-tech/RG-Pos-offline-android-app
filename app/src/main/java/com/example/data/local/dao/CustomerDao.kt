package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET currentCreditBalance = :newBalance WHERE id = :customerId")
    suspend fun updateCreditBalance(customerId: String, newBalance: Double)

    @Query("SELECT * FROM credit_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getCreditTransactionsForCustomer(customerId: String): Flow<List<CreditTransactionEntity>>

    @Query("SELECT * FROM credit_transactions ORDER BY timestamp DESC")
    fun getAllCreditTransactions(): Flow<List<CreditTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditTransaction(transaction: CreditTransactionEntity)
}
