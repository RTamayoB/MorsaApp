package com.example.morsaapp.workmanager

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.morsaapp.data.OdooConn

class RevisionWorker(ctx : Context, params: WorkerParameters) : Worker(ctx, params){

    val context = ctx
    override fun doWork(): Result {

        val pickingId = inputData.getInt("PickingId",0)

        //val sendIssues : List<List<String>> = confirmIssues(pickingId,)
        return Result.failure()
    }

    private fun confirmIssues(id: Int, issues : HashMap<Int,HashMap<String,Any>>, user : String, pass: String, context: Context): List<List<String>> {
        val odooConn = OdooConn(user, pass, context)
        odooConn.authenticateOdoo()
        return odooConn.confirmIssues(id,issues) as List<List<String>>
    }
}