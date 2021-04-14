package com.example.morsaapp

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.morsaapp.adapter.MissingProductsAdapter
import com.example.morsaapp.adapter.ProductsToLocationAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.MissingProductsDatamodel
import com.example.morsaapp.datamodel.ProductsToLocationDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class PickingMovesActivity : AppCompatActivity() {

    private var datamodels = ArrayList<ProductsToLocationDataModel>()
    private var popupDataModel = ArrayList<MissingProductsDatamodel>()
    private lateinit var pickingMovesLv : ListView
    private var pickingIds : String = ""
    private var rackId : String = ""
    //var scan1 : String = ""
    //var scan2 : String = ""
    private var currentOrigin : String = ""
    //var destiny : String = ""
    private var donePickingProcess : Boolean = false
    private lateinit var productsHashMap : HashMap<Int, List<Int>>
    lateinit var adapter : ProductsToLocationAdapter
    private lateinit var popupListViewGlobal:ListView
    private lateinit var popupWindow : PopupWindow
    private lateinit var popupViewGlobal : View
    //lateinit var popupAdapter: MissingProductsAdapter

    //Timer Data
    var timers : HashMap<String, Long> = HashMap()

    lateinit var prefs : SharedPreferences

    private val mScanReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if(action == resources.getString(R.string.activity_intent_action)){
                val value = intent.getStringExtra("barcode_string")
                displayScanResult(value!!)
            }
        }
    }

    private lateinit var progressBar: ProgressBar

    private fun saveHashMap(obj: Any?, context : Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("startupPreferences", 0)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(obj)
        editor.putString("timers", json)
        editor.apply()
    }

    private fun getHashMap(context : Context): HashMap<String, Long> {
        val prefs: SharedPreferences = context.getSharedPreferences("startupPreferences", 0)
        val gson = Gson()
        val json = prefs.getString("timers", "")
        val type = object : TypeToken<HashMap<String?, Long?>?>() {}.type
        return gson.fromJson(json, type)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        productsHashMap = HashMap()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picking_moves)

        prefs = this.getSharedPreferences("startupPreferences", 0)


        if(getHashMap(this).isEmpty()){
            Log.d("Was","Empty")
            saveHashMap(timers,this)
        }

        timers = getHashMap(this)

        pickingMovesLv = findViewById(R.id.picking_moves_lv)
        progressBar = findViewById(R.id.picking_move_progress)
        val timerTxt = findViewById<TextView>(R.id.timer_txt)

        progressBar.isVisible = true

        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.activity_intent_action))
        registerReceiver(mScanReceiver, filter)

        val noPieceMoves = findViewById<FloatingActionButton>(R.id.no_piece_btn)

        pickingIds = intent.getStringExtra("pickingIds")!!
        rackId = intent.getStringExtra("rackId")!!

        val customToast = CustomToast(this, this)
        customToast.show("Escanee la ubicacion origen", 24.0F, Toast.LENGTH_LONG)

        noPieceMoves.setOnClickListener {
            showMissingPopup()
        }

        Log.d("PickingIds", pickingIds)
        /**
         * For each pickingId, reload said picking and its lines
         */
        val dbPicking =
            DBConnect(this, OdooData.DBNAME, null, 1)
        var idsRaw = pickingIds.replace("(", "")
        idsRaw = idsRaw.replace(")", "")
        idsRaw = idsRaw.replace(" ", "")
        val idList = idsRaw.split(",")
        thread {
            for (id in idList) {
                Log.d("Current Id", id)
                if (dbPicking.deleteDataOnTableFromField(OdooData.TABLE_STOCK, "id", id)) {
                    val deferredStockReSync: String = syncInspections(id)
                    val stockItemJson = JSONArray(deferredStockReSync)
                    dbPicking.fillTable(stockItemJson, OdooData.TABLE_STOCK)
                    //Get name
                    val pickingName = dbPicking.getNameFromPicking(id)
                    var name = ""
                    while (pickingName.moveToNext()) {
                        name = pickingName.getString(0)
                    }
                    pickingName.close()
                    val movePickingId = "[$id,\"$name\"]"
                    val finalMovePickingId = movePickingId.replace("/", "\\/")
                    Log.d("PickingId on Move", finalMovePickingId)
                    //With the move Picking Id, reload the lines
                    if (dbPicking.deleteDataOnTableFromField(OdooData.TABLE_STOCK_ITEMS,"picking_id",finalMovePickingId)) {
                        val deferredStockItemsReSync: String = syncInspectionItems(id.toInt())
                        val db = DBConnect(
                            applicationContext,
                            OdooData.DBNAME,
                            null,
                            1
                        )
                        val stockItemJson1 = JSONArray(deferredStockItemsReSync)
                        db.fillTable(stockItemJson1, OdooData.TABLE_STOCK_ITEMS)
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

        if(timers[rackId] == null){
            timers[rackId] = 900000
        }
        val time : Long = timers[rackId] as Long
        Log.d("Time", time.toString())
        timerTxt.setTextColor(Color.GREEN)
        val timer  = object: CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if(millisUntilFinished <= time/2){
                    timerTxt.setTextColor(Color.YELLOW)
                }
                else{
                    timerTxt.setTextColor(Color.GREEN)
                }
                timerTxt.text = String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                            TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(
                                    millisUntilFinished
                                )
                            )
                )
                timers[rackId] = millisUntilFinished
            }

            override fun onFinish() {
                timerTxt.text = resources.getString(R.string.time_over)
                timerTxt.setTextColor(Color.RED)
            }
        }
        timer.start()

    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveHashMap(timers, this)
        finish()
        val intent  = Intent(this, PickingActivity::class.java)
        startActivity(intent)
        unregisterReceiver(mScanReceiver)
    }

    private fun showMissingPopup(){
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
                        val customToast = CustomToast(applicationContext, this@PickingMovesActivity)
                        customToast.show(deferredNofity.await()[1], 24.0F, Toast.LENGTH_LONG)
                    }
                }catch (e: Exception) {
                    Log.d("Error General", e.toString())
                }
                popupWindow.dismiss()
            }catch (e: XmlRpcException){
                Log.d("XMLRPC ERROR", e.toString())
                val customToast = CustomToast(this, this)
                customToast.show("Error en Odoo: $e", 24.0F, Toast.LENGTH_LONG)
            }catch (e: Exception){
                Log.d("ERROR", e.toString())
                val customToast = CustomToast(this, this)
                customToast.show("Error en peticion: $e", 24.0F, Toast.LENGTH_LONG)
            }
        }

        this.popupListViewGlobal = popupView.findViewById(R.id.products_missing_lv)
        populatePopupListview()


        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
    }

    private fun populatePopupListview(){
        popupDataModel.clear()
        for (data in datamodels){
            val item = MissingProductsDatamodel()
            item.id = data.productId.toInt()
            val name = data.stockMoveName
            Log.d("NameParsed", name)
            item.name = name
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

    private fun populateListView(data: String) {

        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            1
        )
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
                val name = cursor1.getString(0).replace("/", "\\/")
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
                Log.d("Sequence", deferredSequence.await())
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
                val customToast = CustomToast(this, this)
                customToast.show("Error: $e", 24.0F, Toast.LENGTH_LONG)
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
                var parsedOrigin = originRaw.substring(originRaw.indexOf("/") + 1)
                parsedOrigin = parsedOrigin.replace("\"]", "")
                Log.d("Parsed Ori", parsedOrigin)
                val realOrigin = parsedOrigin.substring(
                    parsedOrigin.indexOf("/") + 1,
                    parsedOrigin.lastIndex + 1
                )
                realOrigin.replace(" ", "")
                Log.d("Origin", realOrigin)
                orders.origin = realOrigin
                //val name = cursor.getString(cursor.getColumnIndex("product_id"))
                //Parse name
                val description = cursor.getString(cursor.getColumnIndex("product_description"))
                val mixedName  = cursor.getString(cursor.getColumnIndex("product_id"))
                val separateName : Array<String> = mixedName.split(",").toTypedArray()
                val arrayofNames : Array<String> = separateName[1].split(" ").toTypedArray()
                var name = arrayofNames[0]
                name = name.replace("[", "")
                name = name.replace("]", "")
                name = name.replace("\"", "")
                orders.setStockMoveName("$name: $description")
                val locationUnparsed = cursor.getString(cursor.getColumnIndex("location_dest_id"))
                var locationParsed = locationUnparsed
                Log.d("BeforeParsed", locationParsed)
                while(locationParsed.contains("/")){
                    locationParsed = locationParsed.substring(
                        locationParsed.indexOf("/") + 1,
                        locationParsed.lastIndex + 1
                    )
                    Log.d("CurrentParsed", locationParsed)
                }
                locationParsed = locationParsed.replace("\"", "")
                locationParsed = locationParsed.replace("]", "")
                Log.d("AfterParsed", locationParsed)
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

                productsHashMap[orders.id] = listOf(orders.qty, orders.total_qty)
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

    private fun displayScanResult(decodedString: String) {

        Log.d("You Scanned", decodedString)
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
                    Log.d("Scanned Product", scannedProductIdSearch)
                }
            }
        }catch (e: Exception){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error General: $e", 24.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error General", e.toString())
        }catch (xml: XmlRpcException){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error encontrando producto", 24.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error de Red", xml.toString())
        }

        var scanResult = false
        for (i in 0 until pickingMovesLv.adapter.count){
            val item = pickingMovesLv.adapter.getItem(i) as ProductsToLocationDataModel
            Log.d("Current product: ", item.productId)
            if(scanResult){
                break
            }
            else if(decodedString == item.origin){
                scanResult = true
                for(j in 0 until pickingMovesLv.adapter.count){
                    val item2 = pickingMovesLv.adapter.getItem(j) as ProductsToLocationDataModel
                    item2.originScanned = false
                }
                item.originScanned = true
                currentOrigin = decodedString
                val adapterModifier = pickingMovesLv.adapter as ProductsToLocationAdapter
                adapterModifier.notifyDataSetChanged()
                val customToast = CustomToast(this, this)
                customToast.show("Escanee un producto", 24.0F, Toast.LENGTH_LONG)
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
                    builder.setPositiveButton("Ok") { dialog, _ ->
                        try {
                            val num = numberPicker.value
                            val expectedQty = num + item.qty
                            if (expectedQty <= item.total_qty) {
                                item.qty = item.qty+num
                                //Change bar color
                                //Add to hashmap
                                //Sent qty
                                var isCountPicking = false
                                try {
                                    runBlocking {
                                        Log.d(
                                            "Send to Cart Data",
                                            item.id.toString() + "- Qty:" + item.qty.toString()+" Num: "+num
                                        )
                                        val deferredSendToCart: Deferred<List<Any>> =
                                            GlobalScope.async { sendtoCart(item.id, num) }

                                        Log.d(
                                            "Result from deferred",
                                            deferredSendToCart.await().toString()
                                        )
                                        isCountPicking = deferredSendToCart.await()[1] as Boolean

                                    }
                                }catch (e: Exception){
                                    Log.d("Error", e.toString())
                                }
                                if(isCountPicking){
                                    Log.d("IS COUNT", "TRUE")
                                    val countPickingBuilder = AlertDialog.Builder(applicationContext)
                                    countPickingBuilder.setTitle("Conteo de Picking")
                                    countPickingBuilder.setMessage("Favor de contar las piezas:")
                                    val countQty = EditText(applicationContext)
                                    countQty.width = 10
                                    countQty.inputType = InputType.TYPE_CLASS_NUMBER
                                    countPickingBuilder.setView(countQty)
                                    countPickingBuilder.setPositiveButton("Confirmar") { _, _ ->
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

                                val db = DBConnect(
                                    this,
                                    OdooData.DBNAME,
                                    null,
                                    1
                                ).writableDatabase
                                val contentValues = ContentValues()
                                contentValues.put("quantity_done", num)
                                db.update(
                                    OdooData.TABLE_STOCK_ITEMS,
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
                                val customToast = CustomToast(this, this)
                                customToast.show(
                                    "Excediste la cantidad total",
                                    24.0F,
                                    Toast.LENGTH_LONG
                                )
                                dialog.dismiss()
                            }

                            var scanDestiny = true
                            var destiny = ""
                            //Check all lines to show message
                            for(j in 0 until pickingMovesLv.adapter.count){
                                val product = pickingMovesLv.adapter.getItem(j) as ProductsToLocationDataModel
                                destiny = product.location
                                if(product.lineScanned == 0){
                                    scanDestiny = false
                                }
                            }

                            if(scanDestiny){
                                val customToast = CustomToast(this, this)
                                customToast.show(
                                    "Picking completado, escanee ubicación - $destiny",
                                    24.0F,
                                    Toast.LENGTH_LONG
                                )
                            }

                        }catch (e: XmlRpcException){
                            Log.d("XMLRPC ERROR", e.toString())
                            val customToast = CustomToast(this, this)
                            customToast.show("Error en Odoo: $e", 24.0F, Toast.LENGTH_LONG)
                        }catch (e: Exception){
                            Log.d("ERROR", e.toString())
                            val customToast = CustomToast(this, this)
                            customToast.show("Error en peticion: $e", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                    builder.setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                }
                else{
                    val customToast = CustomToast(this, this)
                    customToast.show(
                        "Ubicación incorrecta, actual es - $currentOrigin",
                        24.0F,
                        Toast.LENGTH_LONG
                    )
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
                    builder.setPositiveButton("Ok") { _, _ ->
                        try {
                            //Iterar por Hashmap para enviar los datos de el picking
                                Log.d("Map", productsHashMap.toString())
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

                                            Log.d(
                                                "Product",
                                                product.key.toString() + " - " + qty.toString()
                                            )
                                            val deferredReStock: Deferred<List<Any>> =
                                                GlobalScope.async { sendReStock(product.key, qty) }

                                            Log.d(
                                                "Final Result",
                                                deferredReStock.await().toString()
                                            )

                                            if ((deferredReStock.await()[1].toString()) != "Movimento exitoso") {
                                                countingDone = false
                                            }

                                            //Toast.makeText(applicationContext, deferredReStock.await()[1], Toast.LENGTH_LONG).show()
                                        }
                                    }catch (e: Exception){
                                        Log.d("Error General", e.toString())
                                    }
                                    if(!countingDone){
                                        val goBackToMenuIntent = Intent(
                                            this,
                                            PickingActivity::class.java
                                        )
                                        finish()
                                        startActivity(goBackToMenuIntent)
                                    }
                                    else{
                                        val customToast = CustomToast(this, this)
                                        customToast.show(
                                            "Error al finalizar Picking",
                                            24.0F,
                                            Toast.LENGTH_LONG
                                        )
                                    }
                                } else {
                                    Log.d("Exceeded", "Excedio cantidad total")
                                    //Toast.makeText(applicationContext, "Excediste la cantidad total", Toast.LENGTH_SHORT).show()
                                }
                            }

                        }catch (e: XmlRpcException){
                            Log.d("XMLRPC ERROR", e.toString())
                            val customToast = CustomToast(this, this)
                            customToast.show("Error en Odoo: $e", 24.0F, Toast.LENGTH_LONG)
                        }catch (e: Exception){
                            Log.d("ERROR", e.toString())
                            val customToast = CustomToast(this, this)
                            customToast.show("Error en peticion: $e", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                    builder.setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()
                    Log.d("Hashmap size: ", productsHashMap.size.toString())
                    //Send objects that match that location
                    for (product in productsHashMap) {
                        Log.d(
                            "Product ",
                            "Product: " + product.key + " Qty: " + product.value.toString()
                        )
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
            val customToast = CustomToast(this, this)
            customToast.show(
                "Codigo de barras inválido, se escaneo - $decodedString",
                24.0F,
                Toast.LENGTH_LONG
            )
        }
    }

    private fun sendReStock(moveId: Int, moveQty: Int): List<Any>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.setMovesQty(moveId, moveQty)
    }

    private fun sendtoCart(moveId: Int, moveQty: Int): List<String>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.sendToCart(moveId, moveQty)
    }

    private fun notifyMissing(stockRackId: Int, map: HashMap<Int, Int>): List<String>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.notifyMissing(stockRackId, map)
    }

    private fun sequence(moveList: ArrayList<Int>): String{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.getSequence(moveList)
    }

    private fun searchProduct(product_id: String): String {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.searchProduct(product_id)
    }

    private fun countPicking(moveId: Int, moveQty: Int): List<String>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.countPicking(moveId, moveQty)
    }

    private fun syncInspections(id: String) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockPicking = odoo.getPickingFromRack(id)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

    private fun syncInspectionItems(pickingId: Int) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockPicking = odoo.getInspectionItems(pickingId)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }
}
