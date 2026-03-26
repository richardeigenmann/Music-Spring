package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Tag
import org.richinet.musicbackend.data.projection.TagProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TagRepository : JpaRepository<Tag, Long> {
    fun findByNameAndTagTypeId(name: String, tagTypeId: Long): Tag?

    @Query(
        value = """
        SELECT
            tt.id AS tagTypeId,
            tt.name AS tagTypeName,
            tt.edit AS tagTypeEdit,
            t.id AS tagId,
            t.name AS tagName
        FROM tag_type tt
        JOIN tag t ON tt.id = t.tag_type_id
        ORDER BY
            tt.name,
            t.name
        """,
        nativeQuery = true
    )
    fun findAllEditableTags(): List<TagProjection>

    @Query(
        value = """
        SELECT
            tt.name AS typeName,
            t.id AS tagId,
            t.name AS tagName,
            COUNT(ttag.track_id) AS count
        FROM tag_type tt
        JOIN tag t ON tt.id = t.tag_type_id
        LEFT JOIN track_tag ttag ON t.id = ttag.tag_id
        GROUP BY tt.name, t.id, t.name
        ORDER BY tt.name, count DESC
        """,
        nativeQuery = true
    )
    fun getTagUsageStats(): List<Map<String, Any>>
}
