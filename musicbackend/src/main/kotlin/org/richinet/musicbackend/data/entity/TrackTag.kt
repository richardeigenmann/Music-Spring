package org.richinet.musicbackend.data.entity

import jakarta.persistence.*

@Entity
@Table(name = "track_tag")
@IdClass(TrackTagId::class)
class TrackTag {
    @Id
    @Column(name = "track_id")
    var trackId: Long? = null

    @Id
    @Column(name = "tag_id")
    var tagId: Long? = null

    @Column(name = "sequence")
    var sequence: Int? = null

    @ManyToOne
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    var tag: Tag? = null

    @ManyToOne
    @JoinColumn(name = "track_id", insertable = false, updatable = false)
    var track: Track? = null
}
