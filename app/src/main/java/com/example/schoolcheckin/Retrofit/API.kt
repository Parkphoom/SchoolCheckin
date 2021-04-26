package com.example.schoolcheckin.Retrofit

import retrofit2.Call
import retrofit2.http.*


interface API {

    @Headers("Content-Type: application/json")
    @POST("schoolcore")
    open fun postStudent(
        @Query("service") service:String,
        @Query("action") action:String,
        @Body body: Student
    ): Call<Student?>?

    @Headers("Content-Type: application/json")
    @GET("schoolcore")
    open fun getStudent(
        @Query("service") service:String,
        @Query("action") action:String,
        @Query("uid") uid:String,
    ): Call<Student.StudentResponse?>?

}