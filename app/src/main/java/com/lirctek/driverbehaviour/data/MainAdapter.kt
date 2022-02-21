package com.lirctek.driverbehaviour.data

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lirctek.driverbehaviour.R
import java.text.SimpleDateFormat
import java.util.*

class MainAdapter(val speedDataList: List<SpeedData>) : RecyclerView.Adapter<MainAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mType = itemView.findViewById<TextView>(R.id.mType)
        val mDateTime = itemView.findViewById<TextView>(R.id.mDateTime)
        val mSpeedData = itemView.findViewById<TextView>(R.id.mSpeedData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_data_layout, parent, false)
        return MyViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.mSpeedData.text = speedDataList[position].speed+" MPH"
        holder.mType.text = speedDataList[position].type
//        holder.mDateTime.text = speedDataList[position].dateTimeStamp.toString()
        holder.mDateTime.text = getTimeStampToDate(speedDataList[position].dateTimeStamp)
    }

    override fun getItemCount(): Int {
        return speedDataList.size
    }

    val DISPLAY_DATE_FORMAT = "EEE, d MMM yyyy h:mm a "
    fun getTimeStampToDate(dateTimeStamp : Long): String{
        val formatter = getDateFormatter(DISPLAY_DATE_FORMAT)
        val date = Date(dateTimeStamp)
        return formatter.format(date)
    }

    fun getDateFormatter(format: String) : SimpleDateFormat {
        val simpleDateFormat = SimpleDateFormat(format, Locale.ENGLISH)
        return simpleDateFormat
    }
}