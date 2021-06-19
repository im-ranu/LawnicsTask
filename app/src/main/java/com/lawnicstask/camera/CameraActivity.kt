package com.lawnicstask.camera

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.format.DateFormat.*
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.lawnicstask.R
import com.lawnicstask.camera.model.ImageItems
import com.lawnicstask.databinding.ActivityCameraBinding

import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


class CameraActivity : AppCompatActivity(),View.OnClickListener {


    var binding : ActivityCameraBinding?= null
    val TAG = CameraActivity::class.java.simpleName
    var isCameraActive = true


    var db : FirebaseFirestore? = null
    var storage : FirebaseStorage? = null
    var imagesRef: StorageReference? = null
    var storageRef : StorageReference? = null
    var imageBytes : ByteArray? = null
    var imageTimeStamp : Long = 0L

    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    var imageCapture : ImageCapture? = null


        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera)
        initializationViews()


            firebaseInitialization()


        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)

        },ContextCompat.getMainExecutor(this))
    }

    private fun firebaseInitialization() {


        db = Firebase.firestore
        storage = Firebase.storage
        storageRef = storage!!.reference

        imagesRef = storageRef!!



    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider?) {

        val preview : Preview = Preview.Builder()
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(binding?.viewFinder?.surfaceProvider)

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(binding?.viewFinder?.display?.rotation!!)
            .build()

       val camera = cameraProvider?.bindToLifecycle(this as LifecycleOwner, cameraSelector,imageCapture, preview)




    }

    private fun initializationViews() {
        binding?.icCameraActivityBack?.setOnClickListener(this)
        binding?.icCaptureImage?.setOnClickListener(this)
        binding?.llIcDone?.setOnClickListener(this)
    }



    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ic_camera_activity_back->{
                if (isCameraActive)
                finish()
                else{
                    binding?.ivCapturedImage?.visibility = View.GONE
                    binding?.ivCapturedImage?.setImageDrawable(null)
                    binding?.viewFinder?.visibility = View.VISIBLE
                    isCameraActive = true

                }
            }
            R.id.ic_capture_image->{
//                captureImage()

                if (isCameraActive)
                onClick()
            }
            R.id.ll_icDone->{

                if (!isCameraActive){
                    uploadImage(imageBytes!!,imageTimeStamp)
                }
            }
        }
    }

    fun onClick() {
        val imageFile = createTempFile("anImage", ".jpg")

// Create the output file option from the above file
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

// Initiate image capture

        imageCapture?.takePicture(ContextCompat.getMainExecutor(this),
                        object : ImageCapture.OnImageCapturedCallback(){
                            override fun onCaptureSuccess(image: ImageProxy) {
                                binding?.ivCapturedImage?.setImageBitmap(imageProxyToBitmap(image));
                                binding?.ivCapturedImage?.rotation = image.imageInfo.rotationDegrees.toFloat()
                                binding?.ivCapturedImage?.visibility = View.VISIBLE
//                                cameraProviderFuture.isDone
                                binding?.viewFinder?.visibility = View.GONE
                                isCameraActive = false
                                image.close()
                            }
                        })

    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        imageBytes = bytes
        imageTimeStamp = image.imageInfo.timestamp
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    private fun uploadImage(bytes: ByteArray, imageTimeStamp: Long) {


        val taskEditText = EditText(this)
        val dialog: AlertDialog = AlertDialog.Builder(this)
            .setTitle("Enter Image Name")
            .setMessage("So we can easily know this belongs to you !")
            .setView(taskEditText)
            .setPositiveButton("Add") { dialog, which ->
                val task = taskEditText.text.toString()
                if (bytes.isNotEmpty()) {
                    val progressDialog = ProgressDialog(this)
                    progressDialog.setTitle("Uploading...")
                    progressDialog.show()
                    val ref: StorageReference =
                        storageRef!!.child("captured-images/$task $imageTimeStamp.png")
                    ref.putBytes(bytes)
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(this@CameraActivity, "Uploaded", Toast.LENGTH_SHORT)
                                .show()


                            ref.downloadUrl.addOnSuccessListener {
                                saveIntoRemoteDatabase(ref,imageTimeStamp,task,it.toString())
                            }



                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@CameraActivity,
                                "Failed " + e.message,
                                Toast.LENGTH_SHORT
                            )
                                .show()

                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot
                                .totalByteCount
                            progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
                        }


                }

            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()

    }

    private fun saveIntoRemoteDatabase(
        ref: StorageReference,
        imageTimeStamp: Long,
        task: String,
        downloadedUrl: String
    ) {

        val imageItems = ImageItems()
        imageItems.apply {
            this.date = getDateFromTimeStamp(Calendar.getInstance().timeInMillis).toString()
            this.imageName = task
            this.uniqueImageId =  Random().nextInt(9999).toLong()
            this.imgUrl = downloadedUrl
            this.id = UUID.randomUUID().toString()
            this.imgType = "A4"
            this.userName = "Lawnics"
            this.pages = Random().nextInt(10).toLong()
            this.uploadedTime = getTimeFromTimeStamp(Calendar.getInstance().timeInMillis)
        }
        db?.collection("imageItems")
            ?.add(imageItems)
            ?.addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            ?.addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    private fun getTimeFromTimeStamp(timeInMillis: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm aa", Locale.ENGLISH)
        val timeString: String = dateFormat.format(timeInMillis)
        return timeString
    }


    private fun getDateFromTimeStamp(time: Long): String? {
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = time
        cal.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return format("dd MMMM yyyy", cal).toString()
    }
}