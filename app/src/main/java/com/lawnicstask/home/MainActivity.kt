package com.lawnicstask.home


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lawnicstask.R
import com.lawnicstask.camera.CameraActivity
import com.lawnicstask.camera.model.ImageItems
import com.lawnicstask.databinding.ActivityMainBinding
import com.lawnicstask.home.adapter.ImagesAdapter

class MainActivity : AppCompatActivity(),View.OnClickListener {




    var binding : ActivityMainBinding? = null
    val permissions = arrayOf(Manifest.permission.CAMERA)
    val TAG = MainActivity::class.java.simpleName
    var imageadapter : ImagesAdapter? = null
    var db : FirebaseFirestore? = null
    val imageList =  ArrayList<ImageItems>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setUpToolbar()
        initializeViews()
        firestoreInit()



    }

    override fun onResume() {
        super.onResume()

        try {
            binding?.progressBar?.visibility = View.VISIBLE
            if (binding!=null && imageadapter!=null)
                loadData()
        }catch (e:Exception){

        }

    }

    private fun loadData() {
        db?.collection("imageItems")
            ?.get()
            ?.addOnSuccessListener {
                imageList.clear()
                it.forEach {
                    val uid = it["uniqueImageId"] as Long
                    val id = it["id"] as String
                    val imgUrl = it["imgUrl"] as String
                    val date = it["date"] as String
                    val type = it["imgType"] as String
                    val name = it["imageName"] as String
                    val username = it["userName"] as String
                    val pages = it["pages"] as Long
                    val time = it["uploadedTime"] as String
                    imageList.add(ImageItems(uid,id,name,type,imgUrl,date,username,pages,time))
                }
                if (it.size()==0){
                    binding?.noImageAvailable?.setText("No Uploaded found.\n Click camera button and Upload Instantly")
                    binding?.noImageAvailable?.visibility =View.VISIBLE
                }
                binding?.progressBar?.visibility = View.GONE
                imageadapter?.notifyDataSetChanged()
            }
    }

    private fun firestoreInit() {
        db = Firebase.firestore
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding?.activityMainToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun initializeViews() {
        binding?.fabCamera?.setOnClickListener(this)
        imageadapter = ImagesAdapter(this,imageList)
        binding?.rvImageList?.layoutManager = LinearLayoutManager(this@MainActivity,LinearLayoutManager.VERTICAL,false)
        binding?.rvImageList?.adapter = imageadapter

    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.fab_camera ->{

                Log.d(TAG,"hasNoPermission : ${hasNoPermissions()}")
                if (hasNoPermissions()) requestPermission()
                else goToCameraActivity()



            }
        }
    }

    private fun goToCameraActivity() {
        val intent = Intent(this,CameraActivity::class.java)
        startActivity(intent)
    }


    private fun hasNoPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(){
        ActivityCompat.requestPermissions(this, permissions,0)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar,menu)
        return true
    }




}