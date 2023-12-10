package com.sanskar.mynotes.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sanskar.mynotes.R
import com.sanskar.mynotes.entities.Note

class notesAdapter(private val context: Context, private var notes: List<Note>) :
    RecyclerView.Adapter<notesAdapter.NoteViewHolder>() {

    private var onClickListener: OnClickListener? = null
//    private lateinit var timer: Timer
    private var notesSource: List<Note> = notes

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var texttitle: TextView
        var textnote: TextView
        var textdatetime: TextView
        var imageNote: ImageView
        var layout_note: LinearLayout
        var webLink: TextView

        init {
            texttitle = itemView.findViewById(R.id.textTitle)
            textnote = itemView.findViewById(R.id.textNote)
            textdatetime = itemView.findViewById(R.id.textDateTime)
            layout_note = itemView.findViewById(R.id.layoutNote)
            imageNote = itemView.findViewById(R.id.note_image_path)
            webLink = itemView.findViewById(R.id.note_web_link)
        }

        fun setNote(context: Context, note: Note){
            texttitle.text = note.getTitle();
            if(note.getNote_text()?.trim()?.isEmpty()!!){
                textnote.visibility = View.GONE
            }else{
                textnote.text = note.getNote_text()
            }
            textdatetime.text = note.getDateTime()

            if(note.getImagepath().toString()!="") {
                val bitmap: Bitmap? = BitmapFactory.decodeFile(note.getImagepath())
                imageNote.setImageBitmap(bitmap)
                imageNote.visibility = View.VISIBLE
            }
            else{
                imageNote.visibility = View.GONE
            }

            if(note.getWeblink()!=null){
                Log.i("true", (note.getWeblink()!=null).toString())
                webLink.text = note.getWeblink()
                webLink.visibility = View.VISIBLE
            }else{
                webLink.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(LayoutInflater.from(context).inflate(
            R.layout.item_notes,
            parent,
            false
        ))
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.setNote(holder.itemView.context, notes[position])
        holder.itemView.setOnClickListener {
            if(onClickListener != null) {
                onClickListener?.onClick(position, notes[position])
            }
        }
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener

    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    interface OnClickListener{
        fun onClick(position: Int, note: Note)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun searchNotes(searchKeyword: String){
        Handler().postDelayed({
            if(searchKeyword.trim().isEmpty()){
                notes = notesSource
            }else{
                var temp = ArrayList<Note>()
                for(note in notesSource){
                    if(note.getTitle()?.toLowerCase()?.contains(searchKeyword.toLowerCase())!! ||
                        note.getSubtitle()?.toLowerCase()?.contains(searchKeyword.toLowerCase())!! ||
                        note.getNote_text()?.toLowerCase()?.contains(searchKeyword.toLowerCase())!!
                    ){
                        temp.add(note)
                    }
                }
                notes = temp
            }
            Handler(Looper.getMainLooper()).post {
                notifyDataSetChanged()
            }
        }, 500)
    }



}