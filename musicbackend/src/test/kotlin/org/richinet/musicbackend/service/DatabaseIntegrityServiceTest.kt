package org.richinet.musicbackend.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.richinet.musicbackend.data.entity.TrackFile
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.springframework.test.util.ReflectionTestUtils
import java.io.File
import java.nio.file.Files

class DatabaseIntegrityServiceTest {

  private val trackFileRepository = mock(TrackFileRepository::class.java)
  private val databaseIntegrityService = DatabaseIntegrityService(trackFileRepository)

  @Test
  fun `checkFileIntegrity should identify broken file paths`() {
    // Create a temporary directory for music files
    val tempDir = Files.createTempDirectory("music-test").toFile()
    val existingFile = File(tempDir, "exists.mp3")
    existingFile.createNewFile()

    ReflectionTestUtils.setField(databaseIntegrityService, "musicDirectory", tempDir.absolutePath)

    val trackFile1 = TrackFile().apply {
      id = 1L
      trackId = 10L
      fileName = "exists.mp3"
      fileLocation = "/"
    }

    val trackFile2 = TrackFile().apply {
      id = 2L
      trackId = 20L
      fileName = "missing.mp3"
      fileLocation = "/"
    }

    `when`(trackFileRepository.findAll()).thenReturn(listOf(trackFile1, trackFile2))

    val result = databaseIntegrityService.checkFileIntegrity()

    assertEquals(2, result.totalFilesChecked)
    assertEquals(1, result.brokenFiles.size)
    assertEquals(2L, result.brokenFiles[0].fileId)
    assertEquals("missing.mp3", result.brokenFiles[0].fileName)

    // Cleanup
    existingFile.delete()
    tempDir.delete()
  }
}
