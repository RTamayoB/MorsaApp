package com.example.morsaapp

import android.content.*
import android.device.ScanManager
import android.device.scanner.configuration.PropertyID
import android.device.scanner.configuration.Triggering
import android.media.AudioManager
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.ReStockAdapter
import com.example.morsaapp.datamodel.ReStockDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class ReStockActivity : AppCompatActivity() {

    private var datamodels = ArrayList<ReStockDataModel>()
    private lateinit var reStockLv : ListView
    private var pickingId : Int = 0
    var scan1 : String = ""
    var scan2 : String = ""
    var scan3 : String = ""
    var qtyHold : Int = 0
    var productsListHM : HashMap<Int,Int> = HashMap()
    private lateinit var swipeRefreshLayout : SwipeRefreshLayout

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
                displayScanResult(barcodeStr)
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
        setContentView(R.layout.activity_re_stock)
        val toolbar: Toolbar = findViewById(R.id.re_stock_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Reabastecimiento"

        swipeRefreshLayout = findViewById(R.id.re_stock_lv_refresh)
        prefs = this.getSharedPreferences("startupPreferences", 0)

        /*
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.activity_intent_filter_action_re_stock))
        registerReceiver(myBroadcastReceiver, filter)
        */

        initScan()

        reStockLv = findViewById(R.id.re_stock_lv)


        val resyncFAB  = findViewById<FloatingActionButton>(R.id.sync_fab)
        //Reload the Listview
        resyncFAB.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Actualizar")
            builder.setMessage("¿Seguro que desea refrescar su pantalla?")
            builder.setPositiveButton("Si") { dialog, which ->
                swipeRefreshLayout.isRefreshing = true
                refreshData()
            }
            builder.setNegativeButton("No") { dialog, which ->
                swipeRefreshLayout.isRefreshing = false
                dialog.dismiss()
            }
            builder.show()
        }

        val intent : Intent = intent
        pickingId = intent.getIntExtra("pickingId", 0)

        swipeRefreshLayout.setOnRefreshListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Actualizar")
            builder.setMessage("¿Seguro que desea refrescar su pantalla?")
            builder.setPositiveButton("Si") { dialog, which ->
                refreshData()
            }
            builder.setNegativeButton("No") { dialog, which ->
                swipeRefreshLayout.isRefreshing = false
                dialog.dismiss()
            }
            builder.show()
        }

        swipeRefreshLayout.isRefreshing = true
        refreshData()
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Salir")
        builder.setMessage("¿Desea salir del proceso?")
        builder.setPositiveButton("Si") { dialog, which ->
            val goBackintent = Intent(this, MainMenuActivity::class.java)
            finish()
            startActivity(goBackintent)
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun refreshData(){
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        if(db.deleteDataOnTable(Utilities.TABLE_STOCK_ITEMS)){
            thread {
                try {
                    val deferredInvoice: String = reStock("")
                    Log.d("ReStock Line", deferredInvoice)
                    val invoicejson = JSONArray(deferredInvoice)
                    //Insert data
                    val invoiceUpdate = db.fillTable(invoicejson, Utilities.TABLE_STOCK_ITEMS)
                    if (invoiceUpdate) {
                        runOnUiThread {
                            //If succesfull, delete data from model, insert again and notify the dataset
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = reStockLv.adapter as ReStockAdapter
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

    private fun populateListView()
    {
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val cursor = db.movesTestReStock()
        var orders : ReStockDataModel

        Log.d("ReStock Qty", cursor.count.toString())
        datamodels.clear()
        while (cursor.moveToNext()) {
            orders = ReStockDataModel()
            orders.id = cursor.getInt(6)
            val mixedId  = cursor.getString(cursor.getColumnIndex("product_id"))
            val arrayOfId : Array<Any> = mixedId.split(",").toTypedArray()
            val idAsString = arrayOfId[0] as String
            val replaced = idAsString.replace("[","")
            orders.reProductId = replaced
            val mixedName  = cursor.getString(cursor.getColumnIndex("product_id"))
            val separateName : Array<String> = mixedName.split(",").toTypedArray()
            val arrayofNames : Array<String> = separateName[1].toString().split(" ").toTypedArray()
            var name = arrayofNames[0]
            name = name.replace("[","")
            name = name.replace("]","")
            name = name.replace("\"","")
            orders.reProduct = name+": "+cursor.getString(cursor.getColumnIndex("product_description"))
            val originUnparsed = cursor.getString(cursor.getColumnIndex("location_id"))
            var originParsed = originUnparsed
            Log.d("BeforeOriginParsed",originParsed)
            while(originParsed.contains("/")){
                originParsed = originParsed.substring(
                    originParsed.indexOf("/") + 1,
                    originParsed.lastIndex+1
                )
                Log.d("CurrentOriginParsed",originParsed)
            }
            originParsed = originParsed.replace("\"","")
            originParsed = originParsed.replace("]","")
            Log.d("AfterOriginParsed",originParsed)
            orders.reOrigin = originParsed

            val destinyUnparsed = cursor.getString(cursor.getColumnIndex("location_dest_id"))
            var destinyParsed = destinyUnparsed
            Log.d("BeforeDestinyParsed",destinyParsed)
            while(destinyParsed.contains("/")){
                destinyParsed = destinyParsed.substring(
                    destinyParsed.indexOf("/") + 1,
                    destinyParsed.lastIndex+1
                )
                Log.d("CurrentDestinyParsed",destinyParsed)
            }
            destinyParsed = destinyParsed.replace("\"","")
            destinyParsed = destinyParsed.replace("]","")
            Log.d("AfterDestinyParsed",destinyParsed)
            orders.reDestiny = destinyParsed

            orders.reQty = cursor.getInt(4)
            orders.reTotalQty = cursor.getInt(2)
            datamodels.add(orders)
        }
        obtainList()
        cursor.close()
        db.close()
    }

    private fun obtainList() {
        val adapter =
            ReStockAdapter(datamodels, this)
        reStockLv.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    /*
    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == resources.getString(R.string.activity_intent_filter_action_re_stock)) {
                //  Received a barcode scan
                try {
                    displayScanResult(intent)
                } catch (e: Exception) {
                    Log.e("Error",e.message)
                }

            }
        }

    }
    */

    private fun displayScanResult(decodedData: String) {

        //val decodedSource = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_source))
        //val decodedData = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_data))
        //val decodedLabelType = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_label_type))

        Log.d("Current Data", decodedData)

        //val db = DBConnect(this, Utilities.DBNAME, null, 1).readableDatabase
        //val cursor = db.rawQuery("SELECT id FROM "+Utilities.TABLE_PRODUCT_PRODUCT+" where barcode = ? OR default_code = ?", arrayOf(decodedData, decodedData))
        var scannedProductIdSearch = ""

        try {
            val deferredProductId = GlobalScope.async { searchProduct(decodedData) }
            var code: String
            runBlocking {
                if (deferredProductId.await() != "[]") {
                    val raw = deferredProductId.await()
                    val parse1 = raw.replace("[", "")
                    val parse2 = parse1.replace("]", "")
                    code = parse2
                    val list: List<String> = code.split(",").map { it.trim() }
                    scannedProductIdSearch = list[0]
                    Log.d("Scanned Product", scannedProductIdSearch.toString())
                }

            }
        }catch (e: Exception){
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Error General: $e",
                    Toast.LENGTH_SHORT
                ).show()
            }
            Log.d("Error General",e.toString())
        }catch (xml: XmlRpcException){
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Error Encontrando Producto: $xml",
                    Toast.LENGTH_SHORT
                ).show()
            }
            Log.d("Error de Red",xml.toString())
        }

        for(i in 0 until reStockLv.adapter.count) {
            val item = reStockLv.adapter.getItem(i) as ReStockDataModel
            Log.d("Item Id", item.reProductId)
            if (decodedData == item.reOrigin) {
                scan3 = decodedData
                Toast.makeText(applicationContext, "Escanee un producto", Toast.LENGTH_SHORT).show()
            }
            if (decodedData == item.reDestiny) {
                scan1 = decodedData
                Toast.makeText(applicationContext, "Escanee el producto ha abastecer", Toast.LENGTH_SHORT).show()
            }
            if (scannedProductIdSearch == item.reProductId) {
                scan2 = item.reProductId
                Toast.makeText(applicationContext, "Escanee la ubicación destino", Toast.LENGTH_SHORT).show()
            }

            //Check of Origin and Product have already been scanned - added last check because of error
            if (scan3 == item.reOrigin && scan2 == item.reProductId && scan1 != item.reDestiny) {
                scan3 = ""
                scan2 = ""
                scan1 = ""
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Cantidad a Mover")
                builder.setMessage("Especifique cantidad:")
                val input = EditText(this)
                input.width = 10
                input.inputType = InputType.TYPE_CLASS_NUMBER
                builder.setView(input)
                builder.setPositiveButton("Ok") { dialog, which ->
                    Log.d("Input", input.text.toString())
                    val num = input.text.toString()
                    if (item.reQty <= item.reTotalQty) {
                        qtyHold = num.toInt()
                        productsListHM[item.id] = num.toInt()
                        item.isCounted = 1
                        item.reQty = num.toInt()
                        val adapt = reStockLv.adapter as ReStockAdapter
                        adapt.notifyDataSetChanged()
                        Toast.makeText(applicationContext, "Escanee la ubicación destino", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Excediste la cantidad total", Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("Cancelar") { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }

            if (scan1 == item.reDestiny && scan2 == item.reProductId && scan3 != item.reOrigin) {
                scan1 = ""
                scan2 = ""
                scan3 = ""
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirmar envio")
                builder.setMessage("¿Enviar resurtido?:")
                builder.setPositiveButton("Ok") { dialog, which ->
                    item.reQty = qtyHold
                    val adapterModifier = reStockLv.adapter as ReStockAdapter
                    adapterModifier.notifyDataSetChanged()
                    for ((id,qty) in productsListHM){
                        if(id == item.id){
                            thread {
                                try {
                                    val deferredReStock: List<Any> = sendReStock(id, qty)

                                    Log.d("Final Result", deferredReStock.toString())
                                    if (deferredReStock[1] == "Movimiento exitoso" as String) {
                                        runOnUiThread {
                                            Toast.makeText(
                                                applicationContext,
                                                deferredReStock[1].toString(),
                                                Toast.LENGTH_LONG
                                            ).show()
                                            item.isCounted = 2
                                            val adapt = reStockLv.adapter as ReStockAdapter
                                            adapt.notifyDataSetChanged()
                                        }
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(
                                                applicationContext,
                                                "No se logro enviar la cantidad",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }catch (e: Exception) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            applicationContext,
                                            "Error: $e",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    Log.d("Error General", e.toString())
                                }
                            }
                        }
                    }
                }
                builder.setNegativeButton("Cancelar") { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }
            /*
            if (scan1 == item.reDestiny && scan2 == item.reProductId && scan3 == item.reOrigin) {
                scan1 = ""
                scan2 = ""
                scan3 = ""
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirmar envio")
                builder.setMessage("¿Enviar resurtido?:")
                builder.setPositiveButton("Ok") { dialog, which ->
                    item.reQty = qtyHold
                    val adapterModifier = reStockLv.adapter as ReStockAdapter
                    adapterModifier.notifyDataSetChanged()
                    val deferredReStock : Deferred<List<Any>> = GlobalScope.async { sendReStock(item.id, item.reQty) }

                    runBlocking {
                        Log.d("Final Result", deferredReStock.await().toString())
                        //Toast.makeText(applicationContext,deferredReStock.await()[2].toString(),Toast.LENGTH_LONG).show()
                    }
                }
                builder.setNegativeButton("Cancelar") { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }*/
        }
    }

    private fun reStock(username : String) : String
    {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        val prefs = applicationContext.getSharedPreferences("startupPreferences", 0)
        return odooConn.reStock(username, prefs.getInt("activeUser",0))
    }

    private fun sendReStock(moveId: Int, moveQty: Int): List<Any>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.setMovesQty(moveId, moveQty)
    }

    private fun searchProduct(product_id : String): String {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.searchProduct(product_id)
    }
}
