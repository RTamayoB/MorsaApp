package com.example.morsaapp

import android.os.CountDownTimer
import kotlin.jvm.Synchronized
import com.example.morsaapp.NegativeCountDownTimer
import java.util.*

abstract class NegativeCountDownTimer(
    private val millisInFuture: Long,
    private val countDownInterval: Long
) {
    private var positiveCountDownTimer: CountDownTimer? = null
    private var negativeTimer: Timer? = null
    private var zeroMillisAt: Long = 0
    private var cancelled = false
    @Synchronized
    fun cancel() {
        if (positiveCountDownTimer != null) {
            positiveCountDownTimer!!.cancel()
        }
        if (negativeTimer != null) {
            negativeTimer!!.cancel()
        }
        cancelled = true
    }

    @Synchronized
    fun start(): NegativeCountDownTimer {
        cancelled = false
        positiveCountDownTimer = object : CountDownTimer(millisInFuture, countDownInterval) {
            override fun onTick(millisUntilFinished: Long) {
                if (cancelled) {
                    return
                }
                onTickToc(millisUntilFinished)
            }

            override fun onFinish() {
                onZero()
                zeroMillisAt = System.currentTimeMillis()
                negativeTimer = Timer()
                negativeTimer!!.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        if (cancelled) {
                            return
                        }
                        onTickToc(zeroMillisAt - System.currentTimeMillis())
                    }
                }, 0, countDownInterval)
            }
        }
        return this
    }

    abstract fun onTickToc(millisFromFinished: Long)
    abstract fun onZero()
}