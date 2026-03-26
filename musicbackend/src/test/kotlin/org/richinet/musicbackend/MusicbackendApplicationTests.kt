package org.richinet.musicbackend

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.richinet.musicbackend.controller.MusicDbController
import org.richinet.musicbackend.data.entity.Track
import org.richinet.musicbackend.data.projection.TagProjection
import org.richinet.musicbackend.data.repository.TagTypeRepository
import org.richinet.musicbackend.data.repository.TagRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(MusicDbController::class, org.richinet.musicbackend.config.JacksonConfig::class)
class MusicbackendApplicationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var trackRepository: TrackRepository

    @MockitoBean
    private lateinit var trackDataService: TrackDataService

    @MockitoBean
    private lateinit var tagRepository: TagRepository

    @MockitoBean
    private lateinit var tagTypeRepository: TagTypeRepository

    @MockitoBean
    private lateinit var trackFileRepository: TrackFileRepository

    @MockitoBean
    private lateinit var musicImportService: MusicImportService

    @MockitoBean
    private lateinit var jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate

    @MockitoBean
    private lateinit var audioHandler: org.springframework.web.servlet.resource.ResourceHttpRequestHandler


    @Test
    fun `getTrack should return track data when found`() {
        val trackId = 1L
        val track = Track().apply { this.id = trackId }
        val trackData = mapOf("trackId" to trackId, "trackName" to "Test Track")

        `when`(trackRepository.findById(trackId)).thenReturn(Optional.of(track))
        `when`(trackDataService.serializeTrack(track)).thenReturn(trackData)

        mockMvc.perform(get("/api/track/$trackId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.trackId").value(trackId))
            .andExpect(jsonPath("$.trackName").value("Test Track"))
    }

    @Test
    fun `getTrack should return 404 when not found`() {
        val trackId = 99L
        `when`(trackRepository.findById(trackId)).thenReturn(Optional.empty())

        mockMvc.perform(get("/api/track/$trackId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getTags should return list of tags`() {
        val projection = object : TagProjection {
            override fun getTagTypeId(): Long = 1L
            override fun getTagTypeName(): String = "Genre"
            override fun getTagId(): Long = 100L
            override fun getTagName(): String = "Rock"
            override fun getTagTypeEdit(): String = "S"
        }

        `when`(tagRepository.findAllEditableTags()).thenReturn(listOf(projection))

        mockMvc.perform(get("/api/tags"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].tagTypeName").value("Genre"))
            .andExpect(jsonPath("$[0].tagName").value("Rock"))
    }

    @Test
    fun `getTracksByTag should return list of tracks`() {
        val tagId = 10L
        val track = Track().apply { id = 1L }
        val trackData = mapOf("trackId" to 1L, "trackName" to "Tag Track")

        `when`(trackRepository.findTracksByTagId(tagId)).thenReturn(listOf(track))
        `when`(trackDataService.serializeTrack(track)).thenReturn(trackData)

        mockMvc.perform(get("/api/tracksByTag/$tagId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].trackName").value("Tag Track"))
    }

    @Test
    fun `getTrackFile should return 404 when file record not found`() {
        val fileId = 1L
        `when`(trackFileRepository.findById(fileId)).thenReturn(Optional.empty())

        mockMvc.perform(get("/api/trackFile/$fileId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `updateTrack should return 200 when successful`() {
        val trackId = 1L
        val trackData = mapOf("trackName" to "Updated Track")

        doNothing().`when`(musicImportService).updateTrack(trackId, trackData)

        mockMvc.perform(post("/api/track/$trackId")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"trackName": "Updated Track"}"""))
            .andExpect(status().isOk)
    }

    @Test
    fun `updateTrack should return 404 when track not found`() {
        val trackId = 99L
        val trackData = mapOf("trackName" to "Updated Track")

        doThrow(RuntimeException("Track not found")).`when`(musicImportService).updateTrack(trackId, trackData)

        mockMvc.perform(post("/api/track/$trackId")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"trackName": "Updated Track"}"""))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `deleteTrack should return 200 when successful`() {
        val trackId = 1L

        doNothing().`when`(musicImportService).deleteTrack(trackId)

        mockMvc.perform(delete("/api/track/$trackId"))
            .andExpect(status().isOk)
    }

    @Test
    fun `deleteTrack should return 404 when track not found`() {
        val trackId = 99L

        doThrow(RuntimeException("Track not found")).`when`(musicImportService).deleteTrack(trackId)

        mockMvc.perform(delete("/api/track/$trackId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `searchTracks should return matching tracks`() {
        val query = "test"
        val track = Track().apply { id = 1L; name = "Test Song" }
        val trackData = mapOf("trackId" to 1L, "trackName" to "Test Song")

        `when`(trackRepository.searchTracks(query)).thenReturn(listOf(track))
        `when`(trackDataService.serializeTrack(track)).thenReturn(trackData)

        mockMvc.perform(get("/api/trackSearch").param("query", query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].trackName").value("Test Song"))
    }
}
