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
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.example.morsaapp.adapter.StockBoxesAdapter
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.StockBoxesDataModel
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class RoutesOrdersActivity : AppCompatActivity() {

    //Views
    lateinit var boxesLv : ListView
    lateinit var boxesProgressBar : ProgressBar
    lateinit var finishRouteBtn :Button
    lateinit var routeNameTxt :TextView

    //Private Variables
    private var routeName : String? = ""
    private var routeId : String? = ""
    private var datamodels = ArrayList<StockBoxesDataModel>()

    //Shared Preferences
    lateinit var prefs: SharedPreferences

    //Decode Variables
    val SCAN_ACTION = ScanManager.ACTION_DECODE
    lateinit var mVibrator: Vibrator
    lateinit var mScanManager: ScanManager
    lateinit var soundPool: SoundPool
    var soundid : Int = 0
    lateinit var barcodeStr : String

    var pdfName : String = ""

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
        setContentView(R.layout.activity_routes_orders)

        //initScan()
        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.activity_intent_action))
        registerReceiver(mScanReceiver, filter)

        val file = File(applicationContext.filesDir,"examplePDF")
        val pdfAsBytes = Base64.decode(OdooData.examplePDF, 0)
        val fos = FileOutputStream(file)
        fos.write(pdfAsBytes)
        fos.flush()
        fos.close()

        Log.d("Directory",ContextCompat.getExternalFilesDirs(applicationContext, null).toString())

        prefs = this.getSharedPreferences("startupPreferences", 0)

        boxesLv = findViewById(R.id.boxes_lv)
        boxesProgressBar = findViewById(R.id.boxes_progressBar)
        finishRouteBtn = findViewById(R.id.finishRoute_btn)
        routeNameTxt = findViewById(R.id.routeName_txt)

        //Get intent data
        routeName = intent.getStringExtra("Route")
        routeId = intent.getStringExtra("RouteId")

        routeNameTxt.text = "Ruta $routeName"

        /**
         * First action after variable declaration is to get fill database and get all boxes according to the routeName
         */
        val dbReload =
            DBConnect(this, OdooData.DBNAME, null, prefs.getInt("DBver",1))

        if(dbReload.deleteDataOnTableFromField(OdooData.TABLE_STOCK_BOX,"route",routeName)){
            thread {
                try {
                    val stockBoxesData: String = syncStockBoxes(routeName)
                    val stockBoxesJson = JSONArray(stockBoxesData)
                    val result = dbReload.fillTable(stockBoxesJson, OdooData.TABLE_STOCK_BOX)
                    if (result) {
                        runOnUiThread {
                            boxesProgressBar.isVisible = false
                            datamodels.clear()
                            populateListView(routeName)
                            val adapter = boxesLv.adapter as StockBoxesAdapter
                            adapter.notifyDataSetChanged()
                            Log.d("Boxes", "Loaded")
                        }

                    } else {
                        runOnUiThread {
                            boxesProgressBar.isVisible = false
                            Log.d("Boxes", "Not Loaded")
                        }
                    }
                }catch (e: Exception){
                    Log.d("Error General",e.toString())
                }
            }
        }

        finishRouteBtn.setOnClickListener {
            //Check that all products are scanned
            var allScanned = true
            for(i in 0 until boxesLv.adapter.count){
                val item = boxesLv.adapter.getItem(i) as StockBoxesDataModel
                if (!item.isScanned){
                    allScanned = false
                    val customToast = CustomToast(this, this)
                    customToast.show("Escanee todos los productos", 24.0F, Toast.LENGTH_LONG)
                    break
                }
            }
            if(allScanned){
                Log.d("Can free route","True")
                thread {
                    try {
                        val deferredRouteInfo: List<Any> = getRouteBoxes("1", routeId)
                        Log.d("GetPDF result", deferredRouteInfo.toString())
                        if (!(deferredRouteInfo[0] as Boolean)) {
                            if (!(deferredRouteInfo[0] as Boolean)) {
                                runOnUiThread {
                                    val customToast = CustomToast(this, this)
                                    customToast.show("Cajas pendientes", 24.0F, Toast.LENGTH_LONG)
                                }
                            } else {
                                runOnUiThread {
                                    val customToast = CustomToast(this, this)
                                    customToast.show("No hay paquetes para mover a ruta", 24.0F, Toast.LENGTH_LONG)
                                }
                            }

                        } else {
                            //Save Document

                            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                            val currentDateandTime = sdf.format(Date())
                            val fileName = "$currentDateandTime.pdf"
                            pdfName = fileName
                            Log.d(
                                "Directory",
                                ContextCompat.getExternalFilesDirs(applicationContext, null)
                                    .toString()
                            )
                            val externalStorageVolumes: Array<out File> =
                                ContextCompat.getExternalFilesDirs(applicationContext, null)
                            val primaryExternalStorage = externalStorageVolumes[0]
                            val file = File(applicationContext.filesDir, fileName)
                            val pdfAsBytes = Base64.decode(deferredRouteInfo[1].toString(), 0)
                            Log.d("PDF", deferredRouteInfo[1].toString())
                            val fos = FileOutputStream(file)
                            fos.write(pdfAsBytes)
                            fos.flush()
                            fos.close()

                            //Send plaques
                            val sendPlaquesBuilder = AlertDialog.Builder(this)
                            sendPlaquesBuilder.setCancelable(false)
                            sendPlaquesBuilder.setTitle("Enviando Placas '$routeName'")
                            val input = EditText(this)
                            input.width = 10
                            input.inputType = InputType.TYPE_CLASS_TEXT
                            sendPlaquesBuilder.setView(input)
                            sendPlaquesBuilder.setMessage("Anote la placa del vehiculo")
                            sendPlaquesBuilder.setPositiveButton("Enviar") { dialog, which ->
                                thread {
                                    try {
                                        val deferredPlaques: List<Any> =
                                            sendPlates(routeId, input.text.toString())

                                        runOnUiThread {
                                            val customToast = CustomToast(this, this)
                                            customToast.show(deferredPlaques[1].toString() as String, 24.0F, Toast.LENGTH_LONG)
                                        }
                                        if ((deferredPlaques[0] as Boolean)) {
                                            val file1 = File(applicationContext.filesDir, pdfName)
                                            printPDF(file1.path)
                                        } else {
                                            runOnUiThread {
                                                dialog.dismiss()
                                                val customToast = CustomToast(this, this)
                                                customToast.show("No se pudieron enviar las placas, intente de nuevo", 24.0F, Toast.LENGTH_LONG)
                                            }
                                        }
                                    }catch (e: Exception){
                                        Log.d("Error",e.toString())
                                    }
                                }
                            }
                            runOnUiThread {
                                sendPlaquesBuilder.show()
                            }
                        }
                    }catch (e: Exception){
                        Log.d("Error General",e.toString())
                    }
                }
            }

        }
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

    private fun sendPlates(routeId: String?, plates : String): List<String>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.sendPlates(routeId,plates)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val returnIntent = Intent(applicationContext, RoutesActivity::class.java)
        finish()
        unregisterReceiver(mScanReceiver)
        startActivity(returnIntent)
        //unregisterReceiver(mScanReceiver) //This may stop the working of the scanner, shutdown in the meantime
    }

    private fun populateListView(route : String?)
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        val cursor = db.getStockBoxesFromRoute(route)
        var items : StockBoxesDataModel?

        while (cursor.moveToNext()) {
            //if(cursor.getColumnIndex("revision_qty") < cursor.getColumnIndex("product_qty")){ not working
            items = StockBoxesDataModel()

            items.id = cursor.getString(cursor.getColumnIndex("id")).toInt()
            items.route = cursor.getString(cursor.getColumnIndex("route"))
            items.invoices = cursor.getString(cursor.getColumnIndex("invoices"))
            items.box = cursor.getString(cursor.getColumnIndex("box"))

            datamodels.add(items)
            //}
        }
        obtainList()
        cursor.close()
        db.close()
    }

    private fun obtainList() {
        val adapter =
            StockBoxesAdapter(this, datamodels)
        boxesLv.adapter = adapter
    }

    fun syncStockBoxes(routeName : String?) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val stockPicking = odoo.getRouteBoxes(routeName)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

    private fun getRouteBoxes(sessionId: String, routeId: String?): List<String>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.getPdf(sessionId, routeId)
    }

    private fun scanBox(boxId: String): List<String>{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.scanBox(boxId)
    }

    private fun displayScanResult(decodedString : String, decodedType : String) {
        Log.d("Stock Box - You scanned", decodedString)

        //Using the routeName and the scanned Id, look for boxes with same parameters, when all boxes are scanned allow to free route
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        val matchingBoxes = db.getStockBox(decodedString,routeName)
        //If box found, get Id and change line color
        if(matchingBoxes.count > 0){
            Log.d("Found Matches","True")
            while (matchingBoxes.moveToNext()){
                val boxId = matchingBoxes.getString(matchingBoxes.getColumnIndex("id"))
                Log.d("Match Id", boxId)
                for(i in 0 until boxesLv.adapter.count){
                    val item = boxesLv.adapter.getItem(i) as StockBoxesDataModel
                    if (boxId.toInt() == item.id){
                        Log.d("In list, change color","True")
                        item.isScanned = true
                        thread {
                            try {
                                val result = scanBox(item.id.toString())
                                Log.d("Result of box scan", result.toString())
                            }catch (e: Exception){
                                Log.d("Error", e.toString())
                            }
                        }
                    }
                    val adapter = boxesLv.adapter as StockBoxesAdapter
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

}