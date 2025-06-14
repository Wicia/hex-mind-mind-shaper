package com.example.fastnote.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

import java.util.Date

// #TODO project is getting more complex -> improve code by adding Thought DTO + mapper
@Entity(tableName = "THOUGHTS")
data class ThoughtEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "area")
    val area: String? = null,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "priority")
    val priority: Int = 3
)