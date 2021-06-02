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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.example.morsaapp.adapter.ProductsToLocationAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.ProductsToLocationDataModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class ProductsToLocationActivity : AppCompatActivity() {


    private var datamodels = ArrayList<ProductsToLocationDataModel>()
    private lateinit var productToLocationLv : ListView
    private var pickingId : Int = 0
    private var name : String? = ""
    var scan1 : String = ""
    var scan2 : String = ""

    //Decode Variables
    val SCAN_ACTION = ScanManager.ACTION_DECODE
    lateinit var mVibrator: Vibrator
    lateinit var mScanManager: ScanManager
    lateinit var soundPool: SoundPool
    var soundid : Int = 0
    lateinit var barcodeStr : String

    lateinit var prefs : SharedPreferences

    var completeName : String = ""
    lateinit var progressBar : ProgressBar

    val mScanReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if(action == resources.getString(R.string.activity_intent_action)){
                /*soundPool.play(soundid, 1.0f, 1.0f, 0, 0, 1.0f)
                mVibrator.vibrate(100)

                val barcode  = intent!!.getByteArrayExtra(ScanManager.DECODE_DATA_TAG)
                val barcodelen = intent?.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0)
                val temp = intent.getByteExtra(ScanManager.BARCODE_TYPE_TAG, 0.toByte())
                Log.i("debug", "----codetype--$temp")
                barcodeStr = String(barcode, 0, barcodelen)
                Log.d("Result", barcodeStr)*/
                    val value = intent.getStringExtra("barcode_string")
                displayScanResult(value, "")
                //mScanManager.stopDecode()
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

    override fun onBackPressed() {
        super.onBackPressed()
        val goBackintent = Intent(this, MainMenuActivity::class.java)
        unregisterReceiver(mScanReceiver)
        startActivity(goBackintent)
        finish()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_to_location)
        val toolbar : Toolbar = findViewById(R.id.route_issues_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Picking para Acomodo"

        prefs = this.getSharedPreferences("startupPreferences", 0)
        progressBar = findViewById(R.id.progressBar_location)

        //initScan()
        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.activity_intent_action))
        registerReceiver(mScanReceiver, filter)

        /*
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.activity_intent_filter_action_product_to_location))
        registerReceiver(myBroadcastReceiver, filter)
        */

        productToLocationLv = findViewById(R.id.route_issues_lv)

        val intent : Intent = intent
        pickingId = intent.getIntExtra("pickingId",0)
        //name = intent.getStringExtra("name")
        Log.d("PickingId",pickingId.toString())

        try {
            val deferredMoves: Deferred<String> = GlobalScope.async { movesTest("", pickingId) }

            runBlocking {
                Log.d("Route Issue Moves", deferredMoves.await())
                val data1 = deferredMoves.await().replace("[", "(")
                val dataCorrect = data1.replace("]", ")")
                completeName = "[$pickingId,\"$name\"]"
                Log.d("Complete Name", completeName)
                //populateListView(completeName)
            }
        }catch (e: Exception) {
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error: $e", 24.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error General", e.toString())
        }

        /**
         * Delete the stock.move lines that have that completeName, download them again (so they are the most updated)
         * and show the lines on the ListView
         */
        val dbReload =
            DBConnect(this, OdooData.DBNAME, null, prefs.getInt("DBver",1))
        if(dbReload.deleteDataOnTable(OdooData.TABLE_STOCK_ITEMS)){
            thread {
                try {
                    val deferredStockItemsReSync: String =  syncLocationItems(pickingId)
                    val stockItemJson = JSONArray(deferredStockItemsReSync)
                    val result = dbReload.fillTable(stockItemJson, OdooData.TABLE_STOCK_ITEMS)
                    if (result) {
                        runOnUiThread {
                            progressBar.isVisible = false
                            datamodels.clear()
                            populateListView(completeName)
                            val adapter = productToLocationLv.adapter as ProductsToLocationAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Exito", 24.0F, Toast.LENGTH_LONG)
                        }

                    } else {
                        runOnUiThread {
                            progressBar.isVisible = false
                            val customToast = CustomToast(this, this)
                            customToast.show("Sin Exito", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                }catch (e: Exception){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error General: $e", 24.0F, Toast.LENGTH_LONG)
                        progressBar.isVisible = false
                    }
                    Log.d("Error General",e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error de Red: $xml", 24.0F, Toast.LENGTH_LONG)
                        progressBar.isVisible = false
                    }
                    Log.d("Error de Red",xml.toString())
                }

            }
        }

        val customToast = CustomToast(this, this)
        customToast.show("Escanee la ubicación", 24.0F, Toast.LENGTH_LONG)
    }

    /*
    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == resources.getString(R.string.activity_intent_filter_action_product_to_location)) {
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

    private fun displayScanResult(decodedString : String, decodedType : String) {

        //val decodedSource = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_source))
        //val decodedData = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_data))
        //val decodedLabelType = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_label_type))

        Log.d("Current Data", decodedString)

        //val db = DBConnect(this, Utilities.DBNAME, null, 1).readableDatabase
        //val cursor = db.rawQuery("SELECT id FROM "+Utilities.TABLE_PRODUCT_PRODUCT+" where barcode = ? OR default_code = ?", arrayOf(decodedData, decodedData))
        var scannedProductIdSearch = ""

        try {
            val deferredProductId = GlobalScope.async { searchProduct(decodedString) }
            var code : String
            runBlocking {
                Log.d("Result of code", deferredProductId.await())
                if(deferredProductId.await() != "[]"){
                    val raw = deferredProductId.await()
                    val parse1 = raw.replace("[","")
                    val parse2 = parse1.replace("]","")
                    code = parse2
                    val list : List<String> = code.split(",").map { it.trim() }
                    scannedProductIdSearch = list[0]
                    Log.d("Scanned Product", scannedProductIdSearch.toString())
                }

            }
        }catch (e: Exception){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error General: $e", 24.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error General",e.toString())
        }catch (xml: XmlRpcException){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error encontrando producto", 24.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error de Red",xml.toString())
        }

        for(i in 0 until productToLocationLv.adapter.count) {
            val item = productToLocationLv.adapter.getItem(i) as ProductsToLocationDataModel
            item.originScanned = false
        }

        for(i in 0 until productToLocationLv.adapter.count){
            val item = productToLocationLv.adapter.getItem(i) as ProductsToLocationDataModel
            Log.d("Item Id", item.productId)
            if(decodedString == item.location){
                scan1 = decodedString
                item.originScanned = true
                val adapterModifier = productToLocationLv.adapter as ProductsToLocationAdapter
                adapterModifier.notifyDataSetChanged()
                val customToast = CustomToast(this, this)
                customToast.show("Escanee un Producto", 24.0F, Toast.LENGTH_LONG)
            }

            if (scannedProductIdSearch == item.productId){
                scan2 = item.productId
                val customToast = CustomToast(this, this)
                customToast.show("Escanee una Ubicación", 24.0F, Toast.LENGTH_LONG)
            }

            if(scan1 == item.location && scan2 == item.productId){
                scan1 = ""
                scan2 = ""
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Cantidad a Mover")
                builder.setMessage("Especifique cantidad:")
                val input = EditText(this)
                input.width = 10
                input.inputType = InputType.TYPE_CLASS_NUMBER
                builder.setView(input)
                builder.setPositiveButton("Ok"){dialog, which ->
                    Log.d("Input", input.text.toString())
                    val num = input.text.toString()
//                    if(num.toInt() > item.total_qty){
//                        dialog.dismiss()
//                    }
                    val total = item.qty + num.toInt()
                    if(total > item.total_qty){
                        val customToast = CustomToast(this, this)
                        customToast.show("Excedio la cantidad", 24.0F, Toast.LENGTH_LONG)
                    }
                    else {
                        val db = DBConnect(this, OdooData.DBNAME, null, prefs.getInt("DBver", 1)).writableDatabase
                        val contentValues = ContentValues()
                        contentValues.put("quantity_done", num.toInt())
                        db.update(OdooData.TABLE_STOCK_ITEMS, contentValues, "id=" + item.id, null)
                        item.qty = total
                        val adapterModifier =
                            productToLocationLv.adapter as ProductsToLocationAdapter
                        adapterModifier.notifyDataSetChanged()
                        thread {
                            try {
                                val moves: List<Any> = setMoves(item.id, num.toInt())
                                Log.d("Final Result", moves.toString())
                                runOnUiThread {
                                    val customToast = CustomToast(this, this)
                                    customToast.show(
                                        moves[1].toString(),
                                        24.0F,
                                        Toast.LENGTH_LONG
                                    )
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    val customToast = CustomToast(this, this)
                                    customToast.show("Error: $e", 24.0F, Toast.LENGTH_LONG)
                                }
                                Log.d("Error General", e.toString())
                            }
                        }
                        if (total < item.total_qty) {
                            item.isLineScanned = 2
                            val adapterModifier = productToLocationLv.adapter as ProductsToLocationAdapter
                            adapterModifier.notifyDataSetChanged()
                        } else if (total == item.total_qty) {
                            item.isLineScanned = 1
                            val adapterModifier =
                                productToLocationLv.adapter as ProductsToLocationAdapter
                            adapterModifier.notifyDataSetChanged()
                        } else {
                            val customToast = CustomToast(this, this)
                            customToast.show(
                                "Excediste la cantidad total",
                                24.0F,
                                Toast.LENGTH_LONG
                            )
                            item.qty = 0
                        }
                    }
                }
                builder.setNegativeButton("Cancel"){dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }
        }

    }

    private fun populateListView(data: String)
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        val cursor = db.getLocationMoves(data)
        var orders : ProductsToLocationDataModel
        if(cursor.count <= 0){
            Log.d("NO match", "NO")
        }

        while (cursor.moveToNext()) {
            Log.d("Id from cursor", cursor.getInt(6).toString())
            orders = ProductsToLocationDataModel()
            orders.id = cursor.getInt(6)
            val mixedId = cursor.getString(cursor.getColumnIndex("product_id"))
            val arrayOfId: Array<Any> = mixedId.split(",").toTypedArray()
            val idAsString = arrayOfId[0] as String
            val replaced = idAsString.replace("[", "")
            val originUnparsed = cursor.getString(cursor.getColumnIndex("location_id"))
            val originParsed = originUnparsed.substring(originUnparsed.indexOf("/")+1, originUnparsed.indexOf("]")-1)
            orders.origin = originParsed
            orders.productId = replaced
            val mixedName  = cursor.getString(cursor.getColumnIndex("product_id"))
            val separateName : Array<String> = mixedName.split(",").toTypedArray()
            val arrayofNames : Array<String> = separateName[1].toString().split(" ").toTypedArray()
            var name = arrayofNames[0]
            name = name.replace("[","")
            name = name.replace("]","")
            name = name.replace("\"","")
            orders.setStockMoveName(name)
            val locationUnparsed = cursor.getString(cursor.getColumnIndex("location_dest_id"))
            var locationParsed = locationUnparsed
            Log.d("BeforeParsed",locationParsed)
            while(locationParsed.contains("/")){
                locationParsed = locationParsed.substring(
                    locationParsed.indexOf("/") + 1,
                    locationParsed.lastIndex+1
                )
                Log.d("CurrentParsed",locationParsed)
            }
            locationParsed = locationParsed.replace("\"","")
            locationParsed = locationParsed.replace("]","")
            Log.d("AfterParsed",locationParsed)
            orders.setLocation(locationParsed)
            orders.qty = cursor.getInt(4)
            orders.total_qty = cursor.getInt(2)
            orders.isChecked = false
            if (orders.qty > 0 && orders.qty < orders.total_qty){
                orders.isLineScanned = 2
            }
            if (orders.qty == orders.total_qty){
                orders.isLineScanned = 1
            }
            datamodels.add(orders)
        }
        obtainList()
        cursor.close()
        db.close()
    }

    private fun obtainList() {
        val adapter = ProductsToLocationAdapter(
            datamodels,
            this
        )
        productToLocationLv.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun movesTest(location: String, pickingId : Int) : String
    {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.movesTest(location, pickingId)
    }

    private fun setMoves(moveId : Int, moveQty : Int) : List<Any>
    {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        val noIds = emptyList<Int>()
        return odooConn.setMovesQty(moveId,moveQty)
    }

    fun syncLocationItems(pickingOriginativeId: Int) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockPicking = odoo.getLocationsItems(pickingOriginativeId)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

    private fun searchProduct(product_id : String): String {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.searchProduct(product_id)
    }
}