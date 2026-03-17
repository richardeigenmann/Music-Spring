package org.richinet.musicbackend.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseInitializerTest {

    @Autowired
    private lateinit var groupTypeRepository: GroupTypeRepository

    @Test
    fun `should initialize group types from yml`() {
        val count = groupTypeRepository.count()
        // The count should match the number of items in initial-data.yml (9)
        assertEquals(9, count, "Should have 9 group types initialized from YAML")
        
        val mood = groupTypeRepository.findAll().find { it.groupTypeName == "Mood" }
        assert(mood != null)
        assertEquals("7.00", mood?.groupTypeId.toString())
        assertEquals("S", mood?.groupTypeEdit)
    }
}
