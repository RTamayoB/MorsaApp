package com.example.morsaapp

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.example.morsaapp.data.OdooConn
import kotlinx.android.synthetic.main.activity_pdf.*


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
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.getPdf(sessionId, routeId)
    }

}
