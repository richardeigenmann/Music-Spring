package org.richinet.musicbackend.config

import org.richinet.musicbackend.data.entity.GroupType
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import java.math.BigDecimal

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

    private fun createGroupType(id: String, name: String, edit: String): GroupType {
        val groupType = GroupType()
        groupType.groupTypeId = BigDecimal(id)
        groupType.groupTypeName = name
        groupType.groupTypeEdit = edit
        return groupType
    }
}
