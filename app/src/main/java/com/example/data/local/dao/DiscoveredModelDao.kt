package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DiscoveredModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveredModelDao {

    @Query("SELECT * FROM discovered_models ORDER BY freeTier DESC, supportsStreaming DESC, lastVerified DESC")
    fun getAllModels(): Flow<List<DiscoveredModelEntity>>

    @Query("SELECT * FROM discovered_models WHERE providerType = :providerType ORDER BY freeTier DESC, lastVerified DESC, modelId ASC")
    fun getModelsForProvider(providerType: String): Flow<List<DiscoveredModelEntity>>

    @Query("SELECT * FROM discovered_models WHERE providerType = :providerType")
    suspend fun getModelsForProviderSync(providerType: String): List<DiscoveredModelEntity>

    @Query("SELECT * FROM discovered_models WHERE providerType = :providerType AND status = 'VERIFIED'")
    fun getVerifiedModelsForProvider(providerType: String): Flow<List<DiscoveredModelEntity>>

    @Query("SELECT * FROM discovered_models WHERE id = :id")
    suspend fun getModelById(id: String): DiscoveredModelEntity?

    @Query("SELECT * FROM discovered_models WHERE providerType = :providerType AND modelId = :modelId")
    suspend fun getModelByProviderAndId(providerType: String, modelId: String): DiscoveredModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<DiscoveredModelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: DiscoveredModelEntity)

    @Update
    suspend fun updateModel(model: DiscoveredModelEntity)

    @Query("UPDATE discovered_models SET status = 'UNAVAILABLE', availability = 'UNAVAILABLE' WHERE providerType = :providerType AND modelId = :modelId")
    suspend fun markModelUnavailable(providerType: String, modelId: String)

    @Query("UPDATE discovered_models SET status = 'VERIFIED', lastVerified = :timestamp WHERE providerType = :providerType AND modelId = :modelId")
    suspend fun markModelVerified(providerType: String, modelId: String, timestamp: Long)

    @Query("DELETE FROM discovered_models WHERE providerType = :providerType")
    suspend fun clearModelsForProvider(providerType: String)

    @Query("SELECT COUNT(*) FROM discovered_models WHERE providerType = :providerType")
    suspend fun getDiscoveredCount(providerType: String): Int

    @Query("SELECT COUNT(*) FROM discovered_models WHERE providerType = :providerType AND status = 'VERIFIED'")
    suspend fun getVerifiedCount(providerType: String): Int
}
