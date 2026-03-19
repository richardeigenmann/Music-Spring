package org.richinet.musicbackend.config

import org.richinet.musicbackend.data.entity.GroupType
import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

@Configuration
open class DatabaseInitializer(private val appDefaults: AppDefaults) {

    @Bean
    @Order(1)
    fun initGroupTypes(repository: GroupTypeRepository): CommandLineRunner {
        return CommandLineRunner {
            println("DatabaseInitializer checking GroupType count...")
            if (repository.count() > 0) {
                println("GroupTypes already exist in database. Skipping initialization.")
                return@CommandLineRunner
            }

            val defaultCount = appDefaults.groupTypes.size
            println("Loaded $defaultCount GroupTypes from configuration.")
            
            if (defaultCount == 0) {
                println("WARNING: No GroupTypes found in configuration! Check if initial-data.yml is loaded.")
                return@CommandLineRunner
            }

            println("Initializing GroupTypes from defaults...")
            val groupTypes = appDefaults.groupTypes.map { defaultType ->
                createGroupType(defaultType.id, defaultType.name, defaultType.edit)
            }
            repository.saveAll(groupTypes)
            println("Successfully initialized ${groupTypes.size} GroupTypes.")
        }
    }

    @Bean
    @Order(2)
    fun initGroups(repository: GroupsRepository): CommandLineRunner {
        return CommandLineRunner {
            println("DatabaseInitializer checking Groups count...")
            if (repository.count() > 0) {
                println("Groups already exist in database. Skipping initialization.")
                return@CommandLineRunner
            }

            val defaultCount = appDefaults.groups.size
            println("Loaded $defaultCount Groups from configuration.")

            if (defaultCount == 0) {
                println("WARNING: No Groups found in configuration! Check if initial-data.yml is loaded.")
                return@CommandLineRunner
            }

            println("Initializing Groups from defaults...")
            val groupsList = appDefaults.groups.map { defaultGroup ->
                createGroup(defaultGroup.typeId, defaultGroup.name)
            }
            repository.saveAll(groupsList)
            println("Successfully initialized ${groupsList.size} Groups.")
        }
    }

    private fun createGroupType(id: String, name: String, edit: String): GroupType {
        val groupType = GroupType()
        groupType.groupTypeId = BigDecimal(id)
        groupType.groupTypeName = name
        groupType.groupTypeEdit = edit
        return groupType
    }

    private fun createGroup(typeId: String, name: String): Groups {
        val group = Groups()
        group.groupTypeId = BigDecimal(typeId)
        group.groupName = name
        group.lastModification = Timestamp.from(Instant.now())
        return group
    }
}
