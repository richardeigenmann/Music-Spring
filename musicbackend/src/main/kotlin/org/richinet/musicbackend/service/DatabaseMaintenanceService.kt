package org.richinet.musicbackend.service

import org.richinet.musicbackend.config.DatabaseInitializer
import org.richinet.musicbackend.data.repository.*
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File

@Service
class DatabaseMaintenanceService(
  private val jdbcTemplate: JdbcTemplate,
  private val trackTagRepository: TrackTagRepository,
  private val trackFileRepository: TrackFileRepository,
  private val trackRepository: TrackRepository,
  private val tagRepository: TagRepository,
  private val tagTypeRepository: TagTypeRepository,
  private val databaseInitializer: DatabaseInitializer
) {

  private val logger = LoggerFactory.getLogger(DatabaseMaintenanceService::class.java)

  fun dumpDatabase(path: String) {
    val targetFile = File(path)
    if (targetFile.exists()) {
      targetFile.delete()
      logger.info("Deleted existing dump file at $path")
    }
    try {
      val sql = "SCRIPT TO '$path'"
      jdbcTemplate.execute(sql)
      logger.info("Database dumped to $path")
    } catch (e: Exception) {
      logger.error("Failed to dump database (likely not H2): ${e.message}")
    }
  }

  fun restoreDatabase(path: String) {
    val file = File(path)
    if (file.exists()) {
      try {
        // Drop all objects to ensure a clean state before restoring (H2 specific)
        jdbcTemplate.execute("DROP ALL OBJECTS")
        val sql = "RUNSCRIPT FROM '$path'"
        jdbcTemplate.execute(sql)
        logger.info("Database restored from $path")
      } catch (e: Exception) {
        logger.error("Failed to restore database from SQL dump (likely not H2 or syntax error): ${e.message}")
        throw e // Re-throw to let the caller handle it if needed
      }
    } else {
      println("Dump file not found at $path")
    }
  }

  @Transactional
  fun clearDatabase() {
    trackTagRepository.deleteAllInBatch()
    trackFileRepository.deleteAllInBatch()
    trackRepository.deleteAllInBatch()
    tagRepository.deleteAllInBatch()
    tagTypeRepository.deleteAllInBatch()
    logger.info("Database cleared (including TagType)")
    databaseInitializer.runInitialization()
  }
}
