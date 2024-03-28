package com.selimcinar.artbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.selimcinar.artbook.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream
import java.lang.Exception

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitmap : Bitmap? =null
    private  lateinit var database: SQLiteDatabase

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityArtBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info.equals("new")){
            binding.artNameText.setText("")
            binding.artistNameText .setText("")
            binding.yearText.setText("")
            binding.saveButton.visibility=View.VISIBLE

        }
        else {
            binding.saveButton.visibility=View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val  cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.selectImage.setImageBitmap(bitmap)

            }
            cursor.close()
        }
    }

    fun saveClick(view: View) {
        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearText.text.toString()
        if (selectedBitmap!=null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream= ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)

            val byteArray = outputStream.toByteArray()

            try {
              //  database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB) ")

                val sqlString = "INSERT INTO arts(artname,artistname,year,image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)

                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

            }
            catch (e:Exception){
                e.printStackTrace()
            }

            val intent = Intent(this@ArtActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private  fun  makeSmallerBitmap(image:Bitmap,maximumSize:Int):Bitmap{
        var width = image.width
        var height= image.height

        // ölçekleme boyut azaltma resimde
        val bitmapRatio :Double = width.toDouble() / height.toDouble()
        if (bitmapRatio>1){
            //landscape
            width=maximumSize
            val scaledHeight = width/bitmapRatio
            height = scaledHeight.toInt()
        }
        else {
            //portrait
            height = maximumSize
            val scaledWidth = height*bitmapRatio
            width = scaledWidth.toInt()

        }

        return  Bitmap.createScaledBitmap(image,width,height,true)
    }



    fun imageClick(view: View){
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
            //android 33+ -> Read_Media_Images
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //buraya android işletim sistemi karar verir.
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                ) {
                    Snackbar.make(
                        binding.root,
                        "Permission needed for gallery",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give permission",View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }) .show()

                }
                else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else {
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
        else {
            //Android 32+ -> Read_External_Storage
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //buraya android işletim sistemi karar verir.
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    Snackbar.make(
                        binding.root,
                        "Permission needed for gallery",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give permission",View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }) .show()

                }
                else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            else {
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    private  fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if (result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult!=null){
                    //resim verisini al resme tıklanınca
                    val imageData = intentFromResult.data
             //       binding.selectImage.setImageURI(imageData)
            if (imageData!=null){


                try {
                    if (Build.VERSION.SDK_INT>=28){
                        val source = ImageDecoder.createSource(this@ArtActivity.contentResolver,imageData)
                        selectedBitmap=ImageDecoder.decodeBitmap(source)
                        binding.selectImage.setImageBitmap(selectedBitmap)
                    }
                    else {
                        selectedBitmap =MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                        binding.selectImage.setImageBitmap(selectedBitmap)
                    }

             }
                catch (e:Exception){
                    e.printStackTrace()
                }

                }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if (result){
                // permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
            else {
                //permission denied
                Toast.makeText(this@ArtActivity,"Permission needed!",Toast.LENGTH_LONG).show()
            }

        }
    }


}