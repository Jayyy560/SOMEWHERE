package com.somewhere.app.data.local

import androidx.room.*
import com.somewhere.app.data.model.Drop
import kotlinx.coroutines.flow.Flow

@Dao
interface DropDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(drop: Drop)

    @Query("SELECT * FROM drops ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Drop>>

    @Query("SELECT * FROM drops")
    suspend fun getAllOnce(): List<Drop>

    @Query("DELETE FROM drops WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM drops")
    suspend fun deleteAll()

    /**
     * Coarse bounding-box query — we do precise Haversine filtering in the repository.
     * delta is a rough lat/lon offset for the bounding box.
     */
    @Query(
        """
        SELECT * FROM drops 
        WHERE latitude BETWEEN :lat - :delta AND :lat + :delta 
        AND longitude BETWEEN :lon - :delta AND :lon + :delta
        """
    )
    suspend fun getInBoundingBox(lat: Double, lon: Double, delta: Double): List<Drop>
}
