package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.projection.GroupProjection
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
            g.Group_Id AS group_Id,
            g.Group_Name AS group_Name,
            (SELECT COUNT(*) FROM Track_Group tg WHERE tg.Group_Id = g.Group_Id) AS tracks
        FROM "groups" g
        WHERE g.Group_Type_Id = :typeId
        ORDER BY 2
        """,
        nativeQuery = true
    )
    fun findPlaylistsByTypeId(@Param("typeId") typeId: BigDecimal): List<PlaylistProjection>

    @Query(
        value = """
        SELECT
            gt.Group_Type_Id AS groupTypeId,
            gt.Group_Type_Name AS groupTypeName,
            g.Group_Id AS groupId,
            g.Group_Name AS groupName
        FROM Group_Type gt
        JOIN "groups" g ON gt.Group_Type_Id = g.Group_Type_Id
        WHERE gt.Group_Type_Edit = 'S'
        ORDER BY
            gt.Group_Type_Name,
            g.Group_Name
        """,
        nativeQuery = true
    )
    fun findAllEditableGroups(): List<GroupProjection>
}
