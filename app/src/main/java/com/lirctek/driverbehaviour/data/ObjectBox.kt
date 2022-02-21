package com.lirctek.driverbehaviour.data

import android.content.Context
import io.objectbox.Box
import io.objectbox.BoxStore

object ObjectBox {

    lateinit var boxStore: BoxStore
    lateinit var speedBox: Box<SpeedData>
    lateinit var prefBox: Box<Prefs>
        private set

    fun init(context: Context) {
        boxStore = MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()

        speedBox = boxStore.boxFor(SpeedData::class.java)
        prefBox = boxStore.boxFor(Prefs::class.java)
    }

    fun get(): BoxStore {
        return boxStore
    }

}