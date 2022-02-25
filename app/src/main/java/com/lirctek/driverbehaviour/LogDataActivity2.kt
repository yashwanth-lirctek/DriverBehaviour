package com.lirctek.driverbehaviour

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.lirctek.driverbehaviour.data.LogData

class LogDataActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_data2)

        findViewById<MaterialToolbar>(R.id.mToolBar).setNavigationOnClickListener {
            onBackPressed()
        }

        val data = LogData.getAllData()

        for (i in data.indices){
            viewAddition(data[i])
        }
    }

    fun viewAddition(lodData: LogData){
        val view: View = LayoutInflater.from(this).inflate(R.layout.row_log, null, false)

        val rateOverYawM = view.findViewById<TextView>(R.id.rateOverYaw)
        val yAccCalibratedM = view.findViewById<TextView>(R.id.yAccCalibrated)
        val yPreviousAccM = view.findViewById<TextView>(R.id.yPreviousAcc)
        val yAccelerometerM = view.findViewById<TextView>(R.id.yAccelerometer)

        rateOverYawM.text = lodData.rateOverYaw
        yAccCalibratedM.text = lodData.yAccCalibrated
        yPreviousAccM.text = lodData.yPreviousAcc
        yAccelerometerM.text = lodData.yAccelerometer

        findViewById<LinearLayout>(R.id.mLayoutData).addView(view)
    }
}