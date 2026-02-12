package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.GroupType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface GroupTypeRepository : JpaRepository<GroupType, BigDecimal> {
    fun findByGroupTypeName(groupTypeName: String): GroupType?
}
