package com.suptv.shared.service

import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

actual suspend fun downloadAndDecompressGzip(channel: ByteReadChannel): BufferedReader = withContext(Dispatchers.IO) {
    try {
        // Convert ByteReadChannel to InputStream and return reader for streaming
        val inputStream = channel.toInputStream()
        val gzipInputStream = GZIPInputStream(inputStream, 16384) // 16KB buffer
        BufferedReader(InputStreamReader(gzipInputStream, "UTF-8"), 16384)
    } catch (e: Exception) {
        e.printStackTrace()
        throw Exception("Failed to create gzip reader: ${e.message}", e)
    }
}
