package org.richinet.musicbackend.data.entity

import jakarta.persistence.*

@Entity
@Table(name = "tag")
class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @Column(name = "tag_type_id")
    var tagTypeId: Long? = null

    @Column(name = "name")
    var name: String? = null

    @ManyToOne
    @JoinColumn(name = "tag_type_id", insertable = false, updatable = false)
    var tagType: TagType? = null
}
