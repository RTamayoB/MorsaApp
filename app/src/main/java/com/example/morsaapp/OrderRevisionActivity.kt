package com.example.morsaapp

import android.annotation.SuppressLint
import android.content.*
import android.device.ScanManager
import android.device.scanner.configuration.PropertyID
import android.device.scanner.configuration.Triggering
import android.media.AudioManager
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.example.morsaapp.adapter.IssuesPopupAdapter
import com.example.morsaapp.adapter.OrderRevisionAdapter
import com.example.morsaapp.adapter.ScanIssuesAdapter
import com.example.morsaapp.datamodel.IssuesPopupDataModel
import com.example.morsaapp.datamodel.OrderRevisionDataModel
import com.example.morsaapp.datamodel.ScanIssuesDataModel
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main_menu.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import java.io.*
import java.lang.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread


class OrderRevisionActivity : AppCompatActivity(), Definable {

    lateinit var prefs : SharedPreferences

    private fun serialize(o : Serializable) : String{ //<Int, ArrayList<IssuesPopupDataMOdel>>
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(o)
        oos.close()
        return org.apache.ws.commons.util.Base64.encode(baos.toByteArray())
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Salir")
        builder.setMessage("Interrumpir proceso de Inspeccion")
        builder.setNegativeButton("Cancelar") {dialog, which ->
            dialog.dismiss()
        }
        builder.setPositiveButton("Aceptar") {dialog, which ->
            super.onBackPressed()
            val intent = Intent(applicationContext, RevisionActivity::class.java)
            startActivity(intent)
            unregisterReceiver(mScanReceiver)
            finish()
        }
        builder.show()
    }

    private fun deserialize(s : String) : HashMap<Int, ArrayList<IssuesPopupDataModel>>{
        val data  = org.apache.ws.commons.util.Base64.decode(s)
        val ois = ObjectInputStream(ByteArrayInputStream(data))
        val o = ois.readObject()
        ois.close()
        return o as HashMap<Int, ArrayList<IssuesPopupDataModel>>
    }


    private fun saveCurrentIssues(issues : String) {
        val db = DBConnect(this,Utilities.DBNAME, null, 1)
        val userId = 0
//        Log.d("picking_id", pickingId)
        db.saveIssuesToDB(issues, userId, pickingId.toInt())
        db.close()
    }

    private var activeOrderLinePosition : Int = 0
    private lateinit var popupListViewGlobal:ListView
    lateinit var popupViewGlobal : View
    lateinit var scanIssuesLv :ListView
    private lateinit var orderRevisionLv : ListView
    private var hashMapIssuesPopupAdapter : HashMap<Int, ArrayList<IssuesPopupDataModel>> = HashMap()
    private lateinit var relatedId : String
    private var returnID : Boolean = false
    lateinit var scanPopupWindow : PopupWindow
    lateinit var selectPopupWindow : PopupWindow
     var activeModeId : Int = 0
    var maxNumber = 0

    lateinit var finalHashMap : HashMap<Int,
            HashMap<Int,
                    HashMap<String,Any>
                    >
            >

    lateinit var moveChosedHashMap : HashMap<Int, HashMap<String, Any>>
    lateinit var moveDataHashMap : HashMap<String, Any>

    //Decode Variables
    val SCAN_ACTION = ScanManager.ACTION_DECODE
    lateinit var mVibrator: Vibrator
    lateinit var mScanManager: ScanManager
    lateinit var soundPool: SoundPool
    var soundid : Int = 0
    //lateinit var barcodeStr : String

    val mScanReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if(action == resources.getString(R.string.activity_intent_action)){
                //soundPool.play(soundid, 1.0f, 1.0f, 0, 0, 1.0f)
                //mVibrator.vibrate(100)

                //val barcode  = intent!!.getByteArrayExtra(ScanManager.DECODE_DATA_TAG)
                //val barcodelen = intent?.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0)
                //val temp = intent.getByteExtra(ScanManager.BARCODE_TYPE_TAG, 0.toByte())
                //Log.i("debug", "----codetype--$temp")
                //barcodeStr = String(barcode, 0, barcodelen)
                //Log.d("Result", barcodeStr)
                    val value = intent.getStringExtra("barcode_string")
                    Log.d("INTENT VALUE", value)
                displayScanResult(value, "")
                //mScanManager.stopDecode()
            }
        }
    }

    private fun initScan() {
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mScanManager = ScanManager()
        mScanManager.openScanner()
        mScanManager.triggerMode = Triggering.HOST

        mScanManager.switchOutputMode(0)
        soundPool = SoundPool(1, AudioManager.STREAM_NOTIFICATION, 100) // MODE_RINGTONE
        soundid = soundPool.load("/etc/Scan_new.ogg", 1)

        //Set IntentFilter
        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.activity_intent_action))
        registerReceiver(mScanReceiver, filter)
    }

    /*Unitech piece
    private val myBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (action == resources.getString(R.string.activity_intent_filter_action_order)) {
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

    override fun setNumber(newNum : Int, item: IssuesPopupDataModel) {
    }


    override fun showPopup(value: Int, moveId: Int, Name: String) {
        this.activeModeId = moveId
        var correctedValue=value
        maxNumber = correctedValue
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView : View = inflater.inflate(R.layout.issues_popup, null)

        val width : Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val height : Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true
        //create the popup window
        selectPopupWindow = PopupWindow(popupView, width, height, focusable)
        this.popupViewGlobal =  popupView
        val productName = popupView.findViewById<TextView>(R.id.productName_lbl)
        productName.text = Name
        val confirmBtn = popupView.findViewById<Button>(R.id.confirm_incidencies)
        confirmBtn.setOnClickListener {
            selectPopupWindow.dismiss()
            /**
             * Tells me the result of the incidencies
             */
            Log.d("Qty and Issues", finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("qty").toString()+" - "
                    +finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues").toString())
        }
        val number = popupView.findViewById<TextView>(R.id.incid_txt)

        number.text = getTotalIssuesFromMove(activeModeId).toString()
        /**
         * Hardcoding the Number of incidencies
         */
        number.text = finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("qty").toString()
        this.popupListViewGlobal = popupView.findViewById(R.id.incidencies_popup_lv)
        populateIssuesListView(returnID)

        selectPopupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

    }

    fun getTotalIssuesFromMove(moveId : Int): Int{
        val issues = finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues") ?: return 0
        var totalIssues = 0
        for (issue in issues as ArrayList<ArrayList<Int>>){
            totalIssues += issue[1]
            Log.d("Total Issues", totalIssues.toString())

        }
        return totalIssues
    }

    override fun alterCount(item: IssuesPopupDataModel, position:Int, increase:Boolean) {
        val tvIssues=this.popupViewGlobal.findViewById<TextView>(R.id.incid_txt)
        val adapterIssues: IssuesPopupAdapter = popupListViewGlobal.adapter as IssuesPopupAdapter
        val number=tvIssues.text.toString().toInt()
        val issueChosen = item.incid_id

        val currentQty = finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("qty")
        var totalIssues = 0
        var workingList = arrayListOf<Int>()
        //Test
        if(finalHashMap.get(pickingId.toInt()) == null){
            Log.d("It was null", "Picking is null")
        }
        if(finalHashMap[pickingId.toInt()]?.get(activeModeId) == null){
            Log.d("It was null", "Mode id is null")
        }
        if (currentQty == null){
            Log.d("It was null", finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues").toString())
            return
        }
        else {
            /* CHANGE */
            val issues : ArrayList<ArrayList<Int>>
            if(finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues") == null){
                //finalHashMap[pickingId.toInt()]?.put(activeModeId, hashMapOf("issues" to arrayListOf(arrayListOf(0, 1))))
                Log.d("Null", finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues").toString())
                return
            }
            else{
                issues = finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues") as ArrayList<ArrayList<Int>>
            }
            /*CHANGE*/
            Log.d("HashMap Before Sum/Rest", finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues").toString())
            var exists = false
            for ((key,issue) in  issues.withIndex()) {
                if (issue[0] == issueChosen) {
                    exists = true
                    workingList = issues[key]
                    totalIssues = getTotalIssuesFromMove(activeModeId)

                }
            }
            if (!exists){
                workingList = arrayListOf(issueChosen,0)
                issues.add(workingList)
            }
        }
        if (increase) {
            if (totalIssues < currentQty as Int) {
                item.number++
                workingList[1] = item.number

                Log.d("HashMap After Sum", finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues").toString())
            }
        } else {
            if (totalIssues > 0 && (item.number > 0)) {
                item.number--
                workingList[1] = item.number


                Log.d("HashMap After Rest", finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues").toString())
            }
        }
        adapterIssues.notifyDataSetChanged()

//        val popupGeneralNumber=tvIssues.text.toString().toInt()
//        Log.d("general number",popupGeneralNumber.toString())
//        Log.d("increase",increase.toString())
//
//        val arrayListToSet = adapterIssues.dataSet
//        val setincid = adapterIssues.getItem(position) as IssuesPopupDataModel
//
//        if(tvIssues!=null) {
//            if (increase) {
//                if(!(item.number==0 || ((popupGeneralNumber+1)>popUpMaxNumber))){
//                    item.number--
//                    value = 1
//                }
//
//            } else {
//                if((popupGeneralNumber-1) >= 0){
//                    item.number++
//                    value = -1
//                }
//            }
//            if(value == 0)
//                return
//            setincid.incidHashMap[setincid.incid_id] = item.number
//            tvIssues.text = (number+value).toString()
//            arrayListToSet[position] = item
////           adapter.setData(array_list_to_set)
//            Log.d("Item Number", item.number.toString())
//            adapterIssues.notifyDataSetChanged()
//
//        }else{
//            Log.d("funcionando","es nulo")
//        }
    }

    private var datamodels = ArrayList<OrderRevisionDataModel>()
    private var incidDataModel = ArrayList<IssuesPopupDataModel>()
    private var scanIssuesDataModel = ArrayList<ScanIssuesDataModel>()
    lateinit var pickingId : String
    var labelCount : Int = 0
    var markedAsExcedent : Boolean = false

    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_revision)
        setSupportActionBar(findViewById(R.id.order_revision_tb))
        supportActionBar?.title = "Revisión de Orden"

        prefs = this.getSharedPreferences("startupPreferences", 0)
        progressBar = findViewById(R.id.progressBar_revision)

        /*
        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.activity_intent_filter_action_order))
        registerReceiver(myBroadcastReceiver, filter)
        */

        //initScan()

        val intent : Intent = intent
        pickingId = intent.getStringExtra("ID")
        val name = intent.getStringExtra("Name")
        val correctName = name?.replace("/", "\\/")
        relatedId = "[$pickingId,\"$correctName\"]"
        Log.d("Concatenated Id",relatedId)
        returnID = intent.getBooleanExtra("ReturnId",false)
        Log.d("ReturnId", returnID.toString())

        val orderTxt = findViewById<TextView>(R.id.order_lbl)
        orderTxt.text = "Orden: $name"

        var hashMapRaw = ""
        val db = DBConnect(this, Utilities.DBNAME, null, 1).writableDatabase
        val cursor = db.rawQuery("SELECT issues FROM "+Utilities.TABLE_ISSUES_LIST+" WHERE picking_id = "+pickingId, null)
        if(cursor.count > 0){
            while (cursor.moveToNext()) {
                hashMapRaw = cursor.getString(0)
            }
            val hashMapDeserialized = deserialize(hashMapRaw)
            hashMapIssuesPopupAdapter = hashMapDeserialized
        }

        orderRevisionLv = findViewById(R.id.order_revision_lv)

        finalHashMap= HashMap()
        finalHashMap[pickingId.toInt()] = HashMap()
        moveDataHashMap = HashMap()
        moveChosedHashMap = HashMap()

        Log.d("Return Id", returnID.toString())
        /**
         * Finish inspection and send products once they are inspected
         */

        val confirmIssuesBtn = findViewById<Button>(R.id.confirm_revision_btn)
        confirmIssuesBtn.setOnClickListener {
            var pendingNum = 0
            var allChecked = false
            var pedido: OrderRevisionDataModel
            for(i in 0 until orderRevisionLv.adapter.count) {
                pedido = orderRevisionLv.adapter.getItem(i) as OrderRevisionDataModel
                Log.d("Product ${pedido.productName}","RevQty - ${pedido.revisionQty}, Qty - ${pedido.qty}")
                if(pedido.revisionQty < pedido.qty){
                    pendingNum ++
                }
            }

            Log.d("Pending Num",pendingNum.toString())
            if(pendingNum > 0){
                val pendingbuilder = AlertDialog.Builder(this)
                pendingbuilder.setTitle("Productos pendientes")
                    .setMessage("Tiene $pendingNum de productos pendientes por inspeccionar. ¿Son faltantes?")
                    .setPositiveButton("Son faltantes") { dialog, which ->
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Aceptar Inspeccion")
                            .setMessage("¿Confirmar la Inspeccion de la Orden y sus Incidencias?")
                            .setPositiveButton("Confirmar") { dialog, _ ->
                                allChecked = true
                                try {


                                    dialog.dismiss()
                                    /*
                                   for(i in 0 until orderRevisionLv.count){
                                       val model = orderRevisionLv.getItemAtPosition(i) as OrderRevisionDataModel
                                       if(model.revisionQty < model.qty){
                                           allCheked = false
                                       }
                                   }
                                   */
                                    if (!allChecked) {
                                        val customToast = CustomToast(this, this)
                                        customToast.show("Confirma todas la incidencias", 24.0F, Toast.LENGTH_LONG)
                                    } else {
                                        var error = false
                                        //val issuesToSend: HashMap<Int, HashMap<Int, Int>> = parseIssues()
//                             Log.d("HashMap", issuesToSend.toString())
                                        val toSend = finalHashMap[pickingId.toInt()]
                                        thread {
                                            try {
                                                val sendIssues: List<List<String>> = confirmIssues(
                                                    pickingId.toInt(),
                                                    toSend as HashMap<Int, HashMap<String, Any>>
                                                )
                                                if (sendIssues.isEmpty()) {
                                                    runOnUiThread {
                                                        val customToast = CustomToast(this, this)
                                                        customToast.show("Is Empty", 24.0F, Toast.LENGTH_LONG)
                                                    }
                                                } else {
                                                    runOnUiThread {
                                                        val customToast = CustomToast(this, this)
                                                        customToast.show("Confirmado", 24.0F, Toast.LENGTH_LONG)
                                                    }
                                                    error = false
                                                }
                                                val answer: String = movesTest("0", pickingId.toInt())
                                                Log.d("Send Issues", sendIssues.toString())
                                                Log.d("Move Test", answer.toString())
                                            } catch (e: Exception) {
                                                Log.d("Error", e.toString())
                                                runOnUiThread {
                                                    val customToast = CustomToast(this, this)
                                                    customToast.show(e.toString(), 24.0F, Toast.LENGTH_LONG)
                                                }
                                                error = true
                                            }
                                            Log.d("Id", pickingId)
                                            Log.d("ReturnId", returnID.toString())
                                            val finishProcess =
                                                Intent(applicationContext, MainMenuActivity::class.java)
                                            if (!error) {
                                                if (returnID) {
                                                    Log.d("Has return id", "true")
                                                    var rawReturnId = ""
                                                    val dbReturns = DBConnect(
                                                        applicationContext,
                                                        Utilities.DBNAME,
                                                        null,
                                                        1
                                                    ).readableDatabase
                                                    val cursorReturns = dbReturns.rawQuery(
                                                        "SELECT return_id FROM " + Utilities.TABLE_STOCK + " WHERE id = " + pickingId.toInt(),
                                                        null
                                                    )
                                                    while (cursorReturns.moveToNext()) {
                                                        rawReturnId = cursorReturns.getString(0)
                                                    }
                                                    cursorReturns.close()
                                                    val realReturnId = rawReturnId.substring(
                                                        rawReturnId.indexOf("[") + 1,
                                                        rawReturnId.indexOf(",")
                                                    )
                                                    Log.d("Real Return Id", realReturnId)
                                                    val builder2 = AlertDialog.Builder(this)
                                                    builder2.setTitle("Proceso de Devolucion")
                                                        .setMessage("Aceptar o rechazar la devolucion:")
                                                        .setPositiveButton("Aceptar") { dialog2, which ->
                                                            try {
                                                                val actionClose =
                                                                    GlobalScope.async { actionClose(realReturnId.toInt()) }
                                                                runBlocking {
                                                                    Log.d(
                                                                        "Action Close Result",
                                                                        actionClose.await()
                                                                    )
                                                                }
                                                                finishProcess.putExtra(
                                                                    "pickingId",
                                                                    pickingId.toInt()
                                                                )
                                                                startActivity(finishProcess)
                                                                unregisterReceiver(mScanReceiver)
                                                                finish()
                                                            } catch (e: Exception) {
                                                                Log.d("Error", e.toString())
                                                            }
                                                        }
                                                        .setNegativeButton("Rechazar") { dialog2, which ->
                                                            try {
                                                                val actionRejected = GlobalScope.async {
                                                                    actionRejected(realReturnId.toInt())
                                                                }
                                                                runBlocking {
                                                                    Log.d(
                                                                        "Action Close Result",
                                                                        actionRejected.await()
                                                                    )
                                                                }
                                                                finishProcess.putExtra(
                                                                    "pickingId",
                                                                    pickingId.toInt()
                                                                )
                                                                startActivity(finishProcess)
                                                                unregisterReceiver(mScanReceiver)
                                                                finish()
                                                            } catch (e: Exception) {
                                                                Log.d("Error", e.toString())
                                                            }
                                                        }
                                                    builder2.show()
                                                } else {
                                                    Log.d("Has return id", "true")
                                                    finishProcess.putExtra("pickingId", pickingId.toInt())
                                                    startActivity(finishProcess)
                                                    unregisterReceiver(mScanReceiver)
                                                    finish()
                                                }
                                            }
                                        }

                                    }
                                } catch (e: XmlRpcException) {
                                    Log.d("XMLRPC ERROR", e.toString())
                                    val customToast = CustomToast(this, this)
                                    customToast.show("Error en Odoo: $e", 24.0F, Toast.LENGTH_LONG)
                                } catch (e: Exception) {
                                    Log.d("ERROR", e.toString())
                                    val customToast = CustomToast(this, this)
                                    customToast.show("Error en Peticion: $e", 24.0F, Toast.LENGTH_LONG)
                                }
                            }
                            .setNegativeButton("Cancelar") { dialog, which ->
                                allChecked = false
                                dialog.dismiss()
                            }
                        builder.show()
                    }
                    .setNegativeButton("No son faltantes") { dialog, which ->
                        allChecked = false
                        dialog.dismiss()
                    }
                pendingbuilder.show()
            }
            else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Aceptar Inspeccion")
                    .setMessage("¿Confirmar la Inspeccion de la Orden y sus Incidencias?")
                    .setPositiveButton("Confirmar") { dialog, _ ->
                        allChecked = true
                        try {


                            dialog.dismiss()
                            /*
                           for(i in 0 until orderRevisionLv.count){
                               val model = orderRevisionLv.getItemAtPosition(i) as OrderRevisionDataModel
                               if(model.revisionQty < model.qty){
                                   allCheked = false
                               }
                           }
                           */
                            if (!allChecked) {
                                val customToast = CustomToast(this, this)
                                customToast.show("Confirma todas las incidencias", 24.0F, Toast.LENGTH_LONG)
                            } else {
                                var error = false
                                //val issuesToSend: HashMap<Int, HashMap<Int, Int>> = parseIssues()
//                             Log.d("HashMap", issuesToSend.toString())
                                val toSend = finalHashMap[pickingId.toInt()]
                                thread {
                                    try {
                                        val sendIssues: List<List<String>> = confirmIssues(
                                            pickingId.toInt(),
                                            toSend as HashMap<Int, HashMap<String, Any>>
                                        )
                                        if (sendIssues.isEmpty()) {
                                            runOnUiThread {
                                                val customToast = CustomToast(this, this)
                                                customToast.show("Is Empty", 24.0F, Toast.LENGTH_LONG)
                                            }
                                        } else {
                                            runOnUiThread {
                                                val customToast = CustomToast(this, this)
                                                customToast.show("Confirmado", 24.0F, Toast.LENGTH_LONG)
                                                error = false
                                            }
                                        }
                                        val answer: String = movesTest("0", pickingId.toInt())
                                        Log.d("Send Issues", sendIssues.toString())
                                        Log.d("Move Test", answer.toString())
                                    } catch (e: Exception) {
                                        Log.d("Error", e.toString())
                                        runOnUiThread {
                                            val customToast = CustomToast(this, this)
                                            customToast.show(e.toString(), 24.0F, Toast.LENGTH_LONG)
                                        }
                                        error = true
                                    }
                                    Log.d("Id", pickingId)
                                    Log.d("ReturnId", returnID.toString())
                                    val finishProcess =
                                        Intent(applicationContext, MainMenuActivity::class.java)
                                    if (!error) {
                                        if (returnID) {
                                            Log.d("Has return id", "true")
                                            var rawReturnId = ""
                                            val dbReturns = DBConnect(
                                                applicationContext,
                                                Utilities.DBNAME,
                                                null,
                                                1
                                            ).readableDatabase
                                            val cursorReturns = dbReturns.rawQuery(
                                                "SELECT return_id FROM " + Utilities.TABLE_STOCK + " WHERE id = " + pickingId.toInt(),
                                                null
                                            )
                                            while (cursorReturns.moveToNext()) {
                                                rawReturnId = cursorReturns.getString(0)
                                            }
                                            cursorReturns.close()
                                            val realReturnId = rawReturnId.substring(
                                                rawReturnId.indexOf("[") + 1,
                                                rawReturnId.indexOf(",")
                                            )
                                            Log.d("Real Return Id", realReturnId)
                                            val builder2 = AlertDialog.Builder(this)
                                            builder2.setTitle("Proceso de Devolucion")
                                                .setMessage("Aceptar o rechazar la devolucion:")
                                                .setPositiveButton("Aceptar") { dialog2, which ->
                                                    try {
                                                        val actionClose =
                                                            GlobalScope.async { actionClose(realReturnId.toInt()) }
                                                        runBlocking {
                                                            Log.d(
                                                                "Action Close Result",
                                                                actionClose.await()
                                                            )
                                                        }
                                                        finishProcess.putExtra(
                                                            "pickingId",
                                                            pickingId.toInt()
                                                        )
                                                        startActivity(finishProcess)
                                                        unregisterReceiver(mScanReceiver)
                                                        finish()
                                                    } catch (e: Exception) {
                                                        Log.d("Error", e.toString())
                                                    }
                                                }
                                                .setNegativeButton("Rechazar") { dialog2, which ->
                                                    try {
                                                        val actionRejected = GlobalScope.async {
                                                            actionRejected(realReturnId.toInt())
                                                        }
                                                        runBlocking {
                                                            Log.d(
                                                                "Action Close Result",
                                                                actionRejected.await()
                                                            )
                                                        }
                                                        finishProcess.putExtra(
                                                            "pickingId",
                                                            pickingId.toInt()
                                                        )
                                                        startActivity(finishProcess)
                                                        unregisterReceiver(mScanReceiver)
                                                        finish()
                                                    } catch (e: Exception) {
                                                        Log.d("Error", e.toString())
                                                    }
                                                }
                                            builder2.show()
                                        } else {
                                            Log.d("Has return id", "true")
                                            finishProcess.putExtra("pickingId", pickingId.toInt())
                                            startActivity(finishProcess)
                                            unregisterReceiver(mScanReceiver)
                                            finish()
                                        }
                                    }
                                }
                            }
                        } catch (e: XmlRpcException) {
                            Log.d("XMLRPC ERROR", e.toString())
                            val customToast = CustomToast(this, this)
                            customToast.show("Error en Odoo: $e", 24.0F, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            Log.d("ERROR", e.toString())
                            val customToast = CustomToast(this, this)
                            customToast.show("Error en Peticion: $e", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                if (!isFinishing) {
                    builder.show()
                }
            }
        }

        val dbReload = DBConnect(this, Utilities.DBNAME, null, 1)
        /**
         * Reload the issues table
         */
        if(dbReload.deleteDataOnTable(Utilities.TABLE_STOCK_ISSUES)){
            thread {
                try {
                    val deferredIssuesList : String = getStockMoveIssue()
                    val issuesJson = JSONArray(deferredIssuesList)
                    val issuesUpdate = dbReload.fillTable(issuesJson, Utilities.TABLE_STOCK_ISSUES)
                    if(issuesUpdate){
                    Log.d("Issues Table", "Updated")
                }
                }catch (e: Exception){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error General", 24.0F, Toast.LENGTH_LONG)
                    }
                Log.d("Error General",e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error de Red: $xml", 24.0F, Toast.LENGTH_LONG)
                    }
                    Log.d("Error de Red",xml.toString())
                }
            }

        }


        /**
         * Delete the stock.move lines that have that related Id, download them again (so they are the most updated)
         * and show the lines on the ListView
         */

        if(dbReload.reloadInspectionLines(relatedId)){
            thread {
                val deferredStockItemsReSync: String =  syncInspectionItems(pickingId.toInt())
                    val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
                    val stockItemJson = JSONArray(deferredStockItemsReSync)
                    val result = db.fillTable(stockItemJson, Utilities.TABLE_STOCK_ITEMS)
                    if (result) {
                        runOnUiThread {
                            progressBar.isVisible = false
                            datamodels.clear()
                            populateListView(relatedId)
                            val adapter = orderRevisionLv.adapter as OrderRevisionAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Exito", 24.0F, Toast.LENGTH_LONG)
                        }

                    } else
                        runOnUiThread {
                            progressBar.isVisible = false
                            val customToast = CustomToast(this, this)
                            customToast.show("Sin Exito", 24.0F, Toast.LENGTH_LONG)
                        }
            }
        }

        val filter = IntentFilter()
        filter.addAction("android.intent.ACTION_DECODE_DATA")
        registerReceiver(mScanReceiver, filter)
    }

    fun syncInspectionItems(pickingId: Int) : String{
        val odoo = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odoo.authenticateOdoo()
        val stockPicking = odoo.getInspectionItems(pickingId)
        Log.d("OrderList", stockPicking)
        return stockPicking
    }

    @SuppressLint("UseSparseArrays")
//    private fun parseIssues(): HashMap<Int, HashMap<Int, Int>> {
//            val hashMapToReturn = HashMap<Int,HashMap<Int, Int>>()
//
//            for((key,value) in hashMapIssuesPopupAdapter){
//                val dataModelIte = value.iterator()
//                val issuesHashMap = HashMap<Int, Int>()
//                while (dataModelIte.hasNext()){
//                    val dataModel = dataModelIte.next()
//                    issuesHashMap[dataModel.incid_id] = dataModel.number
//                }
//                hashMapToReturn[key] = issuesHashMap
//            }
//            return hashMapToReturn
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.reception_menu, menu)
        return true
    }

    private fun movesTest(location: String, pickingId : Int) : String
    {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.movesTest(location, pickingId)
    }

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: com.google.gson.reflect.TypeToken<T>() {}.type)

    private fun populateListView(rel_id : String)
    {
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val cursor = db.fillStockitemsListView(rel_id)
        var items : OrderRevisionDataModel?

        Log.d("ProductTotal",cursor.count.toString())
        while (cursor.moveToNext()) {
            //if(cursor.getColumnIndex("revision_qty") < cursor.getColumnIndex("product_qty")){ not working
                items = OrderRevisionDataModel()
                items.Id = cursor.getInt(cursor.getColumnIndex("id"))
                items.revisionQty = cursor.getInt(cursor.getColumnIndex("revision_qty"))
                val issuesRaw = cursor.getString(6)
                if(issuesRaw != null){
                    Log.d("Id", items.Id.toString())
                    Log.d("IssueRaw", issuesRaw)
                    val issuesN = Gson().fromJson<ArrayList<ArrayList<Int>>>(issuesRaw)
                    Log.d("Issues as type", issuesN.toString())
                    val hmpIssues = HashMap<String, Any>()
                    hmpIssues["issues"] = issuesN
                    val hmpQty = HashMap<String, Any>()
                    hmpQty["qty"] = items.revisionQty
                    if(finalHashMap[pickingId.toInt()]?.get(items.Id) == null){
                        Log.d("Move id", "Null, adding")
                        finalHashMap[pickingId.toInt()]?.put(items.Id, hashMapOf("issues" to issuesN))
                        finalHashMap[pickingId.toInt()]?.get(items.Id)?.put("qty", items.revisionQty)
                    }
                    //finalHashMap[pickingId.toInt()]?.get(items.Id)?.put("issues", issuesN)
                    //finalHashMap[pickingId.toInt()]?.get(items.Id)?.put("qty", items.revisionQty)
                    //finalHashMap[pickingId.toInt()]?.put(items.Id, hmpIssues)
                    //finalHashMap[pickingId.toInt()]?.put(items.Id, hmpQty)
                    Log.d("Added",finalHashMap[pickingId.toInt()]?.get(items.Id)?.get("issues").toString())
                }
                val mixedName  = cursor.getString(cursor.getColumnIndex("product_id"))
                val separateName : Array<String> = mixedName.split(",").toTypedArray()
                val arrayofNames : Array<String> = separateName[1].toString().split(" ").toTypedArray()
                var name = arrayofNames[0]
                name = name.replace("[","")
                name = name.replace("]","")
                name = name.replace("\"","")
                items.productName = name
                items.qty = cursor.getInt(1)
                val mixedId  = cursor.getString(cursor.getColumnIndex("product_id"))
                val arrayOfId : Array<Any> = mixedId.split(",").toTypedArray()
                val idAsString = arrayOfId[0] as String
                val replaced = idAsString.replace("[","")
                Log.d("ProductId", replaced)
                items.productId = replaced.toInt()

                items.incidencies = "0"


                datamodels.add(items)
            //}
        }
        obtainList()
        cursor.close()
        db.close()
    }

    private fun obtainList() {
        val adapter = OrderRevisionAdapter(
            this,
            datamodels,
            this
        )
        orderRevisionLv.adapter = adapter
    }


    private fun populateIssuesListView(isDevolution: Boolean)
    {
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)

        val cursor = db.fillIncidenciesListView(isDevolution)
        if (finalHashMap[pickingId.toInt()]?.get(this.activeModeId)?.get("issues") == null){

            incidDataModel.clear()
            var items : IssuesPopupDataModel
            while (cursor.moveToNext()) {
                items = IssuesPopupDataModel()
                items.orderId = activeModeId
                items.incid_id = cursor.getInt(cursor.getColumnIndex("id"))
                items.incid_type = cursor.getString(cursor.getColumnIndex("name"))
                items.number = 0


                incidDataModel.add(items)
                obtainIssuesList()
            }
        }else {
            incidDataModel.clear()
            var items : IssuesPopupDataModel
            while (cursor.moveToNext()) {
                items = IssuesPopupDataModel()
                items.orderId = activeModeId
                items.incid_id = cursor.getInt(cursor.getColumnIndex("id"))
                items.incid_type = cursor.getString(cursor.getColumnIndex("name"))
                items.number = 0
                for (issue in finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues") as ArrayList<ArrayList<Int>>) {
                    if (issue[0] == items.incid_id) {
                        items.number = issue[1]
                    }
                }

                incidDataModel.add(items)
            }
        }


//        if(finalHashMap[pickingId.toInt()]?.get(this.activeModeId)==null){
//            Log.d("This Happened","1")
//            incidDataModel.clear()
//            var items : IssuesPopupDataModel
//            while (cursor.moveToNext()) {
//                items = IssuesPopupDataModel()
//                items.orderId = activeModeId
//                items.incid_id = cursor.getInt(cursor.getColumnIndex("id"))
//                items.incid_type = cursor.getString(cursor.getColumnIndex("name"))
//                items.number = 0
//
//
//                incidDataModel.add(items)
//                obtainIssuesList()
//            }
//        }
//        else{
//            Log.d("This Happened", "2")
//            obtainIssuesList()
//        }
        obtainIssuesList()
        cursor.close()
        db.close()
    }

    private fun obtainIssuesList() {
        popupListViewGlobal.adapter =
            IssuesPopupAdapter(
                this,
                incidDataModel,
                this
            )
//
//        val adapterGlobalListView = popupListViewGlobal.adapter as IssuesPopupAdapter
//        if(finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues") == null)
//        {
//
//            Log.d("HM", finalHashMap.toString())
//            Log.d("HM", activeModeId.toString())
//            adapterGlobalListView.dataSet = incidDataModel
//        }
//        else
//        {
//            var arrayListForPopUpAdapter=ArrayList<IssuesPopupDataModel>()
//
//            for (issue in finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues") as ArrayList<ArrayList<Int>>){
//                val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1).writableDatabase
//                val cursor = db.rawQuery("SELECT name FROM "+Utilities.TABLE_STOCK_ISSUES+" WHERE id = "+ issue[0], null)
//                var name : String = "None"
//                while (cursor.moveToNext()){
//                    name = cursor.getString(0)
//                }
//                arrayListForPopUpAdapter.add(IssuesPopupDataModel(activeModeId,name, issue[0], issue[1]))
//                cursor.close()
//            }
//
//            adapterGlobalListView.dataSet = arrayListForPopUpAdapter
//        }
//        adapterGlobalListView.notifyDataSetChanged()
    }

    var productScannedId = 0

    private fun displayScanResult(decodedString : String, decodedType : String) {

        //val decodedSource = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_source))
        //val decodedData = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_data))
        //val decodedLabelType = initiatingIntent.getStringExtra(resources.getString(R.string.datawedge_intent_key_label_type))

        try {
            scanPopupWindow.dismiss()
            selectPopupWindow.dismiss()
        }catch (e : Exception){
            Log.d("NULL","Es nulo")
        }


        Log.d("Barcode",decodedString)
        var scannedProductIdSearch = 0
        /*
        val db = DBConnect(this, Utilities.DBNAME, null, 1).readableDatabase
        val cursor = db.rawQuery("SELECT id FROM "+Utilities.TABLE_PRODUCT_PRODUCT+" where barcode = ? OR default_code = ?", arrayOf(decodedData, decodedData))

        */
        var pedido: OrderRevisionDataModel
        val deferredProductId = GlobalScope.async { searchProduct(decodedString) }
        var code : String
        try{
            runBlocking {
                if(deferredProductId.await() != "[]"){
                    Log.d("Raw code result",deferredProductId.await().toString())
                    val raw = deferredProductId.await()
                    val parse1 = raw.replace("[","")
                    val parse2 = parse1.replace("]","")
                    code = parse2
                    val list : List<String> = code.split(",").map { it.trim() }
                    scannedProductIdSearch = list[0].toInt()
                    Log.d("Scanned Product", scannedProductIdSearch.toString())
                }
                else{
                    val customToast = CustomToast(applicationContext, this@OrderRevisionActivity)
                    customToast.show("Producto no encontrado", 24.0F, Toast.LENGTH_LONG)
                }
            }
        }catch (e: Exception){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error General", 24.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error General",e.toString())
        }catch (xml: XmlRpcException){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error encontrando Producto", 24.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error de Red",xml.toString())
        }

        for(i in 0 until orderRevisionLv.adapter.count){
            pedido = orderRevisionLv.adapter.getItem(i) as OrderRevisionDataModel
            val productId = pedido.productId
            Log.d("Product Id and Name", productId.toString()+ pedido.productName)
            if(scannedProductIdSearch == productId){
                if(activeModeId != pedido.Id){
                    markedAsExcedent = false
                }
                activeModeId = pedido.Id
                if ((pedido.revisionQty >= pedido.qty) && !markedAsExcedent){
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Producto Excede Cantidad")
                        .setMessage("¿Desea agregar los siguientes productos a excedente?")
                        .setPositiveButton("Aceptar"){ _, _ ->
                            markedAsExcedent = true

                        }
                        .setNegativeButton("Cancelar"){ dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                else {
                    setScannedQuantity(productId)
                    pedido.revisionQty++
                    val arrayAdapter = orderRevisionLv.adapter as OrderRevisionAdapter
                    arrayAdapter.dataSet[i] = pedido
                    arrayAdapter.notifyDataSetChanged()
                    productScannedId = pedido.Id

                    val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1).writableDatabase
                    val contentValues = ContentValues()
                    contentValues.put("revision_qty", pedido.revisionQty)

                    db.update(Utilities.TABLE_STOCK_ITEMS, contentValues, "id = "+pedido.Id,null)
                    Log.d("Updated", "Done")

                    //inflate the layout of popup window
                    val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val popupView: View = inflater.inflate(R.layout.issues_scan_popup, null)

                    scanIssuesLv = popupView.findViewById(R.id.direct_incidencies_lv)
                    val scannedProduct = popupView.findViewById<TextView>(R.id.product_chosed_txt)
                    scannedProduct.text = pedido.productName

                    val noIssuesBtn = popupView.findViewById<Button>(R.id.no_incidencies_btn)
                    noIssuesBtn.setOnClickListener {
                        scanPopupWindow.dismiss()
                    }

                    scanIssuesDataModel.clear()
                    populateScanIssuesListView(returnID)

                    //create the popup window
                    val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
                    val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT
                    val focusable = true

                    scanPopupWindow = PopupWindow(popupView, width, height, focusable)

                    scanPopupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)
                }
            }
        }
    }


    override fun setScannedIssue(issueId: Int) {

            Log.d("scan_id",issueId.toString())
            var issuesQty=1
            var hmFinalPickingId = finalHashMap[pickingId.toInt()]
            var issuesQtyHm = hmFinalPickingId?.get(activeModeId)
            //If the id of the active move is not a key in hashmap, we will make a key from it and assign a hashmap with a "issues" key that will have a list with the scanned issue and 1 (default) issue.
            if(issuesQtyHm == null){
                var list2= listOf(listOf(issueId, 1))
                hmFinalPickingId?.put(activeModeId, hashMapOf("issues" to arrayListOf(arrayListOf(issueId, 1))))
            }
            //If the id of the active move is a key in hashmap.
            else{
                var listIssues=issuesQtyHm.get("issues")
                //issues is not a key, then make a key and assign a list with the scan issue and 1 (default)
                if (listIssues == null) {
                    issuesQtyHm.put("issues",arrayListOf(arrayListOf(issueId, 1)))
                }
                //issues is a key, take the previous value of that issue and add 1 to it.
                else{
                    //iterate and check which one correspond to the id we are manipulating
                    listIssues=listIssues as ArrayList<ArrayList<Int>>
                    //var to know if we modified something
                    var modified=false
                    for (issue in listIssues){
                        if(issue.get(0)==issueId){
                            issue.set(1,issue.get(1).plus(1))
                            //issuesQty=issue.get(1).plus(1)
                            modified=true
                            break
                        }
                    }
                    //if we didn't modify it, we gotta create it
                    if(!modified){
                        var list=finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues") as ArrayList<List<Int>>
                        list.add(arrayListOf(issueId, 1))
                    }
                }
            }
        val listIssues = finalHashMap[pickingId.toInt()]?.get(activeModeId)?.get("issues")
        val objGson = Gson()
        val obj = objGson.toJson(listIssues)
        val bundle = Bundle()
        bundle.putString("key", obj)
        val db = DBConnect(this, Utilities.DBNAME, null, 1).writableDatabase
        Log.d("Saving issues in ",activeModeId.toString())
        db.execSQL("UPDATE "+Utilities.TABLE_STOCK_ITEMS+" SET issues = '"+bundle.get("key").toString()+"' WHERE id = '"+activeModeId+"'")
        try{
            scanPopupWindow.dismiss()
        }catch(error:Exception){
            Log.e("error",error.message)
        }

        Log.d("HashMap Result", finalHashMap.toString())
    }

    fun setScannedQuantity(scanId: Int):Boolean{
        var hmFinalPickingId = finalHashMap[pickingId.toInt()]
        var issuesQtyHm = hmFinalPickingId?.get(activeModeId)
        //If the id of the active move is not a key in hashmap, we will make a key from it and assign a hashmap with a "issues" key that will have a list with the scanned issue and 1 (default) issue.
        if(issuesQtyHm == null){
            hmFinalPickingId?.put(activeModeId, hashMapOf("qty" to 1))
            //Test
        }
        //If the id of the active move is a key in hashmap.
        else{
            var intQty=issuesQtyHm.get("qty")
            //qty is not a key, then make a key and assign a list with the scan issue and 1 (default)
            if (intQty == null) {
                issuesQtyHm.put("qty",1)
            }
            //qty is a key, take the previous value of that qty and add 1 to it.
            else{
                //iterate and check which one correspond to the id we are manipulating
                finalHashMap[pickingId.toInt()]?.get(activeModeId)?.put("qty",(intQty as Int)+1)

            }
        }
        Log.d("HashMap Result", finalHashMap.toString())
        return true
    }


    private fun confirmIssues(id: Int, issues : HashMap<Int,HashMap<String,Any>>): List<List<String>> {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.confirmIssues(id,issues) as List<List<String>>
    }

    private fun actionClose(returnId: Int): String {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.actionClose(returnId) as String
    }

    private fun actionRejected(returnId: Int): String {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.actionRejected(returnId) as String
    }

    private fun sumIssues(arrayListIncidencies: java.util.ArrayList<IssuesPopupDataModel>?) : Int{
        val iteIncidencies= arrayListIncidencies!!.iterator()
        var sum = 0
        while (iteIncidencies.hasNext()){
            val incidencia = iteIncidencies.next()
            sum+=incidencia.number
        }
        return sum
    }

    private fun populateScanIssuesListView(isDevolution : Boolean)
    {
        val db = DBConnect(applicationContext, Utilities.DBNAME, null, 1)
        val cursor = db.fillIncidenciesListView(isDevolution)
        var items : ScanIssuesDataModel
        while (cursor.moveToNext()) {
            items = ScanIssuesDataModel()
            items.scanIssueName = cursor.getString(cursor.getColumnIndex("name"))
            items.scanIssueId = cursor.getInt(cursor.getColumnIndex("id"))
            items.isChecked = 0
            scanIssuesDataModel.add(items)
        }
        obtainScanIssuesList()
        cursor.close()
        db.close()
    }

    private fun obtainScanIssuesList() {
        val adapter = ScanIssuesAdapter(
            scanIssuesDataModel,
            this,
            this
        )
        scanIssuesLv.adapter = adapter
    }

    private fun searchProduct(product_id : String): String {
        val odooConn = OdooConn(prefs.getString("User", ""), prefs.getString("Pass", ""),this)
        odooConn.authenticateOdoo()
        return odooConn.searchProduct(product_id)
    }

    private fun getStockMoveIssue() : String
    {
        val odooConn = OdooConn(prefs.getString("User",""),prefs.getString("Pass",""),this)
        odooConn.authenticateOdoo()
        val noIds = emptyList<Int>()
        return odooConn.stockMoveIssue
    }
}
