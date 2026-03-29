package org.richinet.musicbackend.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.richinet.musicbackend.data.entity.Track
import org.richinet.musicbackend.data.entity.TrackFile
import org.richinet.musicbackend.data.repository.*
import org.mockito.ArgumentMatchers.any

class MusicImportServiceTest {

    private val trackRepository = mock(TrackRepository::class.java)
    private val tagRepository = mock(TagRepository::class.java)
    private val tagTypeRepository = mock(TagTypeRepository::class.java)
    private val trackFileRepository = mock(TrackFileRepository::class.java)
    private val trackTagRepository = mock(TrackTagRepository::class.java)
    private val trackDataService = mock(TrackDataService::class.java)

    private val musicImportService = MusicImportService(
        trackRepository,
        tagRepository,
        tagTypeRepository,
        trackFileRepository,
        trackTagRepository,
        trackDataService
    )

    @Test
    fun `importMusicData should import tracks and their files`() {
        val trackData = listOf(
            mapOf(
                "TrackName" to "Saviour (Vox)",
                "Files" to listOf(
                    mapOf(
                        "FileName" to "VNV Nation - Saviour (Vox).mp3",
                        "FileLocation" to "/",
                        "Duration" to 417
                    )
                )
            )
        )

        `when`(tagTypeRepository.findAll()).thenReturn(emptyList())
        `when`(trackRepository.save(any(Track::class.java))).thenAnswer { 
            val track = it.arguments[0] as Track
            track.id = 1L
            track 
        }

        musicImportService.importMusicData(trackData)

        verify(trackRepository, times(1)).save(any(Track::class.java))
        verify(trackFileRepository, times(1)).save(any(TrackFile::class.java))
    }
}
