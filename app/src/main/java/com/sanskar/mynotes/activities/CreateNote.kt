package com.sanskar.mynotes.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.Image
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sanskar.mynotes.R
import com.sanskar.mynotes.database.NotesDB
import com.sanskar.mynotes.entities.Note
import java.io.InputStream
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.properties.Delegates

class CreateNote : AppCompatActivity() {

    private var nightmode by Delegates.notNull<Boolean>()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var noteColor: String
    private lateinit var selectedImagePath: String

    private val REQUEST_CODE_STORAGE_PERMISSION = 1
    private val REQUEST_CODE_SELECT_IMAGE = 2

    private lateinit var back: LinearLayout
    private lateinit var noteTitle: EditText
    private lateinit var noteSubtitle: EditText
    private lateinit var noteText: EditText
    private lateinit var dateTime: TextView
    private var note: Note = Note()
    private lateinit var saveNoteBtn: LinearLayout
    private lateinit var resetBtn: LinearLayout
    private var notedb: NotesDB.Companion = NotesDB
    private lateinit var taskbtn: LinearLayout
    private lateinit var subTitleIndicator: View
    private lateinit var addImage: LinearLayout
    private lateinit var addUrl: LinearLayout
    private lateinit var noteImage: ImageView
    private lateinit var noteWeblink: TextView
    private lateinit var deleteImage: ImageView
    private lateinit var deleteWeblink: ImageView
    private lateinit var deleteNote: LinearLayout
    private lateinit var adView: AdView

    private var viewOrUpdate: Boolean = false
    private var availableNote: Note? = null

    @SuppressLint("MissingInflatedId", "ResourceType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        back = findViewById(R.id.back_btn)
        noteTitle = findViewById(R.id.note_title_et)
        noteSubtitle = findViewById(R.id.note_subtitle_et)
        noteText = findViewById(R.id.note_et)
        dateTime = findViewById(R.id.date_and_time)
        saveNoteBtn = findViewById(R.id.save_btn)
        taskbtn = findViewById(R.id.task_btn)
        resetBtn = findViewById(R.id.reset_btn)
        subTitleIndicator = findViewById(R.id.indicator)
        noteImage = findViewById(R.id.note_image)
        noteWeblink = findViewById(R.id.note_weblink)
        deleteImage = findViewById(R.id.delete_image)
        deleteWeblink = findViewById(R.id.delete_weblink)
        adView = findViewById(R.id.adView)

        MobileAds.initialize(this) {}

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
//        val view = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)

        sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE)
        nightmode = sharedPreferences.getBoolean("nightMode", false)
        if(nightmode){
            noteColor = "#424242"
        }
        else{
            noteColor = "#FFFFFF"
        }
        selectedImagePath = ""

        viewOrUpdate = intent.getBooleanExtra("isViewOrUpdate", false)
        if(viewOrUpdate){
            availableNote = intent.getSerializableExtra("note") as Note
            setViewOrUpdateNote()
        }

        if(intent.getBooleanExtra("isFromQuickActions", false)){
            val type: String = intent.getStringExtra("quickActionType").toString()
            if(type == "image"){
                selectedImagePath = intent.getStringExtra("imagePath").toString()
                noteImage.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath))
                noteImage.visibility = View.VISIBLE
                deleteImage.visibility = View.VISIBLE
            }else if(type == "weblink"){
                noteWeblink.text = intent.getStringExtra("url")
                noteWeblink.visibility = View.VISIBLE
                deleteWeblink.visibility = View.VISIBLE
            }
        }

        subTitleIndicatorColor()

        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy   hh:mm a")
        dateTime.text = LocalDateTime.now().format(formatter)

        back.setOnClickListener {
            finish()
        }

        saveNoteBtn.setOnClickListener {
            saveNote()
        }

        resetBtn.setOnClickListener {
            resetNote()
        }

        taskbtn.setOnClickListener {
            showBottomSheetDialog()
        }

        deleteWeblink.setOnClickListener {
            noteWeblink.text = ""
            noteWeblink.visibility = View.GONE
            deleteWeblink.visibility = View.GONE
        }

        deleteImage.setOnClickListener {
            noteImage.setImageBitmap(null)
            noteImage.visibility = View.GONE
            deleteImage.visibility = View.GONE
            selectedImagePath = ""
        }
    }

    private fun setViewOrUpdateNote() {
        noteTitle.setText(availableNote?.getTitle())
        noteSubtitle.setText(availableNote?.getSubtitle())
        noteText.setText(availableNote?.getNote_text())
        dateTime.setText(availableNote?.getDateTime())
        if(availableNote?.getImagepath() != null && availableNote?.getImagepath()!!.trim().isNotEmpty()){
            noteImage.setImageBitmap(BitmapFactory.decodeFile(availableNote?.getImagepath()))
            noteImage.visibility = View.VISIBLE
            deleteImage.visibility = View.VISIBLE
            selectedImagePath = availableNote?.getImagepath().toString()
        }
        if(availableNote?.getWeblink() != null && availableNote?.getWeblink()!!.trim().isNotEmpty()){
            noteWeblink.setText(availableNote?.getWeblink())
            noteWeblink.visibility = View.VISIBLE
            deleteWeblink.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun resetNote(){
        noteTitle.text = null
        noteSubtitle.text = null
        noteText.text = null
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy   hh:mm a")
        dateTime.text = LocalDateTime.now().format(formatter)
    }

    fun saveNote(){
        if(noteTitle.text.toString().trim().isEmpty()){
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show()
        }else run {
            note.setTitle(noteTitle.text.toString())
            note.setSubtitle(noteSubtitle.text.toString())
            note.setNote_text(noteText.text.toString())
            note.setDateTime(dateTime.text.toString())
            note.setImagepath(selectedImagePath)
            if(noteWeblink.visibility == View.VISIBLE){
                note.setWeblink(noteWeblink.text.toString())
            }
            if(availableNote!=null){
                note.setId(availableNote?.getId()!!)
            }

            class SaveNoteTask: AsyncTask<Void, Void, Void>() {
                @Deprecated("Deprecated in Java", ReplaceWith("null"))
                override fun doInBackground(vararg p0: Void?): Void? {
                    notedb.getDB(applicationContext)?.notesDao()?.insertNote(note)
                    return null
                }

                @Deprecated("Deprecated in Java")
                override fun onPostExecute(result: Void?) {
                    super.onPostExecute(result)
                    Toast.makeText(this@CreateNote, "Note saved!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK, intent);
                    finish()
                }
            }
            SaveNoteTask().execute()
        }
    }

    private fun subTitleIndicatorColor(){
        val gradientDrawable: GradientDrawable = subTitleIndicator.background as GradientDrawable
        gradientDrawable.setColor(Color.parseColor(noteColor))
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_layout)
        addImage = bottomSheetDialog.findViewById(R.id.bottom_sheet_add_image)!!
        addUrl = bottomSheetDialog.findViewById(R.id.bottom_sheet_add_url)!!
        deleteNote = bottomSheetDialog.findViewById(R.id.bottom_sheet_delete_note)!!

        addImage.setOnClickListener {
            bottomSheetDialog.dismiss()
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
        addUrl.setOnClickListener {
            bottomSheetDialog.dismiss()
            addUrl()
        }

        if(availableNote!=null){
            deleteNote.visibility = View.VISIBLE
            deleteNote.setOnClickListener {
                bottomSheetDialog.dismiss()
                showDeleteNoteDialog()
            }
        }

        bottomSheetDialog.show()
    }

    private fun showDeleteNoteDialog(){
        val dialogDeleteNote: AlertDialog
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val view: View = LayoutInflater.from(this).inflate(
            R.layout.delete_note_dialog,
            findViewById(R.id.ll2)
        )
        builder.setView(view)
        dialogDeleteNote = builder.create()
        view.findViewById<TextView>(R.id.delete_note_btn).setOnClickListener {
            class DeleteNoteTask: AsyncTask<Void, Void, Void>(){
                @Deprecated("Deprecated in Java")
                override fun doInBackground(vararg p0: Void?): Void? {
                    NotesDB.getDB(applicationContext)?.notesDao()?.deleteNote(availableNote!!)
                    return null
                }

                @Deprecated("Deprecated in Java")
                override fun onPostExecute(result: Void?) {
                    super.onPostExecute(result)
                    val intent = Intent()
                    intent.putExtra("isNoteDeleted", true)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
            DeleteNoteTask().execute()
        }
        view.findViewById<TextView>(R.id.cancel_delete_note).setOnClickListener {
            dialogDeleteNote.dismiss()
        }
        dialogDeleteNote.show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==REQUEST_CODE_SELECT_IMAGE && resultCode==RESULT_OK){
            if(data!=null){
                var selectedImageUri: Uri = data.data!!
                try {
                    var inputStream: InputStream = contentResolver.openInputStream(selectedImageUri)!!
                    var bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)
                    noteImage.setImageBitmap(bitmap)
                    noteImage.visibility = View.VISIBLE
                    deleteImage.visibility = View.VISIBLE

                    selectedImagePath = getPathFromUri(selectedImageUri)
                }catch(e: Exception){
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
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
                noteWeblink.text = dialog.findViewById<EditText>(R.id.add_url_et).text.toString()
                Log.i("Weblink", noteWeblink.text.toString())
                dialog.dismiss()
                noteWeblink.visibility = View.VISIBLE
                deleteWeblink.visibility = View.VISIBLE
            }
        }
        dialog.show()
    }
}