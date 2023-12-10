package com.sanskar.mynotes.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sanskar.mynotes.entities.Note

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao


interface NoteDao {

    @Query("Select * From notes order by id Desc")
    open fun getAllNotes(): MutableList<Note>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    open fun insertNote(note: Note)

    @Delete
    open fun deleteNote(note: Note)
}