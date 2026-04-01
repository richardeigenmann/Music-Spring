package org.richinet.musicbackend.service

import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

data class IntegrityCheckResult(
  val totalFilesChecked: Int,
  val brokenFiles: List<BrokenFileDetails>
)

data class BrokenFileDetails(
  val fileId: Long,
  val trackId: Long?,
  val fileName: String?,
  val fileLocation: String?,
  val absolutePath: String
)

@Service
class DatabaseIntegrityService(
  private val trackFileRepository: TrackFileRepository
) {
  private val logger = LoggerFactory.getLogger(DatabaseIntegrityService::class.java)

  @Value("\${app.music-directory}")
  private lateinit var musicDirectory: String

  fun checkFileIntegrity(): IntegrityCheckResult {
    val allTrackFiles = trackFileRepository.findAll()
    val brokenFiles = mutableListOf<BrokenFileDetails>()

    allTrackFiles.forEach { trackFile ->
      val fileLocation = trackFile.fileLocation?.trim('/') ?: ""
      val fileName = trackFile.fileName ?: ""
      
      val file = if (fileLocation.isEmpty()) {
        File(musicDirectory, fileName)
      } else {
        File(File(musicDirectory, fileLocation), fileName)
      }

      if (!file.exists()) {
        brokenFiles.add(
          BrokenFileDetails(
            fileId = trackFile.id!!,
            trackId = trackFile.trackId,
            fileName = trackFile.fileName,
            fileLocation = trackFile.fileLocation,
            absolutePath = file.absolutePath
          )
        )
      }
    }

    logger.info("Integrity check finished. Checked ${allTrackFiles.size} files, found ${brokenFiles.size} broken files.")
    return IntegrityCheckResult(
      totalFilesChecked = allTrackFiles.size,
      brokenFiles = brokenFiles
    )
  }
}
