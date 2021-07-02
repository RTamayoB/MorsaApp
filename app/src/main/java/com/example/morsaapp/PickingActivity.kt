package com.example.morsaapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.PickingAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.PickingDataModel
import com.example.morsaapp.datamodel.ProductsToLocationDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class PickingActivity : AppCompatActivity() {


    private var datamodels = ArrayList<PickingDataModel>()
    private lateinit var adapter : PickingAdapter
    lateinit var pickingLv : ListView

    var timers : HashMap<String, Long> = HashMap<String, Long>()
    var closedracks : HashMap<String, Boolean> = HashMap<String, Boolean>()

    lateinit var prefs : SharedPreferences
    lateinit var swipeRefreshLayout : SwipeRefreshLayout
    lateinit var orderType : String

    fun saveHashMap(key: String?, obj: Any?, context : Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("startupPreferences", 0)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(obj)
        editor.putString(key, json)
        editor.apply() // This line is IMPORTANT !!!
    }

    fun getHashMap(key: String?, context : Context): HashMap<String, Long> {
        val prefs: SharedPreferences = context.getSharedPreferences("startupPreferences", 0)
        val gson = Gson()
        val json = prefs.getString(key, "")
        val type = object : TypeToken<HashMap<String?, Long?>?>() {}.type
        return gson.fromJson(json, type)
    }

    private fun getRackHashMap(context : Context): HashMap<String, Boolean> {
        val prefs: SharedPreferences = context.getSharedPreferences("startupPreferences", 0)
        val gson = Gson()
        val json = prefs.getString("racks", "")
        val type = object : TypeToken<HashMap<String?, Boolean?>?>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveRackHashMap(obj: Any?, context : Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("startupPreferences", 0)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(obj)
        editor.putString("racks", json)
        editor.apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picking)
        val toolbar : Toolbar = findViewById(R.id.the_picking_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setSubtitleTextColor(Color.WHITE)
        supportActionBar?.title = "Pedidos"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        supportActionBar?.subtitle = prefs.getString("User","")

        val syncPickingsFAB = findViewById<FloatingActionButton>(R.id.sync_pickings_fab)
        swipeRefreshLayout = findViewById(R.id.picking_lv_refresh)

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = true
            refreshData()
        }

        timers = getHashMap("timers", this)
        closedracks = getRackHashMap(this)

        pickingLv = findViewById(R.id.the_picking_lv)

        pickingLv.setOnItemClickListener {parent, view, position, id ->
            val intent = Intent(applicationContext, PickingMovesActivity::class.java)
            val model : PickingDataModel = pickingLv.getItemAtPosition(position) as PickingDataModel
            val pickingIds = model.picking_ids
            val rackId = model.id
            orderType = model.orderType
            Log.d("PickingIds", model.picking_ids.toString())
            intent.putExtra("pickingIds",pickingIds)
            intent.putExtra("rackId", rackId)
            intent.putExtra("orderType", orderType)
            startActivity(intent)
        }

        syncPickingsFAB.setOnClickListener {
            swipeRefreshLayout.isRefreshing = true
            refreshData()
        }

        //Iterate trough lv and eliminate from the timers list
        /*
        val ite = timers.iterator()
        while(ite.hasNext()){
            val key = ite.next().key
            var equal = false
            for (i in 0 until pickingLv.count){
                val e = pickingLv.getItemAtPosition(i) as PickingDataModel
                if(e.id == key){
                    equal = true
                }
            }
            if(!equal){
                timers.remove(key)
            }
        }
        saveHashMap("timers",timers,this)
        */

        refreshData()
    }

    private fun refreshData(){
        val db = DBConnect(applicationContext, OdooData.DBNAME, null, prefs.getInt("DBver",1))
        if(db.deleteDataOnTable(OdooData.TABLE_RACK)){
            thread {
                try {
                    val reloadRacks: String = syncRacks()
                    Log.d("Returned Racks", reloadRacks)
                    val rackJson = JSONArray(reloadRacks)
                    val result = db.fillTable(rackJson, OdooData.TABLE_RACK)
                    if (result) {
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = pickingLv.adapter as PickingAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Exito", 24.0F, Toast.LENGTH_LONG)
                            Log.d("Current racks", closedracks.toString())
                            //Delete the racks that dont appear on the list
                            for (i in 0 until pickingLv.adapter.count){
                                val item = pickingLv.adapter.getItem(i) as PickingDataModel
                                var hasId = false
                                for((myId,value) in closedracks) {
                                    if(item.id == myId){
                                        hasId = true
                                    }
                                }
                                if(!hasId){
                                    closedracks.remove(item.id)
                                }
                            }
                            saveRackHashMap(closedracks,this)

                            Log.d("Remaining racks", closedracks.toString())
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
        var racks : PickingDataModel?
        val dbrack = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        val cursorRack = dbrack.stockRack
        Log.d("Picking Qty", cursorRack.count.toString())
        datamodels.clear()
        while (cursorRack.moveToNext()){
            racks = PickingDataModel()

            //Log.d("Picking IDs", cursorRack.getString(cursorRack.getColumnIndex("picking_ids")).toString())
            var pickingIds = cursorRack.getString(cursorRack.getColumnIndex("picking_ids"))
            pickingIds = pickingIds.replace("[","(")
            pickingIds = pickingIds.replace("]",")")
            racks.picking_ids = pickingIds
            Log.d("Final PickingIds",racks.picking_ids)
            racks.name = cursorRack.getString(cursorRack.getColumnIndex("display_name"))
            racks.id = cursorRack.getString(cursorRack.getColumnIndex("id"))
            racks.date = cursorRack.getString(cursorRack.getColumnIndex("create_date"))
            racks.box = "Abierto"
            if(closedracks.isNullOrEmpty()){
                racks.box = "Abierto"
            }
            else {
                for ((myId, value) in closedracks) {
                    if (myId == racks.id) {
                        if (value) {
                            racks.box = "Cerrado"
                        } else {
                            racks.box = "Abierto"
                        }
                    }
                }
            }
            racks.orderType = cursorRack.getString(cursorRack.getColumnIndex("order_type"))
            //Check if local
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
        var list : ArrayList<String> = ArrayList()
        for(e in datamodels.indices){

            Log.d("Rack "+datamodels[e].id, datamodels[e].name)
            Log.d("Rack "+datamodels[e].id, datamodels[e].picking_ids)
            list.add(datamodels[e].id)
        }
        Log.d("Full List", list.toString())
        val ite = timers.iterator()
        while(ite.hasNext()){
            val key = ite.next().key
            var equal = false
            for (id in list){
                if(id == key){
                    equal = true
                }
            }
            if(!equal){
                Log.d("Removed", key)
                ite.remove()
            }
        }
        saveHashMap("timers",timers,this)
    }

    private fun testReStock(): String{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        val prefs = applicationContext.getSharedPreferences("startupPreferences", 0)
        return odooConn.testReStock(prefs.getInt("activeUser",0))
    }

    fun syncRacks() : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockRacks = odoo.getStockRack(prefs.getInt("activeUser",1))
        Log.d("OrderList", stockRacks)
        return stockRacks
    }
}
