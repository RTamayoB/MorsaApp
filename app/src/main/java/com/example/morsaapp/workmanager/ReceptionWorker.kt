package com.example.morsaapp.workmanager

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.morsaapp.data.DBConnect
import com.example.morsaapp.data.OdooConn
import com.example.morsaapp.data.OdooData
import java.lang.Exception

class ReceptionWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    val context = ctx;
    override fun doWork(): Result {

        val id = inputData.getInt("Id", 0)
        val number = inputData.getString("Number")
        val user = inputData.getString("User")
        val pass = inputData.getString("Pass")

        Log.d("Data Received", "Id: $id, User: $user, Pass: $pass")
        try {
            val sendConfirm: List<Any> = confirmInvoice(id, number, user, pass, context)
            if (sendConfirm[0] as Boolean) {
                val bool: Boolean = sendConfirm[0] as Boolean

                //val id: Int = deferredTest.await()[1] as Int

                val db = DBConnect(
                    applicationContext,
                    OdooData.DBNAME,
                    null,
                    1
                )
                db.changeStockState(id.toString())
                db.close()

                Toast.makeText(applicationContext, "Confirmado", Toast.LENGTH_LONG).show()
                Log.d("Error","Succes in process")
                return Result.success()
            } else {
                //Reception failed, add to backlog
                Toast.makeText(applicationContext, "Error en subir invoice", Toast.LENGTH_LONG)
                    .show()
                Log.d("Error","Sending invoice")
                return Result.failure()
            }
        }catch (e: Exception){
            Log.d("Error in catch",e.toString())
            return Result.failure()
        }
    }

    private fun confirmInvoice(id: Int, number: String?, user: String?, pass: String?, context: Context): List<Any> {
        val odooConn = OdooConn(user, pass, context)
        odooConn.authenticateOdoo()
        return odooConn.confirmInvoice(id, number) as List<Any>
    }
}