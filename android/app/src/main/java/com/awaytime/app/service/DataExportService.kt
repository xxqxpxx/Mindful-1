package com.awaytime.app.service

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awaytime.app.data.AwayTimeDatabase
import com.awaytime.app.data.entity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

enum class ExportFormat(
    val displayName: String,
    val fileExtension: String,
    val mimeType: String
) {
    CSV("CSV Spreadsheet", "csv", "text/csv"),
    JSON("JSON Data", "json", "application/json"),
    PDF("PDF Report", "pdf", "application/pdf"),
    HTML("HTML Report", "html", "text/html")
}

enum class ExportDataType(val displayName: String) {
    USAGE_RECORDS("Usage Records"),
    APP_GROUPS("App Groups"),
    SETTINGS("User Settings"),
    FOCUS_SESSIONS("Focus Sessions"),
    ACHIEVEMENTS("Achievements & Goals"),
    ANALYTICS_SUMMARY("Analytics Summary"),
    COMPREHENSIVE("Complete Data Export")
}

data class ExportRequest(
    val dataTypes: Set<ExportDataType>,
    val format: ExportFormat,
    val dateRange: DateRange,
    val includePersonalData: Boolean = true,
    val includeAnalytics: Boolean = true
)

data class DateRange(
    val startDate: Date,
    val endDate: Date
) {
    companion object {
        fun last7Days(): DateRange {
            val endDate = Date()
            val startDate = Date(endDate.time - 7.days.inWholeMilliseconds)
            return DateRange(startDate, endDate)
        }
        
        fun last30Days(): DateRange {
            val endDate = Date()
            val startDate = Date(endDate.time - 30.days.inWholeMilliseconds)
            return DateRange(startDate, endDate)
        }
        
        fun last90Days(): DateRange {
            val endDate = Date()
            val startDate = Date(endDate.time - 90.days.inWholeMilliseconds)
            return DateRange(startDate, endDate)
        }
        
        fun allTime(): DateRange {
            val endDate = Date()
            val startDate = Date(0) // Unix epoch
            return DateRange(startDate, endDate)
        }
    }
}

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0,
    val recordCount: Int = 0,
    val error: String? = null
)

class DataExportService(private val context: Context) : ViewModel() {

    private val database = AwayTimeDatabase.getDatabase(context)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileDateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // MARK: - Public Export API

    suspend fun exportData(request: ExportRequest): ExportResult {
        if (!canExportData()) {
            return ExportResult(
                success = false,
                error = "Data export requires premium subscription"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                _isExporting.value = true
                _exportProgress.value = 0f

                val fileName = generateFileName(request)
                val file = createExportFile(fileName)
                
                // Use streaming approach for large datasets
                val recordCount = when (request.format) {
                    ExportFormat.CSV -> exportToCsvStreaming(file, request)
                    ExportFormat.JSON -> exportToJsonStreaming(file, request)
                    ExportFormat.PDF -> exportToPdfStreaming(file, request)
                    ExportFormat.HTML -> exportToHtmlStreaming(file, request)
                }

                _exportProgress.value = 1f

                ExportResult(
                    success = true,
                    filePath = file.absolutePath,
                    fileName = file.name,
                    fileSize = file.length(),
                    recordCount = recordCount
                )

            } catch (e: Exception) {
                println("❌ Export failed: ${e.message}")
                ExportResult(
                    success = false,
                    error = "Export failed: ${e.message}"
                )
            } finally {
                _isExporting.value = false
                _exportProgress.value = 0f
            }
        }
    }

    fun shareExportFile(exportResult: ExportResult, format: ExportFormat) {
        if (!exportResult.success || exportResult.filePath == null) return

        val file = File(exportResult.filePath)
        if (!file.exists()) return

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AwayTime Data Export - ${exportResult.fileName}")
                putExtra(Intent.EXTRA_TEXT, "Your AwayTime usage data export is attached.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Export File")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            println("❌ Error sharing export file: ${e.message}")
        }
    }

    // MARK: - Data Collection

    private data class ExportData(
        val usageRecords: List<UsageRecordEntity> = emptyList(),
        val appGroups: List<AppGroupEntity> = emptyList(),
        val userSettings: UserSettingsEntity? = null,
        val focusSessions: List<Any> = emptyList(), // Would be FocusSession objects
        val analytics: AnalyticsSummary? = null,
        val exportMetadata: ExportMetadata
    ) {
        val totalRecords: Int
            get() = usageRecords.size + appGroups.size + focusSessions.size + (if (userSettings != null) 1 else 0)
    }

    private data class ExportMetadata(
        val exportDate: Date = Date(),
        val appVersion: String = "1.0",
        val dataTypes: Set<ExportDataType>,
        val dateRange: DateRange,
        val totalRecords: Int = 0
    )

    private data class AnalyticsSummary(
        val totalUsageTime: Long,
        val averageDailyUsage: Long,
        val mostUsedApp: String,
        val longestStreak: Int,
        val goalCompletionRate: Float,
        val topCategories: Map<String, Long>
    )

    private suspend fun collectExportData(request: ExportRequest): ExportData {
        val usageRecords = if (request.dataTypes.contains(ExportDataType.USAGE_RECORDS) || 
                              request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
            database.usageRecordDao().getUsageRecordsForDateRange(
                request.dateRange.startDate.time,
                request.dateRange.endDate.time
            )
        } else emptyList()

        val appGroups = if (request.dataTypes.contains(ExportDataType.APP_GROUPS) ||
                           request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
            database.appGroupDao().getAllAppGroups().first()
        } else emptyList()

        val userSettings = if (request.dataTypes.contains(ExportDataType.SETTINGS) ||
                              request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
            database.userSettingsDao().getUserSettingsSync()
        } else null

        val analytics = if (request.includeAnalytics && 
                           (request.dataTypes.contains(ExportDataType.ANALYTICS_SUMMARY) ||
                            request.dataTypes.contains(ExportDataType.COMPREHENSIVE))) {
            generateAnalyticsSummary(usageRecords, appGroups)
        } else null

        return ExportData(
            usageRecords = usageRecords,
            appGroups = appGroups,
            userSettings = userSettings,
            analytics = analytics,
            exportMetadata = ExportMetadata(
                dataTypes = request.dataTypes,
                dateRange = request.dateRange,
                totalRecords = usageRecords.size + appGroups.size + (if (userSettings != null) 1 else 0)
            )
        )
    }

    private fun generateAnalyticsSummary(
        usageRecords: List<UsageRecordEntity>,
        appGroups: List<AppGroupEntity>
    ): AnalyticsSummary {
        val totalUsage = usageRecords.sumOf { it.usageMinutes.toLong() }
        val averageDaily = if (usageRecords.isNotEmpty()) {
            val days = usageRecords.groupBy { 
                Date(it.date).let { date ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                }
            }.size
            totalUsage / maxOf(days, 1)
        } else 0L

        val mostUsedApp = usageRecords
            .groupBy { it.appGroupName }
            .maxByOrNull { it.value.sumOf { record -> record.usageMinutes } }
            ?.key ?: "None"

        val topCategories = usageRecords
            .groupBy { it.appGroupName }
            .mapValues { (_, records) -> records.sumOf { it.usageMinutes.toLong() } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()

        return AnalyticsSummary(
            totalUsageTime = totalUsage,
            averageDailyUsage = averageDaily,
            mostUsedApp = mostUsedApp,
            longestStreak = 0, // Would calculate from usage data
            goalCompletionRate = 0.0f, // Would calculate from goals
            topCategories = topCategories
        )
    }

    // MARK: - Export Format Implementations

    private fun exportToCsv(data: ExportData, file: File, request: ExportRequest) {
        FileWriter(file).use { writer ->
            // Export metadata
            writer.appendLine("# AwayTime Data Export")
            writer.appendLine("# Export Date: ${dateFormatter.format(data.exportMetadata.exportDate)}")
            writer.appendLine("# Date Range: ${dateFormatter.format(request.dateRange.startDate)} to ${dateFormatter.format(request.dateRange.endDate)}")
            writer.appendLine("# Total Records: ${data.totalRecords}")
            writer.appendLine("")

            // Usage Records
            if (data.usageRecords.isNotEmpty()) {
                writer.appendLine("## Usage Records")
                writer.appendLine("Date,App Group,Usage Minutes,Limit Exceeded,Pickup Count")
                data.usageRecords.forEach { record ->
                    writer.appendLine("${dateFormatter.format(Date(record.date))},${record.appGroupName},${record.usageMinutes},${record.limitExceeded},${record.pickupCount}")
                }
                writer.appendLine("")
            }

            // App Groups
            if (data.appGroups.isNotEmpty()) {
                writer.appendLine("## App Groups")
                writer.appendLine("Name,Daily Limit (min),Active,Created Date,Apps")
                data.appGroups.forEach { group ->
                    writer.appendLine("${group.name},${group.dailyLimitMinutes},${group.isActive},${dateFormatter.format(Date(group.createdDate))},\"${group.selectedAppsJson}\"")
                }
                writer.appendLine("")
            }

            // Analytics Summary
            data.analytics?.let { analytics ->
                writer.appendLine("## Analytics Summary")
                writer.appendLine("Metric,Value")
                writer.appendLine("Total Usage Time (minutes),${analytics.totalUsageTime}")
                writer.appendLine("Average Daily Usage (minutes),${analytics.averageDailyUsage}")
                writer.appendLine("Most Used App,${analytics.mostUsedApp}")
                writer.appendLine("Longest Streak,${analytics.longestStreak}")
                writer.appendLine("Goal Completion Rate,${String.format("%.1f%%", analytics.goalCompletionRate * 100)}")
                writer.appendLine("")
                
                writer.appendLine("## Top Categories")
                writer.appendLine("Category,Usage Minutes")
                analytics.topCategories.forEach { (category, usage) ->
                    writer.appendLine("$category,$usage")
                }
            }
        }
    }

    private fun exportToJson(data: ExportData, file: File, request: ExportRequest) {
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"metadata\": {\n")
        jsonBuilder.append("    \"exportDate\": \"${dateFormatter.format(data.exportMetadata.exportDate)}\",\n")
        jsonBuilder.append("    \"appVersion\": \"${data.exportMetadata.appVersion}\",\n")
        jsonBuilder.append("    \"dateRange\": {\n")
        jsonBuilder.append("      \"start\": \"${dateFormatter.format(request.dateRange.startDate)}\",\n")
        jsonBuilder.append("      \"end\": \"${dateFormatter.format(request.dateRange.endDate)}\"\n")
        jsonBuilder.append("    },\n")
        jsonBuilder.append("    \"totalRecords\": ${data.totalRecords}\n")
        jsonBuilder.append("  },\n")

        // Usage Records
        if (data.usageRecords.isNotEmpty()) {
            jsonBuilder.append("  \"usageRecords\": [\n")
            data.usageRecords.forEachIndexed { index, record ->
                jsonBuilder.append("    {\n")
                jsonBuilder.append("      \"date\": \"${dateFormatter.format(Date(record.date))}\",\n")
                jsonBuilder.append("      \"appGroupName\": \"${record.appGroupName}\",\n")
                jsonBuilder.append("      \"usageMinutes\": ${record.usageMinutes},\n")
                jsonBuilder.append("      \"limitExceeded\": ${record.limitExceeded},\n")
                jsonBuilder.append("      \"pickupCount\": ${record.pickupCount}\n")
                jsonBuilder.append("    }")
                if (index < data.usageRecords.size - 1) jsonBuilder.append(",")
                jsonBuilder.append("\n")
            }
            jsonBuilder.append("  ],\n")
        }

        // App Groups
        if (data.appGroups.isNotEmpty()) {
            jsonBuilder.append("  \"appGroups\": [\n")
            data.appGroups.forEachIndexed { index, group ->
                jsonBuilder.append("    {\n")
                jsonBuilder.append("      \"name\": \"${group.name}\",\n")
                jsonBuilder.append("      \"dailyLimitMinutes\": ${group.dailyLimitMinutes},\n")
                jsonBuilder.append("      \"isActive\": ${group.isActive},\n")
                jsonBuilder.append("      \"createdDate\": \"${dateFormatter.format(Date(group.createdDate))}\",\n")
                jsonBuilder.append("      \"selectedApps\": ${group.selectedAppsJson}\n")
                jsonBuilder.append("    }")
                if (index < data.appGroups.size - 1) jsonBuilder.append(",")
                jsonBuilder.append("\n")
            }
            jsonBuilder.append("  ],\n")
        }

        // Analytics
        data.analytics?.let { analytics ->
            jsonBuilder.append("  \"analytics\": {\n")
            jsonBuilder.append("    \"totalUsageTime\": ${analytics.totalUsageTime},\n")
            jsonBuilder.append("    \"averageDailyUsage\": ${analytics.averageDailyUsage},\n")
            jsonBuilder.append("    \"mostUsedApp\": \"${analytics.mostUsedApp}\",\n")
            jsonBuilder.append("    \"longestStreak\": ${analytics.longestStreak},\n")
            jsonBuilder.append("    \"goalCompletionRate\": ${analytics.goalCompletionRate},\n")
            jsonBuilder.append("    \"topCategories\": {\n")
            analytics.topCategories.entries.forEachIndexed { index, (category, usage) ->
                jsonBuilder.append("      \"$category\": $usage")
                if (index < analytics.topCategories.size - 1) jsonBuilder.append(",")
                jsonBuilder.append("\n")
            }
            jsonBuilder.append("    }\n")
            jsonBuilder.append("  }\n")
        }

        jsonBuilder.append("}")

        file.writeText(jsonBuilder.toString())
    }

    private fun exportToPdf(data: ExportData, file: File, request: ExportRequest) {
        // PDF generation would require a PDF library like iText
        // For now, create a basic text representation
        val content = buildString {
            appendLine("AWAYTIME USAGE REPORT")
            appendLine("====================")
            appendLine()
            appendLine("Export Date: ${dateFormatter.format(data.exportMetadata.exportDate)}")
            appendLine("Report Period: ${dateFormatter.format(request.dateRange.startDate)} to ${dateFormatter.format(request.dateRange.endDate)}")
            appendLine("Total Records: ${data.totalRecords}")
            appendLine()

            data.analytics?.let { analytics ->
                appendLine("SUMMARY STATISTICS")
                appendLine("-----------------")
                appendLine("Total Usage Time: ${analytics.totalUsageTime} minutes")
                appendLine("Average Daily Usage: ${analytics.averageDailyUsage} minutes")
                appendLine("Most Used App: ${analytics.mostUsedApp}")
                appendLine("Current Streak: ${analytics.longestStreak} days")
                appendLine()

                appendLine("TOP CATEGORIES")
                appendLine("-------------")
                analytics.topCategories.forEach { (category, usage) ->
                    appendLine("$category: $usage minutes")
                }
                appendLine()
            }

            if (data.usageRecords.isNotEmpty()) {
                appendLine("DETAILED USAGE RECORDS")
                appendLine("----------------------")
                data.usageRecords.take(50).forEach { record -> // Limit for readability
                    appendLine("${dateFormatter.format(Date(record.date))}: ${record.appGroupName} - ${record.usageMinutes} min")
                }
            }
        }

        file.writeText(content)
    }

    private fun exportToHtml(data: ExportData, file: File, request: ExportRequest) {
        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html><head>")
            appendLine("<title>AwayTime Usage Report</title>")
            appendLine("<style>")
            appendLine("body { font-family: Arial, sans-serif; margin: 20px; }")
            appendLine("h1, h2 { color: #2196F3; }")
            appendLine("table { border-collapse: collapse; width: 100%; margin: 20px 0; }")
            appendLine("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            appendLine("th { background-color: #f2f2f2; }")
            appendLine(".summary { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0; }")
            appendLine("</style>")
            appendLine("</head><body>")
            
            appendLine("<h1>AwayTime Usage Report</h1>")
            appendLine("<div class='summary'>")
            appendLine("<p><strong>Export Date:</strong> ${dateFormatter.format(data.exportMetadata.exportDate)}</p>")
            appendLine("<p><strong>Report Period:</strong> ${dateFormatter.format(request.dateRange.startDate)} to ${dateFormatter.format(request.dateRange.endDate)}</p>")
            appendLine("<p><strong>Total Records:</strong> ${data.totalRecords}</p>")
            appendLine("</div>")

            data.analytics?.let { analytics ->
                appendLine("<h2>Summary Statistics</h2>")
                appendLine("<table>")
                appendLine("<tr><th>Metric</th><th>Value</th></tr>")
                appendLine("<tr><td>Total Usage Time</td><td>${analytics.totalUsageTime} minutes</td></tr>")
                appendLine("<tr><td>Average Daily Usage</td><td>${analytics.averageDailyUsage} minutes</td></tr>")
                appendLine("<tr><td>Most Used App</td><td>${analytics.mostUsedApp}</td></tr>")
                appendLine("<tr><td>Longest Streak</td><td>${analytics.longestStreak} days</td></tr>")
                appendLine("</table>")

                appendLine("<h2>Top Categories</h2>")
                appendLine("<table>")
                appendLine("<tr><th>Category</th><th>Usage (minutes)</th></tr>")
                analytics.topCategories.forEach { (category, usage) ->
                    appendLine("<tr><td>$category</td><td>$usage</td></tr>")
                }
                appendLine("</table>")
            }

            if (data.usageRecords.isNotEmpty()) {
                appendLine("<h2>Recent Usage Records</h2>")
                appendLine("<table>")
                appendLine("<tr><th>Date</th><th>App Group</th><th>Usage (min)</th><th>Limit Exceeded</th></tr>")
                data.usageRecords.take(100).forEach { record ->
                    appendLine("<tr>")
                    appendLine("<td>${dateFormatter.format(Date(record.date))}</td>")
                    appendLine("<td>${record.appGroupName}</td>")
                    appendLine("<td>${record.usageMinutes}</td>")
                    appendLine("<td>${if (record.limitExceeded) "Yes" else "No"}</td>")
                    appendLine("</tr>")
                }
                appendLine("</table>")
            }

            appendLine("</body></html>")
        }

        file.writeText(html)
    }

    // MARK: - Utility Functions

    private fun generateFileName(request: ExportRequest): String {
        val timestamp = fileDateFormatter.format(Date())
        val dataTypeSuffix = when {
            request.dataTypes.contains(ExportDataType.COMPREHENSIVE) -> "complete"
            request.dataTypes.size == 1 -> request.dataTypes.first().displayName.lowercase().replace(" ", "_")
            else -> "custom"
        }
        return "awaytime_export_${dataTypeSuffix}_$timestamp.${request.format.fileExtension}"
    }

    private fun createExportFile(fileName: String): File {
        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return File(exportDir, fileName)
    }

    private fun canExportData(): Boolean {
        val subscriptionService = SubscriptionManager.getService()
        return subscriptionService?.canUseFeature(PremiumFeature.EXPORT_DATA) ?: false
    }

    // MARK: - Streaming Export Methods for Large Datasets
    
    private suspend fun exportToCsvStreaming(file: File, request: ExportRequest): Int {
        var recordCount = 0
        
        FileWriter(file).use { writer ->
            // Export metadata
            writer.appendLine("# AwayTime Data Export")
            writer.appendLine("# Export Date: ${dateFormatter.format(Date())}")
            writer.appendLine("# Date Range: ${dateFormatter.format(request.dateRange.startDate)} to ${dateFormatter.format(request.dateRange.endDate)}")
            writer.appendLine("")
            
            // Stream usage records if requested
            if (request.dataTypes.contains(ExportDataType.USAGE_RECORDS) || 
                request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
                
                writer.appendLine("## Usage Records")
                writer.appendLine("Date,App Group,Usage Minutes,Limit Exceeded,Pickup Count")
                
                // Process in batches to avoid memory issues
                val batchSize = 1000
                var offset = 0
                
                do {
                    val batch = database.usageRecordDao().getUsageRecordsForDateRangePaged(
                        startDate = request.dateRange.startDate.time,
                        endDate = request.dateRange.endDate.time,
                        limit = batchSize,
                        offset = offset
                    )
                    
                    batch.forEach { record ->
                        writer.appendLine("${dateFormatter.format(Date(record.date))},${record.appGroupName},${record.usageMinutes},${record.limitExceeded},${record.pickupCount}")
                        recordCount++
                    }
                    
                    // Update progress
                    _exportProgress.value = minOf(0.8f, offset.toFloat() / 10000f) // Estimate based on typical data size
                    
                    offset += batchSize
                    
                    // Yield to prevent blocking
                    if (offset % (batchSize * 5) == 0) {
                        delay(10)
                    }
                    
                } while (batch.size == batchSize)
                
                writer.appendLine("")
            }
            
            // Add other data types without streaming (typically smaller)
            if (request.dataTypes.contains(ExportDataType.APP_GROUPS) ||
                request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
                
                writer.appendLine("## App Groups")
                writer.appendLine("Name,Daily Limit (min),Active,Created Date,Apps")
                
                val appGroups = database.appGroupDao().getAllAppGroupsSync()
                appGroups.forEach { group ->
                    writer.appendLine("${group.name},${group.dailyLimitMinutes},${group.isActive},${dateFormatter.format(Date(group.createdDate))},\"${group.selectedAppsJson}\"")
                    recordCount++
                }
                writer.appendLine("")
            }
        }
        
        return recordCount
    }
    
    private suspend fun exportToJsonStreaming(file: File, request: ExportRequest): Int {
        var recordCount = 0
        
        file.bufferedWriter().use { writer ->
            writer.write("{\n")
            writer.write("  \"metadata\": {\n")
            writer.write("    \"exportDate\": \"${dateFormatter.format(Date())}\",\n")
            writer.write("    \"dateRange\": {\n")
            writer.write("      \"start\": \"${dateFormatter.format(request.dateRange.startDate)}\",\n")
            writer.write("      \"end\": \"${dateFormatter.format(request.dateRange.endDate)}\"\n")
            writer.write("    }\n")
            writer.write("  },\n")
            
            // Stream usage records
            if (request.dataTypes.contains(ExportDataType.USAGE_RECORDS) ||
                request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
                
                writer.write("  \"usageRecords\": [\n")
                
                val batchSize = 1000
                var offset = 0
                var isFirstRecord = true
                
                do {
                    val batch = database.usageRecordDao().getUsageRecordsForDateRangePaged(
                        startDate = request.dateRange.startDate.time,
                        endDate = request.dateRange.endDate.time,
                        limit = batchSize,
                        offset = offset
                    )
                    
                    batch.forEach { record ->
                        if (!isFirstRecord) writer.write(",\n")
                        isFirstRecord = false
                        
                        writer.write("    {\n")
                        writer.write("      \"date\": \"${dateFormatter.format(Date(record.date))}\",\n")
                        writer.write("      \"appGroupName\": \"${record.appGroupName}\",\n")
                        writer.write("      \"usageMinutes\": ${record.usageMinutes},\n")
                        writer.write("      \"limitExceeded\": ${record.limitExceeded},\n")
                        writer.write("      \"pickupCount\": ${record.pickupCount}\n")
                        writer.write("    }")
                        
                        recordCount++
                    }
                    
                    // Update progress
                    _exportProgress.value = minOf(0.8f, offset.toFloat() / 10000f)
                    
                    offset += batchSize
                    
                    // Yield to prevent blocking
                    if (offset % (batchSize * 5) == 0) {
                        delay(10)
                    }
                    
                } while (batch.size == batchSize)
                
                writer.write("\n  ]\n")
            }
            
            writer.write("}")
        }
        
        return recordCount
    }
    
    private suspend fun exportToPdfStreaming(file: File, request: ExportRequest): Int {
        // For now, use the same logic as the non-streaming version
        // In a production app, you'd want to use a proper PDF library with streaming support
        val exportData = collectExportDataSafely(request)
        exportToPdf(exportData, file, request)
        return exportData.totalRecords
    }
    
    private suspend fun exportToHtmlStreaming(file: File, request: ExportRequest): Int {
        // For HTML, we can stream the content efficiently
        var recordCount = 0
        
        file.bufferedWriter().use { writer ->
            // HTML Header
            writer.write("<!DOCTYPE html>\n")
            writer.write("<html><head>\n")
            writer.write("<title>AwayTime Usage Report</title>\n")
            writer.write("<style>\n")
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; }\n")
            writer.write("h1, h2 { color: #2196F3; }\n")
            writer.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n")
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            writer.write("th { background-color: #f2f2f2; }\n")
            writer.write("</style>\n")
            writer.write("</head><body>\n")
            
            writer.write("<h1>AwayTime Usage Report</h1>\n")
            writer.write("<p>Report Period: ${dateFormatter.format(request.dateRange.startDate)} to ${dateFormatter.format(request.dateRange.endDate)}</p>\n")
            
            // Stream usage records table
            if (request.dataTypes.contains(ExportDataType.USAGE_RECORDS) ||
                request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
                
                writer.write("<h2>Usage Records</h2>\n")
                writer.write("<table>\n")
                writer.write("<tr><th>Date</th><th>App Group</th><th>Usage (min)</th><th>Limit Exceeded</th></tr>\n")
                
                val batchSize = 1000
                var offset = 0
                
                do {
                    val batch = database.usageRecordDao().getUsageRecordsForDateRangePaged(
                        startDate = request.dateRange.startDate.time,
                        endDate = request.dateRange.endDate.time,
                        limit = batchSize,
                        offset = offset
                    )
                    
                    batch.forEach { record ->
                        writer.write("<tr>")
                        writer.write("<td>${dateFormatter.format(Date(record.date))}</td>")
                        writer.write("<td>${record.appGroupName}</td>")
                        writer.write("<td>${record.usageMinutes}</td>")
                        writer.write("<td>${if (record.limitExceeded) "Yes" else "No"}</td>")
                        writer.write("</tr>\n")
                        recordCount++
                    }
                    
                    // Update progress
                    _exportProgress.value = minOf(0.8f, offset.toFloat() / 10000f)
                    
                    offset += batchSize
                    
                    // Yield to prevent blocking
                    if (offset % (batchSize * 5) == 0) {
                        delay(10)
                    }
                    
                } while (batch.size == batchSize)
                
                writer.write("</table>\n")
            }
            
            writer.write("</body></html>\n")
        }
        
        return recordCount
    }
    
    private suspend fun collectExportDataSafely(request: ExportRequest): ExportData {
        // Safer version that doesn't load all data at once
        val usageRecords = if (request.dataTypes.contains(ExportDataType.USAGE_RECORDS) ||
                              request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
            // Limit to reasonable amount for memory safety
            database.usageRecordDao().getUsageRecordsForDateRangePaged(
                startDate = request.dateRange.startDate.time,
                endDate = request.dateRange.endDate.time,
                limit = 1000,
                offset = 0
            )
        } else emptyList()
        
        val appGroups = if (request.dataTypes.contains(ExportDataType.APP_GROUPS) ||
                           request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
            database.appGroupDao().getAllAppGroupsSync()
        } else emptyList()
        
        val userSettings = if (request.dataTypes.contains(ExportDataType.SETTINGS) ||
                              request.dataTypes.contains(ExportDataType.COMPREHENSIVE)) {
            database.userSettingsDao().getUserSettingsSync()
        } else null
        
        return ExportData(
            usageRecords = usageRecords,
            appGroups = appGroups,
            userSettings = userSettings,
            exportMetadata = ExportMetadata(
                dataTypes = request.dataTypes,
                dateRange = request.dateRange,
                totalRecords = usageRecords.size + appGroups.size + (if (userSettings != null) 1 else 0)
            )
        )
    }

    // MARK: - Predefined Export Templates

    fun getQuickExportTemplates(): List<ExportRequest> {
        return listOf(
            ExportRequest(
                dataTypes = setOf(ExportDataType.USAGE_RECORDS),
                format = ExportFormat.CSV,
                dateRange = DateRange.last30Days()
            ),
            ExportRequest(
                dataTypes = setOf(ExportDataType.ANALYTICS_SUMMARY),
                format = ExportFormat.PDF,
                dateRange = DateRange.last30Days()
            ),
            ExportRequest(
                dataTypes = setOf(ExportDataType.COMPREHENSIVE),
                format = ExportFormat.JSON,
                dateRange = DateRange.allTime()
            )
        )
    }
}
