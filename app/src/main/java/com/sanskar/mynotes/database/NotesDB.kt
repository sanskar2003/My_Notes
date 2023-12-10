package com.sanskar.mynotes.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sanskar.mynotes.dao.NoteDao
import com.sanskar.mynotes.entities.Note

@Database(entities = [Note::class], version = 7, exportSchema = false)
abstract class NotesDB : RoomDatabase() {
     abstract fun notesDao(): NoteDao?

    companion object {
        private var notesDB: NotesDB? = null
        @Synchronized
//        fun getDB(context: Context?): NotesDB? {
//            if (notesDB == null) {
//                notesDB = Room.databaseBuilder(context!!, NotesDB::class.java, "notes_db").build()
//            }
//            return notesDB
//        }
        fun getDB(context: Context): NotesDB? {
            if (notesDB == null) {
                synchronized(NotesDB::class.java) {
                    if (notesDB == null) {
                        notesDB = Room.databaseBuilder(context.applicationContext,
                            NotesDB::class.java, "abc_db")
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return notesDB
        }
    }
}