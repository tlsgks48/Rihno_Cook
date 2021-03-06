package com.example.rihno_cook

import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.rihno_cook.Common.Common
import com.example.rihno_cook.Retrofit.INodeJS
import com.example.rihno_cook.Retrofit.IUploadAPI
import com.example.rihno_cook.Retrofit.RetrofitClient
import com.example.rihno_cook.Utils.ProgressRequestBody
import com.ipaulpro.afilechooser.utils.FileUtils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_talk_update.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Call
import java.time.LocalDate

class Talk_update : AppCompatActivity(), ProgressRequestBody.UploadCallbacks {
    override fun onProgressUpdate(percentage: Int) {
        Toast.makeText(this,"업로딩 진행중", Toast.LENGTH_SHORT).show()
    }

    lateinit var mService: IUploadAPI
    internal var compositeDisposable = CompositeDisposable()
    lateinit var myAPI: INodeJS

    private val PICK_IMAGE_REQUEST:Int = 1001
    private val PICK_PHOTO_REQUEST:Int = 1002
    var fileUri: Uri? = null

    // 현재날짜
    @RequiresApi(Build.VERSION_CODES.O)
    val onlyDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk_update)

        //Service
        mService = Common.apiUpload
        //Inot API
        val retrofit = RetrofitClient.instance
        myAPI = retrofit.create(INodeJS::class.java)

        Talk_update_imageView_layout.visibility = View.INVISIBLE
        // 시작 채워넣기
        if(Common.selected_talk!!.image != "") {
            Glide.with(this).load(Common.selected_talk!!.image).into(Talk_update_imageView)
            Talk_update_imageView_layout.visibility = View.VISIBLE
        }

        Talk_update_text.setText(Common.selected_talk!!.text)

        // 사진추가하기를 누른다면...
        Talk_update_add.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("사진 선택")
            dialog.setMessage("사진을 촬영하시거나, 갤러리에서 원하시는 사진을 선택해주세요.")
            //pick a photo from gallery
            fun d_p() {
                val getCountIntent = FileUtils.createGetContentIntent()
                val intent = Intent.createChooser(getCountIntent, "Select a file")
                startActivityForResult(intent,PICK_IMAGE_REQUEST)
            }
            fun d_n() {
                val values = ContentValues(1)
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                fileUri = contentResolver
                    .insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values)
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if(intent.resolveActivity(packageManager) != null) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    startActivityForResult(intent, PICK_PHOTO_REQUEST)
                }
            }
            var dialog_listner = object : DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    when(which){
                        DialogInterface.BUTTON_POSITIVE ->
                            d_p()
                        DialogInterface.BUTTON_NEGATIVE ->
                            d_n()
                    }
                }
            }
            dialog.setPositiveButton("갤러리",dialog_listner)
            dialog.setNegativeButton("사진 촬영",dialog_listner)
            dialog.setNeutralButton("Cancel",null)
            dialog.show()

        }

        // 사진제거하기
        Talk_update_imageView_Delete.setOnClickListener {
            Talk_update_imageView_layout.visibility = View.INVISIBLE
            fileUri = null
        }

        // 작성 버튼 누른다면
        Talk_update_submit.setOnClickListener {uploadFile()}

    }

    private fun uploadFile() {
        if (fileUri != null) {
            val file = FileUtils.getFile(this, fileUri)
            val requestFile = ProgressRequestBody(file, this)
            val body = MultipartBody.Part.createFormData("talkfile", file.name, requestFile)

            Thread(Runnable {
                mService.TalkUpdateFile(body, Common.selected_talk!!.id,Talk_update_text.text.toString(),onlyDate.toString()).enqueue(object: retrofit2.Callback<String> {
                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Toast.makeText(this@Talk_update, t.message, Toast.LENGTH_LONG).show()
                    }
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        Toast.makeText(this@Talk_update, "업로딩 성공!!", Toast.LENGTH_LONG).show()
                        val nextIntent = Intent(this@Talk_update, MainActivity::class.java)
                        nextIntent.putExtra("번호",3)
                        startActivity(nextIntent)

                    }
                })
            }).start()
        }
        else {
            Thread(Runnable {
                mService.TalkUpdateFile2(Common.selected_talk!!.id,Talk_update_text.text.toString(),onlyDate.toString()).enqueue(object: retrofit2.Callback<String> {
                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Toast.makeText(this@Talk_update, t.message, Toast.LENGTH_LONG).show()
                    }
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        Toast.makeText(this@Talk_update, "업로딩 성공!!", Toast.LENGTH_LONG).show()
                        val nextIntent = Intent(this@Talk_update, MainActivity::class.java)
                        nextIntent.putExtra("번호",3)
                        startActivity(nextIntent)
                    }
                })
            }).start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK
            && requestCode == PICK_IMAGE_REQUEST) {
            //display the photo on the imageview
            fileUri = data?.data
            Glide.with(this).load(fileUri).into(Talk_update_imageView)
            Talk_update_imageView_layout.visibility = View.VISIBLE
        }
        else if(resultCode == Activity.RESULT_OK
            && requestCode == PICK_PHOTO_REQUEST) {
            Glide.with(this).load(fileUri).into(Talk_update_imageView)
            Talk_update_imageView_layout.visibility = View.VISIBLE
        }
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
}
