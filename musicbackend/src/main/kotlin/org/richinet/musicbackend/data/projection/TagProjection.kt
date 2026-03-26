package org.richinet.musicbackend.data.projection

interface TagProjection {
    fun getTagTypeId(): Long?
    fun getTagTypeName(): String?
    fun getTagTypeEdit(): String?
    fun getTagId(): Long?
    fun getTagName(): String?
}
