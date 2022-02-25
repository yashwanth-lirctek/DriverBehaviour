package com.lirctek.driverbehaviour.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class LogData {

    @Id
    var id: Long = 0
    var rateOverYaw: String? = null
    var yAccCalibrated: String? = null
    var yPreviousAcc: String? = null
    var yAccelerometer: String? = null

    companion object{
        fun insertData(money: LogData) {
            ObjectBox.logBox.put(money)
        }

        fun getAllData(): MutableList<LogData> {
            return ObjectBox.logBox.query().build().find()
        }
    }

}