package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.TrackGroup
import org.richinet.musicbackend.data.entity.TrackGroupId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TrackGroupRepository : JpaRepository<TrackGroup, TrackGroupId>
