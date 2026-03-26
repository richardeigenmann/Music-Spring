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

    @Autowired
    private lateinit var databaseInitializer: DatabaseInitializer

    @Test
    fun `should initialize group types from yml`() {
        databaseInitializer.runInitialization()
        val count = groupTypeRepository.count()
        // The count should match the number of items in initial-data.yml (8)
        assertEquals(8, count, "Should have 8 group types initialized from YAML")

        val mood = groupTypeRepository.findAll().find { it.groupTypeName == "Mood" }
        assert(mood != null)
        assert(java.math.BigDecimal("7").compareTo(mood?.groupTypeId) == 0)
        assertEquals("S", mood?.groupTypeEdit)
    }
}
