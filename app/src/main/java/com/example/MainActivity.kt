package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.delay
import java.io.File
import kotlin.random.Random

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppContent()
      }
    }
  }
}

@Composable
fun AppContent() {
  var introFinished by remember { mutableStateOf(false) }

  if (!introFinished) {
    GlitchIntroScreen(onFinished = { introFinished = true })
  } else {
    MainScannerScreen()
  }
}

@Composable
fun GlitchIntroScreen(onFinished: () -> Unit) {
  var textIndex by remember { mutableIntStateOf(0) }
  val glitchTexts = listOf(
    "INITIALIZING...",
    "ACCESSING KERNEL...",
    "BYPASSING SECURITY...",
    "LOADING DEDSEC OS...",
    "DEDSEC OS"
  )
  
  var xOffset by remember { mutableFloatStateOf(0f) }
  var yOffset by remember { mutableFloatStateOf(0f) }
  var colorState by remember { mutableStateOf(Color.Green) }

  LaunchedEffect(Unit) {
    for (i in glitchTexts.indices) {
      textIndex = i
      // Glitch effect loop for current text
      repeat(10) {
        xOffset = Random.nextInt(-10, 10).toFloat()
        yOffset = Random.nextInt(-10, 10).toFloat()
        colorState = if (Random.nextBoolean()) Color.Green else Color.Red
        delay(Random.nextLong(20, 80))
        xOffset = 0f
        yOffset = 0f
        colorState = Color.Green
        delay(Random.nextLong(20, 100))
      }
      delay(300)
    }
    delay(500)
    onFinished()
  }

  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(Color.Black),
      contentAlignment = Alignment.Center
  ) {
    Text(
        text = glitchTexts[textIndex],
        color = colorState,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.offset(xOffset.dp, yOffset.dp)
    )
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScannerScreen() {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(
                    onObjectsDetected = { objects ->
                        detectedObjects = objects
                    }
                )
                ScannerOverlay(detectedObjects)
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "AWAITING CAMERA ACCESS...",
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black)
                    ) {
                        Text(text = "GRANT ACCESS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(onObjectsDetected: (List<DetectedObject>) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    
    val objectAnalyzer = remember {
        try {
            val dir = File(context.filesDir, "com.google.mlkit.acceleration")
            if (!dir.exists()) {
                dir.mkdirs()
            }
        } catch (e: Exception) {
            Log.e("CameraPreview", "Failed to create mlkit dir", e)
        }
        
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
        val detector = ObjectDetection.getClient(options)
        
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        detector.process(image)
                            .addOnSuccessListener { detectedObjects ->
                                // For portrait, swap width and height
                                val imgWidth = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.width else imageProxy.height
                                val imgHeight = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.height else imageProxy.width
                                
                                val result = detectedObjects.map { obj ->
                                    val rect = obj.boundingBox
                                    val x = rect.left.toFloat() / imgWidth
                                    val y = rect.top.toFloat() / imgHeight
                                    val w = rect.width().toFloat() / imgWidth
                                    val h = rect.height().toFloat() / imgHeight
                                    
                                    val label = obj.labels.firstOrNull()?.text ?: "UNKNOWN_OBJECT"
                                    val conf = obj.labels.firstOrNull()?.confidence ?: 1.0f
                                    
                                    DetectedObject(label, x, y, w, h, conf)
                                }
                                onObjectsDetected(result)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }
            }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        objectAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScannerOverlay(detectedObjects: List<DetectedObject>) {
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Overlay Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Scan line
            val yPos = scanLineY * canvasHeight
            drawLine(
                color = Color.Green.copy(alpha = 0.5f),
                start = Offset(0f, yPos),
                end = Offset(canvasWidth, yPos),
                strokeWidth = 4f
            )

            // Draw bounding boxes around ML objects
            detectedObjects.forEach { obj ->
                val rectWidth = obj.width * canvasWidth
                val rectHeight = obj.height * canvasHeight
                val rectX = obj.x * canvasWidth
                val rectY = obj.y * canvasHeight
                
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(rectX, rectY),
                    size = Size(rectWidth, rectHeight),
                    style = Stroke(width = 4f)
                )
            }
            
            // Corner brackets
            val bracketSize = 40.dp.toPx()
            val bracketStroke = 4.dp.toPx()
            
            // Top Left
            drawLine(Color.Green, Offset(0f, 0f), Offset(bracketSize, 0f), bracketStroke)
            drawLine(Color.Green, Offset(0f, 0f), Offset(0f, bracketSize), bracketStroke)
            // Top Right
            drawLine(Color.Green, Offset(canvasWidth, 0f), Offset(canvasWidth - bracketSize, 0f), bracketStroke)
            drawLine(Color.Green, Offset(canvasWidth, 0f), Offset(canvasWidth, bracketSize), bracketStroke)
            // Bottom Left
            drawLine(Color.Green, Offset(0f, canvasHeight), Offset(bracketSize, canvasHeight), bracketStroke)
            drawLine(Color.Green, Offset(0f, canvasHeight), Offset(0f, canvasHeight - bracketSize), bracketStroke)
            // Bottom Right
            drawLine(Color.Green, Offset(canvasWidth, canvasHeight), Offset(canvasWidth - bracketSize, canvasHeight), bracketStroke)
            drawLine(Color.Green, Offset(canvasWidth, canvasHeight), Offset(canvasWidth, canvasHeight - bracketSize), bracketStroke)
        }

        // Object labels
        detectedObjects.forEach { obj ->
            Box(
                modifier = Modifier
                    .offset(
                        x = screenWidth * obj.x,
                        y = (screenHeight * obj.y) - 40.dp
                    )
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(4.dp)
            ) {
                Text(
                    text = "ID: ${obj.name}\nCONFIDENCE: ${(obj.confidence * 100).toInt()}%",
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // HUD Text
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp)
        ) {
            Text("DEDSEC_OS v3.2.1", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("MOTION DETECTION: ACTIVE", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("OBJECT RECOGNITION: ACTIVE (ML)", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("PROFILER: ONLINE", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("TARGETS FOUND: ${detectedObjects.size}", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

data class DetectedObject(
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val confidence: Float
)
