package com.example.morsaapp

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.ReceptionAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.ReceptionDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_refunds.*
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

var mType = "";
var mPartner = "";

class RefundsActivity : AppCompatActivity() {

    private var datamodels = ArrayList<ReceptionDataModel>() //ArrayList for the Reception items
    private lateinit var swipeRefreshLayout : SwipeRefreshLayout
    private lateinit var adapter : ReceptionAdapter //Adapter for the Listview of reception

    lateinit var prefs : SharedPreferences
    var userId : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refunds)
        val toolbar : Toolbar = findViewById(R.id.refunds_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Devoluciones"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        swipeRefreshLayout = findViewById(R.id.refunds_lv_refresh)

        val refundsLv = findViewById<ListView>(R.id.refunds_lv) //Instantiate Listview
        val syncRefundsFAB = findViewById<FloatingActionButton>(R.id.sync_refunds_fab) //Instantiate reload btn

        //Select the click item and open its purchase order
        refundsLv!!.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(applicationContext, RefundDetailsActivity::class.java)
            val model : ReceptionDataModel = refundsLv.getItemAtPosition(position) as ReceptionDataModel
            intent.putExtra("ID",model.getId())
            intent.putExtra("Display Name", model.getDisplayName())
            intent.putExtra("Name",model.getNum())
            intent.putExtra("Number", model.num)
            intent.putExtra("UserId", userId)
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
        syncRefundsFAB.setOnClickListener{
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
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        if (db.deleteDataOnTable(OdooData.TABLE_STOCK_RETURN)) {
            thread {
                try {
                    val deferredStockReturn: String = syncStockReturn(/*invoiceIdList*/)
                    Log.d("StockReturn Line", deferredStockReturn)
                    val stockReturnjson = JSONArray(deferredStockReturn)
                    //Insert data
                    val stockReturnUpdate = db.fillTable(stockReturnjson, OdooData.TABLE_STOCK_RETURN)
                    if (stockReturnUpdate) {
                        runOnUiThread {
                            //If succesfull, delete data from model, insert again and notify the dataset
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = refunds_lv.adapter as ReceptionAdapter
                            adapter.notifyDataSetChanged()
                            //Add new Custom Toast method
                            val customToast = CustomToast(this, this)
                            customToast.show("Lista Actualizada", 24.0F, Toast.LENGTH_LONG)
                        }
                    } else {
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            val customToast = CustomToast(this,this)
                            customToast.show("Sin Exito", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                }catch (e: Exception){
                    runOnUiThread {
                        val customToast = CustomToast(this,this)
                        customToast.show("Error General: $e", 24.0F, Toast.LENGTH_LONG)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    Log.d("Error General",e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error Red: $xml", 24.0F, Toast.LENGTH_LONG)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    Log.d("Error de Red",xml.toString())
                }
            }
        } else {
            val customToast = CustomToast(this, this)
            customToast.show("Error al Cargar", 24.0F, Toast.LENGTH_LONG)
        }


    }

    //Fills the lines in datamodel
    private fun populateListView()
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        val refundCursor = db.fillRefundListView()

        var orders : ReceptionDataModel?
        Log.d("Refund Qty", refundCursor.count.toString())
        while (refundCursor.moveToNext()) {
            orders = ReceptionDataModel(
                this,
                null,
                null
            )
            orders.id = refundCursor.getString(refundCursor.getColumnIndex("id"))
            var rawUser = refundCursor.getString(refundCursor.getColumnIndex("user_id"))
            rawUser = rawUser.replace("[","")
            rawUser = rawUser.replace("]","")
            rawUser = rawUser.replace("\"","")
            val user = rawUser.split(",")
            userId = user[0].toInt()
            orders.num = user[1]
            orders.displayName = refundCursor.getString(refundCursor.getColumnIndex("user_id"))
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

            datamodels.add(orders)
        }
        obtainList()
        refundCursor.close()
        db.close()
    }

    //Fills the adapter with datamodel
    private fun obtainList() {
        adapter = ReceptionAdapter(this, datamodels)
        refunds_lv!!.adapter = adapter
    }

    //Returns the purchases
    fun syncStockReturn(/*idList : List<Int>*/) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val invoice = odoo.stockReturn
        Log.d("OrderList", invoice)
        return invoice
    }
}