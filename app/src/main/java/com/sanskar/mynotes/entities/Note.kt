package com.sanskar.mynotes.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes")

class Note(): Serializable{
    @PrimaryKey(autoGenerate = true)
    private var id = 0

    @ColumnInfo(name = "title")
    private var title: String? = null

    @ColumnInfo(name = "dateTime")
    private var dateTime: String? = null

    @ColumnInfo(name = "subtitle")
    private var subtitle: String? = null

    @ColumnInfo(name = "note_text")
    private var note_text: String? = null

    @ColumnInfo(name = "imagepath")
    private var imagepath: String? = null

    @ColumnInfo(name = "weblink")
    private var weblink: String? = null

    fun getId(): Int {
        return id
    }

    fun setId(id: Int){
        this.id = id
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?){
        this.title = title
    }

    fun getDateTime(): String? {
        return dateTime
    }

    fun setDateTime(dateTime: String?){
        this.dateTime = dateTime
    }

    fun getSubtitle(): String? {
        return subtitle
    }

    fun setSubtitle(subtitle: String?){
        this.subtitle = subtitle
    }

    fun getNote_text(): String? {
        return note_text
    }

    fun setNote_text(note_text: String?){
        this.note_text = note_text
    }

    fun setImagepath(imagepath: String?){
        this.imagepath = imagepath
    }

    fun getImagepath(): String? {
        return imagepath
    }

    fun getWeblink(): String? {
        return weblink
    }

    fun setWeblink(weblink: String?){
        this.weblink = weblink
    }

    override fun toString(): String {
        return title + " : " + dateTime
    }
}