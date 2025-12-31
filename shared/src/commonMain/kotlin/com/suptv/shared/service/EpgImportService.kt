package com.suptv.shared.service

import android.util.Log
import com.suptv.shared.db.SupTvDatabase
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

expect suspend fun downloadAndDecompressGzip(channel: ByteReadChannel): java.io.BufferedReader

class EpgImportService(
    private val database: SupTvDatabase,
    private val httpClient: HttpClient
) {
    
    suspend fun importEpgFromUrl(
        url: String,
        providerId: Long,
        onProgress: (progress: Float, message: String) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.Default) {
        try {
            // Clear old EPG data before importing to avoid duplicates
            println("Clearing old EPG data...")
            onProgress(0.05f, "Clearing old EPG data...")
            database.epgProgramQueries.deleteAll()
            database.epgChannelQueries.deleteAll()
            
            // Download EPG data with streaming
            onProgress(0.1f, "Downloading EPG file...")
            val response: HttpResponse = httpClient.get(url)
            
            // Process with streaming decompression - NO memory loading
            onProgress(0.15f, "Processing EPG data...")
            val count = if (url.endsWith(".gz")) {
                val reader = downloadAndDecompressGzip(response.bodyAsChannel())
                val result = parseAndImportXmlTvStreaming(reader, providerId, onProgress)
                reader.close()
                result
            } else {
                val xmlContent = response.bodyAsText()
                parseAndImportXmlTvChunked(xmlContent, providerId, onProgress)
            }
            
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun parseAndImportXmlTvStreaming(
        reader: java.io.BufferedReader, 
        providerId: Long,
        onProgress: (progress: Float, message: String) -> Unit = { _, _ -> }
    ): Int {
        var programCount = 0
        var channelCount = 0
        val channelMap = mutableMapOf<String, Long>()
        
        // Batch buffers
        data class ChannelData(val id: String, val displayName: String)
        data class ProgramData(
            val channelId: String, val title: String, val description: String?,
            val startTime: Long, val endTime: Long, val category: String?, val icon: String?
        )
        
        val channelBatch = mutableListOf<ChannelData>()
        val programBatch = mutableListOf<ProgramData>()
        val batchSize = 2000
        
        // State machine for streaming XML parsing
        val buffer = StringBuilder(2048)
        var currentTag: String? = null
        var insideChannel = false
        var insideProgram = false
        
        println("Starting streaming EPG parse...")
        
        reader.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                
                // Channel parsing
                when {
                    trimmed.startsWith("<channel ") -> {
                        insideChannel = true
                        buffer.clear()
                        buffer.append(trimmed)
                    }
                    insideChannel -> {
                        buffer.append(" ").append(trimmed)
                        if (trimmed.contains("</channel>")) {
                            insideChannel = false
                            try {
                                val channelXml = buffer.toString()
                                val idMatch = """id="([^"]+)"""".toRegex().find(channelXml)
                                val nameMatch = """<display-name>([^<]+)</display-name>""".toRegex().find(channelXml)
                                
                                if (idMatch != null && nameMatch != null) {
                                    val channelId = idMatch.groupValues[1]
                                    val displayName = nameMatch.groupValues[1].trim()
                                    
                                    channelBatch.add(ChannelData(channelId, displayName))
                                    channelCount++
                                    
                                    // Batch insert channels
                                    if (channelBatch.size >= batchSize) {
                                        database.transaction {
                                            channelBatch.forEach { channel ->
                                                database.epgChannelQueries.insert(
                                                    id = channel.id,
                                                    displayName = channel.displayName,
                                                    icon = null,
                                                    url = null
                                                )
                                            }
                                        }
                                        val progress = 0.2f + (channelCount.toFloat() / 100000f) * 0.1f
                                        onProgress(progress.coerceAtMost(0.3f), "Importing channels: $channelCount")
                                        // println("Inserted ${channelBatch.size} channels (total: $channelCount)...")
                                        channelBatch.clear()
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip invalid channel
                            }
                        }
                    }
                    
                    // Program parsing
                    trimmed.startsWith("<programme ") -> {
                        insideProgram = true
                        buffer.clear()
                        buffer.append(trimmed)
                    }
                    insideProgram -> {
                        buffer.append(" ").append(trimmed)
                        if (trimmed.contains("</programme>")) {
                            insideProgram = false
                            try {
                                val programXml = buffer.toString()
                                val channelMatch = """channel="([^"]+)"""".toRegex().find(programXml)
                                val startMatch = """start="([^"]+)""".toRegex().find(programXml)
                                val stopMatch = """stop="([^"]+)""".toRegex().find(programXml)
                                val titleMatch = """<title[^>]*>([^<]+)</title>""".toRegex().find(programXml)
                                
                                if (channelMatch != null && startMatch != null && stopMatch != null && titleMatch != null) {
                                    val channelId = channelMatch.groupValues[1]
                                    val title = titleMatch.groupValues[1].trim()
                                    val startTime = parseXmlTvTime(startMatch.groupValues[1])
                                    val endTime = parseXmlTvTime(stopMatch.groupValues[1])
                                    
                                    val descMatch = """<desc[^>]*>([^<]+)</desc>""".toRegex().find(programXml)
                                    val description = descMatch?.groupValues?.get(1)?.trim()
                                    
                                    val catMatch = """<category[^>]*>([^<]+)</category>""".toRegex().find(programXml)
                                    val category = catMatch?.groupValues?.get(1)?.trim()
                                    
                                    val iconMatch = """<icon src="([^"]+)"""".toRegex().find(programXml)
                                    val icon = iconMatch?.groupValues?.get(1)
                                    
                                    if (startTime > 0 && endTime > 0) {
                                        programBatch.add(ProgramData(channelId, title, description, startTime, endTime, category, icon))
                                        programCount++
                                        
                                        // Batch insert programs
                                        if (programBatch.size >= batchSize) {
                                            database.transaction {
                                                programBatch.forEach { program ->
                                                    database.epgProgramQueries.insert(
                                                        channelId = program.channelId,
                                                        title = program.title,
                                                        description = program.description,
                                                        startTime = program.startTime,
                                                        endTime = program.endTime,
                                                        category = program.category,
                                                        icon = program.icon
                                                    )
                                                }
                                            }
                                            val progress = 0.3f + (programCount.toFloat() / 200000f) * 0.6f
                                            onProgress(progress.coerceAtMost(0.9f), "Importing programs: $programCount")
                                            // println("Inserted ${programBatch.size} programs (total: $programCount)...")
                                            programBatch.clear()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip invalid program
                            }
                        }
                    }
                }
            }
        }
        
        // Insert remaining batches
        if (channelBatch.isNotEmpty()) {
            database.transaction {
                channelBatch.forEach { channel ->
                    database.epgChannelQueries.insert(
                        id = channel.id,
                        displayName = channel.displayName,
                        icon = null,
                        url = null
                    )
                }
            }
            println("Inserted final ${channelBatch.size} channels")
        }
        
        if (programBatch.isNotEmpty()) {
            onProgress(0.95f, "Finalizing import...")
            database.transaction {
                programBatch.forEach { program ->
                    database.epgProgramQueries.insert(
                        channelId = program.channelId,
                        title = program.title,
                        description = program.description,
                        startTime = program.startTime,
                        endTime = program.endTime,
                        category = program.category,
                        icon = program.icon
                    )
                }
            }
            println("Inserted final ${programBatch.size} programs")
        }
        
        onProgress(1.0f, "Import complete!")
        println("EPG import complete: $channelCount channels, $programCount programs")
        return programCount
    }
    
    private fun parseAndImportXmlTvChunked(
        xmlContent: String, 
        providerId: Long,
        onProgress: (progress: Float, message: String) -> Unit = { _, _ -> }
    ): Int {
        var programCount = 0
        
        // Process channels first with optimized regex
        val channelRegex = """<channel id="([^"]+)">.*?<display-name>([^<]+)</display-name>""".toRegex()
        
        // Parse channels in batches
        val channelMatches = channelRegex.findAll(xmlContent)
        channelMatches.forEach { match ->
            try {
                val channelId = match.groupValues[1]
                val displayName = match.groupValues[2].trim()
                
                database.epgChannelQueries.insert(
                    id = channelId,
                    displayName = displayName,
                    icon = null,
                    url = null
                )
            } catch (e: Exception) {
                // Skip invalid channel, continue processing
            }
        }
        
        // Process programs in smaller chunks to avoid regex catastrophic backtracking
        // Split by program tags first
        val programChunks = xmlContent.split("<programme")
        
        for (chunk in programChunks) {
            if (!chunk.contains("channel=")) continue
            
            try {
                // Extract basic attributes
                val channelMatch = """channel="([^"]+)"""".toRegex().find(chunk)
                val startMatch = """start="([^"]+)""".toRegex().find(chunk)
                val stopMatch = """stop="([^"]+)""".toRegex().find(chunk)
                val titleMatch = """<title[^>]*>([^<]+)</title>""".toRegex().find(chunk)
                
                if (channelMatch != null && startMatch != null && stopMatch != null && titleMatch != null) {
                    val channelId = channelMatch.groupValues[1]
                    val startStr = startMatch.groupValues[1]
                    val stopStr = stopMatch.groupValues[1]
                    val title = titleMatch.groupValues[1].trim()
                    
                    // Parse optional fields
                    val descMatch = """<desc[^>]*>([^<]+)</desc>""".toRegex().find(chunk)
                    val description = descMatch?.groupValues?.get(1)?.trim()
                    
                    val catMatch = """<category[^>]*>([^<]+)</category>""".toRegex().find(chunk)
                    val category = catMatch?.groupValues?.get(1)?.trim()
                    
                    val iconMatch = """<icon src="([^"]+)"""".toRegex().find(chunk)
                    val icon = iconMatch?.groupValues?.get(1)
                    
                    // Convert times
                    val startTime = parseXmlTvTime(startStr)
                    val endTime = parseXmlTvTime(stopStr)
                    
                    if (startTime > 0 && endTime > 0) {
                        database.epgProgramQueries.insert(
                            channelId = channelId,
                            title = title,
                            description = description,
                            startTime = startTime,
                            endTime = endTime,
                            category = category,
                            icon = icon
                        )
                        programCount++
                    }
                }
            } catch (e: Exception) {
                // Skip invalid program, continue processing
            }
        }
        
        return programCount
    }
    
    private fun parseXmlTvTime(timeStr: String): Long {
        // Format: YYYYMMDDHHmmss +HHMM
        // Example: 20231227140000 +0000 or 20231227140000 +0200 20251228122500 +0300
        try {
            // Extract date/time and timezone parts
            val dateTimePart = timeStr.substring(0, 14)
            val timezonePart = if (timeStr.length > 15) timeStr.substring(15).trim() else "+0000"
            
            // Parse the main datetime
            val year = dateTimePart.substring(0, 4).toInt()
            val month = dateTimePart.substring(4, 6).toInt() - 1 // Calendar month is 0-based
            val day = dateTimePart.substring(6, 8).toInt()
            val hour = dateTimePart.substring(8, 10).toInt()
            val minute = dateTimePart.substring(10, 12).toInt()
            val second = dateTimePart.substring(12, 14).toInt()
            
            // Parse timezone offset
            val tzSign = if (timezonePart.startsWith("-")) -1 else 1
            val tzHours = timezonePart.substring(1, 3).toInt()
            val tzMinutes = timezonePart.substring(3, 5).toInt()
            
            // Create calendar in UTC
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendar.set(year, month, day, hour, minute, second)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            // Adjust for the timezone offset in the EPG data
            // If EPG says "+0200", the time is 2 hours ahead of UTC, so we subtract
            val tzOffsetMillis = tzSign * (tzHours * 3600000L + tzMinutes * 60000L)
            // Log.i("EpgImportService", "Parsing time string: $timeStr, offset: $tzOffsetMillis, original time: ${calendar.time}")
            val utcTimeMillis = calendar.timeInMillis - tzOffsetMillis
            
            return utcTimeMillis
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }
}
