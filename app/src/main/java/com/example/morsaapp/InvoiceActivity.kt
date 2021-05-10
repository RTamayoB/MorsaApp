package com.example.morsaapp

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.work.*
import com.example.morsaapp.adapter.InvoiceAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.InvoiceDataModel
import com.example.morsaapp.workmanager.ReceptionWorker
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class InvoiceActivity : AppCompatActivity() {

    var datamodels = ArrayList<InvoiceDataModel>()
    lateinit var invoiceLv : ListView
    lateinit var adapter : InvoiceAdapter
    lateinit var progressBar: ProgressBar

    lateinit var prefs : SharedPreferences

    private var user : String? = ""
    private var pass : String? = ""
    private var invoiceId : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice)

        prefs = this.getSharedPreferences("startupPreferences", 0)
        Log.d("Data start of invoice", prefs.getString("User","Null")+" - "+prefs.getString("Pass","Null"))
        user = prefs.getString("User","Null")
        pass = prefs.getString("Pass","Null")

        val intent : Intent = intent
        invoiceId = intent.getStringExtra("ID")
        val purchaseId = intent.getStringExtra("Purchase Id")
        val number = intent.getStringExtra("Number")
        val displayName = intent.getStringExtra("Display Name")
        val realDisplayName = displayName.replace("/","\\/")
        val name = intent.getStringExtra("Name")
        val supplier = intent.getStringExtra("Supplier")
        val total = intent.getStringExtra("Total")
        val address = intent.getStringExtra("Address")
        //val ref = intent.getStringExtra("Ref")
        val relatedId = "[$invoiceId,\"$realDisplayName\"]"
        Log.d("RelatedId", relatedId)

        val supplierTxt = findViewById<TextView>(R.id.supplier_txt)
        val invoiceTxt = findViewById<TextView>(R.id.invoice_txt)
        val totalTxt = findViewById<TextView>(R.id.total_txt)
        val addressTxt = findViewById<TextView>(R.id.address_txt)

        supplierTxt.text = supplier
        invoiceTxt.text = name
        totalTxt.text = "$$total MXN"
        addressTxt.text = address

        invoiceLv = findViewById(R.id.product_lv)
        progressBar = findViewById(R.id.progressBar_invoice)

        /**
         * Downloads the moves for this invoice
         */
        refreshData(relatedId)

        val confirmPurchase = findViewById<Button>(R.id.button)
        confirmPurchase.setOnClickListener {
            val prefs = this.getSharedPreferences("backlogPrefs", 0)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Recibir Productos")
                .setMessage("Recibir Orden de Compra?")
                .setPositiveButton("Aceptar"){ _, _ ->
                    try {

                        val workManager = WorkManager.getInstance(application)
                        val builder = Data.Builder()
                        builder.putInt("Id",purchaseId.toInt())
                        builder.putString("Number",number)
                        builder.putString("User", user)
                        builder.putString("Pass", pass)
                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                        val request = OneTimeWorkRequestBuilder<ReceptionWorker>()
                            .setInputData(builder.build())
                            .setConstraints(constraints)
                            .build()
                        workManager.enqueue(request)
                        /*
                        val deferredTest: Deferred<List<Any>> =
                            GlobalScope.async { confirmInvoice(purchaseId.toInt()) }
                        runBlocking {
                            if (deferredTest.await()[0] as Boolean) {
                                val bool: Boolean = deferredTest.await()[0] as Boolean

                                //val id: Int = deferredTest.await()[1] as Int

                                val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
                                db.changeStockState(id.toString())
                                db.close()

                                Toast.makeText(applicationContext, "Confirmado", Toast.LENGTH_LONG)
                                    .show()
                            } else {
                                //Reception failed, add to backlog
                                Toast.makeText(
                                    applicationContext,
                                    "Error en subir invoice",
                                    Toast.LENGTH_LONG
                                ).show()
                                val editor = prefs.edit()
                                val rawList: String? = prefs.getString("InvoiceIds", "")
                                val list = rawList?.split(",") as ArrayList<String>
                                list.add(id)
                                editor.putString("InvoiceIds", list.toString())
                            }
                        }

                         */
                        val intent = Intent(applicationContext, MainMenuActivity::class.java)
                        startActivity(intent)
                        finish()
                    }catch (e : XmlRpcException){
                        Log.d("XMLRPC ERROR", e.toString())
                        val customToast = CustomToast(this, this)
                        customToast.show("Error en Odoo $e", 24.0F, Toast.LENGTH_LONG)
                    }catch (e : Exception){
                        Log.d("ERROR", e.toString())
                        val customToast = CustomToast(this, this)
                        customToast.show("Error en Petición $e", 24.0F, Toast.LENGTH_LONG)
                    }
                }
                .setNegativeButton("Cancelar"){ dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Salir")
        builder.setMessage("Interrumpir proceso de Recepcion")
        builder.setNegativeButton("Cancelar") {dialog, which ->
            dialog.dismiss()
        }
        builder.setPositiveButton("Aceptar") {dialog, which ->
            val intent = Intent(applicationContext, ReceptionActivity::class.java)
            startActivity(intent)
            finish()
        }
        builder.show()
    }

    private fun refreshData(relatedId: String){
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )

        if(db.deleteDataOnTableFromField(OdooData.TABLE_INVOICE_LINE,"invoice_id",relatedId)){
            thread {
                try {
                    val id = invoiceId!!.toInt()
                    val deferredInvoiceLine: String = syncInvoiceLines(id)
                    val db = DBConnect(
                        applicationContext,
                        OdooData.DBNAME,
                        null,
                        prefs.getInt("DBver",1)
                    )
                    val invoiceLineJson = JSONArray(deferredInvoiceLine)
                    //Insert data
                    val invoiceLineUpdate =
                        db.fillTable(invoiceLineJson, OdooData.TABLE_INVOICE_LINE)
                    if (invoiceLineUpdate) {
                        Log.d("Loaded Lines", "Loading")
                        //If succesfull, delete data from model, insert again and notify the dataset
                        runOnUiThread {
                            progressBar.isVisible = false
                            datamodels.clear()
                            populateListView(relatedId)
                            val adapter = invoiceLv.adapter as InvoiceAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Lista Actualizada", 24.0F, Toast.LENGTH_LONG)
                        }
                    } else{
                        runOnUiThread {
                            progressBar.isVisible = false
                            val customToast = CustomToast(this, this)
                            customToast.show("Sin Exito", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                }catch (e: Exception){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Sin General $e", 24.0F, Toast.LENGTH_LONG)
                        progressBar.isVisible = false
                    }
                    Log.d("Error General",e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error de Red $xml", 24.0F, Toast.LENGTH_LONG)
                        progressBar.isVisible = false
                    }
                    Log.d("Error de Red",xml.toString())
                }
            }

        }
    }

    private fun populateListView(id : String)
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        val invoiceItemsCursor = db.fillInvoiceItemsListView(id)
        var items : InvoiceDataModel?


        while (invoiceItemsCursor.moveToNext()) {
            items = InvoiceDataModel()
            val name = invoiceItemsCursor.getString(0)
            items.product = name +" x "+invoiceItemsCursor.getString(1) +"\n" +
                    invoiceItemsCursor.getString(2) +" c/u "
            items.imports = invoiceItemsCursor.getString(3) + " MXN"


            datamodels.add(items)
        }
        obtainList()
        invoiceItemsCursor.close()
        db.close()
    }

    private fun obtainList() {
        adapter = InvoiceAdapter(datamodels, this)
        invoiceLv.adapter = adapter
    }

    private fun confirmInvoice(id: Int): List<Any> {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.confirmInvoice(id,"") as List<Any>
    }

    //Returns the purchases lines
    fun syncInvoiceLines(invoiceId : Int) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val invoiceLine = odoo.reloadInvoiceLines(invoiceId)
        return invoiceLine
    }
}
