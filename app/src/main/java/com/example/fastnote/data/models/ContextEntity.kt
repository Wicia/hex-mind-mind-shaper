package com.example.fastnote.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

import java.util.Date

// #TODO when project is getting more & more complex -> improve code by adding Thought DTO + mapper
@Entity(tableName = "CONTEXTS")
data class ContextEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "areaIdentifier")
    val areaIdentifier: AreaIdentifier = AreaIdentifier.NOT_SET,

    @ColumnInfo(name = "thread")
    val thread: String? = null,
)