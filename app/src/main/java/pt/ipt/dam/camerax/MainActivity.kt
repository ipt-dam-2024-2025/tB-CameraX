package pt.ipt.dam.camerax

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pt.ipt.dam.camerax.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val s = "Use case binding failed"

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
     /*
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        } */
        viewBinding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // ask for permissions to access camera
        if(allPermissionsGranted()){
            startCamera()
        }
        else{
            requestPermissions()
        }

        // read the 'click' event of the button
        // findViewById<Button>(R.id.image_capture_button).setOnClickListener {  }
        viewBinding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }

        // Configure the access to camera
        // we are using the Singleton pattern
        cameraExecutor=Executors.newSingleThreadExecutor()
    }


    /**
     * define if all permissions has been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    /**
     * ask for permissions
     */
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }


    /**
     * if it is necessary to ask for permissions,
     * this will evaluate the answers provided by user
     * and start the camera, or inform user that it can not use the camera
     */
    private val activityResultLauncher =
        registerForActivityResult(  ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->

            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                // test for all types of permissions
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    R.string.permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }




    /**
     * start the camera
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * function to take the photo and save it to storage
     * it must define the name and location of photo
     */
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create a time stamped name
        // import java.text.SimpleDateFormat
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.UK).format(System.currentTimeMillis())
        // define the type of image, and where it should be stored
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Images")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "${R.string.photo_ok}: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG, msg)
                }
            })
    }



    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                // import android.Manifest
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }


    /**
     * at end, destroy the 'cameraExecutor' object
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}