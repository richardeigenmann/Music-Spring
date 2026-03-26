package org.richinet.musicbackend.config

import org.richinet.musicbackend.data.entity.GroupType
import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

@Component
open class DatabaseInitializer(
    private val appDefaults: AppDefaults,
    private val jdbcTemplate: JdbcTemplate,
    private val groupTypeRepository: GroupTypeRepository,
    private val groupsRepository: GroupsRepository
) {

    fun runInitialization() {
        initGroupTypes()
        initGroups()
    }

    @Bean
    @Order(0)
    fun initPostgresExtensions(): CommandLineRunner {
        return CommandLineRunner {
            try {
                println("Ensuring musicdatabase schema exists...")
                jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS musicdatabase")

                println("Ensuring fuzzystrmatch extension for SOUNDEX...")
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS fuzzystrmatch")
            } catch (e: Exception) {
                println("Warning: Database setup error (schema/extension). Error: ${e.message}")
            }
        }
    }

    fun initGroupTypes() {
        println("DatabaseInitializer checking GroupType count...")
        if (groupTypeRepository.count() > 0) {
            println("GroupTypes already exist in database. Skipping initialization.")
            return
        }

        val defaultCount = appDefaults.groupTypes.size
        println("Loaded $defaultCount GroupTypes from configuration.")

        if (defaultCount == 0) {
            println("WARNING: No GroupTypes found in configuration! Check if initial-data.yml is loaded.")
            return
        }

        println("Initializing GroupTypes from defaults...")
        val groupTypes = appDefaults.groupTypes.map { defaultType ->
            GroupType().apply {
                groupTypeId = BigDecimal(defaultType.id)
                groupTypeName = defaultType.name
                groupTypeEdit = defaultType.edit
            }
        }
        groupTypeRepository.saveAll(groupTypes)
        println("Successfully initialized ${groupTypes.size} GroupTypes.")
    }

    fun initGroups() {
        println("DatabaseInitializer checking Groups count...")
        if (groupsRepository.count() > 0) {
            println("Groups already exist in database. Skipping initialization.")
            return
        }

        val defaultCount = appDefaults.groups.size
        println("Loaded $defaultCount Groups from configuration.")

        if (defaultCount == 0) {
            println("WARNING: No Groups found in configuration! Check if initial-data.yml is loaded.")
            return
        }

        println("Initializing Groups from defaults...")
        val groupsList = appDefaults.groups.map { defaultGroup ->
            Groups().apply {
                groupTypeId = BigDecimal(defaultGroup.typeId)
                groupName = defaultGroup.name
                lastModification = Timestamp.from(Instant.now())
            }
        }
        groupsRepository.saveAll(groupsList)
        println("Successfully initialized ${groupsList.size} Groups.")
    }
}
