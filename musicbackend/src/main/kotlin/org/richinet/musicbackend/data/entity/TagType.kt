package org.richinet.musicbackend.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tag_type")
class TagType {
    @Id
    @Column(name = "id")
    var id: Long? = null

    @Column(name = "name")
    var name: String? = null

    @Column(name = "edit")
    var edit: String? = null
}
