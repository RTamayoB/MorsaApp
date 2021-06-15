package com.example.morsaapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.lang.Exception

class LoginActivity : AppCompatActivity() {

    lateinit var prefs : SharedPreferences

    fun saveHashMap(key: String?, obj: Any?, context : Context) {
        val prefs: SharedPreferences = context.getSharedPreferences("startupPreferences", 0)
        val editor = prefs.edit()
        val gson = Gson()
        val json = gson.toJson(obj)
        editor.putString(key, json)
        editor.apply() // This line is IMPORTANT !!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = this.getSharedPreferences("startupPreferences", 0)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val toolbar : Toolbar = findViewById(R.id.login_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Ingresar"

        val isDBLoaded = prefs.getBoolean("databaseLoaded", false)
        if (!isDBLoaded) {
            saveHashMap("timers", HashMap<String, Long>(), this)
            saveHashMap("racks", HashMap<String, Boolean>(), this)
            val db2 = DBConnect(
                applicationContext,
                OdooData.DBNAME,
                null,
                prefs.getInt("DBver",1)
            ).writableDatabase
            db2.execSQL("INSERT INTO " + OdooData.TABLE_STOCK_ITEMS + " (revision_qty) VALUES (0)")
        }
        if(prefs.getString("racks","").isNullOrEmpty()){
            Log.d("Saved","Racks")
            saveHashMap("racks", HashMap<String, Boolean>(), this)
        }
        if(prefs.getString("missings","").isNullOrEmpty()){
            Log.d("Saved","Missings")
            saveHashMap("missings", HashMap<String, Boolean>(), this)
        }
        val editor = prefs.edit()
        editor.putBoolean("databaseLoaded", true)
        editor.commit()
        editor.apply()

/*
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this,0,intent,0)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),5*60*1000, pendingIntent)
        Log.d("Setup","Alarm set")
        */

        /*
        val BackLogAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val backintent = Intent(this, BackLogReceiver::class.java)
        val backpendingIntent = PendingIntent.getBroadcast(this,0,backintent,0)
        BackLogAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),10*60*1000, backpendingIntent)
        Log.d("Setup","BackLogSet set")
        */


        val userNameTxt = findViewById<EditText>(R.id.username_txt)
        val passwordTxt = findViewById<EditText>(R.id.password_txt)
        val loginBtn = findViewById<Button>(R.id.login_btn)
        val serverData = findViewById<TextView>(R.id.serverData_txt)

        loginBtn.setOnClickListener {

            val user = userNameTxt.text.toString()
            val pass = passwordTxt.text.toString()

//            val deferred = GlobalScope.async { checkLogin(user, pass) }
//
//            runBlocking {
//                Log.d("Help",deferred.await().toString())
//            }

            try {
                val deferredId = GlobalScope.async { checkLogin(user,pass) }
                runBlocking {
                    if (deferredId.await()) {
                        val editor = prefs.edit()
                        editor.putString("User", user)
                        editor.putString("Pass", pass)
                        editor.commit()
                        editor.apply()
                        val intent = Intent(applicationContext, MainMenuActivity::class.java)
                        intent.putExtra("Usuario",userNameTxt.text.toString())
                        startActivity(intent)
                    }
                    else{
                        Toast.makeText(applicationContext, "Usuario o contraseña invalido",Toast.LENGTH_LONG).show()
                        userNameTxt.setText("")
                        passwordTxt.setText("")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Usuario o contraseña invalidos",Toast.LENGTH_LONG).show()
                Log.d("Error login",e.toString())
                userNameTxt.setText("")
                passwordTxt.setText("")
            }

        }

        serverData.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.login_menu, menu)
        return true
    }


    override fun onBackPressed() {
        Log.d("You cannot","Kek")
    }

    private fun checkLogin(user:String, pass:String) : Boolean
    {
        val odooConn = OdooConn(user, pass, this)
        val isConnected = odooConn.authenticateOdoo()
        if(isConnected){

            val prefs = applicationContext.getSharedPreferences("startupPreferences", 0)

            val db = DBConnect(
                applicationContext,
                OdooData.DBNAME,
                null,
                prefs.getInt("DBver",1)
            ).writableDatabase
            val contentValues = ContentValues()
            contentValues.put("username",user)
            contentValues.put("app_password", pass)
            contentValues.put("user_id", odooConn.uid)
            val cursor = db.rawQuery("SELECT user_id FROM "+ OdooData.TABLE_USERS+" where user_id = "+odooConn.uid,null)
            Log.d("Error", "Error 2 here")
            if(cursor.count <= 0){
                db.insert(OdooData.TABLE_USERS,null, contentValues)
                Log.d("Table Result", "Row created")
            }else {
                db.update(OdooData.TABLE_USERS, contentValues, "user_id = "+ odooConn.uid, null)
                Log.d("Table Result", "Row updated")
            }
            val editor = prefs.edit()
            Log.d("User Id", odooConn.uid.toString())
            editor.putInt("activeUser", odooConn.uid)
            editor.commit()
            editor.apply()
            return isConnected
        }else{
            return false
        }

    }

    private fun sequence(): String{
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        val noIds = emptyList<Int>()
        return odooConn.getSequence(noIds)
    }

}
