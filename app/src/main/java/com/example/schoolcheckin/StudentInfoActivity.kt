package com.example.schoolcheckin

import android.Manifest
import android.R.attr.bitmap
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.schoolcheckin.Retrofit.API
import com.example.schoolcheckin.Retrofit.Student
import com.example.schoolcheckin.databinding.ActivityStudentInfoBinding
import com.google.gson.Gson
import com.tbruyelle.rxpermissions3.RxPermissions
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream


class StudentInfoActivity : AppCompatActivity(), SurfaceHolder.Callback {

    val binding: ActivityStudentInfoBinding by lazy {
        ActivityStudentInfoBinding.inflate(
            layoutInflater
        )
    }
    private var mCamera: Camera? = null
    private var bitmap: Bitmap? = null
    private var TAG = "StudentLog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        FullScreencall()

        binding.nameTv.text = intent.getStringExtra("Student_Name")
        binding.idTv.text = intent.getStringExtra("Student_Code")
        binding.roomTv.text = intent.getStringExtra("Student_Room")
        binding.timeTv.text = Public().getDateTimeNow()


    }

    override fun onStart() {
        super.onStart()
        val rxPermissions: RxPermissions = RxPermissions(this)

        rxPermissions.request(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
            .subscribe { granted ->
                if (granted) { // Always true pre-M
                    val index: Int = getFrontCameraId()
                    if (index == -1) {
//                        Toast.makeText(applicationContext, "No front camera", Toast.LENGTH_LONG).show()
                    } else {
                        val sv = findViewById(R.id.cameraView) as SurfaceView;
                        val sHolder = sv.getHolder()
                        sHolder.addCallback(this)
                        sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
                    }
                } else {
                    // Oups permission denied
                }
            }
    }

    fun getFrontCameraId(): Int {
        val ci = Camera.CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, ci)
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i
        }
        return -1 // No front-facing camera found
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val index = getFrontCameraId()
        if (index == -1) {
//            Toast.makeText(applicationContext, "No front camera", Toast.LENGTH_LONG).show()
        } else {
            mCamera = Camera.open(index)
//            Toast.makeText(applicationContext, "With front camera", Toast.LENGTH_LONG).show()
        }
        mCamera = Camera.open(index)
        try {
            mCamera!!.setPreviewDisplay(holder)
        } catch (exception: IOException) {
            mCamera!!.release()
            mCamera = null
        }

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        cameraTask().execute()
//        val parameters = mCamera!!.getParameters()
//        mCamera!!.setParameters(parameters)
//        mCamera!!.startPreview()
//
//        val mCall = Camera.PictureCallback { data, camera ->
//            val uriTarget = contentResolver.insert( //(Media.EXTERNAL_CONTENT_URI, image);
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues()
//            )
//            val imageFileOS: OutputStream?
//            try {
//                Log.d(TAG, "byteArray: $data")
//                imageFileOS = contentResolver.openOutputStream(uriTarget!!)
//                imageFileOS!!.write(data)
//                imageFileOS!!.flush()
//                imageFileOS!!.close()
////                Toast.makeText(this@StudentInfoActivity, "Image saved: $uriTarget", Toast.LENGTH_LONG).show()
//            } catch (e: FileNotFoundException) {
//                e.printStackTrace()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//            //mCamera.startPreview();
//            val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
//            binding.progressCircular.visibility = View.GONE
//            binding.studentImage?.setImageBitmap(bmp)
//            bitmap = bmp
//
//                postStudent()
//
//        }
//
//        mCamera!!.takePicture(null, null, mCall)
    }

    inner class cameraTask() : AsyncTask<Void, Void, String>() {
        val parameters = mCamera!!.getParameters()

        override fun doInBackground(vararg params: Void?): String? {
            mCamera!!.setParameters(parameters)
            mCamera!!.startPreview()

            val mCall = Camera.PictureCallback { data, camera ->
                val uriTarget = contentResolver.insert( //(Media.EXTERNAL_CONTENT_URI, image);
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues()
                )
                val imageFileOS: OutputStream?
                try {
                    Log.d(TAG, "byteArray: $data")
                    imageFileOS = contentResolver.openOutputStream(uriTarget!!)
                    imageFileOS!!.write(data)
                    imageFileOS!!.flush()
                    imageFileOS!!.close()
//                Toast.makeText(this@StudentInfoActivity, "Image saved: $uriTarget", Toast.LENGTH_LONG).show()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                //mCamera.startPreview();
                val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
                runOnUiThread {
                    binding.studentImage?.setImageBitmap(bmp)
                }

                bitmap = bmp
            }

            mCamera!!.takePicture(null, null, mCall)
            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            postStudent()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mCamera!!.stopPreview();
        mCamera!!.release();
        mCamera = null;
    }

    private fun postStudent() {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.API_URL))
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        val student = Student()
        student.Camno = 1
        student.Code = binding.idTv.text.toString()
        student.Temperature = 36
        student.datetime = binding.timeTv.text.toString()

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        student.Faceimage = Base64.encodeToString(byteArray, Base64.NO_WRAP);
//        student.Faceimage = "aaa"

        val api: API = retrofit.create(API::class.java)
        try {

            val userCall: Call<Student?>? = api.postStudent("cam", "save", student)
            userCall!!.enqueue(object : Callback<Student?> {
                override fun onResponse(
                    call: Call<Student?>,
                    response: Response<Student?>
                ) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "onResponse: $response")
                        Log.e(TAG, "onResponse : " + Gson().toJson(response.body()))
                        Handler().postDelayed({
                            //doSomethingHere()
                            finish()
                        }, 1500)

                    }
                }

                override fun onFailure(call: Call<Student?>, t: Throwable) {
                    Log.d(TAG, "onFailure: ")
                }

            })

        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun FullScreencall() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            val v = this.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val decorView = window.decorView
            val uiOptions =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.systemUiVisibility = uiOptions
        }
    }
}