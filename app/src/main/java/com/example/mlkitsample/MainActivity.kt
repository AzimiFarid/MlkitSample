package com.example.mlkitsample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.security.Permission
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var context: Context
    lateinit var messageTextView: TextView
    var left: Double = 0.0
    private var top: Double = 0.0
    private var right: Double = 0.0
    private var bottom: Double = 0.0
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var outputDirectory: File
    private val fileNameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
    private lateinit var containerFrameLayout: FrameLayout
    private lateinit var imageView: ImageView
    private lateinit var firstLinearLayout: LinearLayout
    private lateinit var actionsList: ArrayList<String>
    var savedUri = ""
    private var steps = 0
    private var message = MutableLiveData<String>()
    private var randomTripleActions: ArrayList<String>? = null
    private val actionSet = setOf(
        "rightBlink", "leftBlink", "smile", "turnToRight",
        "turnToLeft", "turnToUp"
    )


    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        containerFrameLayout = findViewById(R.id.container_frame_layout)
        imageView = findViewById(R.id.image_view)
        firstLinearLayout = findViewById(R.id.first_layout)
        messageTextView = findViewById(R.id.message_text_view)
        message.postValue(resources.getString(R.string.take_camera_in_front_of_your_face))
        message.observe(this) {
            messageTextView.text = message.value
        }
        actionsList = actionSet.toMutableList() as ArrayList<String>
        randomTripleActions = getTripleOperations() as ArrayList<String>
        setupPermission()
        showTheFrame()
        startCamera()

    }

    private fun showTheFrame() {
        containerFrameLayout.doOnLayout {
            val bitmap: Bitmap = Bitmap.createBitmap(
                it.measuredWidth,
                it.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            left = (it.measuredWidth * 0.125)
            top = (it.measuredHeight * 0.125)
            right = (it.measuredWidth * 0.875)
            bottom = (it.measuredHeight * 0.875)

            val shapeDrawable = ShapeDrawable(OvalShape())
            shapeDrawable.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            shapeDrawable.paint.style = Paint.Style.STROKE
            shapeDrawable.paint.color = ContextCompat.getColor(this, R.color.white)
            shapeDrawable.paint.strokeWidth = 5F
            shapeDrawable.draw(canvas)
            imageView.background = BitmapDrawable(resources, bitmap)
        }
    }


    @SuppressLint(
        "UnsafeExperimentalUsageError", "UnsafeOptInUsageError", "SetTextI18n",
        "RestrictedApi"
    )
    private fun startCamera() {
        val options = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        val detector = FaceDetection.getClient(options)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Used to bind the lifecycle of cameras to the lifecycle owner
        cameraProvider = cameraProviderFuture.get()

        val viewFinder: PreviewView = findViewById(R.id.viewFinder)

        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(viewFinder.createSurfaceProvider())
            }

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val imageCapture = ImageCapture.Builder()
            .build()

        outputDirectory = getOutputDirectory()

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                fileNameFormat, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        for (face in faces) {
                            if (steps < 4) {
                                if (face.boundingBox.left < 0 || face.boundingBox.top < 0) {
                                    message.value =
                                        resources.getString(R.string.put_your_face_in_the_frame)
                                } else {
                                    imageCapture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(this),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onError(exc: ImageCaptureException) {
                                                Log.e(
                                                    "",
                                                    "Photo capture failed: ${exc.message}",
                                                    exc
                                                )
                                            }

                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                savedUri =
                                                    Uri.fromFile(photoFile).toString()
                                            }
                                        })


                                    //TODO : choose random operations
                                    when (steps) {

                                        0 -> {
                                            message.value =
                                                chooseMessageText(randomTripleActions!![0])
                                            chooseFunctionFromFunctionName(
                                                randomTripleActions!![0],
                                                face
                                            )

                                        }
                                        1 -> {
                                            message.value =
                                                chooseMessageText(randomTripleActions!![1])
                                            chooseFunctionFromFunctionName(
                                                randomTripleActions!![1],
                                                face
                                            )

                                        }
                                        2 -> {
                                            message.value =
                                                chooseMessageText(randomTripleActions!![2])
                                            chooseFunctionFromFunctionName(
                                                randomTripleActions!![2],
                                                face
                                            )

                                        }

                                        3 -> message.value = "It is Done!"


                                    }
                                }
                            }

                            if (face.trackingId != null) {
                                face.trackingId
                            }
                        }
                        imageProxy.close()
                        Log.e("error: ", "run")
                    }
                    .addOnFailureListener { e ->
                        e.message?.let { Log.e("error: ", it) }
                    }
            }
        }

        cameraProvider.bindToLifecycle(
            this, cameraSelector, imageCapture, imageAnalysis, preview
        )
    }

    override fun onPause() {
        cameraProvider.unbindAll()
        super.onPause()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    @SuppressLint("SetTextI18n")
    private fun rightBlink(face: Face) {
        if (face.rightEyeOpenProbability != null && face.leftEyeOpenProbability != null) {
            val leftEyeOpenProb = face.leftEyeOpenProbability
            val rightEyeOpenProb = face.rightEyeOpenProbability
            if (rightEyeOpenProb!! < 0.1 && leftEyeOpenProb!! > 0.3) {
                Log.d("OPERATOR >>> ", "blink")
                Log.d("STEP >>> ", steps.toString())
                steps++
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun leftBlink(face: Face) {
        if (face.rightEyeOpenProbability != null && face.leftEyeOpenProbability != null) {
            val leftEyeOpenProb = face.leftEyeOpenProbability
            val rightEyeOpenProb = face.rightEyeOpenProbability
            if (leftEyeOpenProb!! < 0.1 && rightEyeOpenProb!! > 0.3) {
                Log.d("OPERATOR >>> ", "blink")
                Log.d("STEP >>> ", steps.toString())
                steps++
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun smile(face: Face) {
        if (face.smilingProbability != null) {
            if (face.smilingProbability!! > 0.9) {
                Log.d("OPERATOR >>> ", "smile")
                Log.d("STEP >>> ", steps.toString())
                steps++
            }
        }

    }


    @SuppressLint("SetTextI18n")
    private fun turnToRight(face: Face) {

        if (face.headEulerAngleZ > 35) {
            Log.d("OPERATOR >>> ", "turnToRight")
            Log.d("STEP >>> ", steps.toString())
            steps++
        }
    }

    @SuppressLint("SetTextI18n")
    private fun turnToLeft(face: Face) {

        if (face.headEulerAngleY > 40) {
            Log.d("OPERATOR >>> ", "turnToLeft")
            Log.d("STEP >>> ", steps.toString())
            steps++

        }

    }

    @SuppressLint("SetTextI18n")
    private fun turnToUp(face: Face) {

        if (face.headEulerAngleX > 40) {
            Log.d("OPERATOR >>> ", "turnToUp")
            Log.d("STEP >>> ", steps.toString())
            steps++

        }
    }


    private fun chooseFunctionFromFunctionName(functionName: String, face: Face) {
        when (functionName) {
            actionsList[0] -> rightBlink(face)
            actionsList[1] -> leftBlink(face)
            actionsList[2] -> smile(face)
            actionsList[3] -> turnToRight(face)
            actionsList[4] -> turnToLeft(face)
            actionsList[5] -> turnToUp(face)
        }

    }

    private fun getTripleOperations(): List<String> {
        val list: MutableList<String> = mutableListOf()
        val tempList = actionSet.toMutableList()
        for (i in 0..2) {
            val selectedItem = tempList[Random().nextInt(3)]
            list.add(selectedItem)
            tempList.remove(selectedItem)
        }
        return list
    }

    private fun chooseMessageText(operation: String): String {
        return when (operation) {
            actionsList[0] -> getString(R.string.please_blink_right_eye)
            actionsList[1] -> getString(R.string.please_blink_left_eye)
            actionsList[2] -> getString(R.string.please_smile)
            actionsList[3] -> getString(R.string.please_turn_your_neck_right)
            actionsList[4] -> getString(R.string.please_turn_your_neck_left)
            actionsList[5] -> getString(R.string.please_turn_your_neck_up)
            else -> {
                ""
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupPermission() {
        val permission = checkSelfPermission(Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makePermissionRequest()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun makePermissionRequest() {
        this.requestPermissions(arrayOf(Manifest.permission.CAMERA), 1917)

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1917 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
            AlertDialog.Builder(this)
                .setTitle("Alarm!")
                .setMessage("You have to enable Camera Permission to continue.")
                .setIcon(getDrawable(android.R.drawable.alert_dark_frame))
                .setCancelable(true)
                .show()
        }
    }

}