package org.richinet.musicbackend.config

import org.richinet.musicbackend.data.entity.TagType
import org.richinet.musicbackend.data.entity.Tag
import org.richinet.musicbackend.data.repository.TagTypeRepository
import org.richinet.musicbackend.data.repository.TagRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.core.annotation.Order
import javax.sql.DataSource // Import DataSource

@Component
open class DatabaseInitializer(
    private val appDefaults: AppDefaults,
    private val jdbcTemplate: JdbcTemplate,
    private val tagTypeRepository: TagTypeRepository,
    private val tagRepository: TagRepository,
    private val dataSource: DataSource // Inject DataSource
) {

    fun runInitialization() {
        println("Running DB initialisation (if required)...")
        initTagTypes()
        initTags()
    }

    @Bean
    @Order(0)
    fun initPostgresExtensions(): CommandLineRunner {
        return CommandLineRunner {
            // Get the JDBC URL from the DataSource
            val connectionUrl = (dataSource.connection.metaData.url ?: "").lowercase()

            if (connectionUrl.contains("postgresql")) {
                println("Detected PostgreSQL. Running schema and extension setup...")
                try {
                    println("Ensuring musicdatabase schema exists...")
                    jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS musicdatabase")

                    println("Ensuring fuzzystrmatch extension for SOUNDEX...")
                    jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS fuzzystrmatch")
                } catch (e: Exception) {
                    println("Warning: Database setup error (schema/extension) for PostgreSQL. Error: ${e.message}")
                }
            } else if (connectionUrl.contains("h2")) {
                println("Detected H2 database. Skipping PostgreSQL-specific schema and extension setup.")
            } else {
                println("Detected unknown database type (URL: $connectionUrl). Skipping PostgreSQL-specific schema and extension setup.")
            }
        }
        }

        @Bean
        @Order(1) // Run after initPostgresExtensions (Order 0)
        fun initializeData(): CommandLineRunner {
            return CommandLineRunner {
                println("Running data initialization...")
                runInitialization() // This calls initTagTypes() and initTags()
            }
        }

        fun initTagTypes() {
            println("DatabaseInitializer checking TagType count...")
            if (tagTypeRepository.count() > 0) {
                println("TagTypes already exist in database. Skipping initialization.")
                return
            }

            val defaultCount = appDefaults.tagTypes.size
            println("Loaded $defaultCount TagTypes from configuration.")

            if (defaultCount == 0) {
                println("WARNING: No TagTypes found in configuration! Check if initial-data.yml is loaded.")
                return
            }

            println("Initializing TagTypes from defaults...")
            val tagTypes = appDefaults.tagTypes.map { defaultType ->
                TagType().apply {
                    id = defaultType.id.toLong()
                    name = defaultType.name
                    edit = defaultType.edit
                }
            }
            tagTypeRepository.saveAll(tagTypes)
            println("Successfully initialized ${tagTypes.size} TagTypes.")
        }

        fun initTags() {
            println("DatabaseInitializer checking Tags count...")
            if (tagRepository.count() > 0) {
                println("Tags already exist in database. Skipping initialization.")
                return
            }

            val defaultCount = appDefaults.tags.size
            println("Loaded $defaultCount Tags from configuration.")

            if (defaultCount == 0) {
                println("WARNING: No Tags found in configuration! Check if initial-data.yml is loaded.")
                return
            }

            println("Initializing Tags from defaults...")
            val tagsList = appDefaults.tags.map { defaultTag ->
                Tag().apply {
                    tagTypeId = defaultTag.typeId.toLong()
                    name = defaultTag.name
                }
            }
            tagRepository.saveAll(tagsList)
            println("Successfully initialized ${tagsList.size} Tags.")
        }
        }
