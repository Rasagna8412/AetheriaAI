package com.aetheria.ai.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aetheria.ai.data.local.entity.CustomModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomModelDao {

    @Query("SELECT * FROM custom_models ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllModels(): Flow<List<CustomModelEntity>>

    @Query("SELECT * FROM custom_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): CustomModelEntity?

    @Query("SELECT * FROM custom_models WHERE name LIKE '%' || :query || '%' OR modelId LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY isFavorite DESC, createdAt DESC")
    fun searchModels(query: String): Flow<List<CustomModelEntity>>

    @Query("SELECT * FROM custom_models WHERE category = :category ORDER BY isFavorite DESC, createdAt DESC")
    fun getModelsByCategory(category: String): Flow<List<CustomModelEntity>>

    @Query("SELECT * FROM custom_models WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteModels(): Flow<List<CustomModelEntity>>

    @Query("SELECT COUNT(*) FROM custom_models")
    suspend fun getModelCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: CustomModelEntity)

    @Update
    suspend fun updateModel(model: CustomModelEntity)

    @Delete
    suspend fun deleteModel(model: CustomModelEntity)

    @Query("DELETE FROM custom_models WHERE id = :id")
    suspend fun deleteModelById(id: String)
}
