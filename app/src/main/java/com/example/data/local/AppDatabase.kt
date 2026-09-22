package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        BusinessProfileEntity::class,
        StaffEntity::class,
        AuditLogEntity::class,
        ImportHistoryEntity::class,
        ProductEntity::class,
        PackagingFormEntity::class,
        FormPriceEntity::class,
        StockMovementEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        SalePaymentEntity::class,
        CashShiftEntity::class,
        ExpenseEntity::class,
        CustomerEntity::class,
        CreditTransactionEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierAndPurchaseDao(): SupplierAndPurchaseDao
    abstract fun financeAndShiftDao(): FinanceAndShiftDao
    abstract fun businessAndStaffDao(): BusinessAndStaffDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rg_pos_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
