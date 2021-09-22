package com.example.morsaapp

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.example.morsaapp.adapter.*
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.example.morsaapp.datamodel.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_invoice.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.xmlrpc.XmlRpcException
import org.json.JSONArray
import kotlin.concurrent.thread

class RefundDetailsActivity : AppCompatActivity(), Definable {
    var datamodels = ArrayList<RefundDetailsDataModel>()
    lateinit var refundDetailsLv : ListView
    lateinit var adapter : RefundDetailsAdapter
    lateinit var progressBar: ProgressBar

    lateinit var prefs : SharedPreferences

    private var user : String? = ""
    private var pass : String? = ""
    private var userId : Int = 0

    //Inspection variables
    lateinit var scanPopupWindow : PopupWindow
    lateinit var selectPopupWindow : PopupWindow
    var activeModeId : Int = 0
    var markedAsExcedent : Boolean = false
    var productScannedId = 0
    lateinit var scanIssuesLv :ListView
    private var scanIssuesDataModel = ArrayList<ScanIssuesDataModel>()
    private var returnID : Boolean = false
    lateinit var finalHashMap : HashMap<Int, HashMap<Int, HashMap<String,Any>>>
    lateinit var pickingId : String
    var maxNumber = 0
    lateinit var popupViewGlobal : View
    private lateinit var popupListViewGlobal:ListView
    private var incidDataModel = ArrayList<IssuesPopupDataModel>()
    private var pzs = 0

    private val mScanReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if(action == resources.getString(R.string.activity_intent_action)){
                val serverPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                val value = intent.getStringExtra(serverPrefs.getString("scanner_key","data"))
                displayScanResult(value!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refund_details)

        prefs = this.getSharedPreferences("startupPreferences", 0)
        Log.d("Data start of invoice", prefs.getString("User","Null")+" - "+prefs.getString("Pass","Null"))
        user = prefs.getString("User","Null")
        pass = prefs.getString("Pass","Null")

        val filter = IntentFilter()
        filter.addAction(resources.getString(R.string.activity_intent_action))
        registerReceiver(mScanReceiver, filter)

        val intent : Intent = intent
        userId = intent.getIntExtra("UserId",1)
        Log.d("UserId", userId.toString())
        pickingId = intent.getStringExtra("ID")!!
        //val purchaseId = intent.getStringExtra("Purchase Id")
        val number = intent.getStringExtra("Number")
        val displayName = intent.getStringExtra("Display Name")
        val realDisplayName = displayName.replace("/","\\/")
        val name = intent.getStringExtra("Name")
        //val ref = intent.getStringExtra("Ref")
        val relatedId = "[$userId,\"$number\"]"
        Log.d("RelatedId", relatedId)

        val partnerTxt = findViewById<TextView>(R.id.partner_txt)
        val invoiceTxt = findViewById<TextView>(R.id.invoice_txt)
        val totalTxt = findViewById<TextView>(R.id.total_txt)
        val motiveTxt = findViewById<TextView>(R.id.motive_txt)

        invoiceTxt.text = name

        refundDetailsLv = findViewById(R.id.product_lv)
        progressBar = findViewById(R.id.progressBar_invoice)

        /**
         * Downloads the moves for this invoice
         */
        refreshData(userId)

        val confirmPurchase = findViewById<Button>(R.id.button)
        confirmPurchase.setOnClickListener {
            try {
                onBackPressed()
            }catch (e: Exception){
                Log.d("Error on Back", e.toString())
            }
            /*
            val prefs = this.getSharedPreferences("backlogPrefs", 0)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Recibir Productos")
                .setMessage("Recibir Garantia/Devolución?")
                .setPositiveButton("Aceptar"){ _, _ ->
                    try {
                        Log.d("Id", userId.toString())
                        val deferredTest: Deferred<List<Any>> = GlobalScope.async { confirmStockReturn(userId.toString().toInt()) }
                        var result = ""
                        runBlocking {
                            result = deferredTest.await()[1].toString()
                            Log.d("Result Stock Return", result)
                        }
                        runOnUiThread {
                            val customToast = CustomToast(this, this)
                            customToast.show(result, 8.0F, Toast.LENGTH_LONG)
                        }
                        onBackPressed()
                    }catch (e : XmlRpcException){
                        Log.d("XMLRPC ERROR", e.toString())
                        val customToast = CustomToast(this, this)
                        customToast.show("Error en Odoo $e", 24.0F, Toast.LENGTH_LONG)
                    }catch (e : Exception){
                        Log.d("ERROR", e.toString())
                        val customToast = CustomToast(this, this)
                        customToast.show("Error en Petición $e", 24.0F, Toast.LENGTH_LONG)
                    }
                }
                .setNegativeButton("Cancelar"){ dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            */
        }

        //Inspection
        finalHashMap= HashMap()
        finalHashMap[pickingId.toInt()] = HashMap()

        val dbReload =
            DBConnect(this, OdooData.DBNAME, null, prefs.getInt("DBver",1))
        /**
         * Reload the issues table
         */
        if(dbReload.deleteDataOnTable(OdooData.TABLE_STOCK_ISSUES)){
            thread {
                try {
                    val deferredIssuesList : String = getStockMoveIssue()
                    val issuesJson = JSONArray(deferredIssuesList)
                    val issuesUpdate = dbReload.fillTable(issuesJson, OdooData.TABLE_STOCK_ISSUES)
                    if(issuesUpdate){
                        Log.d("Issues Table", "Updated")
                    }
                }catch (e: java.lang.Exception){
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

    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Salir")
        builder.setMessage("Interrumpir proceso de Devolucion")
        builder.setNegativeButton("Cancelar") {dialog, which ->
            dialog.dismiss()
        }
        builder.setPositiveButton("Aceptar") {dialog, which ->
            super.onBackPressed()
        }
        builder.show()
    }

    private fun refreshData(relatedId: Int){
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        if(db.deleteDataOnTable(OdooData.TABLE_STOCK_RETURN_LINE)){
            thread {
                try {
                    val deferredStockReturnLine: String = syncStockReturnLines(relatedId)
                    val stockLineJson = JSONArray(deferredStockReturnLine)
                    //Insert data
                    val stockLineUpdate =
                        db.fillTable(stockLineJson, OdooData.TABLE_STOCK_RETURN_LINE)
                    if (stockLineUpdate) {
                        Log.d("Loaded Lines", "Loading")
                        //If succesfull, delete data from model, insert again and notify the dataset
                        runOnUiThread {
                            progressBar.isVisible = false
                            datamodels.clear()
                            populateListView(relatedId.toString())
                            val adapter = refundDetailsLv.adapter as RefundDetailsAdapter
                            adapter.notifyDataSetChanged()
                            val customToast = CustomToast(this, this)
                            customToast.show("Lista Actualizada", 24.0F, Toast.LENGTH_LONG)
                        }
                    } else{
                        runOnUiThread {
                            progressBar.isVisible = false
                            val customToast = CustomToast(this, this)
                            customToast.show("Sin Exito", 24.0F, Toast.LENGTH_LONG)
                        }
                    }
                }catch (e: Exception){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Sin General $e", 24.0F, Toast.LENGTH_LONG)
                        progressBar.isVisible = false
                    }
                    Log.d("Error General",e.toString())
                }catch (xml: XmlRpcException){
                    runOnUiThread {
                        val customToast = CustomToast(this, this)
                        customToast.show("Error de Red $xml", 24.0F, Toast.LENGTH_LONG)
                        progressBar.isVisible = false
                    }
                    Log.d("Error de Red",xml.toString())
                }
            }

        }
    }

    private fun populateListView(id : String)
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
        val refundItemsCursor = db.fillRefundItemsListView(id)
        var items : RefundDetailsDataModel?


        while (refundItemsCursor.moveToNext()) {
            items = RefundDetailsDataModel()
            val productId = refundItemsCursor.getString(2).split(",")
            Log.d("ProductId",productId.toString())
            var name = productId[1]
            name = name.replace("\"", "")
            name = name.replace("]", "")
            var myId = productId[0]
            myId = myId.replace("[","")

            items.Id = refundItemsCursor.getString(refundItemsCursor.getColumnIndex("id")).toInt()
            items.productName = name
            items.name = refundItemsCursor.getString(refundItemsCursor.getColumnIndex("name"))
            items.productId = myId.toInt()
            items.qty = refundItemsCursor.getInt(refundItemsCursor.getColumnIndex("qty"))
            items.multiple = refundItemsCursor.getInt(refundItemsCursor.getColumnIndex("multiple"))
            pzs += items.qty
            val acceptedQty = refundItemsCursor.getInt(refundItemsCursor.getColumnIndex("accepted_qty")).toInt()
            val rejectedQty = refundItemsCursor.getInt(refundItemsCursor.getColumnIndex("rejected_qty")).toInt()
            Log.d("Values ", "$acceptedQty-$rejectedQty")
            items.revisionQty = acceptedQty+rejectedQty

            if(items.revisionQty < items.qty && items.revisionQty > 0){
                items.lineScanned = 1
            }
            else if(items.revisionQty == items.qty){
                items.lineScanned = 2
            }


            datamodels.add(items)
        }
        total_txt.text = pzs.toString()
        obtainList()
        refundItemsCursor.close()
        db.close()
    }

    private fun obtainList() {
        adapter = RefundDetailsAdapter(datamodels,this)
        refundDetailsLv.adapter = adapter
    }

    private fun displayScanResult(decodedString : String) {
        try {
            scanPopupWindow.dismiss()
            selectPopupWindow.dismiss()
        }catch (e : java.lang.Exception){
            Log.d("NULL","Es nulo")
        }


        Log.d("Barcode",decodedString)
        var scannedProductIdSearch = 0
        /*
        val db = DBConnect(this, Utilities.DBNAME, null, 1).readableDatabase
        val cursor = db.rawQuery("SELECT id FROM "+Utilities.TABLE_PRODUCT_PRODUCT+" where barcode = ? OR default_code = ?", arrayOf(decodedData, decodedData))

        */
        var pedido: RefundDetailsDataModel
        val deferredProductId = GlobalScope.async { searchProduct(decodedString) }
        var code : String
        var scannedCode : String = ""
        try{
            runBlocking {
                scannedCode = deferredProductId.await()
            }
            if(scannedCode!= "[]"){
                Log.d("Raw code result",scannedCode)
                scannedCode = scannedCode.replace("[","")
                scannedCode = scannedCode.replace("]","")
                val list = scannedCode.split(",").map { it.trim() }
                scannedProductIdSearch = list[0].toInt()
                Log.d("Scanned Product", scannedProductIdSearch.toString())
            }
            else{
                val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                customToast.show("Producto no encontrado", 24.0F, Toast.LENGTH_LONG)
            }
        }catch (e: java.lang.Exception){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error General", 14.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error General",e.toString())
        }catch (xml: XmlRpcException){
            runOnUiThread {
                val customToast = CustomToast(this, this)
                customToast.show("Error encontrando Producto", 14.0F, Toast.LENGTH_LONG)
            }
            Log.d("Error de Red",xml.toString())
        }

        var isproduct  = false
        for(i in 0 until refundDetailsLv.adapter.count){
            pedido = refundDetailsLv.adapter.getItem(i) as RefundDetailsDataModel
            val productId = pedido.productId
            Log.d("Product Id and Name",pedido.Id.toString()+" - "+productId.toString()+" - "+pedido.productName)
            if(scannedProductIdSearch == productId){
                val item = pedido
                val ID = pedido.Id
                Log.d("Found Product", pedido.productName+" - "+ID.toString())
                isproduct = true
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Producto - ${pedido.productName}")
                    .setMessage("¿Aceptar o Rechazar Producto?")
                    .setPositiveButton("Aceptar"){ dialog, _ ->
                        try{
                            val qty : HashMap<String, Int> = HashMap()
                            qty["accepted"] = 1*pedido.multiple
                            val refund = GlobalScope.async { doRefund(ID, qty) }
                            var result =""
                            runBlocking {
                                Log.d("Refund Result", refund.await())
                                result = refund.await()
                            }
                            if(result.equals("[true, Successful Update]")){
                                item.revisionQty++
                                if(item.revisionQty < item.qty && item.revisionQty > 0){
                                    item.lineScanned = 1
                                }
                                else if(item.revisionQty == item.qty){
                                    item.lineScanned = 2
                                }
                                val adapter = refundDetailsLv.adapter as RefundDetailsAdapter
                                adapter.notifyDataSetChanged()
                                val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                                customToast.show("Aceptado", 24.0F, Toast.LENGTH_LONG)
                            }
                            else{
                                val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                                customToast.show("Error en Petición", 14.0F, Toast.LENGTH_LONG)
                            }
                            dialog.dismiss()
                        }catch(e : Exception){
                            Log.d("Error General", e.toString())
                            val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                            customToast.show(e.toString(), 14.0F, Toast.LENGTH_LONG)
                        }catch (xml : XmlRpcException){
                            Log.d("Error Red", xml.toString())
                            val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                            customToast.show(xml.toString(), 14.0F, Toast.LENGTH_LONG)
                        }

                    }
                    .setNegativeButton("Rechazar"){ dialog, _ ->
                        val confirmBuilder = AlertDialog.Builder(this)
                        confirmBuilder.setTitle("Rechazar Producto")
                        confirmBuilder.setMessage("¿Seguro que desea rechazar producto?")
                        confirmBuilder.setPositiveButton("Rechazar") { dialog, which ->
                            try{
                                val qty : HashMap<String, Int> = HashMap()
                                qty["rejected"] = 1*pedido.multiple
                                Log.d("Id", pedido.Id.toString())
                                val refund = GlobalScope.async { doRefund(ID, qty) }
                                var result =""
                                runBlocking {
                                    Log.d("Refund Result", refund.await())
                                    result = refund.await()
                                }
                                if(result.equals("[true, Successful Update]")){
                                    item.revisionQty++
                                    if(item.revisionQty < item.qty && item.revisionQty > 0){
                                        item.lineScanned = 1
                                    }
                                    else if(item.revisionQty == item.qty){
                                        item.lineScanned = 2
                                    }
                                    val adapter = refundDetailsLv.adapter as RefundDetailsAdapter
                                    adapter.notifyDataSetChanged()
                                    val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                                    customToast.show("Rechazado", 24.0F, Toast.LENGTH_LONG)
                                }
                                else{
                                    val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                                    customToast.show("Error en Petición", 14.0F, Toast.LENGTH_LONG)
                                }
                                dialog.dismiss()
                            }catch(e : Exception){
                                Log.d("Error General", e.toString())
                                val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                                customToast.show(e.toString(), 14.0F, Toast.LENGTH_LONG)
                            }catch (xml : XmlRpcException){
                                Log.d("Error Red", xml.toString())
                                val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
                                customToast.show(xml.toString(), 14.0F, Toast.LENGTH_LONG)
                            }
                        }
                        confirmBuilder.setNegativeButton("Cancelar") { dialog, which ->
                            dialog.dismiss()
                        }
                        confirmBuilder.show()
                    }
                    .show()
            }
        }
        if(!isproduct){
            val customToast = CustomToast(applicationContext, this@RefundDetailsActivity)
            customToast.show("Producto no Encontrado", 24.0F, Toast.LENGTH_LONG)
        }
    }

    private fun populateScanIssuesListView(isDevolution : Boolean)
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )
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

    private fun searchProduct(product_id : String): String {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.searchProduct(product_id)
    }

    private fun getStockMoveIssue() : String
    {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        val noIds = emptyList<Int>()
        return odooConn.stockMoveIssue
    }

    private fun confirmStockReturn(id: Int): List<Any> {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        return odooConn.confirmStockReturn(id) as List<Any>
    }

    //Returns the purchases lines
    fun syncStockReturnLines(returnId : Int) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val invoiceLine = odoo.reloadStockReturnLines(returnId)
        return invoiceLine
    }

    fun doRefund(productId : Int, qty : HashMap<String, Int>) : String{
        val odoo = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odoo.authenticateOdoo()
        val invoiceLine = odoo.doRefund(productId, qty)
        return invoiceLine.toString()
    }

    override fun showPopup(value: Int, moveId: Int, Name: String?) {
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

    private fun populateIssuesListView(isDevolution: Boolean)
    {
        val db = DBConnect(
            applicationContext,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        )

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

    override fun alterCount(item: IssuesPopupDataModel, position: Int?, increase: Boolean) {
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

    override fun setNumber(newvalue: Int, item: IssuesPopupDataModel?) {
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
        val db = DBConnect(
            this,
            OdooData.DBNAME,
            null,
            prefs.getInt("DBver",1)
        ).writableDatabase
        Log.d("Saving issues in ",activeModeId.toString())
        db.execSQL("UPDATE "+ OdooData.TABLE_STOCK_ITEMS+" SET issues = '"+bundle.get("key").toString()+"' WHERE id = '"+activeModeId+"'")
        try{
            scanPopupWindow.dismiss()
        }catch(error: java.lang.Exception){
            Log.e("error",error.message)
        }

        Log.d("HashMap Result", finalHashMap.toString())
    }
}