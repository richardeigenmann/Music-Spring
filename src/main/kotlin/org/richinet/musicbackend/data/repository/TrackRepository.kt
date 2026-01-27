package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Track
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TrackRepository : JpaRepository<Track, Long>
