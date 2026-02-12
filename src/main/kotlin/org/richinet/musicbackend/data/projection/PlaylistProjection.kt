package org.richinet.musicbackend.data.projection

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Projection for Playlist data")
interface PlaylistProjection {
    @Schema(description = "The unique identifier of the group/playlist")
    fun getGroupId(): Long

    @Schema(description = "The name of the playlist")
    fun getGroupName(): String

    @Schema(description = "The number of tracks in the playlist")
    fun getTracks(): Int
}
