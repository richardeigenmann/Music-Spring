package org.richinet.musicbackend.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "GroupType")
class GroupType {
    @Id
    @Column(name = "GroupTypeId")
    var groupTypeId: BigDecimal? = null

    @Column(name = "GroupTypeName")
    var groupTypeName: String? = null

    @Column(name = "GroupTypeEdit")
    var groupTypeEdit: String? = null
}
