package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Track
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TrackRepository : JpaRepository<Track, Long> {
    @Query("SELECT t FROM Track t JOIN t.trackTags tt WHERE tt.tagId = :tagId ORDER BY tt.sequence ASC")
    fun findTracksByTagId(@Param("tagId") tagId: Long): List<Track>

    @Query(
        value = """
        SELECT DISTINCT t.* FROM track t
        JOIN track_tag tt ON t.id = tt.track_id
        JOIN tag tag ON tt.tag_id = tag.id
        WHERE LOWER(t.name) = LOWER(:title)
          AND tag.tag_type_id = 2
          AND LOWER(tag.name) = LOWER(:artist)
        """,
        nativeQuery = true
    )
    fun findByTitleAndArtist(@Param("title") title: String, @Param("artist") artist: String): List<Track>

    @Query(
        value = """
        SELECT DISTINCT t.* FROM track t
        LEFT JOIN track_tag tt ON t.id = tt.track_id
        LEFT JOIN tag tag ON tt.tag_id = tag.id
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(tag.name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR SOUNDEX(t.name) = SOUNDEX(:query)
           OR SOUNDEX(tag.name) = SOUNDEX(:query)
        """,
        nativeQuery = true
    )
    fun searchTracks(@Param("query") query: String): List<Track>

    // The S tags are the group select tags. Tracks with less than 3 group
    // tags are deemed to be unclassified.
    @Query(
        value = """
        SELECT * FROM musicdatabase.track t WHERE t.id NOT IN (
            SELECT tt.track_id
			      FROM musicdatabase.track_tag tt
            JOIN musicdatabase.tag tag ON tt.tag_id = tag.id
            JOIN musicdatabase.tag_type tt_type ON tag.tag_type_id = tt_type.id
            WHERE tt_type.edit = 'S'
			      group by tt.track_id
			      having count(tt.tag_id) < 3
        )
        """,
        nativeQuery = true
    )
    fun findUnclassifiedTracks(): List<Track>
}
