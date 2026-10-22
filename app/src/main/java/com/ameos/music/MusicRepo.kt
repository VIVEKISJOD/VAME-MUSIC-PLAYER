package com.ameos.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class Song(
    val title: String,
    val artist: String,
    val url: String,
    val cover: String?,
    val durationSec: Long
)

object MusicRepo {

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.getSearchExtractor(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            ""
        )
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { item ->
                Song(
                    title = item.name ?: "Unknown",
                    artist = item.uploaderName ?: "",
                    url = item.url,
                    cover = bigCover(item.thumbnails.maxByOrNull { it.height }?.url),
                    durationSec = item.duration
                )
            }
    }

    suspend fun audioUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(videoUrl)
        val streams = info.audioStreams
        val progressive = streams.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
        val pool = if (progressive.isNotEmpty()) progressive else streams
        val best = pool.maxByOrNull { it.averageBitrate }
            ?: throw IllegalStateException("Is song ki audio nahi mili")
        best.content
    }

    private fun bigCover(url: String?): String? {
        if (url == null) return null
        return if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) {
            url.replace(Regex("=w\\d+-h\\d+.*$"), "=w600-h600-l90-rj")
        } else {
            url
        }
    }
}
