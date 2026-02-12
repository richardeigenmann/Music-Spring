package org.richinet.musicbackend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.richinet.musicbackend.controller.MusicDbController
import org.richinet.musicbackend.data.entity.Track
import org.richinet.musicbackend.data.entity.TrackFile
import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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
    private lateinit var trackFileRepository: TrackFileRepository

    @Test
    fun `getTrack should return track data when found`() {
        val trackId = 1L
        val track = Track().apply { this.trackId = trackId }
        val trackData = mapOf("TrackName" to "Test Track")

        `when`(trackRepository.findById(trackId)).thenReturn(Optional.of(track))
        `when`(trackDataService.serializeTrack(track)).thenReturn(trackData)

        mockMvc.perform(get("/api/track/$trackId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
    fun `getPlaylists should return list of playlists`() {
        val projection = object : PlaylistProjection {
            override fun getGroupId(): Long = 10
            override fun getGroupName(): String = "My Playlist"
            override fun getTracks(): Int = 5
        }

        `when`(groupsRepository.findPlaylistsByTypeId(BigDecimal(4))).thenReturn(listOf(projection))

        mockMvc.perform(get("/api/playlists"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].groupName").value("My Playlist"))
            .andExpect(jsonPath("$[0].tracks").value(5))
    }

    @Test
    fun `getTracksByGroup should return list of tracks`() {
        val groupId = 10L
        val track = Track().apply { trackId = 1L }
        val trackData = mapOf("TrackName" to "Group Track")

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
}
