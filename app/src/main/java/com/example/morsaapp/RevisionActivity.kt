package com.example.morsaapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.RevisionAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.ReceptionDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class RevisionActivity : AppCompatActivity() {

    var datamodels = ArrayList<ReceptionDataModel>()
    private lateinit var adapter : RevisionAdapter
    private lateinit var pedidosLv : ListView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    lateinit var prefs : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revision)
        setSupportActionBar(findViewById(R.id.revision_toolbar))
        supportActionBar?.title = "Inspección"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        val syncRevisionFAB = findViewById<FloatingActionButton>(R.id.sync_revision_fab)
        pedidosLv = findViewById(R.id.revision_lv)
        swipeRefreshLayout = findViewById(R.id.revision_lv_refresh)
        populateListView()

        pedidosLv.setOnItemClickListener { _, view, position, _ ->
            val intent = Intent(applicationContext, OrderRevisionActivity::class.java)
            val model : ReceptionDataModel = pedidosLv.getItemAtPosition(position) as ReceptionDataModel
            val id = model.getId()
            val name = model.num
            intent.putExtra("ID", id)
            intent.putExtra("ReturnId", model.returnId)
            Log.d("Stock Picking Id", id)
            intent.putExtra("Name", name)
            intent.putExtra("InInspection",model.inInspection)
            startActivity(intent)
            finish()
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        /**
         * THis now does the same as swiping up
         */
        syncRevisionFAB.setOnClickListener{
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
        menuInflater.inflate(R.menu.reception_menu, menu)

        val searchItem : MenuItem? = menu?.findItem(R.id.action_search)
        val searchView : SearchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

    private fun refreshData(){

        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver", 1)
        )
        val cursor = db.writableDatabase.rawQuery(
            "SELECT id from ${OdooData.TABLE_STOCK} WHERE in_inspection == 'true'",
            null
        )
        val list = ArrayList<String>()
        while (cursor.moveToNext()){
            Log.d("Picking with inspection",cursor.getString(0))
            list.add(cursor.getString(0))
        }

        for (i in 0 until list.size) {
            list.set(i, "'" + list.get(i).toString() + "'")
        }
        Log.d("List", list.toString())

        val intList = ArrayList<Int>()
        for (i in 0 until list.size) {
            val value = list[i].replace("'","")
            intList.add(value.toInt())
        }
        //db.writableDatabase.execSQL("DELETE FROM stock_picking WHERE id in $list")
        var realList = list.toString().replace("[","(")
        realList = realList.replace("]",")")
        if(db.deleteDataOnTableNotIn(OdooData.TABLE_STOCK, "id", realList)){
            thread {
                try {
                    val deferredStockReSync: String = syncInspectionsByList(intList)
                    Log.d("Returned Stock", deferredStockReSync)
                    val stockJson = JSONArray(deferredStockReSync)
                    val result = db.fillTable(stockJson, OdooData.TABLE_STOCK)
                    if (result) {
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = pedidosLv.adapter as RevisionAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Exito", 24.0F, Toast.LENGTH_LONG)
                        }
                    } else {
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            val customToast = CustomToast(this, this)
                            customToast.show("Sin Exito", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                }catch (e: Exception){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error General $e", 24.0F, Toast.LENGTH_LONG)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    Log.d("Error General", e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error de Red $xml", 24.0F, Toast.LENGTH_LONG)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    Log.d("Error de Red", xml.toString())
                }
            }

        }
        else{
            Log.d("Error", "Rows not deleting")
        }
    }

    private fun populateListView()
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver", 1)
        )
        val cursor = db.fillStockListView()
        var orders : ReceptionDataModel?

        Log.d("Revision Qty", cursor.count.toString())
        datamodels.clear()
        while (cursor.moveToNext()) {
            Log.d("Current inspection id", cursor.getString(cursor.getColumnIndex("id")))
            orders =
                ReceptionDataModel(this, "0", "q")
            orders.id = cursor.getString(cursor.getColumnIndex("id"))
            orders.num = cursor.getString(cursor.getColumnIndex("name"))
            Log.d(
                "ReturnId,Origin,Purcha",
                cursor.getString(cursor.getColumnIndex("return_id")) + "-" +
                        cursor.getString(cursor.getColumnIndex("origin")) + "-" +
                        cursor.getString(cursor.getColumnIndex("origin_invoice_purchase"))
            )
            if (cursor.getString(cursor.getColumnIndex("return_id")) == "false"){
                orders.returnId = false
            }
            else{
                orders.returnId = true
            }
            if(orders.returnId){
                orders.displayName = cursor.getString(cursor.getColumnIndex("origin"))
            }
            else {
                if(cursor.getString(cursor.getColumnIndex("origin_invoice_purchase")) == ""){
                    orders.displayName = cursor.getString(cursor.getColumnIndex("origin"))
                }
                else {
                    orders.displayName =
                        cursor.getString(cursor.getColumnIndex("origin_invoice_purchase"))
                }
            }
            if(cursor.getString(cursor.getColumnIndex("date_done")) != "false") {
                val dateTime = cursor.getString(cursor.getColumnIndex("date_done"))
                val separated = dateTime.split(" ")
                val date = separated[0]
                val time = separated[1]
                orders.date = date
                orders.time = time
            }else{
                orders.date = "false"
                orders.time = "false"
            }

            if(cursor.getString(cursor.getColumnIndex("partner_id")) != "false") {
                val partnerRaw = cursor.getString(cursor.getColumnIndex("partner_id")).split(",")
                val partner1 = partnerRaw[1].replace("\"", "")
                val partner2 = partner1.replace("]", "")
                val partner3 = partner2.replace("[", "")
                orders.company = partner3
            }else{
                orders.company = "false"
            }
            val id = cursor.getString(0)
            val name = cursor.getString(1)
            val relatedId = "[$id,\"$name\"]"
            val correctId = relatedId.replace("/", "\\/")
            val cursor2 = db.fillStockitemsListView(correctId)
            orders.box = "Cajas: "+cursor2.count
            orders.inInspection = if(cursor.getString(cursor.getColumnIndex("in_inspection")) == "true"){
                "true"
            } else{
                "false"
            }

            datamodels.add(orders)
        }
        obtainList()
        cursor.close()
        db.close()
    }

    private fun obtainList() {
        adapter = RevisionAdapter(this, datamodels)
        pedidosLv.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    fun syncInspections() : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockPicking = odoo.inspections
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

    fun syncInspectionsByList(list: List<Int>) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockPicking = odoo.getInspectionsByList(list)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

}
