package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Groups
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface GroupsRepository : JpaRepository<Groups, Long> {
    fun findByGroupNameAndGroupTypeId(groupName: String, groupTypeId: BigDecimal): Groups?
}
