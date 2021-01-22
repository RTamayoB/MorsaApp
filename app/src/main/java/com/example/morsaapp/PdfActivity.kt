package com.example.morsaapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception
import java.net.URL
import java.net.URLConnection
import java.util.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri
import android.os.Environment
import android.util.Base64
import kotlinx.android.synthetic.main.activity_pdf.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat


class PdfActivity : AppCompatActivity() {

    val PDF_LINK : String = "http"
    val MY_PDF : String = "my_pdf.pdf"
    lateinit var PDFViewer : PDFView

    lateinit var prefs : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        prefs = this.getSharedPreferences("startupPreferences", 0)

        val fileName = intent.getStringExtra("File")

        val file = getFileStreamPath(fileName)
        pdf_viewer.fromFile(file)
            .enableSwipe(true)
            .swipeHorizontal(true)
            .onError { t ->
                Toast.makeText(applicationContext, "Error al abrir documento", Toast.LENGTH_LONG).show()
                Log.d("Error", t.toString())
            }
            .enableAntialiasing(true)
            .defaultPage(0)
            .spacing(0)
            .load()
    }



    private fun getPdf(sessionId: String, routeId: String): List<String>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""))
        odooConn.authenticateOdoo()
        return odooConn.getPdf(sessionId, routeId)
    }

}
