package org.richinet.musicbackend

import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.richinet.musicbackend.controller.MusicDbController
import org.richinet.musicbackend.data.entity.Track
import org.richinet.musicbackend.data.projection.GroupProjection
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.util.*

@WebMvcTest(MusicDbController::class)
class MusicbackendApplicationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var trackRepository: TrackRepository

    @MockBean
    private lateinit var trackDataService: TrackDataService

    @MockBean
    private lateinit var groupsRepository: GroupsRepository

    @MockBean
    private lateinit var groupTypeRepository: GroupTypeRepository

    @MockBean
    private lateinit var trackFileRepository: TrackFileRepository

    @MockBean
    private lateinit var musicImportService: MusicImportService

    @MockBean
    private lateinit var jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate

    @MockBean
    private lateinit var audioHandler: org.springframework.web.servlet.resource.ResourceHttpRequestHandler


    @Test
    fun `getTrack should return track data when found`() {
        val trackId = 1L
        val track = Track().apply { this.trackId = trackId }
        val trackData = mapOf("TrackId" to trackId, "TrackName" to "Test Track")

        `when`(trackRepository.findById(trackId)).thenReturn(Optional.of(track))
        `when`(trackDataService.serializeTrack(track)).thenReturn(trackData)

        mockMvc.perform(get("/api/track/$trackId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.TrackId").value(trackId))
            .andExpect(jsonPath("$.TrackName").value("Test Track"))
    }

    @Test
    fun `getTrack should return 404 when not found`() {
        val trackId = 99L
        `when`(trackRepository.findById(trackId)).thenReturn(Optional.empty())

        mockMvc.perform(get("/api/track/$trackId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getGroups should return list of groups`() {
        val projection = object : GroupProjection {
            override fun getGroupTypeId(): BigDecimal = BigDecimal(1)
            override fun getGroupTypeName(): String = "Genre"
            override fun getGroupId(): Long = 100
            override fun getGroupName(): String = "Rock"
        }

        `when`(groupsRepository.findAllEditableGroups()).thenReturn(listOf(projection))

        mockMvc.perform(get("/api/groups"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].groupTypeName").value("Genre"))
            .andExpect(jsonPath("$[0].groupName").value("Rock"))
    }

    @Test
    fun `getTracksByGroup should return list of tracks`() {
        val groupId = 10L
        val track = Track().apply { trackId = 1L }
        val trackData = mapOf("TrackId" to 1L, "TrackName" to "Group Track")

        `when`(trackRepository.findTracksByGroupId(groupId)).thenReturn(listOf(track))
        `when`(trackDataService.serializeTrack(track)).thenReturn(trackData)

        mockMvc.perform(get("/api/tracksByGroup/$groupId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].TrackName").value("Group Track"))
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
        val trackData = mapOf("TrackName" to "Updated Track")

        doNothing().`when`(musicImportService).updateTrack(trackId, trackData)

        mockMvc.perform(post("/api/track/$trackId")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"TrackName": "Updated Track"}"""))
            .andExpect(status().isOk)
    }

    @Test
    fun `updateTrack should return 404 when track not found`() {
        val trackId = 99L
        val trackData = mapOf("TrackName" to "Updated Track")

        doThrow(RuntimeException("Track not found")).`when`(musicImportService).updateTrack(trackId, trackData)

        mockMvc.perform(post("/api/track/$trackId")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"TrackName": "Updated Track"}"""))
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
        val track = Track().apply { trackId = 1L; trackName = "Test Song" }
        val trackData = mapOf("TrackId" to 1L, "TrackName" to "Test Song")

        `when`(trackRepository.searchTracks(query)).thenReturn(listOf(track))
        `when`(trackDataService.serializeTrack(track)).thenReturn(trackData)

        mockMvc.perform(get("/api/trackSearch").param("query", query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].TrackName").value("Test Song"))
    }
}
