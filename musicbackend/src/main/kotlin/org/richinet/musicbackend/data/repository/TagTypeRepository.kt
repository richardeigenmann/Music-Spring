package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.TagType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TagTypeRepository : JpaRepository<TagType, Long> {
    fun findByName(name: String): TagType?
}
