package com.example.morsaapp

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import org.json.JSONArray
import java.lang.Exception
import kotlin.concurrent.thread

class MainMenuActivity : AppCompatActivity() {

    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        val toolbar : Toolbar = findViewById(R.id.main_menu_toolbar)
        toolbar.setSubtitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Menu Principal"

        prefs = this.getSharedPreferences("startupPreferences", 0)

        supportActionBar?.subtitle = prefs.getString("User","")

        val receptionBtn = findViewById<ImageButton>(R.id.reception_btn)
        val revisionBtn = findViewById<ImageButton>(R.id.revision_btn)
        val transferBtn = findViewById<ImageButton>(R.id.transfer_btn)
        val reStockBtn = findViewById<ImageButton>(R.id.re_stock_btn)
        val pickingBtn = findViewById<ImageButton>(R.id.picking_btn)
        val countBtn = findViewById<ImageButton>(R.id.count_btn)
        val routeBtn = findViewById<ImageButton>(R.id.routes_btn)
        val settingsBtn = findViewById<ImageButton>(R.id.settings_btn)
        val refundsBtn = findViewById<ImageButton>(R.id.refunds_btn)

        receptionBtn.setOnClickListener {
            val intent = Intent(applicationContext, ReceptionActivity::class.java)
            startActivity(intent)
        }

        revisionBtn.setOnClickListener {
            val intent = Intent(applicationContext, RevisionActivity::class.java)
            startActivity(intent)
        }

        transferBtn.setOnClickListener {
            val intent = Intent(applicationContext, LocationActivity::class.java)
            startActivity(intent)
        }

        reStockBtn.setOnClickListener {
            val intent = Intent(applicationContext, ReStockActivity::class.java)
            startActivity(intent)
        }

        pickingBtn.setOnClickListener {
            try {
                val intent = Intent(applicationContext, PickingActivity::class.java)
                startActivity(intent)
            }catch (e : Exception){
                Log.d("Error", e.toString())
            }
        }

        countBtn.setOnClickListener {
            val intent = Intent(applicationContext, CountActivity::class.java)
            startActivity(intent)
        }

        routeBtn.setOnClickListener {
            try{
            val intent = Intent(applicationContext, RoutesActivity::class.java)
            startActivity(intent)
            }catch (e : Exception){
                Log.d("Error", e.toString())
            }
        }

        refundsBtn.setOnClickListener {
            try{
                val intent = Intent(applicationContext, RefundsActivity::class.java)
                startActivity(intent)
            }catch (e : Exception){
                Log.d("Error", e.toString())
            }
        }

        settingsBtn.setOnClickListener {
            try{
                val intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(intent)
            }catch (e : Exception){
                Log.d("Error", e.toString())
            }
        }

/*
        /**
         * Check the current logged in user with the res.user table
         * If user exists (lock in case 2 users with same username, right now check is with username) return the line in res.users
         * Iterate trough line to check his permision and enable the according buttons
         */
        val loggedUser : String? = prefs.getString("User","")
        //Look for user and temporaly save it to check for permissions
        val db = DBConnect(applicationContext, OdooData.DBNAME, null, prefs.getInt("DBver",1))
        if(db.deleteDataOnTable(OdooData.TABLE_RES_USERS)){
            thread {
                val matchingUsers: String = getPermissions(loggedUser)
                val usersjson = JSONArray(matchingUsers)
                //Insert data
                val userUpdate = db.fillTable(usersjson, OdooData.TABLE_RES_USERS)
                if (userUpdate) {
                    //Once it has been saved, run the cursor and block the permissions
                    runOnUiThread {
                        val cursor = db.lookForUser(loggedUser)
                        if(cursor.count == 1){
                            Log.d("Setting","Permissions")
                            //Blocking permision
                            while (cursor.moveToNext()){
                                if(cursor.getString(cursor.getColumnIndex("in_group_56")) == "false"){
                                    val view: View = receptionBtn.parent as View
                                    view.setBackgroundColor(Color.RED)
                                    receptionBtn.isClickable = false
                                }
                                if(cursor.getString(cursor.getColumnIndex("in_group_57")) == "false"){
                                    val view: View = transferBtn.parent as View
                                    view.setBackgroundColor(Color.RED)
                                    transferBtn.isClickable = false
                                }
                                if(cursor.getString(cursor.getColumnIndex("in_group_58")) == "false"){
                                    val view: View = pickingBtn.parent as View
                                    view.setBackgroundColor(Color.RED)
                                    pickingBtn.isClickable = false
                                }
                                if(cursor.getString(cursor.getColumnIndex("in_group_59")) == "false"){
                                    val view: View = routeBtn.parent as View
                                    view.setBackgroundColor(Color.RED)
                                    routeBtn.isClickable = false
                                }
                                if(cursor.getString(cursor.getColumnIndex("in_group_60")) == "false"){
                                    val view: View = countBtn.parent as View
                                    view.setBackgroundColor(Color.RED)
                                    countBtn.isClickable = false
                                }
                            }
                        }
                    }
                } else
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Sin Exito", Toast.LENGTH_SHORT).show()
                    }
            }
        }
*/
        if(!prefs.contains("DBver")){
            Log.d("Null","Yes")
            prefs.edit().putInt("DBver",6).apply()
        }

        if(prefs.getInt("DBver",1) != 6){
            Log.d("DB ver before", prefs.getInt("DBver",1).toString())
            val db = DBConnect(
                applicationContext,
                OdooData.DBNAME,
                null,
                6
            ).writableDatabase
            Log.d("DB ver now", db.version.toString())
            prefs.edit().putInt("DBver",db.version).apply()
        }
        else{
            val db = DBConnect(
                applicationContext,
                OdooData.DBNAME,
                null,
                prefs.getInt("DBver",1)
            ).writableDatabase
            Log.d("DB ver", db.version.toString())
            prefs.edit().putInt("DBver",db.version).apply()
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.login_menu, menu)
        return true
    }

    private fun getPermissions(user: String?) : String
    {
        val odooConn = OdooConn(
            prefs.getString("User", ""),
            prefs.getString("Pass", ""),
            this
        )
        odooConn.authenticateOdoo()
        val noIds = emptyList<Int>()
        return odooConn.getPermissions(user)
    }
}
