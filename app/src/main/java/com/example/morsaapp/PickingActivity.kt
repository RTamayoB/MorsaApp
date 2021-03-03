package com.example.morsaapp

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.PickingAdapter
import com.example.morsaapp.datamodel.PickingDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class PickingActivity : AppCompatActivity() {


    private var datamodels = ArrayList<PickingDataModel>()
    private lateinit var adapter : PickingAdapter
    lateinit var pickingLv : ListView

    lateinit var prefs : SharedPreferences
    lateinit var swipeRefreshLayout : SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picking)
        val toolbar : Toolbar = findViewById(R.id.the_picking_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Pedidos"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        val syncPickingsFAB = findViewById<FloatingActionButton>(R.id.sync_pickings_fab)
        swipeRefreshLayout = findViewById(R.id.picking_lv_refresh)

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = true
            refreshData()
        }

        pickingLv = findViewById(R.id.the_picking_lv)

        pickingLv.setOnItemClickListener {parent, view, position, id ->
            val intent = Intent(applicationContext, PickingMovesActivity::class.java)
            val model : PickingDataModel = pickingLv.getItemAtPosition(position) as PickingDataModel
            val pickingIds = model.picking_ids
            val rackId = model.id
            Log.d("PickingIds", model.picking_ids.toString())
            intent.putExtra("pickingIds",pickingIds)
            intent.putExtra("rackId", rackId)
            finish()
            startActivity(intent)
        }

        syncPickingsFAB.setOnClickListener {
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
    }

    private fun refreshData(){
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        if(db.deleteDataOnTable(Utilities.TABLE_RACK)){
            thread {
                try {
                    val reloadRacks: String = syncRacks()
                    Log.d("Returned Racks", reloadRacks)
                    val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
                    val rackJson = JSONArray(reloadRacks)
                    val result = db.fillTable(rackJson, Utilities.TABLE_RACK)
                    if (result) {
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = pickingLv.adapter as PickingAdapter
                            adapter.notifyDataSetChanged()
                            Toast.makeText(applicationContext, "Exito", Toast.LENGTH_SHORT)
                                .show()
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

        }
    }

    private fun populateListView()
    {
        var racks : PickingDataModel?
        val dbrack = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val cursorRack = dbrack.stockRack
        Log.d("Picking Qty", cursorRack.count.toString())
        datamodels.clear()
        while (cursorRack.moveToNext()){
            var idList = arrayListOf<String>()
            Log.d("Picking IDs", cursorRack.getString(cursorRack.getColumnIndex("picking_ids")).toString())
            racks = PickingDataModel()
            var id = cursorRack.getString(cursorRack.getColumnIndex("picking_ids"))
            id = id.replace("[","")
            id = id.replace("]","")
            Log.d("IDs",id)
            val temp = id.split(",")
            Log.d("Temp", temp.toString())
            val tempArrayList = arrayListOf<String>()
            for (array in temp.indices){
                Log.d("Value", temp[array])
                tempArrayList.add(temp[array])
            }
            idList.addAll(tempArrayList)
            var finalList = idList.toString()
            finalList = finalList.replace("[","(")
            finalList = finalList.replace("]",")")
            racks.picking_ids = finalList
            racks.name = cursorRack.getString(cursorRack.getColumnIndex("display_name"))
            racks.id = cursorRack.getString(cursorRack.getColumnIndex("id"))
            racks.date = cursorRack.getString(cursorRack.getColumnIndex("create_date"))
            racks.box = "1 (Temp)"

            datamodels.add(racks)
        }
        obtainList()
        cursorRack.close()
        dbrack.close()
    }

    private fun obtainList() {
        adapter = PickingAdapter(this, datamodels)
        pickingLv.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun testReStock(): String{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        val prefs = applicationContext.getSharedPreferences("startupPreferences", 0)
        return odooConn.testReStock(prefs.getInt("activeUser",0))
    }

    fun syncRacks() : String{
        val odoo = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odoo.authenticateOdoo()
        val stockRacks = odoo.getStockRack(prefs.getInt("activeUser",1))
        Log.d("OrderList", stockRacks)
        return stockRacks
    }
}
