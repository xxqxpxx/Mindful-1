package com.awaytime.app.data.dao

import androidx.room.*
import com.awaytime.app.data.entity.AppGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppGroupDao {

    @Query("SELECT * FROM app_groups ORDER BY createdDate DESC")
    fun getAllAppGroups(): Flow<List<AppGroupEntity>>

    @Query("SELECT * FROM app_groups WHERE isActive = 1 ORDER BY createdDate DESC")
    fun getActiveAppGroups(): Flow<List<AppGroupEntity>>

    @Query("SELECT * FROM app_groups WHERE id = :id LIMIT 1")
    suspend fun getAppGroupById(id: String): AppGroupEntity?

    @Query("SELECT * FROM app_groups WHERE name = :name LIMIT 1")
    suspend fun getAppGroupByName(name: String): AppGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppGroup(appGroup: AppGroupEntity)

    @Update
    suspend fun updateAppGroup(appGroup: AppGroupEntity)

    @Delete
    suspend fun deleteAppGroup(appGroup: AppGroupEntity)

    @Query("UPDATE app_groups SET isActive = :isActive WHERE id = :id")
    suspend fun updateAppGroupStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM app_groups WHERE id = :id")
    suspend fun deleteAppGroupById(id: String)

    @Query("SELECT COUNT(*) FROM app_groups WHERE isActive = 1")
    suspend fun getActiveAppGroupCount(): Int
    
    @Query("SELECT * FROM app_groups ORDER BY createdDate DESC")
    suspend fun getAllAppGroupsSync(): List<AppGroupEntity>
    
    @Query("DELETE FROM app_groups")
    suspend fun deleteAllAppGroups()
}