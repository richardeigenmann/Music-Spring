package org.richinet.musicbackend.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.richinet.musicbackend.data.repository.TagTypeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagTypeInitializerTest {

    @Autowired
    private lateinit var tagTypeRepository: TagTypeRepository

    @Autowired
    private lateinit var databaseInitializer: DatabaseInitializer

    @Test
    fun `should initialize tag types from yml`() {
        databaseInitializer.runInitialization()
        val count = tagTypeRepository.count()
        // The count should match the number of items in initial-data.yml (8)
        assertEquals(8, count, "Should have 8 tag types initialized from YAML")

        val mood = tagTypeRepository.findAll().find { it.name == "Mood" }
        assert(mood != null)
        assertEquals(7L, mood?.id)
        assertEquals("S", mood?.edit)
    }
}
