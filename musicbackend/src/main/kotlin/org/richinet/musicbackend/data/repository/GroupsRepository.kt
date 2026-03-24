package org.richinet.musicbackend.data.repository

import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.projection.GroupProjection
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

    @Query(
        value = """
        SELECT
            gt.Group_Type_Name AS typeName,
            g.Group_Id AS groupId,
            g.Group_Name AS groupName,
            COUNT(tg.Track_Id) AS count
        FROM Group_Type gt
        JOIN "groups" g ON gt.Group_Type_Id = g.Group_Type_Id
        LEFT JOIN Track_Group tg ON g.Group_Id = tg.Group_Id
        WHERE gt.Group_Type_Edit = 'S'
        GROUP BY gt.Group_Type_Name, g.Group_Id, g.Group_Name
        ORDER BY gt.Group_Type_Name, count DESC
        """,
        nativeQuery = true
    )
    fun getGroupUsageStats(): List<Map<String, Any>>
}
