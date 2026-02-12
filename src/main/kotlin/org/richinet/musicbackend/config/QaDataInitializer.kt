package org.richinet.musicbackend.config

import org.richinet.musicbackend.data.entity.GroupType
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import java.math.BigDecimal

@Configuration
@Profile("qa")
class QaDataInitializer {

    @Bean
    @Order(1)
    fun initGroupTypes(repository: GroupTypeRepository): CommandLineRunner {
        return CommandLineRunner {
            val groupTypes = listOf(
                createGroupType("1.00", "Media Name", "T"),
                createGroupType("2.00", "Artist", "T"),
                createGroupType("3.00", "Composer", "T"),
                createGroupType("4.00", "Playlist", "S"),
                createGroupType("5.00", "Richi Wertung", "S"),
                createGroupType("6.00", "Music Style", "S"),
                createGroupType("7.00", "Mood", "S"),
                createGroupType("8.00", "Sandra Wertung", "S"),
                createGroupType("9.00", "Original Artist", "T")
            )
            repository.saveAll(groupTypes)
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
