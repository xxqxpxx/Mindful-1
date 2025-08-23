package com.awaytime.app.domain.usecase

import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.domain.model.AppGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
/**
 * Mindful Use Case: Get all app groups
 * Clean architecture implementation following Mindful patterns
 */
class GetAppGroupsUseCase(
    private val repository: AwayTimeRepository
) {
    operator fun invoke(): Flow<List<AppGroup>> {
        return repository.getAllAppGroups().map { entities ->
            entities.map { entity ->
                AppGroup(
                    id = entity.id,
                    name = entity.name,
                    packageNames = entity.getSelectedApps(),
                    dailyLimitMinutes = entity.dailyLimitMinutes,
                    isActive = entity.isActive
                )
            }
        }
    }
    
    fun getActiveAppGroups(): Flow<List<AppGroup>> {
        return repository.getActiveAppGroups().map { entities ->
            entities.map { entity ->
                AppGroup(
                    id = entity.id,
                    name = entity.name,
                    packageNames = entity.getSelectedApps(),
                    dailyLimitMinutes = entity.dailyLimitMinutes,
                    isActive = entity.isActive
                )
            }
        }
    }
}