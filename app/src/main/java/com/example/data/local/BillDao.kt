package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: String): BillEntity?

    @Query("SELECT * FROM bills WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getBillsForCustomer(customerId: String): Flow<List<BillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<BillEntity>)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM bills")
    suspend fun getCount(): Int
}
