package com.example.schoolcheckin

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.Camera
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.callbacks.onCancel
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.example.schoolcheckin.Retrofit.API
import com.example.schoolcheckin.Retrofit.Student
import com.example.schoolcheckin.databinding.ActivityMainBinding
import com.google.gson.Gson
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import kotlin.experimental.and


class MainActivity : AppCompatActivity(), View.OnClickListener {
    val TAG = "MainLOG"
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var mCamera: Camera? = null
    var sharedPreferences: SharedPreferences? = null
    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private var mFilters: Array<IntentFilter>? = null
    private var mTechLists: Array<Array<String>>? = null
    private var mTag: Tag? = null
    private var IP = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FullScreencall()
        setContentView(binding.root)
        binding.settingBtn.setOnClickListener(this)
        sharedPreferences = getSharedPreferences(getString(R.string.SettingPref), MODE_PRIVATE)
        IP = sharedPreferences?.getString(
            getString(R.string.ServerIP_Pref),
            getString(R.string.API_URL)
        ).toString()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            prepareNFC()
        }


//        getStudent("0123456789")

    }


    private fun getStudent(uid: String) {
        IP = sharedPreferences?.getString(
            getString(R.string.ServerIP_Pref),
            getString(R.string.API_URL)
        ).toString()
        try {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(IP)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
            val api: API = retrofit.create(API::class.java)
            val dialog = createDialog()
            dialog.show()
            try {

                val userCall: Call<Student.StudentResponse?>? = api.getStudent("student", "5", uid)
                userCall!!.enqueue(object : Callback<Student.StudentResponse?> {
                    override fun onResponse(
                        call: Call<Student.StudentResponse?>,
                        response: Response<Student.StudentResponse?>
                    ) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "onResponse: $response")
                            Log.e(TAG, "onResponse : " + Gson().toJson(response.body()))
                            val result = response.body()

                            dialog.cancel()

                            if (result?.Code.isNullOrEmpty()) {
                                Toast.makeText(this@MainActivity, "ไม่พบข้อมูล", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                startActivity(
                                    Intent(this@MainActivity, StudentInfoActivity::class.java)
                                        .putExtra(
                                            "Student_Name",
                                            "${result!!.Title} ${result.FirstName} ${result!!.LastName}"
                                        )
                                        .putExtra("Student_Code", result.Code)
                                        .putExtra("Student_Room", result.RoomName)
                                        .putExtra("Student_Grade", result.GradeName)
                                )
                            }


                        }
                    }

                    override fun onFailure(call: Call<Student.StudentResponse?>, t: Throwable) {
                        Log.d(TAG, "onFailure: $t")
                        dialog.cancel()
                        createErrorDialog(t.toString()).show()
                    }

                })

            } catch (e: JSONException) {
                e.printStackTrace()
                Log.d(TAG, "onFailure: $e")
                dialog.cancel()
                createErrorDialog(e.toString()).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            createErrorDialog(e.toString()).show()
            Log.d(TAG, "onFailure: $e")
        }


    }


    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun prepareNFC() {
        //prepare NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (mNfcAdapter == null) {
            Log.d(TAG, "mNfcAdapter: 1")
            Toast.makeText(this@MainActivity, R.string.nfc_not_support, Toast.LENGTH_SHORT).show()
        } else {
            if (!mNfcAdapter!!.isEnabled) {
                Log.d(TAG, "mNfcAdapter: 2")
                Toast.makeText(this@MainActivity, R.string.nfc_disabled, Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "mNfcAdapter: 3")
                // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
                // will fill in the intent with the details of the discovered tag before delivering to
                // this activity.
                mPendingIntent = PendingIntent.getActivity(
                    this, 0,
                    Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
                )

                // Setup an intent filter for all MIME based dispatches
                val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
                try {
                    ndef.addDataType("*/*")
                    ndef.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
                    ndef.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
                    ndef.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
                    Log.d(TAG, "mNfcAdapter: 4")
                } catch (e: IntentFilter.MalformedMimeTypeException) {
                    Log.d(TAG, "mNfcAdapter: 5")
                    throw RuntimeException("fail", e)

                }
                mFilters = arrayOf(
                    ndef
                )

                // Setup a tech list for all Ndef tags
                mTechLists = arrayOf(
                    arrayOf(IsoDep::class.java.name),
                    arrayOf(MifareClassic::class.java.name),
                    arrayOf(MifareUltralight::class.java.name),
                    arrayOf(Ndef::class.java.name),
                    arrayOf(
                        NfcA::class.java.name
                    ),
                    arrayOf(NfcB::class.java.name),
                    arrayOf(NfcBarcode::class.java.name),
                    arrayOf(
                        NfcF::class.java.name
                    ),
                    arrayOf(NfcV::class.java.name)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FullScreencall()

        if (mNfcAdapter != null && mPendingIntent != null) {
            mNfcAdapter!!.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists)
        }
    }

    public override fun onPause() {
        super.onPause()
        if (mNfcAdapter != null) {
            mNfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val techList = mTag!!.techList
        Log.d(TAG, "======tag tech list======")
        for (i in techList.indices) {
            Log.d(TAG, techList[i])
            if (techList[i] == NfcA::class.java.name && techList[i + 1] == MifareClassic::class.java.name) {

                Log.d(TAG, "======Read======")
                readMifareClassic(mTag)

                break
            } else {
                Log.d(TAG, "TODO: need parser!")
                break
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun readMifareClassic(tag: Tag?) {
        val mc = MifareClassic.get(tag)
        val nfca = NfcA.get(tag)
        try {
            var auth = false
            var metaInfo = ""
            mc.connect()
            val id = nfca.tag.id
            Log.d(TAG, "id:" + bytesToHexString2(id))
            Log.d(TAG, "id:" + convertByteArrayToDecString(id))
            getStudent(convertByteArrayToDecString(id)!!)
//            getStudent("1033144821")

            Log.d(TAG, metaInfo)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ArrayIndexOutOfBoundsException) {
            Toast.makeText(this, "ผิดพลาด", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } finally {
            try {
                mc.close()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private val HEX_ARRAY = "0123456789ABCDEF".toByteArray(StandardCharsets.US_ASCII)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun bytesToHexString(src: ByteArray?): String? {
        val hexChars = ByteArray(src!!.size * 2)
        for (j in src.indices) {
            val v: Int = (src[j] and 0xFF.toByte()).toInt()
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars, StandardCharsets.UTF_8)
    }

    private fun bytesToHexString2(src: ByteArray?): String? {
        val stringBuilder = java.lang.StringBuilder()
        if (src == null || src.size <= 0) {
            return null
        }
        val buffer = CharArray(2)
        for (i in src.indices) {
            buffer[0] = Character.forDigit(src[i].toInt() ushr 4 and 0x0F, 16)
            buffer[1] = Character.forDigit((src[i] and 0x0F).toInt(), 16)
            stringBuilder.append(buffer)
        }
        return stringBuilder.toString()
    }

    fun convertByteArrayToDecString(b: ByteArray?): String? {
        return if (b != null) {
            val n = BigInteger(1, b)
            n.toString()
        } else {
            ""
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

    private fun createDialog(): SweetAlertDialog {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = Color.parseColor("#A5DC86")
        pDialog.titleText = "Loading"
        pDialog.setCancelable(false)
        pDialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        );
        return pDialog
    }

    private fun createErrorDialog(contenttext: String): SweetAlertDialog {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
        pDialog.progressHelper.barColor = Color.parseColor("#E53935")
        pDialog.titleText = "Error"
        pDialog.contentText = contenttext
        pDialog.setCancelable(true)
        pDialog.confirmText = "ปิด"
        pDialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        );
        return pDialog
    }

    override fun onClick(v: View?) {
        if (v == binding.settingBtn) {
            com.afollestad.materialdialogs.MaterialDialog(this@MainActivity).show {
                val IPad = sharedPreferences?.getString(
                    getString(R.string.ServerIP_Pref),
                    getString(R.string.API_URL)
                )
                title(R.string.ipsetting)
                input(
                    waitForPositiveButton = false,
                    prefill = IPad,
                ) { dialog, text ->
                    val inputField = dialog.getInputField()
                    val isValid = text.startsWith("", true)

                    inputField?.error = if (isValid) null else "กรุณากรอก IP"
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)

                    sharedPreferences?.edit()
                        ?.putString(
                            getString(R.string.ServerIP_Pref),
                            text.toString()
                        )
                        ?.apply()

                }
                positiveButton(R.string.save) {
                    FullScreencall()
                }
                negativeButton(R.string.cancle)
                {
                    FullScreencall()
                }
                onDismiss {
                    FullScreencall()
                }

            }
        }
    }
}

