package com.awaytime.app.domain.usecase

import com.awaytime.app.data.repository.AwayTimeRepository
import com.awaytime.app.domain.model.AppGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/**
 * Mindful Use Case: Create new app group
 * Clean architecture implementation following Mindful patterns
 */
class CreateAppGroupUseCase(
    private val repository: AwayTimeRepository
) {
    suspend operator fun invoke(
        name: String,
        selectedApps: List<String>,
        dailyLimitMinutes: Int = 120
    ): Result<AppGroup> = withContext(Dispatchers.IO) {
        try {
            if (selectedApps.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No apps selected"))
            }
            
            val groupName = name.ifBlank { "My Apps" }
            
            val entity = repository.saveAppGroup(
                name = groupName,
                dailyLimitMinutes = dailyLimitMinutes,
                selectedApps = selectedApps
            )
            
            val appGroup = AppGroup(
                id = entity.id,
                name = entity.name,
                packageNames = entity.getSelectedApps(),
                dailyLimitMinutes = entity.dailyLimitMinutes,
                isActive = entity.isActive
            )
            
            Result.success(appGroup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}