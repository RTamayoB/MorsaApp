package com.example.morsaapp

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.ReceptionAdapter
import com.example.morsaapp.datamodel.ReceptionDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_reception.*
import kotlinx.android.synthetic.main.issues_popup_item.*
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class ReceptionActivity : AppCompatActivity() {

    private var datamodels = ArrayList<ReceptionDataModel>() //ArrayList for the Reception items
    private lateinit var swipeRefreshLayout : SwipeRefreshLayout
    private lateinit var adapter : ReceptionAdapter //Adapter for the Listview of reception

    lateinit var prefs : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reception)
        val toolbar : Toolbar = findViewById(R.id.reception_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Pedidos"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        swipeRefreshLayout = findViewById(R.id.pedidos_lv_refresh)

        val pedidosLv = findViewById<ListView>(R.id.pedidos_lv) //Instantiate Listview
        val syncReceptionFAB = findViewById<FloatingActionButton>(R.id.sync_reception_fab) //Instantiate reload btn

        //Select the click item and open its purchase order
        pedidosLv!!.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(applicationContext, InvoiceActivity::class.java)
            val model : ReceptionDataModel = pedidosLv.getItemAtPosition(position) as ReceptionDataModel
            val id = model.getId()
            val name = model.getNum()
            val displayName = model.displayName
            val purchaseId = model.purchaseId
            val number = model.number
            val supplier = model.company
            val total = model.total
            val address = model.address
            intent.putExtra("ID",id)
            intent.putExtra("Display Name", displayName)
            intent.putExtra("Purchase Id", purchaseId)
            intent.putExtra("Number", number)
            intent.putExtra("Name",name)
            intent.putExtra("Supplier",supplier )
            intent.putExtra("Total", total)
            intent.putExtra("Address", address)
            /*
            Log.d("Ref", model.ref)
            if (model.ref == "false"){
                intent.putExtra("Ref","")
            }
            else{
                val ref = model.ref
                intent.putExtra("Ref", " ($ref)")
            }
            */
            startActivity(intent)
            finish()
        }

        /**
         * This deletes the previous account.invoice and loads the most recent ones that meet the parameters
         */
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        //Reload the Listview
        /**
         * This now does the same as swiping up
         */
        syncReceptionFAB.setOnClickListener{
            swipeRefreshLayout.isRefreshing = true
            refreshData()
        }

        swipeRefreshLayout.isRefreshing = true
        refreshData()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val goBackintent = Intent(this, MainMenuActivity::class.java)
        startActivity(goBackintent)
        finish()
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.reception_menu,menu)

        val searchItem : MenuItem? = menu?.findItem(R.id.action_search)
        val searchView : SearchView = searchItem?.actionView as SearchView
        menu.getItem(0).icon = ContextCompat.getDrawable(this, R.drawable.lupa_img_white)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }

        })
        return true
    }

    //Reload the database for new instances
    private fun refreshData(){
            val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
            if (db.deleteDataOnTable(Utilities.TABLE_INVOICE)) {
                thread {
                    try {
                        val deferredInvoice: String = syncInvoice(/*invoiceIdList*/)
                        Log.d("Inventory Line", deferredInvoice)
                        val invoicejson = JSONArray(deferredInvoice)
                        //Insert data
                        val invoiceUpdate = db.fillTable(invoicejson, Utilities.TABLE_INVOICE)
                        if (invoiceUpdate) {
                            runOnUiThread {
                                //If succesfull, delete data from model, insert again and notify the dataset
                                swipeRefreshLayout.isRefreshing = false
                                datamodels.clear()
                                populateListView()
                                val adapter = pedidos_lv.adapter as ReceptionAdapter
                                adapter.notifyDataSetChanged()
                                //Add new Custom Toast method
                                val customToast = CustomToast(this, this)
                                customToast.show("Lista Actualizada", 24.0F, Toast.LENGTH_LONG)
                            }
                        } else {
                            runOnUiThread {
                                swipeRefreshLayout.isRefreshing = false
                                Toast.makeText(applicationContext, "Sin Exito", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }catch (e: Exception){
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Error General: $e",
                                Toast.LENGTH_SHORT
                            ).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                        Log.d("Error General",e.toString())
                    }catch (xml: XmlRpcException){
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Error de Red: $xml",
                                Toast.LENGTH_SHORT
                            ).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                        Log.d("Error de Red",xml.toString())
                    }
                }
            } else {
                Toast.makeText(applicationContext, "Error al cargar", Toast.LENGTH_SHORT).show()
            }


    }

    //Fills the lines in datamodel
    private fun populateListView()
    {
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val invoiceCursor = db.fillInvoiceListView()

        var orders : ReceptionDataModel?
        Log.d("Reception Qty", invoiceCursor.count.toString())
        while (invoiceCursor.moveToNext()) {
            orders = ReceptionDataModel(
                this,
                null,
                null
            )
            orders.id = invoiceCursor.getString(0)
            orders.num = invoiceCursor.getString(invoiceCursor.getColumnIndex("origin"))
            val date = invoiceCursor.getString(invoiceCursor.getColumnIndex("datetime_invoice"))
            val dateSeparator = date.split(" ")
            orders.date = dateSeparator[0]
            orders.time = dateSeparator[1]
            val rawCompany = invoiceCursor.getString(invoiceCursor.getColumnIndex("partner_id"))
            val comp1 = rawCompany.split(",")
            val comp2 = comp1[1].replace("]","")
            orders.company = comp2
            orders.total = invoiceCursor.getString(invoiceCursor.getColumnIndex("amount_total"))
            orders.address = ""
            orders.displayName = invoiceCursor.getString(invoiceCursor.getColumnIndex("display_name"))
            val purchaseIdRaw = invoiceCursor.getString(invoiceCursor.getColumnIndex("purchase_id"))
            val purchase1 = purchaseIdRaw.split(",")
            val purchase2 = purchase1[0].replace("[","")
            orders.purchaseId = purchase2
            orders.number = invoiceCursor.getString(invoiceCursor.getColumnIndex("number"))
            /*
            orders.ref = cursor.getString(8)
            var relatedId = "[$id,\"$name\"]"
            if (orders.ref == "false"){
                relatedId = "[$id,\"$name\"]"
            }
            else{
                relatedId = "[$id,\"$name (${orders.ref})\"]"
            }
            val cursor2 = db.fillItemsListView(relatedId)
            */
            val comp3 = comp2.replace("\"","")
            val comp4 = comp3.replace("[","")
            orders.box = comp4

            datamodels.add(orders)
        }
        obtainList()
        invoiceCursor.close()
        db.close()
    }

    //Fills the adapter with datamodel
    private fun obtainList() {
        adapter = ReceptionAdapter(this, datamodels)
        pedidos_lv!!.adapter = adapter
    }

    //Returns the purchases
    fun syncInvoice(/*idList : List<Int>*/) : String{
        val odoo = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odoo.authenticateOdoo()
        val invoice = odoo.getInvoice(/*idList*/)
        Log.d("OrderList", invoice)
        return invoice
    }
}