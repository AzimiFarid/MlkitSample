package com.example.mlkitsample

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
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

    var savedUri = ""
    private lateinit var outputDirectory: File
    private val fileNameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"
    private lateinit var containerFrameLayout: FrameLayout
    private lateinit var imageView: ImageView
    private lateinit var firstLinearLayout: LinearLayout
    var steps = 0


    private val LIVE_DETECTION_OPERATIONS = arrayListOf(
        "blink", "smile", "turnToRight",
        "turnToLeft", "turnToUp"
    )


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        containerFrameLayout = findViewById(R.id.container_frame_layout)
        imageView = findViewById(R.id.image_view)
        firstLinearLayout = findViewById(R.id.first_layout)
        messageTextView = findViewById(R.id.message_text_view)

        messageTextView.text =
            resources.getString(R.string.take_camera_in_front_of_your_face)
        drawOval()
        startCamera()

    }

    private fun drawOval() {
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
        val selectedOperations = getTripleOperations() as ArrayList<String>


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
                            steps++
                            if (steps < 3) {
                                if (face.boundingBox.left < 0 || face.boundingBox.top < 0) {
                                    messageTextView.text =
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
                                    messageTextView.text = selectedOperations[0]

                                    //TODO : choose random operations
                                    when (steps) {

                                        1 -> {
                                            chooseFunctionFromFunctionName(
                                                selectedOperations[0],
                                                face
                                            )
                                            messageTextView.text = selectedOperations[1]

                                        }
                                        2 -> {
                                            chooseFunctionFromFunctionName(
                                                selectedOperations[1],
                                                face
                                            )
                                            messageTextView.text = selectedOperations[2]

                                        }
                                        3 -> {
                                            chooseFunctionFromFunctionName(
                                                selectedOperations[2],
                                                face
                                            )
                                            messageTextView.text = "Congrats! It is OK."

                                        }

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
    private fun blink(face: Face) {
        if (face.rightEyeOpenProbability != null && face.rightEyeOpenProbability != null) {
            val rightEyeOpenProb = face.rightEyeOpenProbability
            val leftEyeOpenProb = face.leftEyeOpenProbability

            if (rightEyeOpenProb!! < 0.1 && leftEyeOpenProb!! < 0.1) {
                Log.d("OPERATOR >>> ", "blink")
                Log.d("STEP >>> ",steps.toString())
                steps++
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun smile(face: Face) {
        if (face.smilingProbability != null) {
            if (face.smilingProbability!! > 0.9) {
                Log.d("OPERATOR >>> ", "smile")
                Log.d("STEP >>> ",steps.toString())
                steps++
            }
        }

    }


    @SuppressLint("SetTextI18n")
    private fun turnToRight(face: Face) {

        if (face.headEulerAngleZ > 40) {
            Log.d("OPERATOR >>> ", "turnToRight")
            Log.d("STEP >>> ",steps.toString())
            steps++
        }
    }

    @SuppressLint("SetTextI18n")
    private fun turnToLeft(face: Face) {

        if (face.headEulerAngleY > 40) {
            Log.d("OPERATOR >>> ","turnToLeft")
            Log.d("STEP >>> ",steps.toString())
            steps++

        }

    }

    @SuppressLint("SetTextI18n")
    private fun turnToUp(face: Face) {

        if (face.headEulerAngleX > 40) {
            Log.d("OPERATOR >>> ", "turnToUp")
            Log.d("STEP >>> ",steps.toString())
            steps++

        }
    }


    private fun chooseFunctionFromFunctionName(functionName: String, face: Face) {
        when (functionName) {
            LIVE_DETECTION_OPERATIONS[0] -> blink(face)
            LIVE_DETECTION_OPERATIONS[1] -> smile(face)
            LIVE_DETECTION_OPERATIONS[2] -> turnToRight(face)
            LIVE_DETECTION_OPERATIONS[3] -> turnToLeft(face)
            LIVE_DETECTION_OPERATIONS[4] -> turnToUp(face)
        }

    }

    private fun getTripleOperations(): MutableList<String> {
        val list: MutableList<String> = mutableListOf()
        val tempList = LIVE_DETECTION_OPERATIONS.toMutableList()
        for (i in 0..2) {
            val selectedItem = tempList.random()
            list.add(selectedItem)
            tempList.remove(selectedItem)
        }
        return list
    }

}