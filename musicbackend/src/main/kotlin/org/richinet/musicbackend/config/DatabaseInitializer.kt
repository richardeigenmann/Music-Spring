package org.richinet.musicbackend.config

import org.richinet.musicbackend.data.entity.GroupType
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import java.math.BigDecimal

@Configuration
class DatabaseInitializer(private val appDefaults: AppDefaults) {

    @Bean
    @Order(1)
    fun initGroupTypes(repository: GroupTypeRepository): CommandLineRunner {
        return CommandLineRunner {
            if (repository.count() > 0) {
                println("GroupTypes already exist. Skipping initialization.")
                return@CommandLineRunner
            }

            println("Initializing GroupTypes from defaults...")
            val groupTypes = appDefaults.groupTypes.map { defaultType ->
                createGroupType(defaultType.id, defaultType.name, defaultType.edit)
            }
            repository.saveAll(groupTypes)
            println("Initialized ${groupTypes.size} GroupTypes.")
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
