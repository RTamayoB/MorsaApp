package com.example.morsaapp

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.ReceptionAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.ReceptionDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class LocationActivity : AppCompatActivity() {

    lateinit var locationsLv : ListView
    private var datamodels = ArrayList<ReceptionDataModel>()
    var pickingId: Int = 0
    private lateinit var adapter : ReceptionAdapter

    lateinit var prefs : SharedPreferences
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        val toolbar : Toolbar = findViewById(R.id.location_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Acomodo de Pickings Aprobados"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        swipeRefreshLayout = findViewById(R.id.location_lv_refresh)

        val handler = Handler()
        val delay : Long = 3000

        handler.postDelayed({
            Log.d("Message", "Running handler")
        },delay)

        locationsLv = findViewById(R.id.location_lv)
        val locationBtn = findViewById<Button>(R.id.location_btn)
        val pickingFAB = findViewById<FloatingActionButton>(R.id.sync_pickloc_fab)

        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }

        pickingFAB.setOnClickListener {
            swipeRefreshLayout.isRefreshing = true
            refreshData()
        }

        val intent = Intent(applicationContext, ProductsToLocationActivity::class.java)
        locationsLv.setOnItemClickListener { parent, view, position, id ->
            val model : ReceptionDataModel = locationsLv.getItemAtPosition(position) as ReceptionDataModel
            Log.d("Model Id", model.getId())
            pickingId = model.getId().toInt()
            intent.putExtra("pickingId",pickingId)
            startActivity(intent)
            finish()
        }

        locationBtn.setOnClickListener {
            val intent = Intent(applicationContext, MainMenuActivity::class.java)
            startActivity(intent)
            finish()
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

    private fun refreshData(){
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        if(db.deleteDataOnTable(OdooData.TABLE_STOCK_ARRANGEMENT)){
            thread {
                try {
                    val deferredStockReSync: String = syncLocations()
                    Log.d("Returned Stock", deferredStockReSync)
                    val db = DBConnect(
                        applicationContext,
                        OdooData.DBNAME,
                        null,
                        prefs.getInt("DBver",1)
                    )
                    val stockJson = JSONArray(deferredStockReSync)
                    val result = db.fillTable(stockJson, OdooData.TABLE_STOCK_ARRANGEMENT)
                    if (result) {
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = locationsLv.adapter as ReceptionAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Exito", 24.0F, Toast.LENGTH_LONG)
                            }
                        } else {
                            runOnUiThread {
                                swipeRefreshLayout.isRefreshing = false


                            }
                        }
                }catch (e: Exception){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error General: $e", 24.0F, Toast.LENGTH_LONG)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    Log.d("Error General",e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error de Red: $xml", 24.0F, Toast.LENGTH_LONG)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    Log.d("Error de Red",xml.toString())
                }
            }
        }
    }

    private fun populateListView()
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )

        val cursor = db.fillLocationsListView()
        var orders : ReceptionDataModel?

        Log.d("Location Qty", cursor.count.toString())
        datamodels.clear()
        while (cursor.moveToNext()) {
            orders =
                ReceptionDataModel(this, "0", "q")
            orders.id = cursor.getString(0)
            orders.num = cursor.getString(cursor.getColumnIndex("name"))
            orders.box = "Folio: "+cursor.getString(cursor.getColumnIndex("folio"))

            datamodels.add(orders)
        }
        obtainList()
        cursor.close()
        db.close()

    }

    private fun obtainList() {
        adapter = ReceptionAdapter(this, datamodels)
        locationsLv.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    fun syncLocations() : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockArr = odoo.locations
        Log.d("OrderList", stockArr)
        return stockArr
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

}
