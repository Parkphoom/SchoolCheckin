package com.example.schoolcheckin.Retrofit

import com.google.gson.annotations.SerializedName

class Student {

    @SerializedName("Camno")
    var Camno: Int? = null

   @SerializedName("Code")
    var Code: String? = null

   @SerializedName("Temperature")
    var Temperature: Int? = null

   @SerializedName("Datetime")
    var datetime: String? = null

   @SerializedName("Faceimage")
    var Faceimage: String? = null

    class StudentResponse{

        @SerializedName("Code")
        var Code: String? = null

        @SerializedName("Rfid")
        var Rfid: String? = null

        @SerializedName("Title")
        var Title: String? = null

        @SerializedName("FirstName")
        var FirstName: String? = null

        @SerializedName("LastName")
        var LastName: String? = null

        @SerializedName("NickName")
        var NickName: String? = null

        @SerializedName("RoomNo")
        var RoomNo: String? = null

        @SerializedName("RoomName")
        var RoomName: String? = null

        @SerializedName("GradeName")
        var GradeName: String? = null

    }
}