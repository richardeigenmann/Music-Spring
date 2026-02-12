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
}
