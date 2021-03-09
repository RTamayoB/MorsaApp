package com.example.morsaapp

import android.content.*
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
//
import android.device.ScanManager
import android.device.scanner.configuration.PropertyID
import android.device.scanner.configuration.Triggering
import android.media.AudioManager
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.CountAdapter
import com.example.morsaapp.datamodel.CountDataModel
import com.example.morsaapp.datamodel.ReceptionDataModel
import org.apache.xmlrpc.XmlRpcException
import java.lang.Exception
import kotlin.concurrent.thread


class CountActivity : AppCompatActivity() {

    var datamodels = ArrayList<CountDataModel>() //Array of data for the list of products
    lateinit var countLv : ListView //ListView view for the items
    var location : String = "" //Variable to hold the location
    var lineId :Int = 0 //Variable to hold the lineID
    lateinit var products : HashMap<Int, Int> //Hashmap that store the lineId (key) and the qty of the line (value)
    lateinit var adapter : CountAdapter //Adapter for the ListView
    lateinit var swipeRefreshLayout : SwipeRefreshLayout
    lateinit var progressBar: ProgressBar

    //Decode Variables
    val SCAN_ACTION = ScanManager.ACTION_DECODE
    lateinit var mVibrator: Vibrator
    lateinit var mScanManager: ScanManager
    lateinit var soundPool: SoundPool
    var soundid : Int = 0
    lateinit var barcodeStr : String

    lateinit var prefs : SharedPreferences

    val mScanReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if(action == "android.intent.ACTION_DECODE_DATA"){
                soundPool.play(soundid, 1.0f, 1.0f, 0, 0, 1.0f)
                mVibrator.vibrate(100)

                val barcode  = intent!!.getByteArrayExtra(ScanManager.DECODE_DATA_TAG)
                val barcodelen = intent?.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0)
                val temp = intent.getByteExtra(ScanManager.BARCODE_TYPE_TAG, 0.toByte())
                Log.i("debug", "----codetype--$temp")
                barcodeStr = String(barcode, 0, barcodelen)
                Log.d("Result", barcodeStr)
                displayScanResult(barcodeStr, temp.toString())
                mScanManager.stopDecode()
            }
        }
    }

    private fun initScan() {
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mScanManager = ScanManager()
        mScanManager.openScanner()
        mScanManager.triggerMode = Triggering.HOST

        mScanManager.switchOutputMode(0)
        soundPool = SoundPool(1, AudioManager.STREAM_NOTIFICATION, 100) // MODE_RINGTONE
        soundid = soundPool.load("/etc/Scan_new.ogg", 1)

        //Set IntentFilter
        val filter = IntentFilter()
        val idbuf : IntArray= intArrayOf(PropertyID.WEDGE_INTENT_ACTION_NAME, PropertyID.WEDGE_INTENT_DATA_STRING_TAG)
        val value_buf = mScanManager.getParameterString(idbuf)
        if(value_buf != null && value_buf[0] != null && !value_buf[0].equals("")) {
            filter.addAction(value_buf[0])
        } else {
            filter.addAction(SCAN_ACTION)
        }
        registerReceiver(mScanReceiver, filter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_count)
        setSupportActionBar(findViewById(R.id.count_tb))

        initScan()


        prefs = this.getSharedPreferences("startupPreferences", 0)
        /*
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.activity_intent_filter_action_count))
        registerReceiver(myBroadcastReceiver, filter)
        */

        products = HashMap() //Instantiate the product hashmap
        countLv = findViewById(R.id.count_lv) //Instantiate Listview
        swipeRefreshLayout = findViewById(R.id.count_lv_refresh)
        progressBar = findViewById(R.id.progress_count)
        val finalizeBtn = findViewById<Button>(R.id.finalize_count_btn) //Instantiate finalize button
        val resyncFAB = findViewById<FloatingActionButton>(R.id.resync_fab) // Instantiate reload button

        progressBar.isVisible = false

        /**
         * Load
         */
        swipeRefreshLayout.isRefreshing = true
        refreshData()

        //Finish activity
        finalizeBtn.setOnClickListener {
            //This now has to just exit from the activity
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setTitle("Finalizar")
            builder.setMessage("¿Has finalizado el conteo?")
            builder.setNegativeButton("Cancelar") {dialog, which ->
                dialog.dismiss()
            }
            builder.setPositiveButton("Si") {dialog, which ->
                val intent = Intent(applicationContext, MainMenuActivity::class.java)
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("No") {dialog, which ->
                dialog.dismiss()
            }
            builder.show()
        }

        //Reload the Listview
        resyncFAB.setOnClickListener {
            swipeRefreshLayout.isRefreshing = true
            refreshData()
        }

        countLv.setOnItemClickListener { parent, view, position, id ->
            val model : CountDataModel = countLv.getItemAtPosition(position) as CountDataModel
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Producto - "+model.code)
            builder.setMessage("¿Reportar producto como no encontrado?")
            builder.setPositiveButton("Reportar") { dialog, which ->
                    thread {
                        try {
                            val reportHashMap : HashMap<Int,Int> = HashMap()
                            reportHashMap[model.lineId] = 0
                            val deferredSendCount = sendCount(reportHashMap)
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "Reportado",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                Log.d("Result of count", deferredSendCount)
                            }
                        }catch (e: XmlRpcException){
                            Toast.makeText(applicationContext,e.toString(), Toast.LENGTH_LONG).show()
                        }
                        catch (c: Exception){
                            Toast.makeText(applicationContext,c.toString(), Toast.LENGTH_LONG).show()
                        }
                    }
            }
            builder.setNegativeButton("Cancelar") {dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.reception_menu,menu)

        val searchItem : MenuItem? = menu?.findItem(R.id.action_search)
        val searchView : SearchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
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

    /*
    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == resources.getString(R.string.activity_intent_filter_action_count)) {
                //  Received a barcode scan
                try {
                    displayScanResult(intet)
                } catch (e: Exception) {
                    Log.e("Error",e.message)
                }

            }
        }

    }
     */


    private fun refreshData(){
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        if(db.deleteDataOnTable(Utilities.TABLE_INVENTORY_LINE)){
            thread {
                try {
                    val deferredInvoice: String = getInventoryLine()
                    Log.d("Inventory Line", deferredInvoice)
                    val invoicejson = JSONArray(deferredInvoice)
                    //Insert data
                    val invoiceUpdate = db.fillTable(invoicejson, Utilities.TABLE_INVENTORY_LINE)
                    if (invoiceUpdate) {
                        runOnUiThread {
                            //If succesfull, delete data from model, insert again and notify the dataset
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = countLv.adapter as CountAdapter
                            adapter.notifyDataSetChanged()
                            Toast.makeText(
                                applicationContext,
                                "Lista Actualizada",
                                Toast.LENGTH_SHORT
                            ).show()
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
        else{
            Toast.makeText(applicationContext,"Error al cargar",Toast.LENGTH_SHORT).show()
        }
    }

    //Fills the lines in the datamodel for inventory_line
    private fun populateListView()
    {
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val cursor = db.inventory
        var items : CountDataModel?

        Log.d("Count Qty", cursor.count.toString())
        datamodels.clear()
        Log.d("QTY",cursor.count.toString())
        while (cursor.moveToNext()) {
            Log.d("New", "Item")
            items = CountDataModel()
            items.lineId = cursor.getInt(3)
            val locRaw = cursor.getString(0)
            val realLoc = locRaw.substring(locRaw.indexOf("/")+1, locRaw.indexOf("\"]"))
            val realLoc2 = realLoc.substring(realLoc.indexOf("/")+1, realLoc.lastIndex+1)
            items.realLocation = cursor.getString(0)
            items.location = realLoc2
            items.code = cursor.getString(cursor.getColumnIndex("product_name"))+": "+cursor.getString(cursor.getColumnIndex("product_description")) //Was 1 (product_code)
            items.theoricalQty = cursor.getString(2)
            items.totalQty = "0"


            datamodels.add(items)
        }

        //datamodels.sort()

        obtainList()
        cursor.close()
        db.close()
    }

    //Fills the adapter with the datamodel
    private fun obtainList() {
        Log.d("Loading to adapter", "Loading")
        adapter = CountAdapter(datamodels, this)
        countLv.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun displayScanResult(decodedString: String, decodedType : String) {

        //val decodedSource = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_source))
        //val decodedData = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_data))
        //val decodedLabelType = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_label_type))

        var isCode = false
        var code : String
        var productName = ""

        try {
            val deferredProductId = GlobalScope.async { searchProduct(decodedString) }
            runBlocking {
                if (deferredProductId.await() != "[]") {
                    isCode = true
                    Log.d("Raw code result", deferredProductId.await().toString())
                    val raw = deferredProductId.await()
                    val name = raw.substring(raw.indexOf("=") + 1, raw.indexOf(","))
                    Log.d("Name", name)
                    productName = name
                    val parse1 = raw.replace("[", "")
                    val parse2 = parse1.replace("]", "")
                    code = parse2
                    val list: List<String> = code.split(",").map { it.trim() }
                    Log.d("List", list.toString())

                } else {
                    isCode = false
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
                    "Error encontrando Producto",
                    Toast.LENGTH_SHORT
                ).show()
                swipeRefreshLayout.isRefreshing = false
            }
            Log.d("Error de Red",xml.toString())
        }

        var showMessage : Boolean = false
        for (i in 0 until countLv.adapter.count) {
            val item = countLv.adapter.getItem(i) as CountDataModel
            if (decodedString == item.location) {
                location = item.realLocation
                Log.d("Location", location)
                showMessage = true
            }
        }

        if(showMessage){
            Log.d("Showing Message","True")
            Toast.makeText(applicationContext,"Escanee el producto", Toast.LENGTH_LONG).show()
        }

        Log.d("You Scanned", decodedString)
        val builder = AlertDialog.Builder(this)

        //Based on the scanned code check if it corresponds to a Route
        val db = DBConnect(this, Utilities.DBNAME, null, 1)
        //Check for routes with that name
        Log.d("Product - Location", "$productName-$location")
        //If not location, check If is product and is related to the location
        val checkProduct = db.checkCountProduct(productName, location )
        if(checkProduct.count > 0){
            Log.d("Entered", "in")
            //Show loading bar
            progressBar.isVisible = true
            while (checkProduct.moveToNext()){
                lineId = checkProduct.getInt(0)
                Log.d("lINEiD", lineId.toString())
            }

            for(i in 0 until countLv.adapter.count) {
                val item = countLv.adapter.getItem(i) as CountDataModel
                if (lineId == item.lineId) {
                    item.isCounted = true
                    Log.d("Yes", "There are")
                    //Compute qty
                }
            }
            try {
                runBlocking {
                    val deferredTheoriticalQty: Deferred<List<Any>> =
                        GlobalScope.async { computeTheoreticalQty(lineId) }
                    Log.d("Update", deferredTheoriticalQty.toString())
                    if (deferredTheoriticalQty.await()[0] as Boolean) {
                        //Got qty
                        progressBar.isVisible = false
                        val adap = countLv.adapter as CountAdapter
                        adap.notifyDataSetChanged()
                        builder.setCancelable(false)
                        builder.setTitle("Especifica cantidad")
                        val input = EditText(applicationContext)
                        input.width = 10
                        input.inputType = InputType.TYPE_CLASS_NUMBER
                        builder.setView(input)
                        builder.setNegativeButton("Cancelar") { dialog, which ->
                            dialog.dismiss()
                        }
                        builder.setPositiveButton("Aceptar") { dialog, which ->
                            val product: HashMap<Int, Int> = HashMap()
                            //Add to hashmap
                            val value = input.text.toString()
                            Log.d("Value", value)
                            product[lineId] = value.toInt()
                            thread {
                                try {
                                    val deferredSendCount = sendCount(product)
                                    runOnUiThread {
                                        Toast.makeText(
                                            applicationContext,
                                            "Enviado ",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                        Log.d("Result of count", deferredSendCount)
                                    }
                                }catch (e: Exception){
                                    Log.d("Error",e.toString())
                                }
                            }
                        }
                        builder.show()

                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Error obteninedo cantidad",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }catch (e: Exception){
                Log.d("Error",e.toString())
            }
        }
        else if (checkProduct.count == 0 && isCode){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Producto inexistente")
            builder.setMessage("Producto '$decodedString' no correspondiente a ubicacion")
            builder.setPositiveButton("Entendido") { dialog, which ->
                val goBackintent = Intent(this, MainMenuActivity::class.java)
                startActivity(goBackintent)
            }
            builder.show()
        }
        else if(!showMessage && !isCode){
            Toast.makeText(
                applicationContext,
                "Codigo '$decodedString' no es producto ni ubicacion",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //Function that conects to Odoo and sends the qty as a hashmap
    private fun sendCount(products : HashMap<Int, Int>): String{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.sendCount(products)
    }

    //Function that returns you the theoretical qty of a line
    private fun computeTheoreticalQty(id : Int): List<Any>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.computeTheoreticalQty(id)
    }

    //Returns the inventoryLines corresponding to that user
    private fun getInventoryLine() : String
    {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        val noIds = emptyList<Int>()
        return odooConn.getStockInventoryLine(prefs.getInt("activeUser",1))
    }

    private fun searchProduct(product_id : String): String {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""), this)
        odooConn.authenticateOdoo()
        return odooConn.searchProductName(product_id)
    }

}
