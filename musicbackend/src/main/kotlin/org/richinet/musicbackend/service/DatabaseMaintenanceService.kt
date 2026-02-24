package org.richinet.musicbackend.service

import org.richinet.musicbackend.data.repository.*
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File

@Service
class DatabaseMaintenanceService(
  private val jdbcTemplate: JdbcTemplate,
  private val trackGroupRepository: TrackGroupRepository,
  private val trackFileRepository: TrackFileRepository,
  private val trackRepository: TrackRepository,
  private val groupsRepository: GroupsRepository
) {

  private val logger = LoggerFactory.getLogger(DatabaseMaintenanceService::class.java)

  fun dumpDatabase(path: String) {
    val targetFile = File(path)
    if (targetFile.exists()) {
      targetFile.delete()
      logger.info("Deleted existing dump file at $path")
    }
    val sql = "SCRIPT TO '$path'"
    jdbcTemplate.execute(sql)
    logger.info("Database dumped to $path")
  }

  fun restoreDatabase(path: String) {
    val file = File(path)
    if (file.exists()) {
      // Drop all objects to ensure a clean state before restoring
      jdbcTemplate.execute("DROP ALL OBJECTS")
      val sql = "RUNSCRIPT FROM '$path'"
      jdbcTemplate.execute(sql)
      logger.info("Database restored from $path")
    } else {
      println("Dump file not found at $path")
    }
  }

  @Transactional
  fun clearDatabase() {
    trackGroupRepository.deleteAllInBatch()
    trackFileRepository.deleteAllInBatch()
    trackRepository.deleteAllInBatch()
    groupsRepository.deleteAllInBatch()
    logger.info("Database cleared (except GroupType)")
  }
}
