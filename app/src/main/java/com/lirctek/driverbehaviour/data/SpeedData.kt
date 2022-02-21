package com.lirctek.driverbehaviour.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class SpeedData {

    @Id
    var id: Long = 0
    var speed: String? = null
    var type: String? = null
    var dateTimeStamp: Long = 0
    var note: String? = null

    companion object{

        fun insertData(money: SpeedData) {
            ObjectBox.speedBox.put(money)
        }

        fun getAllData(): MutableList<SpeedData> {
            return ObjectBox.speedBox.query().orderDesc(SpeedData_.dateTimeStamp).build().find()
        }

        fun clearAllData(){
            ObjectBox.speedBox.removeAll()
        }

        fun getLastTimeStamp(): Long{
            val data = ObjectBox.speedBox.query().orderDesc(SpeedData_.dateTimeStamp).build().findFirst()
            if (data != null) {
                return data.dateTimeStamp
            }else{
                return 0
            }
        }

    }
}