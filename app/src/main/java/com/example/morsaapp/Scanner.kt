package com.example.morsaapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import java.lang.Exception

class Scanner : AppCompatActivity() {

    lateinit var res : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)



        val filter = IntentFilter()
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        filter.addAction(resources.getString(R.string.activity_intent_filter_action_order))
        registerReceiver(myBroadcastReceiver, filter)

        res = findViewById(R.id.textView)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(myBroadcastReceiver)
    }

    private val myBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action : String? = intent?.action
            val b : Bundle? = intent?.extras

            if(action.equals(resources.getString(R.string.activity_intent_filter_action_order))){
                try{
                    displayScanResult(intent,"via Broadcast")
                }catch (e : Exception){

                }
            }
        }
    }

    private fun displayScanResult(initiatingIntent : Intent?, howDataReceived : String){
        val decodedSource = initiatingIntent?.getStringExtra(resources.getString(R.string.datawedge_intent_key_source))
        val decodedData = initiatingIntent?.getStringExtra(resources.getString(R.string.datawedge_intent_key_data))
        val decodedLabelType = initiatingIntent?.getStringExtra(resources.getString(R.string.datawedge_intent_key_label_type))

        res.text = decodedData.toString()

    }
}
