package org.richinet.musicbackend.data.entity

import java.io.Serializable
import java.math.BigDecimal

class TrackGroupId : Serializable {
    var trackId: BigDecimal? = null
    var groupId: BigDecimal? = null
}
