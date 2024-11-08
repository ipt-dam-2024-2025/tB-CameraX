package pt.ipt.dam.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pt.ipt.dam.camerax.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService

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




    private fun startCamera() { }

    private fun takePhoto() { }


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

}