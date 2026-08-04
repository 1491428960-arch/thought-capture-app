package com.thoughtcapture.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "thought_entries", indices = [Index(value = ["status"])])
data class ThoughtEntry(
    @PrimaryKey
    val id: String,
    val type: String,
    val status: String,
    val source: String,
    val content: String,
    val mediaPath: String?,
    val tags: String,
    val createdAt: Long
)
