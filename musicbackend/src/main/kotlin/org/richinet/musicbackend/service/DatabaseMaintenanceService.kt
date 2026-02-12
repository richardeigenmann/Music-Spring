package org.richinet.musicbackend.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.io.File

@Service
class DatabaseMaintenanceService(private val jdbcTemplate: JdbcTemplate) {

    fun dumpDatabase(path: String) {
        val sql = "SCRIPT TO '$path'"
        jdbcTemplate.execute(sql)
    }

    fun restoreDatabase(path: String) {
        val file = File(path)
        if (file.exists()) {
            // Drop all objects to ensure a clean state before restoring
            jdbcTemplate.execute("DROP ALL OBJECTS")
            val sql = "RUNSCRIPT FROM '$path'"
            jdbcTemplate.execute(sql)
        } else {
            println("Dump file not found at $path")
        }
    }
}
