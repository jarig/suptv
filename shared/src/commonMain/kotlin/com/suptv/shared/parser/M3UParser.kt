package com.suptv.shared.parser

import com.suptv.shared.model.Channel
import com.suptv.shared.model.Playlist

/**
 * Parser for M3U playlist format
 * 
 * M3U format example:
 * #EXTM3U
 * #EXTINF:-1 tvg-id="channel1" tvg-name="Channel Name" tvg-logo="http://logo.png" group-title="Group",Channel Name
 * http://stream.url/channel1
 */
class M3UParser {
    
    fun parse(content: String, playlistName: String = "M3U Playlist"): Playlist {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        
        var currentInfo: Map<String, String>? = null
        var channelName: String? = null
        
        for (line in lines) {
            val trimmed = line.trim()
            
            when {
                trimmed.startsWith("#EXTINF:") -> {
                    // Parse channel info line
                    val info = parseExtInf(trimmed)
                    currentInfo = info
                    channelName = info["name"] ?: info["tvg-name"]
                }
                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    // This is a stream URL
                    if (channelName != null && currentInfo != null) {
                        channels.add(
                            Channel(
                                id = currentInfo["tvg-id"] ?: channels.size.toString(),
                                name = channelName,
                                logo = currentInfo["tvg-logo"],
                                group = currentInfo["group-title"],
                                url = trimmed,
                                epgId = currentInfo["tvg-id"]
                            )
                        )
                    }
                    currentInfo = null
                    channelName = null
                }
            }
        }
        
        return Playlist(name = playlistName, channels = channels)
    }
    
    private fun parseExtInf(line: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // Extract attributes using regex
        val attrRegex = """(\w+(?:-\w+)*)="([^"]*)"""".toRegex()
        attrRegex.findAll(line).forEach { match ->
            result[match.groupValues[1]] = match.groupValues[2]
        }
        
        // Extract channel name (text after last comma)
        val lastComma = line.lastIndexOf(',')
        if (lastComma != -1) {
            result["name"] = line.substring(lastComma + 1).trim()
        }
        
        return result
    }
}
