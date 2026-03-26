package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.TrackTag
import org.richinet.musicbackend.data.entity.TrackTagId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TrackTagRepository : JpaRepository<TrackTag, TrackTagId> {
    fun deleteByTrackId(trackId: Long)
    fun deleteByTagId(tagId: Long)

    @Modifying
    @Query("DELETE FROM TrackTag tt WHERE tt.trackId = :trackId AND tt.tag.tagTypeId = :tagTypeId")
    fun deleteByTrackIdAndTagTypeId(trackId: Long, tagTypeId: Long)
}
