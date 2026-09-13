package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiConfigDao {
    @Query("SELECT * FROM api_configs ORDER BY isDefault DESC, name ASC")
    fun getAllConfigs(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs WHERE isEnabled = 1")
    fun getEnabledConfigs(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs WHERE isEnabled = 1 ORDER BY isDefault DESC, name ASC")
    suspend fun getEnabledConfigsSync(): List<ApiConfigEntity>

    @Query("SELECT * FROM api_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: String): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE isDefault = 1 AND isEnabled = 1 LIMIT 1")
    suspend fun getDefaultConfig(): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE isDefault = 1 AND isEnabled = 1 LIMIT 1")
    fun getDefaultConfigFlow(): Flow<ApiConfigEntity?>

    @Query("SELECT * FROM api_configs WHERE category = :category AND isEnabled = 1 LIMIT 1")
    suspend fun getConfigForCategory(category: String): ApiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ApiConfigEntity)

    @Update
    suspend fun updateConfig(config: ApiConfigEntity)

    @Query("DELETE FROM api_configs WHERE id = :id")
    suspend fun deleteConfig(id: String)

    @Query("UPDATE api_configs SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Transaction
    suspend fun setDefault(id: String) {
        clearAllDefaults()
        setAsDefault(id)
    }

    @Query("UPDATE api_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: String)
}
