package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.TrackGroup
import org.richinet.musicbackend.data.entity.TrackGroupId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface TrackGroupRepository : JpaRepository<TrackGroup, TrackGroupId> {
    fun deleteByTrackId(trackId: Long)
    fun deleteByGroupId(groupId: Long)

    @Modifying
    @Query("DELETE FROM TrackGroup tg WHERE tg.trackId = :trackId AND tg.group.groupTypeId = :groupTypeId")
    fun deleteByTrackIdAndGroupTypeId(trackId: Long, groupTypeId: BigDecimal)
}
