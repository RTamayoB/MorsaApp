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
import com.example.morsaapp.datamodel.InvoiceDataModel
import com.example.morsaapp.workmanager.ReceptionWorker
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class RefundDetailsActivity : AppCompatActivity() {
    var datamodels = ArrayList<InvoiceDataModel>()
    lateinit var refundDetailsLv : ListView
    lateinit var adapter : InvoiceAdapter
    lateinit var progressBar: ProgressBar

    lateinit var prefs : SharedPreferences

    private var user : String? = ""
    private var pass : String? = ""
    private var invoiceId : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refund_details)

        prefs = this.getSharedPreferences("startupPreferences", 0)
        Log.d("Data start of invoice", prefs.getString("User","Null")+" - "+prefs.getString("Pass","Null"))
        user = prefs.getString("User","Null")
        pass = prefs.getString("Pass","Null")

        val intent : Intent = intent
        invoiceId = intent.getStringExtra("ID")
        //val purchaseId = intent.getStringExtra("Purchase Id")
        val number = intent.getStringExtra("Number")
        val displayName = intent.getStringExtra("Display Name")
        val realDisplayName = displayName.replace("/","\\/")
        val name = intent.getStringExtra("Name")
        val supplier = intent.getStringExtra("Supplier")
        Log.d("Supplier", supplier)
        val total = intent.getStringExtra("Total")
        val address = intent.getStringExtra("Address")
        //val ref = intent.getStringExtra("Ref")
        val relatedId = "[$invoiceId,\"$realDisplayName\"]"
        Log.d("RelatedId", relatedId)

        val partnerTxt = findViewById<TextView>(R.id.partner_txt)
        val invoiceTxt = findViewById<TextView>(R.id.invoice_txt)
        val totalTxt = findViewById<TextView>(R.id.total_txt)
        val motiveTxt = findViewById<TextView>(R.id.motive_txt)

        partnerTxt.text = supplier
        invoiceTxt.text = name
        totalTxt.text = "$$total MXN"
        motiveTxt.text = address

        refundDetailsLv = findViewById(R.id.product_lv)
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
                .setMessage("Recibir Garantia/Devolución?")
                .setPositiveButton("Aceptar"){ _, _ ->
                    try {
                        Log.d("Id", invoiceId.toString())
                        val deferredTest: Deferred<List<Any>> = GlobalScope.async { confirmStockReturn(invoiceId.toString().toInt()) }
                        var result = ""
                        runBlocking {
                            result = deferredTest.await()[1].toString()
                            Log.d("Result Stock Return", result)
                        }
                        runOnUiThread {
                            val customToast = CustomToast(this, this)
                            customToast.show(result, 8.0F, Toast.LENGTH_LONG)
                        }
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
        builder.setMessage("Interrumpir proceso de Devolucion")
        builder.setNegativeButton("Cancelar") {dialog, which ->
            dialog.dismiss()
        }
        builder.setPositiveButton("Aceptar") {dialog, which ->
            val intent = Intent(applicationContext, RefundsActivity::class.java)
            startActivity(intent)
            finish()
        }
        builder.show()
    }

    private fun refreshData(relatedId: String){
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        if(db.deleteDataOnTable(Utilities.TABLE_STOCK_RETURN_LINE)){
            thread {
                try {
                    val id = invoiceId!!.toInt()
                    val deferredStockReturnLine: String = syncStockReturnLines(id)
                    val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
                    val stockLineJson = JSONArray(deferredStockReturnLine)
                    //Insert data
                    val stockLineUpdate =
                        db.fillTable(stockLineJson, Utilities.TABLE_STOCK_RETURN_LINE)
                    if (stockLineUpdate) {
                        Log.d("Loaded Lines", "Loading")
                        //If succesfull, delete data from model, insert again and notify the dataset
                        runOnUiThread {
                            progressBar.isVisible = false
                            datamodels.clear()
                            populateListView(relatedId)
                            val adapter = refundDetailsLv.adapter as InvoiceAdapter
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
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val refundItemsCursor = db.fillRefundItemsListView(id)
        var items : InvoiceDataModel?


        while (refundItemsCursor.moveToNext()) {
            items = InvoiceDataModel()
            var name = refundItemsCursor.getString(2).split(",")[1]
            name = name.replace("\"", "")
            name = name.replace("]", "")
            items.product = name +" x "+refundItemsCursor.getString(4) +"\n" +
                    refundItemsCursor.getString(3) +" c/u "
            items.imports = refundItemsCursor.getString(3) + " MXN"


            datamodels.add(items)
        }
        obtainList()
        refundItemsCursor.close()
        db.close()
    }

    private fun obtainList() {
        adapter = InvoiceAdapter(datamodels, this)
        refundDetailsLv.adapter = adapter
    }

    private fun confirmStockReturn(id: Int): List<Any> {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.confirmStockReturn(id) as List<Any>
    }

    //Returns the purchases lines
    fun syncStockReturnLines(returnId : Int) : String{
        val odoo = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odoo.authenticateOdoo()
        val invoiceLine = odoo.reloadStockReturnLines(returnId)
        return invoiceLine
    }
}