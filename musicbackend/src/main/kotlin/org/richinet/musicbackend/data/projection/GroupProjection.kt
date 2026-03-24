package org.richinet.musicbackend.data.projection

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Projection for Group data with Type info")
interface GroupProjection {
    @Schema(description = "The ID of the group type")
    fun getGroupTypeId(): BigDecimal

    @Schema(description = "The name of the group type")
    fun getGroupTypeName(): String

    @Schema(description = "The unique identifier of the group")
    fun getGroupId(): Long

    @Schema(description = "The name of the group")
    fun getGroupName(): String

    @Schema(description = "The edit type of the group type (S or T)")
    fun getGroupTypeEdit(): String
}
