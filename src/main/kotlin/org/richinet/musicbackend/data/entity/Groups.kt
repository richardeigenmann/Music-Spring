package org.richinet.musicbackend.data.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "`Groups`")
class Groups {
    @Id
    @Column(name = "GroupId")
    var groupId: BigDecimal? = null

    @Column(name = "GroupTypeId")
    var groupTypeId: BigDecimal? = null

    @Column(name = "GroupName")
    var groupName: String? = null

    @Column(name = "LastModification")
    var lastModification: Timestamp? = null

    @ManyToOne
    @JoinColumn(name = "GroupTypeId", insertable = false, updatable = false)
    var groupType: GroupType? = null
}
