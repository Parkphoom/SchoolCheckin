package com.example.schoolcheckin

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

class Public {
    @SuppressLint("SimpleDateFormat")
    fun getDateTimeNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val currentDate = sdf.format(Date())
        return currentDate
    }
}