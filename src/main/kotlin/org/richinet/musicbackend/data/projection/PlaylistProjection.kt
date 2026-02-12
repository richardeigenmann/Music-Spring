package org.richinet.musicbackend.data.projection

interface PlaylistProjection {
    fun getGroupId(): Long
    fun getGroupName(): String
    fun getTracks(): Int
}
