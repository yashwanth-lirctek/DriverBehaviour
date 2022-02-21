package com.lirctek.driverbehaviour.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class Prefs {

    @Id
    var id: Long = 0

    var speedLimit: Int = 40
        set(value){
            field = value
            insert(this)
        }

    companion object{

        private val appPrefData  : Prefs by lazy {
            var appPref: Prefs? = ObjectBox.prefBox.query().build().findFirst()
            if (appPref == null) {
                appPref = Prefs()
            }
            return@lazy appPref
        }

        fun getAppPref(): Prefs {
            return appPrefData
        }

        fun insert(prefs: Prefs){
            prefs.id = ObjectBox.prefBox.put(prefs)
        }
    }
}