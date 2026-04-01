package org.richinet.musicandroid

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class ApiService(private val baseUrl: String) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    suspend fun getVersion(): BackendVersionInfo = client.get("$baseUrl/api/version").body()

    suspend fun getTags(): List<Tag> = client.get("$baseUrl/api/tags").body()

    suspend fun getTracksByTag(tagId: Long): List<Track> {
        val jsonList = client.get("$baseUrl/api/tracksByTag/$tagId").body<List<JsonObject>>()
        return jsonList.map { parseTrack(it) }
    }

    suspend fun searchTracks(query: String): List<Track> {
        val jsonList = client.get("$baseUrl/api/trackSearch") {
            parameter("query", query)
        }.body<List<JsonObject>>()
        return jsonList.map { parseTrack(it) }
    }

    suspend fun getTagUsageStats(): List<TagUsage> = client.get("$baseUrl/api/stats/tagUsage").body()

    suspend fun filterTracks(
        mustHaveIds: List<Long>,
        canHaveIds: List<Long>,
        mustNotHaveIds: List<Long>
    ): List<Track> {
        val request = FilterRequest(mustHaveIds, canHaveIds, mustNotHaveIds)
        val jsonList = client.post("$baseUrl/api/filterTracks") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<List<JsonObject>>()
        return jsonList.map { parseTrack(it) }
    }

    suspend fun createTag(tagType: String, tagName: String, trackIds: List<Long> = emptyList()): Tag {
        val request = CreateTagRequest(tagType, tagName, trackIds)
        return client.post("$baseUrl/api/tags") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    private fun parseTrack(jsonObject: JsonObject): Track {
        val trackId = jsonObject["trackId"]?.toString()?.toLong() ?: 0L
        val trackName = jsonObject["trackName"]?.toString()?.trim('"') ?: ""
        val filesJson = jsonObject["files"]?.let {
            Json.decodeFromJsonElement<List<TrackFile>>(it)
        } ?: emptyList()

        // Extract everything else as metadata
        val metadata = jsonObject.filter { it.key !in listOf("trackId", "trackName", "files") }

        return Track(trackId, trackName, filesJson, metadata)
    }

    fun getStreamUrl(fileId: Long): String = "$baseUrl/api/trackFile/$fileId"
}
