package com.sanskar.mynotes.activities

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sanskar.mynotes.R
import com.sanskar.mynotes.adapters.notesAdapter
import com.sanskar.mynotes.database.NotesDB
import com.sanskar.mynotes.entities.Note
import java.lang.Exception
import java.util.Collections.addAll
import java.util.Objects
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_ADD_NOTE = 1
    private val REQUEST_CODE_UPDATE_NOTE = 2
    private val REQUEST_CODE_SHOW_NOTES = 3
    private val REQUEST_CODE_SELECT_IMAGE = 4
    private val REQUEST_CODE_STORAGE_PERMISSION = 5

    private var nightmode by Delegates.notNull<Boolean>()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var themeBtn: ImageView
    private lateinit var create_note: LinearLayout
    private lateinit var notesView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var addCircle: ImageView
    private lateinit var addImageShortcut: ImageView
    private lateinit var addWeblinkShortcut: ImageView

    private lateinit var noteList: ArrayList<Note>
    private lateinit var nadapter: notesAdapter
    private var noteClickedPos = -1;


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        themeBtn = findViewById(R.id.theme_icon)
        create_note = findViewById(R.id.create_note_icon)
        notesView = findViewById(R.id.recyclerView)
        searchInput = findViewById(R.id.search_et)
        addCircle = findViewById(R.id.add_circle)
        addImageShortcut = findViewById(R.id.image_icon)
        addWeblinkShortcut = findViewById(R.id.web_link_icon)

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE)
        nightmode = sharedPreferences.getBoolean("nightMode", false)


        if(nightmode){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        themeBtn.setOnClickListener {
            if(nightmode){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor = sharedPreferences.edit()
                editor.putBoolean("nightMode", false)
            }
            else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor = sharedPreferences.edit()
                editor.putBoolean("nightMode", true)
            }
            editor.apply()
        }

        notesView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        noteList = ArrayList<Note>()
        nadapter = notesAdapter(this, noteList)
        notesView.adapter = nadapter
        nadapter.setOnClickListener(object: notesAdapter.OnClickListener{
            override fun onClick(position: Int, note: Note) {
                noteClickedPos = position
                val intent = Intent(applicationContext, CreateNote::class.java)
                intent.putExtra("isViewOrUpdate", true)
                intent.putExtra("note", note)
                searchInput.setText("")
                startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE)
            }
        })

        create_note.setOnClickListener {
            var intent = Intent(this, CreateNote::class.java)
            searchInput.setText("")
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        addCircle.setOnClickListener {
            var intent = Intent(this, CreateNote::class.java)
            searchInput.setText("")
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        getNotes(REQUEST_CODE_SHOW_NOTES, false)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(noteList.size!=0){
                    nadapter.searchNotes(s.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        addImageShortcut.setOnClickListener {
            if(ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                selectImage()
            }else{
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            }
        }

        addWeblinkShortcut.setOnClickListener {
            addUrl()
        }

    }

    private fun selectImage(){
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty()){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage()
            }else{
                Toast.makeText(this, "Permission Denied!!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String{
        lateinit var filePath: String
        val cursor: Cursor = contentResolver.query(uri, null, null, null, null)!!
        if(cursor==null){
            filePath = uri.path!!
        }else{
            cursor.moveToFirst()
            var index: Int = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
            cursor.close()
        }
        return filePath
    }

    private fun getNotes(requestCode: Int, isNoteDeleted: Boolean): Void? {
        class GetNotesTask: AsyncTask<Void, Void, List<Note>>(){
            override fun doInBackground(vararg p0: Void?): MutableList<Note>? {
                return NotesDB.getDB(applicationContext)?.notesDao()?.getAllNotes();
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onPostExecute(notes: List<Note>?) {
                super.onPostExecute(notes)
                when (requestCode) {
                    REQUEST_CODE_SHOW_NOTES -> {
                        noteList.addAll(notes!!)
                        nadapter.notifyDataSetChanged()
                    }
                    REQUEST_CODE_ADD_NOTE -> {
                        noteList.add(0, notes?.get(0)!!)
                        nadapter.notifyItemInserted(0)
                        notesView.smoothScrollToPosition(0)
                    }
                    REQUEST_CODE_UPDATE_NOTE -> {
                        noteList.removeAt(noteClickedPos)
                        if(isNoteDeleted){
                            nadapter.notifyItemRemoved(noteClickedPos)
                        }else{
                            noteList.add(noteClickedPos, notes?.get(noteClickedPos)!!)
                            nadapter.notifyItemChanged(noteClickedPos)
                        }
                    }
                }
            }
        }
        GetNotesTask().execute()
        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK){
            getNotes(REQUEST_CODE_ADD_NOTE, false)
        }else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == RESULT_OK){
            if(data!=null){
                getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false))
            }
        }else if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK){
            if(data!=null){
                val selectedImageUri: Uri = data.data!!
                try {
                    val selectedImagePath: String = getPathFromUri(selectedImageUri)
                    val intent = Intent(this, CreateNote::class.java)
                    intent.putExtra("isFromQuickActions", true)
                    intent.putExtra("quickActionType", "image")
                    intent.putExtra("imagePath", selectedImagePath)
                    searchInput.setText("")
                    startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)
                }catch (e: Exception){
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addUrl(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.add_url_layout)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.window?.attributes?.windowAnimations = androidx.appcompat.R.anim.abc_popup_enter
        dialog.findViewById<TextView>(R.id.Cancel_Btn).setOnClickListener {
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.Add_Btn).setOnClickListener {
            if(dialog.findViewById<EditText>(R.id.add_url_et).text.trim().isEmpty()){
                Toast.makeText(this, "Please enter URL!", Toast.LENGTH_SHORT).show()
            }else if(!Patterns.WEB_URL.matcher(dialog.findViewById<EditText>(R.id.add_url_et).text).matches()){
                Toast.makeText(this, "Please enter valid URL!", Toast.LENGTH_SHORT).show()
            }
            else{
                dialog.dismiss()
                val intent = Intent(this, CreateNote::class.java)
                intent.putExtra("isFromQuickActions", true)
                intent.putExtra("quickActionType", "weblink")
                intent.putExtra("url", dialog.findViewById<EditText>(R.id.add_url_et).text.toString())
                searchInput.setText("")
                startActivityForResult(intent, REQUEST_CODE_ADD_NOTE)
            }
        }
        dialog.show()
    }
}