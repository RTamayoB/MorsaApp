package com.example.morsaapp

//
import android.content.*
import android.device.ScanManager
import android.device.scanner.configuration.PropertyID
import android.device.scanner.configuration.Triggering
import android.graphics.Color
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.morsaapp.adapter.CountAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.CountDataModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_count.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
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

            if(action == resources.getString(R.string.activity_intent_action)){
                /*soundPool.play(soundid, 1.0f, 1.0f, 0, 0, 1.0f)
                mVibrator.vibrate(100)

                val barcode  = intent!!.getByteArrayExtra(ScanManager.DECODE_DATA_TAG)
                val barcodelen = intent?.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0)
                val temp = intent.getByteExtra(ScanManager.BARCODE_TYPE_TAG, 0.toByte())
                Log.i("debug", "----codetype--$temp")
                barcodeStr = String(barcode, 0, barcodelen)
                Log.d("Result", barcodeStr)*/
                val serverPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                val value = intent.getStringExtra(serverPrefs.getString("scanner_key","data"))
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_count)
        val toolbar : Toolbar = findViewById(R.id.count_tb)
        setSupportActionBar(toolbar)
        toolbar.setSubtitleTextColor(Color.WHITE)
        toolbar.setTitleTextColor(Color.WHITE)
        supportActionBar?.title = "Conteo Cíclico"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        supportActionBar?.subtitle = prefs.getString("User","")

        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.activity_intent_action))
        registerReceiver(mScanReceiver, filter)


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
                unregisterReceiver(mScanReceiver)
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
            val qtybuilder = AlertDialog.Builder(this)
            qtybuilder.setCancelable(false)
            qtybuilder.setTitle("Especifica cantidad")
            val input = EditText(applicationContext)
            input.width = 10
            input.inputType = InputType.TYPE_CLASS_NUMBER
            qtybuilder.setView(input)
            qtybuilder.setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }
            qtybuilder.setPositiveButton("Aceptar") { dialog, which ->
                if(input.text.isNullOrEmpty()){
                    Log.d("Damm","It null")
                }
                if(!input.text.isNullOrEmpty()){
                    val product: HashMap<Int, Int> = HashMap()
                    //Add to hashmap
                    val value = (input.text.toString().toInt() * model.multiple)
                    product[model.lineId] = value
                    thread {
                        try {
                            val deferredSendCount = sendCount(product)
                            model.totalQty = value.toString()
                            model.isCounted = true
                            val adap = countLv.adapter as CountAdapter
                            adap.notifyDataSetChanged()
                            runOnUiThread {
                                val customToast = CustomToast(applicationContext,this@CountActivity)
                                customToast.show("Enviado", 24.0F, Toast.LENGTH_LONG)
                                Log.d("Result of count", deferredSendCount)
                            }
                        }catch (e: Exception){
                            Log.d("Error",e.toString())
                        }
                    }
                }
                else{
                    runOnUiThread {
                        val customToast = CustomToast(applicationContext,this@CountActivity)
                        customToast.show("Ingrese una cantidad", 24.0F, Toast.LENGTH_LONG)
                        dialog.dismiss()
                    }
                }

            }
            qtybuilder.setNeutralButton("Reportar") { dialog, which ->
                val reportBuilder = AlertDialog.Builder(this)
                reportBuilder.setTitle("Producto - "+model.code)
                reportBuilder.setMessage("¿Reportar producto como no encontrado?")
                reportBuilder.setPositiveButton("Reportar") { dialog, which ->
                    thread {
                        try {
                            val reportHashMap : HashMap<Int,Int> = HashMap()
                            reportHashMap[model.lineId] = 0
                            val deferredSendCount = sendCount(reportHashMap)
                            runOnUiThread {
                                model.isReported = true
                                model.totalQty = "0"
                                val adapter = countLv.adapter as CountAdapter
                                adapter.notifyDataSetChanged()
                                val customToast = CustomToast(this, this)
                                customToast.show("Reportado", 24.0F, Toast.LENGTH_LONG)
                                Log.d("Result of count", deferredSendCount)
                            }
                        }catch (e: XmlRpcException){
                            runOnUiThread {
                                val customToast = CustomToast(this, this)
                                customToast.show("$e", 24.0F, Toast.LENGTH_LONG)
                            }
                        }
                        catch (c: Exception){
                            runOnUiThread {
                                val customToast = CustomToast(this, this)
                                customToast.show("$c", 24.0F, Toast.LENGTH_LONG)
                            }
                        }
                    }
                }
                reportBuilder.setNegativeButton("Cancelar") {dialog, _ ->
                    dialog.dismiss()
                }
                reportBuilder.show()
            }
            qtybuilder.show()
        }

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
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
        resync_fab.isClickable = false
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        if(db.deleteDataOnTable(OdooData.TABLE_INVENTORY_LINE)){
            thread {
                try {
                    val deferredInvoice: String = getInventoryLine()
                    Log.d("Inventory Line", deferredInvoice)
                    val invoicejson = JSONArray(deferredInvoice)
                    //Insert data
                    val invoiceUpdate = db.fillTable(invoicejson, OdooData.TABLE_INVENTORY_LINE)
                    if (invoiceUpdate) {
                        runOnUiThread {
                            //If succesfull, delete data from model, insert again and notify the dataset
                            swipeRefreshLayout.isRefreshing = false
                            datamodels.clear()
                            populateListView()
                            val adapter = countLv.adapter as CountAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Lista actualizada", 24.0F, Toast.LENGTH_LONG)
                        }
                    } else {
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            val customToast = CustomToast(this, this)
                            customToast.show("Sin Exito", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                    resync_fab.isClickable = true
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
                        customToast.show("Error de Red $xml", 24.0F, Toast.LENGTH_LONG)
                        swipeRefreshLayout.isRefreshing = false
                    }
                    Log.d("Error de Red",xml.toString())
                }
            }
        }
        else{
            val customToast = CustomToast(this, this)
            customToast.show("Error al cargar", 24.0F, Toast.LENGTH_LONG)
        }
    }

    //Fills the lines in the datamodel for inventory_line
    private fun populateListView()
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
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
            items.multiple = cursor.getInt(cursor.getColumnIndex("multiple"))


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
                val customToast = CustomToast(this, this)
                customToast.show("Error General: $e", 24.0F, Toast.LENGTH_LONG)
                swipeRefreshLayout.isRefreshing = false
            }
            Log.d("Error General",e.toString())
        }catch (xml: XmlRpcException){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error encontrando producto", 24.0F, Toast.LENGTH_LONG)
                swipeRefreshLayout.isRefreshing = false
            }
            Log.d("Error de Red",xml.toString())
        }

        var showMessage : Boolean = false

        for(j in 0 until countLv.adapter.count){
            val module = countLv.adapter.getItem(j) as CountDataModel
            module.isLocation = false
        }
        val adap2 = countLv.adapter as CountAdapter
        adap2.notifyDataSetChanged()

        for (i in 0 until countLv.adapter.count) {
            val item = countLv.adapter.getItem(i) as CountDataModel
            if (decodedString == item.location) {
                item.isLocation = true
                location = item.realLocation
                val adap = countLv.adapter as CountAdapter
                adap.notifyDataSetChanged()
                Log.d("Location", location)
                showMessage = true
            }
        }

        if(showMessage){
            Log.d("Showing Message","True")
            val customToast = CustomToast(this, this)
            customToast.show("Escanee el producto", 24.0F, Toast.LENGTH_LONG)
        }

        Log.d("You Scanned", decodedString)

        //Based on the scanned code check if it corresponds to a Route
        val db =
            DBConnect(this, OdooData.DBNAME, null, prefs.getInt("DBver",1))
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
                    try {
                                //Got qty
                                progressBar.isVisible = false
                                val product: HashMap<Int, Int> = HashMap()
                                //Add to hashmap
                                Log.d("Value", item.totalQty)
                                item.totalQty = (item.totalQty.toInt()+(1*item.multiple)).toString()
                                product[lineId] = item.totalQty.toInt()
                                val adap = countLv.adapter as CountAdapter
                                adap.notifyDataSetChanged()
                                thread {
                                    try {
                                        val deferredSendCount = sendCount(product)
                                        runOnUiThread {
                                            val customToast = CustomToast(applicationContext,this@CountActivity)
                                            customToast.show("Enviado", 24.0F, Toast.LENGTH_LONG)
                                            Log.d("Result of count", deferredSendCount)
                                        }
                                    }catch (e: Exception){
                                        Log.d("Error",e.toString())
                                    }
                                }
                    }catch (e: Exception){
                        Log.d("Error",e.toString())
                    }
                }
            }

        }
        else if (checkProduct.count == 0 && isCode){
            val builder = AlertDialog.Builder(this)
            val qtyInput = EditText(this)
            qtyInput.inputType = InputType.TYPE_CLASS_NUMBER
            qtyInput.hint = "Cantidad"
            builder.setView(qtyInput)
            // TODO: Add validation, when product exits but is not on count list, add option to upload it with qty (addCount)
            builder.setTitle("Producto no encontrado en lista")
            builder.setMessage("¿Añadir '$decodedString' a conteo?")
            builder.setPositiveButton("Añadir") { dialog, which ->
                thread {
                    val deferredAddCount = addCount(decodedString, qtyInput.text.toString().toInt())
                    if(deferredAddCount[0] as Boolean){
                        runOnUiThread {
                            val customToast = CustomToast(applicationContext, this@CountActivity)
                            customToast.show(deferredAddCount[1] as String, 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                    else{
                        runOnUiThread {
                            val customToast = CustomToast(applicationContext, this@CountActivity)
                            customToast.show(deferredAddCount[1] as String, 24.0F, Toast.LENGTH_LONG
                            )
                        }
                    }
                }
                /*
                val goBackintent = Intent(this, MainMenuActivity::class.java)
                finish()
                unregisterReceiver(mScanReceiver)
                startActivity(goBackintent)
                */
            }
            builder.setNegativeButton("Cancelar"){ dialog, which ->
                dialog.dismiss()
            }
            builder.show()
        }


        else if(!showMessage && !isCode){
            val customToast = CustomToast(this, this)
            customToast.show("Codigo $decodedString no es producto ni ubicación", 24.0F, Toast.LENGTH_LONG)
        }
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Salir")
        builder.setMessage("¿Desea salir del proceso?")
        builder.setPositiveButton("Si") { dialog, which ->
            super.onBackPressed()
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    //Function that conects to Odoo and sends the qty as a hashmap
    private fun sendCount(products : HashMap<Int, Int>): String{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.sendCount(products)
    }

    private fun addCount(productName : String, qty : Int): List<Any>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.addCount(productName,qty) as List<Any>
    }

    //Function that returns you the theoretical qty of a line
    private fun computeTheoreticalQty(id : Int): List<Any>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.computeTheoreticalQty(id)
    }

    //Returns the inventoryLines corresponding to that user
    private fun getInventoryLine() : String
    {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.getStockInventoryLine(prefs.getInt("activeUser",1))
    }

    private fun searchProduct(product_id : String): String {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.searchProductName(product_id)
    }

}
