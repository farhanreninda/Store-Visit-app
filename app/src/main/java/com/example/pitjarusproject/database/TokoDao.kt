package com.example.pitjarusproject.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TokoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertToko(data: Toko)

    @Query("SELECT * FROM toko")
    fun getToko(): LiveData<List<Toko>>

    @Query("DELETE FROM toko WHERE id = :tokoId")
    fun deleteToko(tokoId: Long)

    @Query("DELETE FROM toko")
    fun clearToko()

    @Update
    suspend fun updateToko(data: Toko)
}