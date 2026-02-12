package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface GroupsRepository : JpaRepository<Groups, Long> {
    fun findByGroupNameAndGroupTypeId(groupName: String, groupTypeId: BigDecimal): Groups?

    @Query(
        value = """
        SELECT 
            g.Group_Id AS groupId, 
            g.Group_Name AS groupName,
            (SELECT COUNT(*) FROM Track_Group tg WHERE tg.Group_Id = g.Group_Id) AS tracks 
        FROM "groups" g
        WHERE g.Group_Type_Id = :typeId 
        ORDER BY 2
        """, 
        nativeQuery = true
    )
    fun findPlaylistsByTypeId(@Param("typeId") typeId: BigDecimal): List<PlaylistProjection>
}
