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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.example.morsaapp.adapter.MissingProductsAdapter
import com.example.morsaapp.adapter.ProductsToLocationAdapter
import com.example.morsaapp.datamodel.MissingProductsDatamodel
import com.example.morsaapp.datamodel.ProductsToLocationDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import java.lang.Exception
import kotlin.concurrent.thread

class PickingMovesActivity : AppCompatActivity() {

    private var datamodels = ArrayList<ProductsToLocationDataModel>()
    var popupDataModel = ArrayList<MissingProductsDatamodel>()
    private lateinit var pickingMovesLv : ListView
    private var pickingIds : String = ""
    private var rackId : String = ""
    var scan1 : String = ""
    var scan2 : String = ""
    var currentOrigin : String = ""
    var destiny : String = ""
    var donePickingProcess : Boolean = false
    lateinit var productsHashMap : HashMap<Int, List<Int>>
    lateinit var adapter : ProductsToLocationAdapter
    private lateinit var popupListViewGlobal:ListView
    lateinit var popupWindow : PopupWindow
    lateinit var popupViewGlobal : View
    lateinit var popupAdapter: MissingProductsAdapter

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
                //mVibrator.vibrate(100)

                val barcode  = intent!!.getByteArrayExtra(ScanManager.DECODE_DATA_TAG)
                val barcodelen = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0)
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

    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        productsHashMap = HashMap()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picking_moves)

        prefs = this.getSharedPreferences("startupPreferences", 0)

        pickingMovesLv = findViewById(R.id.picking_moves_lv)
        progressBar = findViewById(R.id.picking_move_progress)

        progressBar.isVisible = true

        initScan()

        val noPieceMoves = findViewById<FloatingActionButton>(R.id.no_piece_btn)

        /*
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.activity_intent_filter_action_picking))
        registerReceiver(myBroadcastReceiver, filter)
        */

        pickingIds = intent.getStringExtra("pickingIds")
        rackId = intent.getStringExtra("rackId")

        Toast.makeText(applicationContext,"Escanee la Ubicacion Origen", Toast.LENGTH_SHORT).show()

        noPieceMoves.setOnClickListener {
            showMissingPopup()
        }

        Log.d("PickingIds",pickingIds)
        /**
         * For each pickingId, reload said picking and its lines
         */
        val dbPicking = DBConnect(this, Utilities.DBNAME, null, 1)
        var idsRaw = pickingIds.replace("(", "")
        idsRaw = idsRaw.replace(")", "")
        idsRaw = idsRaw.replace(" ", "")
        val idList = idsRaw.split(",")
        thread {
            for (id in idList) {
                Log.d("Current Id", id)
                if (dbPicking.reloadPickings(id)) {
                    val deferredStockReSync: String = syncInspections(id)
                    val stockItemJson = JSONArray(deferredStockReSync)
                    val result = dbPicking.fillTable(stockItemJson, Utilities.TABLE_STOCK)
                    //Get name
                    val pickingName = dbPicking.getNameFromPicking(id)
                    var name: String = ""
                    while (pickingName.moveToNext()) {
                        name = pickingName.getString(0)
                    }
                    pickingName.close()
                    val movePickingId = "[$id,\"$name\"]"
                    val finalMovePickingId = movePickingId.replace("/","\\/")
                    Log.d("PickingId on Move", finalMovePickingId)
                    //With the move Picking Id, reload the lines
                    if (dbPicking.reloadInspectionLines(finalMovePickingId)) {
                        val deferredStockItemsReSync: String = syncInspectionItems(id.toInt())
                        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
                        val stockItemJson = JSONArray(deferredStockItemsReSync)
                        val results = db.fillTable(stockItemJson, Utilities.TABLE_STOCK_ITEMS)
                    }
                }
            }

            runOnUiThread {
                dbPicking.close()
                datamodels.clear()
                populateListView(pickingIds)
                val adapter = pickingMovesLv.adapter as ProductsToLocationAdapter
                adapter.notifyDataSetChanged()
                progressBar.isVisible = false
            }
        }


        /**
         * Manual way of doing the picking process, in case product doesnt have a code
         * This is temporary, as manual proces should only be done by the admin
         */
        /*
        pickingMovesLv.setOnItemClickListener { parent, view, position, id ->
            val model : ProductsToLocationDataModel = pickingMovesLv.getItemAtPosition(position) as ProductsToLocationDataModel
            if(currentOrigin == model.origin){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Cantidad a Mover")
                builder.setMessage("Especifique cantidad:")
                val input = EditText(this)
                input.width = 10
                input.inputType = InputType.TYPE_CLASS_NUMBER
                builder.setView(input)
                builder.setPositiveButton("Ok") { dialog, which ->
                    try {
                        val num = input.text.toString()
                        if (num.toInt() <= model.total_qty) {
                            model.qty = num.toInt()
                            //Add to hashmap


                            //Sent qty
                            val deferredSendToCart: Deferred<List<Any>> = GlobalScope.async { sendtoCart(model.id, model.qty) }

                            var isCountPicking = false
                            runBlocking {
                                Log.d("Result from deferred",deferredSendToCart.await().toString())
                                isCountPicking = deferredSendToCart.await()[1] as Boolean
                            }
                            if(isCountPicking){
                                Log.d("IS COUNT","TRUE")
                                val countPickingBuilder = AlertDialog.Builder(applicationContext)
                                countPickingBuilder.setTitle("Conteo de Picking")
                                countPickingBuilder.setMessage("Favor de contar las piezas:")
                                val countQty = EditText(applicationContext)
                                input.width = 10
                                input.inputType = InputType.TYPE_CLASS_NUMBER
                                countPickingBuilder.setView(countQty)
                                countPickingBuilder.setPositiveButton("Confirmar") { dialog, which ->
                                    val deferredCount : Deferred<List<Any>> = GlobalScope.async { countPicking(model.id, model.qty) }
                                    runBlocking {
                                        Log.d("Result of count picking", deferredCount.await().toString())
                                    }
                                }
                            }
                            else{
                                Log.d("IS COUNT", "FALSE")
                                Toast.makeText(applicationContext,"Completado", Toast.LENGTH_LONG).show()
                            }

                            productsHashMap[model.id] = listOf(model.qty, model.total_qty)

                            val db = DBConnect(this, Utilities.DBNAME, null, 1).writableDatabase
                            val contentValues = ContentValues()
                            contentValues.put("quantity_done", num.toInt())
                            db.update(
                                Utilities.TABLE_STOCK_ITEMS,
                                contentValues,
                                "id=" + model.id,
                                null
                            )
                            val adapterModifier = pickingMovesLv.adapter as ProductsToLocationAdapter
                            adapterModifier.notifyDataSetChanged()
                            val deferredToCart: Deferred<List<String>> = GlobalScope.async { sendtoCart(model.id, model.qty) }

                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Excediste la cantidad total",
                                Toast.LENGTH_LONG
                            ).show()
                            dialog.dismiss()
                        }

                    }catch (e : XmlRpcException){
                        Log.d("XMLRPC ERROR", e.toString())
                        Toast.makeText(this, "Error en Odoo: $e",Toast.LENGTH_SHORT).show()
                    }catch (e : Exception){
                        Log.d("ERROR", e.toString())
                        Toast.makeText(this, "Error en peticion",Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("Cancelar") { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }
            else{
                Toast.makeText(applicationContext,"Ubicacion origen no coincide", Toast.LENGTH_LONG).show()
            }
        }
         */
    }

    fun syncInspections(id: String) : String{
        val odoo = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odoo.authenticateOdoo()
        val stockPicking = odoo.getPickingFromRack(id)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

    fun syncInspectionItems(pickingId: Int) : String{
        val odoo = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odoo.authenticateOdoo()
        val stockPicking = odoo.getInspectionItems(pickingId)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        val intent  = Intent(this, PickingActivity::class.java)
        startActivity(intent)
    }

    fun showMissingPopup(){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView : View = inflater.inflate(R.layout.no_product_popup, null)

        val widht : Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val height : Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true
        popupWindow = PopupWindow(popupView, widht, height, focusable)
        this.popupViewGlobal = popupView
        val sendMissingBtn = popupView.findViewById<Button>(R.id.send_missing_btn)
        sendMissingBtn.setOnClickListener {
            try{
                val missingProducts = HashMap<Int, Int>()
                for (i in 0 until popupListViewGlobal.adapter.count){
                val item = popupListViewGlobal.adapter.getItem(i) as MissingProductsDatamodel
                if(item.isChecked){
                    missingProducts[item.id] = item.missingQty
                }
                }
                Log.d("RACK AND MISSING PROD", rackId + missingProducts.toString())
                try {
                    val deferredNofity: Deferred<List<String>> =
                        GlobalScope.async { notifyMissing(rackId.toInt(), missingProducts) }
                    runBlocking {
                        Log.d("Result from Notify", deferredNofity.await().toString())
                        Toast.makeText(
                            applicationContext,
                            deferredNofity.await()[1].toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }catch (e: Exception) {
                    Log.d("Error General", e.toString())
                }
                popupWindow.dismiss()
            }catch (e : XmlRpcException){
                Log.d("XMLRPC ERROR", e.toString())
                Toast.makeText(this, "Error en Odoo: $e",Toast.LENGTH_SHORT).show()
            }catch (e : Exception){
                Log.d("ERROR", e.toString())
                Toast.makeText(this, "Error en peticion",Toast.LENGTH_SHORT).show()
            }
        }

        this.popupListViewGlobal = popupView.findViewById(R.id.products_missing_lv)
        populatePopupListview()


        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
    }

    fun populatePopupListview(){
        popupDataModel.clear()
        for (data in datamodels){
            var item = MissingProductsDatamodel()
            item.id = data.productId.toInt()
            val Name = data.stockMoveName
            Log.d("NameParsed", Name)
            item.name = Name
            item.qty = data.qty
            item.totalQty = data.total_qty
            popupDataModel.add(item)
        }

        popupListViewGlobal.adapter =
            MissingProductsAdapter(
                popupDataModel,
                this
            )
    }

    /*
    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == resources.getString(R.string.activity_intent_filter_action_picking)) {
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

    private fun populateListView(data: String) {

        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        var idsRaw = data.replace("(", "")
        idsRaw = idsRaw.replace(")", "")
        idsRaw = idsRaw.replace(" ", "")
        idsRaw = idsRaw.replace("/", "\\/")
        val idList = idsRaw.split(",")
        val pickingsList = arrayListOf<String>()
        Log.d("List", idList.toString())
        for (id in idList.indices) {
            Log.d("Id", idList[id])
            val cursor1 = db.getNameFromPicking(idList[id])
            while (cursor1.moveToNext()) {
                Log.d("Name", cursor1.getString(0))
                val name = cursor1.getString(0).replace("/","\\/")
                pickingsList.add("[" + idList[id] + ",\"" + name + "\"]")
            }
            cursor1.close()
        }
        Log.d("PickingsList", pickingsList.toString())
        /*
        val deferredSequence: Deferred<String> = GlobalScope.async { sequence() }
        runBlocking {
            val json = JSONArray(deferredSequence.await())
            for (i in 0 until json.length()) {
                Log.d("Id in Sequence", json.get(i).toString())
            }
        }
        */
        val moveList = arrayListOf<Int>()

        for (name in pickingsList.indices) {

            Log.d("Name", pickingsList[name])
            val moveIds = db.getPickingMoveIds(pickingsList[name])
            while (moveIds.moveToNext()) {
                Log.d("Id move", moveIds.getString(0))
                moveList.add(moveIds.getInt(0))
            }
        }
        val finalIdList = arrayListOf<Int>()
        try {
            val deferredSequence: Deferred<String> = GlobalScope.async { sequence(moveList) }
            runBlocking {
                Log.d("Sequence", deferredSequence.await().toString())
                val json = JSONArray(deferredSequence.await())
                for (i in 0 until json.length()) {
                    Log.d("Added to sequence", i.toString())
                    val id = json.get(i).toString()
                    val idParsed = id.substring(id.indexOf(":") + 1, id.indexOf("}"))
                    finalIdList.add(idParsed.toInt())
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

        for(i in 0 until finalIdList.size){
            Log.d("List", moveList.toString())
            val cursor = db.pickingMovesById(finalIdList[i].toString())
            var orders: ProductsToLocationDataModel
            Log.d("Count", cursor.count.toString())

            while (cursor.moveToNext()) {
                orders =
                    ProductsToLocationDataModel()
                orders.id = cursor.getInt(6)
                val mixedId = cursor.getString(cursor.getColumnIndex("product_id"))
                val arrayOfId: Array<Any> = mixedId.split(",").toTypedArray()
                val idAsString = arrayOfId[0] as String
                val replaced = idAsString.replace("[", "")
                orders.productId = replaced
                val originRaw = cursor.getString(cursor.getColumnIndex("location_id"))
                var parsedOrigin = originRaw.substring(originRaw.indexOf("/")+1)
                parsedOrigin = parsedOrigin.replace("\"]","")
                Log.d("Parsed Ori",parsedOrigin)
                val realOrigin = parsedOrigin.substring(parsedOrigin.indexOf("/")+1, parsedOrigin.lastIndex+1)
                realOrigin.replace(" ","")
                Log.d("Origin", realOrigin)
                orders.origin = realOrigin
                val Name = cursor.getString(cursor.getColumnIndex("product_id"))
                val realName = Name.substring(Name.indexOf(",\"")+2, Name.length -2)
                orders.setStockMoveName(realName)
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
                orders.lineScanned = 0
                if(orders.qty == orders.total_qty){
                    orders.lineScanned = 1
                }
                else if(orders.qty > 0 && orders.qty < orders.total_qty){
                    orders.lineScanned = 2
                }
                datamodels.add(orders)
            }
        }

        obtainList()
        db.close()
    }

    private fun obtainList() {
        adapter = ProductsToLocationAdapter(
            datamodels,
            this
        )
        pickingMovesLv.adapter = adapter
    }

    private fun displayScanResult(decodedString: String, decodedType: String) {

        //val decodedSource = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_source))
        //val decodedData = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_data))
        //val decodedLabelType = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_label_type))

        Log.d("You Scanned", "$decodedString type - $decodedType")
        /**
         * When data recieved, determine if its an origin, product, or
         * destination and act accordingly, if not, show message of 'Codigo Invalido'
         **/
        var code : String
        var scannedProductIdSearch = ""

        try {
            val productId = GlobalScope.async { searchProduct(decodedString) }
            runBlocking {
                if (productId.await() != "[]") {
                    val parse1 = productId.await().replace("[", "")
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
                    "Error encontrando Producto",
                    Toast.LENGTH_SHORT
                ).show()
            }
            Log.d("Error de Red",xml.toString())
        }

        var scanResult = false
        for (i in 0 until pickingMovesLv.adapter.count){
            val item = pickingMovesLv.adapter.getItem(i) as ProductsToLocationDataModel
            Log.d("Current product: ",item.productId)
            if(scanResult){
                break
            }
            else if(decodedString == item.origin){
                scanResult = true
                for(i in 0 until pickingMovesLv.adapter.count){
                    val item2 = pickingMovesLv.adapter.getItem(i) as ProductsToLocationDataModel
                    item2.originScanned = false
                }
                item.originScanned = true
                currentOrigin = decodedString
                val adapterModifier = pickingMovesLv.adapter as ProductsToLocationAdapter
                adapterModifier.notifyDataSetChanged()
                Toast.makeText(this, "Escanee un Producto", Toast.LENGTH_SHORT).show()
                break
            }
            else if(scannedProductIdSearch == item.productId){
                scanResult = true
                Log.d("Current Origin", currentOrigin)
                Log.d("Scanned Product Origin", item.origin)
                if (currentOrigin == item.origin) {
                    Log.d("Products", "Enter to save product")
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Cantidad a Mover")
                    builder.setMessage("Especifique cantidad:")
                    val numberPicker = NumberPicker(this)
                    numberPicker.maxValue = 100
                    numberPicker.minValue = 1
                    builder.setView(numberPicker)
                    builder.setPositiveButton("Ok") { dialog, which ->
                        try {
                            val num = numberPicker.value.toInt()
                            val expectedQty = num.toInt() + item.qty
                            if (expectedQty <= item.total_qty) {
                                item.qty = item.qty+num.toInt()
                                //Change bar color
                                //Add to hashmap
                                //Sent qty
                                var isCountPicking = false
                                try {
                                    runBlocking {
                                        val deferredSendToCart: Deferred<List<Any>> =
                                            GlobalScope.async { sendtoCart(item.id, item.qty) }

                                        Log.d("Result from deferred", deferredSendToCart.toString())
                                        isCountPicking = deferredSendToCart.await()[1] as Boolean

                                    }
                                }catch (e: Exception){
                                    Log.d("Error",e.toString())
                                }
                                if(isCountPicking){
                                    Log.d("IS COUNT","TRUE")
                                    val countPickingBuilder = AlertDialog.Builder(applicationContext)
                                    countPickingBuilder.setTitle("Conteo de Picking")
                                    countPickingBuilder.setMessage("Favor de contar las piezas:")
                                    val countQty = EditText(applicationContext)
                                    countQty.width = 10
                                    countQty.inputType = InputType.TYPE_CLASS_NUMBER
                                    countPickingBuilder.setView(countQty)
                                    countPickingBuilder.setPositiveButton("Confirmar") { dialog, which ->
                                        thread {
                                            try {
                                                val deferredCount: List<Any> =
                                                    countPicking(item.id, item.qty)
                                                Log.d(
                                                    "Result of count picking",
                                                    deferredCount.toString()
                                                )
                                            }catch (e: Exception){
                                                Log.d("Error", e.toString())
                                            }
                                        }
                                    }
                                }

                                productsHashMap[item.id] = listOf(item.qty, item.total_qty)

                                val db = DBConnect(this, Utilities.DBNAME, null, 1).writableDatabase
                                val contentValues = ContentValues()
                                contentValues.put("quantity_done", num.toInt())
                                db.update(
                                    Utilities.TABLE_STOCK_ITEMS,
                                    contentValues,
                                    "id=" + item.id,
                                    null
                                )
                                if(item.qty == item.total_qty){
                                    item.lineScanned = 1
                                }
                                else{
                                    item.lineScanned = 2
                                }
                                val adapterModifier = pickingMovesLv.adapter as ProductsToLocationAdapter
                                adapterModifier.notifyDataSetChanged()
                                //val deferredToCart: Deferred<List<String>> = GlobalScope.async { sendtoCart(item.id, item.qty) }

                            } else {
                                Toast.makeText(applicationContext, "Excediste la cantidad total", Toast.LENGTH_LONG).show()
                                dialog.dismiss()
                            }

                            var scanDestiny = true
                            var destiny = ""
                            //Check all lines to show message
                            for(i in 0 until pickingMovesLv.adapter.count){
                                val item = pickingMovesLv.adapter.getItem(i) as ProductsToLocationDataModel
                                destiny = item.location
                                if(item.lineScanned == 0){
                                    scanDestiny = false
                                }
                            }

                            if(scanDestiny){
                                Toast.makeText(applicationContext, "Picking completado, escanee ubicacion - $destiny", Toast.LENGTH_LONG).show()
                            }

                        }catch (e : XmlRpcException){
                            Log.d("XMLRPC ERROR", e.toString())
                            Toast.makeText(this, "Error en Odoo: $e",Toast.LENGTH_SHORT).show()
                        }catch (e : Exception){
                            Log.d("ERROR", e.toString())
                            Toast.makeText(this, "Error en peticion",Toast.LENGTH_SHORT).show()
                        }
                    }
                    builder.setNegativeButton("Cancelar") { dialog, which ->
                        dialog.dismiss()
                    }
                    builder.show()
                }
                else{
                    Toast.makeText(applicationContext, "Ubicacion origen incorrecta, actual es - $currentOrigin", Toast.LENGTH_SHORT).show()
                    break
                }
                break
            }
            else if(decodedString == item.location){
                scanResult = true
                if(!donePickingProcess) {
                    //Confirmar envio de picking
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Confirmar Picking")
                    builder.setPositiveButton("Ok") { dialog, which ->
                        try {
                            //Iterar por Hashmap para enviar los datos de el picking
                            var countingDone = false
                            for (product in productsHashMap) {
                                val list = product.value
                                val qty = list[0]
                                val totalQty = list[1]
                                //Escanear producto con su cantidad y
                                if (qty <= totalQty) {
                                    Log.d("Done picking process", donePickingProcess.toString())
                                    try {
                                        runBlocking {
                                            val deferredReStock: Deferred<List<Any>> =
                                                GlobalScope.async { sendReStock(product.key, qty) }

                                            Log.d("Final Result", deferredReStock.toString())

                                            if ((deferredReStock.await()[1].toString()) != "Movimento exitoso") {
                                                countingDone = false
                                            }

                                            //Toast.makeText(applicationContext, deferredReStock.await()[1], Toast.LENGTH_LONG).show()
                                        }
                                    }catch (e: Exception){
                                        Log.d("Error General",e.toString())
                                    }
                                    if(!countingDone){
                                        val goBackToMenuIntent = Intent(this, PickingActivity::class.java)
                                        startActivity(goBackToMenuIntent)
                                        finish()
                                    }
                                    else{
                                        Toast.makeText(applicationContext,"Error al finalizar Picking",Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Log.d("Exceeded", "Excedio cantidad total")
                                    //Toast.makeText(applicationContext, "Excediste la cantidad total", Toast.LENGTH_SHORT).show()
                                }
                            }

                        }catch (e : XmlRpcException){
                            Log.d("XMLRPC ERROR", e.toString())
                            Toast.makeText(this, "Error en Odoo: $e",Toast.LENGTH_SHORT).show()
                        }catch (e : Exception){
                            Log.d("ERROR", e.toString())
                            Toast.makeText(this, "Error en peticion",Toast.LENGTH_SHORT).show()
                        }
                    }
                    builder.setNegativeButton("Cancelar") { dialog, which ->
                        dialog.dismiss()
                    }
                    builder.show()
                    Log.d("Hashmap size: ", productsHashMap.size.toString())
                    //Send objects that match that location
                    for (product in productsHashMap) {
                        Log.d("Product ", "Product: " + product.key + " Qty: " + product.value.toString())
                    }
                }
                break
            }
            /*
            else{
                Toast.makeText(applicationContext, "Codigo de barras invalido", Toast.LENGTH_SHORT).show()
            }
            */
        }

        //Show message acording to result
        if(!scanResult){
            Toast.makeText(applicationContext, "Codigo de barras invalido, se escaneo - $decodedString", Toast.LENGTH_SHORT).show()
        }

        //val db = DBConnect(this, Utilities.DBNAME, null, 1).readableDatabase
        //val cursor = db.rawQuery("SELECT id FROM "+Utilities.TABLE_PRODUCT_PRODUCT+" where barcode = ? OR default_code = ?", arrayOf(decodedData, decodedData))
        //var scannedProductIdSearch = ""
/*
        for(i in 0 until pickingMovesLv.adapter.count) {
            val item = pickingMovesLv.adapter.getItem(i) as ProductsToLocationDataModel
            Log.d("Item Id", item.productId)
            if (decodedData == item.location) {
                scan1 = decodedData
                Toast.makeText(applicationContext, "Escanee un producto", Toast.LENGTH_SHORT).show()
            }

            if (scannedProductIdSearch == item.productId) {
                scan2 = item.productId
                val builder = AlertDialog.Builder(this)
                builder.setTitle("¿Moviendo Producto a Carrito o Rack?")
                builder.setPositiveButton("Carrito") { dialog, which ->
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Cantidad a Mover")
                    builder.setMessage("Especifique cantidad:")
                    val input = EditText(this)
                    input.width = 10
                    input.inputType = InputType.TYPE_CLASS_NUMBER
                    builder.setView(input)
                    builder.setPositiveButton("Ok") { dialog, which ->

                        val num = input.text.toString()
                        item.qty = num.toInt()

                        val db = DBConnect(this, Utilities.DBNAME, null, 1).writableDatabase
                        val contentValues = ContentValues()
                        contentValues.put("quantity_done",num.toInt())
                        db.update(Utilities.TABLE_STOCK_ITEMS,contentValues, "id="+item.id,null)
                        val adapterModifier = pickingMovesLv.adapter as ProductsToLocationAdapter
                        adapterModifier.notifyDataSetChanged()
                        val deferredToCart : Deferred<List<String>> = GlobalScope.async { sendtoCart(item.id,item.qty) }

                        runBlocking {
                            Log.d("Final Result", deferredToCart.await()[1])

                            Toast.makeText(applicationContext,deferredToCart.await()[1],Toast.LENGTH_LONG).show()
                        }
                    }
                    builder.setNegativeButton("Cancel") { dialog, which ->
                        dialog.dismiss()
                    }
                    builder.show()
                }
                builder.setNegativeButton("Rack") { dialog, which ->
                    Toast.makeText(applicationContext, "Escanee una ubicación", Toast.LENGTH_SHORT).show()
                }
                builder.show()

            }

            Log.d("Location","Location = "+item.location+", Scanner = "+scan1)
            Log.d("Product","Product = "+item.productId+", Scanner = "+scan2)
            if (scan1 == item.location && scan2 == item.productId) {
                scan1 = ""
                scan2 = ""
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
//                    if(num.toInt() > item.total_qty){
//                        dialog.dismiss()
//                    }
                    if (item.qty <= item.total_qty) {
                        item.qty = num.toInt()
                        val adapterModifier = pickingMovesLv.adapter as ProductsToLocationAdapter
                        adapterModifier.notifyDataSetChanged()
                        val deferredReStock : Deferred<List<String>> = GlobalScope.async { sendReStock(item.id, item.qty) }

                        runBlocking {
                            Log.d("Final Result", deferredReStock.await()[1])

                            Toast.makeText(applicationContext,deferredReStock.await()[1],Toast.LENGTH_LONG).show()
                        }

                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Excediste la cantidad total",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }
                builder.show()
            }
        }
        */
    }

    private fun sendReStock(moveId: Int, moveQty: Int): List<Any>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.setMovesQty(moveId, moveQty)
    }

    private fun sendtoCart(moveId: Int, moveQty: Int): List<String>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.sendToCart(moveId, moveQty)
    }

    private fun notifyMissing(stockRackId : Int, map : HashMap<Int, Int>): List<String>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.notifyMissing(stockRackId, map)
    }

    private fun sequence(moveList : ArrayList<Int>): String{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.getSequence(moveList)
    }

    private fun searchProduct(product_id : String): String {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.searchProduct(product_id)
    }

    private fun countPicking(moveId: Int, moveQty: Int): List<String>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.countPicking(moveId, moveQty)
    }
}
