package br.com.marco.pocpdf

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.FileNotFoundException
import java.io.IOException


class MainActivity : AppCompatActivity() {

    lateinit var renderer: PdfRenderer
    private lateinit var button: Button
    lateinit var imageView: ImageView
    private lateinit var spinner: Spinner
    private lateinit var spinnerAdapter: Adapter
    private var totalPages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.btnPickPdf)
        imageView = findViewById(R.id.ivPdf)
        spinner = findViewById(R.id.spPdf)
        button.setOnClickListener {
            requestReadExternalStoragePermission()
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                imageView.setImageBitmap(displayPage(position,renderer))
            }

        }
    }

    private fun requestReadExternalStoragePermission() {
        readExternalStoragePermission.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }

    private val readExternalStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            ) {
                startGallery()
            }
        }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                val cr = this.contentResolver
                try {
                    val parcelFileDescriptor = imageUri?.let {
                        cr
                            .openFileDescriptor(it, "r")
                    }
                    if(parcelFileDescriptor != null) {
                        renderer = PdfRenderer(parcelFileDescriptor)
                        totalPages = renderer.pageCount
                        setPdfAdapter(totalPages)
                    }
                } catch (fnfe: FileNotFoundException){
                    Log.d("Error", fnfe.message.toString())

                } catch (e : IOException){
                    Log.d("Error", e.message.toString())
                }
            }
        }

    private fun setPdfAdapter(totalPages: Int) {
        val array = arrayListOf<Int>()
        for (i in 1..totalPages) {
            array.add(i)
        }
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, array)
        spinner.adapter = spinnerAdapter as ArrayAdapter<*>
        spinner.setSelection(0)
        spinner.visibility = View.VISIBLE
    }

    private fun displayPage(pageIndex: Int,  renderer: PdfRenderer?): Bitmap? {
        return if(renderer != null){
            val page = renderer.openPage(pageIndex)
            val mBitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(mBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            mBitmap
        }else
            null
    }
    private fun startGallery() {
        startForResult.launch(openGalleryForPdf())
    }

    private fun openGalleryForPdf(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        return intent
    }
}