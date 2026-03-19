package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Track
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TrackRepository : JpaRepository<Track, Long> {
    @Query("SELECT t FROM Track t JOIN t.trackGroups tg WHERE tg.groupId = :groupId ORDER BY tg.sequence ASC")
    fun findTracksByGroupId(@Param("groupId") groupId: Long): List<Track>

    @Query(
        value = """
        SELECT DISTINCT t.* FROM Track t
        JOIN Track_Group tg ON t.Track_Id = tg.Track_Id
        JOIN "groups" g ON tg.Group_Id = g.Group_Id
        WHERE LOWER(t.Track_Name) = LOWER(:title)
          AND g.Group_Type_Id = 2
          AND LOWER(g.Group_Name) = LOWER(:artist)
        """,
        nativeQuery = true
    )
    fun findByTitleAndArtist(@Param("title") title: String, @Param("artist") artist: String): List<Track>

    @Query(
        value = """
        SELECT DISTINCT t.* FROM Track t
        LEFT JOIN Track_Group tg ON t.Track_Id = tg.Track_Id
        LEFT JOIN "groups" g ON tg.Group_Id = g.Group_Id
        WHERE LOWER(t.Track_Name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(g.Group_Name) LIKE LOWER(CONCAT('%', :query, '%'))
           OR SOUNDEX(t.Track_Name) = SOUNDEX(:query)
           OR SOUNDEX(g.Group_Name) = SOUNDEX(:query)
        """,
        nativeQuery = true
    )
    fun searchTracks(@Param("query") query: String): List<Track>

    @Query(
        value = """
        SELECT * FROM Track t WHERE t.Track_Id NOT IN (
            SELECT tg.Track_Id FROM Track_Group tg
            JOIN "groups" g ON tg.Group_Id = g.Group_Id
            JOIN Group_Type gt ON g.Group_Type_Id = gt.Group_Type_Id
            WHERE gt.Group_Type_Edit = 'S'
        )
        """,
        nativeQuery = true
    )
    fun findUnclassifiedTracks(): List<Track>
}
