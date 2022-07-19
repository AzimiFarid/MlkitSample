package com.example.mlkitsample

import android.annotation.SuppressLint
import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var context: Context
    lateinit var messageTextView: TextView
    var left: Double = 0.0
    var top: Double = 0.0
    private var right: Double = 0.0
    private var bottom: Double = 0.0

    lateinit var cameraProvider: ProcessCameraProvider

    var savedUri = ""
    private lateinit var outputDirectory: File
    private val fileNameFormat = "yyyy-MM-dd-HH-mm-ss-SSS"

    private lateinit var containerFrameLayout: FrameLayout
    private lateinit var imageView: ImageView
    private lateinit var firstLinearLayout: LinearLayout
    private lateinit var secondLinearLayout: LinearLayout
    private lateinit var thirdLinearLayout: LinearLayout

    private val seriousUnicode = 0x1F610
    private val blinkUnicode = 0x1F611

    lateinit var selectedOperations :ArrayList<String>

    val stepCounter = MutableLiveData<Int>().apply { postValue(3) }

    private val  LIVE_DETECTION_OPERATIONS = arrayListOf("blink", "smile", "turnToRight",
        "turnToLeft", "turnToUp" )

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        containerFrameLayout = findViewById(R.id.container_frame_layout)
        imageView = findViewById(R.id.image_view)
        firstLinearLayout = findViewById(R.id.first_layout)
        secondLinearLayout = findViewById(R.id.second_layout)
        thirdLinearLayout = findViewById(R.id.third_layout)
        messageTextView = findViewById(R.id.message_text_view)

        messageTextView.text =
            resources.getString(R.string.take_camera_in_front_of_your_face) + "  " + getEmojiByUnicode(
                seriousUnicode
            )

        drawOval()
        startCamera()

    }

    private fun drawOval() {
        containerFrameLayout.doOnLayout { it ->
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


    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError", "SetTextI18n",
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
                                firstLinearLayout.setBackgroundColor(
                                    ContextCompat.getColor(
                                        this,
                                        R.color.purple_200
                                    )
                                )
                                //TODO : choose random operations
                                selectedOperations = getTripleOperations() as ArrayList<String>
                                stepCounter.observe(this) {
                                    when (stepCounter.value) {
                                        1 -> chooseFunctionFromFunctionName(
                                            selectedOperations[0],
                                            face
                                        )
                                        2 -> chooseFunctionFromFunctionName(
                                            selectedOperations[1],
                                            face
                                        )
                                        3 -> chooseFunctionFromFunctionName(
                                            selectedOperations[2],
                                            face
                                        )
                                    }
                                    if (stepCounter.value == 3) {
                                        Toast.makeText(this, "It is DONE!", Toast.LENGTH_LONG)
                                            .show()
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

    fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }


    @SuppressLint("SetTextI18n")
    private fun blink(face: Face) {
        if (face.rightEyeOpenProbability != null && face.rightEyeOpenProbability != null) {
            val rightEyeOpenProb = face.rightEyeOpenProbability
            val leftEyeOpenProb = face.leftEyeOpenProbability

            if (rightEyeOpenProb!! < 0.1 && leftEyeOpenProb!! < 0.1) {
                secondLinearLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.purple_200
                    )
                )
                messageTextView.text =
                    resources.getString(R.string.please_blink) + "   " + getEmojiByUnicode(
                        blinkUnicode
                    )


            }
        }
    }

    private fun smile(face: Face) {
        if (face.smilingProbability != null) {
            if (face.smilingProbability!! > 0.9) {
                thirdLinearLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.purple_200
                    )
                )
                Toast.makeText(this , "It is Ok!" , Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun turnToRight(face: Face) {

        if (face.headEulerAngleZ > 40) {
            Log.d("headEulerAngleZ >>> ", face.headEulerAngleZ.toString())
            thirdLinearLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.purple_200
                )
            )
            Toast.makeText(this, "It is OK!", Toast.LENGTH_LONG).show()


        }
    }

    private fun turnToLeft(face: Face) {

        if (face.headEulerAngleY > 40) {
            Log.d("headEulerAngleY >>> ", face.headEulerAngleY.toString())
            thirdLinearLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.purple_200
                )
            )
            Toast.makeText(this, "It is OK!", Toast.LENGTH_LONG).show()


        }
    }

    private fun turnToUp(face: Face) {

        if (face.headEulerAngleX > 40) {
            Log.d("headEulerAngleX >>> ", face.headEulerAngleX.toString())
            thirdLinearLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.purple_200
                )
            )
            Toast.makeText(this, "It is OK!", Toast.LENGTH_LONG).show()


        }
    }


    private fun chooseFunctionFromFunctionName(functionName : String , face: Face){
        stepCounter.value?.minus(1)
        when(functionName){
            LIVE_DETECTION_OPERATIONS[0] -> blink(face)
            LIVE_DETECTION_OPERATIONS[1] -> smile(face)
            LIVE_DETECTION_OPERATIONS[2] -> turnToRight(face)
            LIVE_DETECTION_OPERATIONS[3] -> turnToLeft(face)
            LIVE_DETECTION_OPERATIONS[4] -> turnToUp(face)
        }
    }

    private fun getTripleOperations() : MutableList<String> {
        val list : MutableList<String> = mutableListOf()
        var tempList = LIVE_DETECTION_OPERATIONS.toMutableList()
        for (i in 0..2){
            val selectedItem = tempList.random()
            list.add(selectedItem)
            tempList.remove(selectedItem)
        }
        return list
    }


}