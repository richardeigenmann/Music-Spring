package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.TrackFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TrackFileRepository : JpaRepository<TrackFile, Long>
