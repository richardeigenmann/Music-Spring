package org.richinet.musicbackend.data.entity

import java.io.Serializable
import java.util.Objects

class TrackTagId(
    var trackId: Long? = null,
    var tagId: Long? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackTagId) return false
        return Objects.equals(trackId, other.trackId) && Objects.equals(tagId, other.tagId)
    }

    override fun hashCode(): Int {
        return Objects.hash(trackId, tagId)
    }
}
