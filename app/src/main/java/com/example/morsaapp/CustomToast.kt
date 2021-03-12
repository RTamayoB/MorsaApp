package com.example.morsaapp

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast

class CustomToast(context: Context, activity: Activity) {
    var mContext = context
    var mActivity = activity


    public fun show(message: String, textSize: Float, duration : Int) {
        val inflater = LayoutInflater.from(mContext)
        val layout = inflater.inflate(R.layout.custom_toast, mActivity.findViewById(R.id.custom_toast_layout)) as View
        val text = layout.findViewById<TextView>(R.id.custom_toast_layout_text)
        text.text = message // Lista actualizada
        text.textSize = textSize
        text.gravity = Gravity.CENTER

        val toast = Toast(mContext)
        toast.setGravity(Gravity.BOTTOM, 0, 0)
        toast.duration = duration
        toast.view = layout
        toast.show()
    }
}