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
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.morsaapp.adapter.RoutesAdapter
import com.example.morsaapp.datamodel.RoutesDataModel
import kotlinx.android.synthetic.main.activity_pdf.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import java.lang.Exception
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class RoutesActivity : AppCompatActivity() {

    private var datamodels = ArrayList<RoutesDataModel>()
    private lateinit var adapter : RoutesAdapter
    lateinit var routesLv : ListView
    lateinit var sessionId : String
    //Route Data
    var routeName : String = ""
    lateinit var routeId : String
    lateinit var  scannedRouteIdSearch : String
    lateinit var alert : AlertDialog
    //Box counter
    var totalBoxes : Int = 0
    var currentBoxes : Int = 0

    private lateinit var popupListViewGlobal:ListView
    lateinit var popupWindow : PopupWindow
    lateinit var popupViewGlobal : View

    //Decode Variables
    val SCAN_ACTION = ScanManager.ACTION_DECODE
    lateinit var mVibrator: Vibrator
    lateinit var mScanManager: ScanManager
    lateinit var soundPool: SoundPool
    var soundid : Int = 0
    lateinit var barcodeStr : String

    var pdfName : String = ""

    lateinit var prefs: SharedPreferences

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
        setContentView(R.layout.activity_routes)
        val toolbar : Toolbar = findViewById(R.id.routes_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Rutas"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        initScan()
        /*
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.activity_intent_filter_action_route))
        registerReceiver(myBroadcastReceiver, filter)
        */

        routesLv = findViewById(R.id.routes_lv)

        routesLv.setOnItemClickListener { parent, view, position, id ->
            /*
            val int|ent = Intent(this, PdfActivity::class.java)
            val model : RoutesDataModel = routesLv.getItemAtPosition(position) as RoutesDataModel
            val ruta = model.routeName
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Liberar Ruta '$ruta'?")
            builder.setMessage("Num de Cajas: "+1+"(Temp)")
            builder.setPositiveButton("SI") { dialog, which ->

                /*
                val url = "http://148.72.171.110:8011/web/session/authenticate"
                val queue = Volley.newRequestQueue(this)
                val rawBody = "{\"jsonrpc\": \"2.0\",\"params\": {\"db\": \"ERP_MORSA\",\"login\": \"admin\",\"password\": \"1411\"}}"
                val body = JSONObject(rawBody)
                val stringRequest = JsonObjectRequest(Request.Method.POST, url, body, Response.Listener<JSONObject> { response ->
                    val result  = JSONObject(response.get("result").toString())
                    sessionId = result.get("session_id").toString()
                    intent.putExtra("SessionId", sessionId)
                    intent.putExtra("RouteId", model.routeId)
                    startActivity(intent)
                }, Response.ErrorListener { error: VolleyError? ->
                    Log.d("Error Volley", error.toString())
                })
                queue.add(stringRequest)
                */

            }
            builder.setNegativeButton("NO"){dialog, which ->
                dialog.dismiss()
            }
            builder.show()
             */
        }

        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        if(db.deleteDataOnTable(Utilities.TABLE_ROUTES)){
            thread {
                try {
                    val deferredRoutes: String = syncRoutes()
                    Log.d("Inventory Line", deferredRoutes)
                    val routesjson = JSONArray(deferredRoutes)
                    //Insert data
                    val routesUpdate = db.fillTable(routesjson, Utilities.TABLE_ROUTES)
                    if (routesUpdate) {
                        runOnUiThread {
                            //If succesfull, delete data from model, insert again and notify the dataset
                            datamodels.clear()
                            populateListView()
                            val adapter = routesLv.adapter as RoutesAdapter
                            adapter.notifyDataSetChanged()
                            Toast.makeText(
                                applicationContext,
                                "Escanee una Ruta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        runOnUiThread {
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
                    }
                    Log.d("Error General",e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Error de Red: $xml",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.d("Error de Red",xml.toString())
                }
            }
        }
        else{
            Toast.makeText(applicationContext,"Error al cargar",Toast.LENGTH_SHORT).show()
        }

    }

    fun syncRoutes() : String{
        val odoo = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odoo.authenticateOdoo()
        val invoice = odoo.routes
        Log.d("OrderList", invoice)
        return invoice
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val goBackintent = Intent(this, MainMenuActivity::class.java)
        startActivity(goBackintent)
    }

    /*
    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == resources.getString(R.string.activity_intent_filter_action_route)) {
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

    private fun populateListView()
    {
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val cursor = db.routes

        var orders : RoutesDataModel?

        Log.d("Routes Qty", cursor.count.toString())
        while(cursor.moveToNext()){
            orders = RoutesDataModel()
            orders.routeId = cursor.getString(0)
            orders.setRouteName(cursor.getString(1))
            datamodels.add(orders)
        }

        obtainList()
    }

    private fun obtainList() {
        adapter = RoutesAdapter(this, datamodels)
        routesLv.adapter = adapter
    }

    private fun displayScanResult(decodedData : String) {

        //val decodedSource = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_source))
        //val decodedData = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_data))
        //val decodedLabelType = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_label_type))

        /*
        val path = Uri.parse(dwldsPath.absolutePath)
        val intent = Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent)
         */

        /*
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, 2)
        */

        /*
        val pdf = Intent(this, PdfActivity::class.java)
        pdf.putExtra("File", fileName)
        startActivity(pdf)
        */

        Log.d("You Scanned", decodedData.toString())
        val builder = AlertDialog.Builder(this)

        //Based on the scanned code check if it corresponds to a Route
        val db = DBConnect(this, Utilities.DBNAME, null, 1)
        //Check for routes with that name
        val getRoute = db.getRoute(decodedData)

        //Look for boxes related to route

        if(getRoute.count>0){
            while (getRoute.moveToNext()){
                routeName = getRoute.getString(1)
                routeId = getRoute.getString(0)
            }
        }

        Log.d("Route Name", routeName)
        try {
            val deferredStockBoxes: Deferred<String> =
                GlobalScope.async { getStockBoxes(routeName) }
            runBlocking {
                //Check if Route has boxes
                Log.d("Stock Boxes from Route", deferredStockBoxes.await().toString())
                if (deferredStockBoxes.await().equals("[]")) {
                    Toast.makeText(applicationContext, "Ruta no tiene cajas", Toast.LENGTH_LONG)
                        .show()
                }
                //If not show list in new activity
                else {
                    val intent = Intent(applicationContext, RoutesOrdersActivity::class.java)
                    intent.putExtra("Route", routeName)
                    intent.putExtra("RouteId", routeId)
                    startActivity(intent)
                    finish()
                }
            }
        }catch (e: Exception){
            Log.d("Error",e.toString())
        }
    }

    fun openPDF(fileName : String){
        val file = getFileStreamPath(fileName)

        pdf_viewer.fromFile(file)
            .enableSwipe(true)
            .swipeHorizontal(true)
            .onError { t ->
                Toast.makeText(applicationContext, "Error al abrir documento", Toast.LENGTH_LONG).show()
                Log.d("Error", t.toString())
            }
            .enableAntialiasing(true)
            .defaultPage(0)
            .spacing(0)
            .load()

    }

    fun printPDF(filePath : String){
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        try{
            val printDocumentAdapter = PdfDocumentAdapter(this,filePath)
            printManager.print("Document", printDocumentAdapter, PrintAttributes.Builder().build())
        }catch ( e : Exception){
            Log.e("ERROR", e.message)
        }
    }

    private fun getRouteBoxes(sessionId: String, routeId: String): List<String>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.getPdf(sessionId, routeId)
    }

    private fun getStockBoxes(route: String): String{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.getRouteBoxes(route)
    }

    private fun sendPlates(routeId: String, plates : String): List<String>{
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.sendPlates(routeId,plates)
    }

}
