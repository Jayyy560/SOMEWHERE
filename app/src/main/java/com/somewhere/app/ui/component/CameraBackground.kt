package com.somewhere.app.ui.component

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CameraBackground(
    isActive: Boolean,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCameraControlCreated: (CameraControl) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    PermissionGate(
        title = "Camera Access",
        description = "We need camera access to capture drops.",
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.RECORD_AUDIO
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val factory = SurfaceOrientedMeteringPointFactory(
                                view.width.toFloat(), view.height.toFloat()
                            )
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            cameraControl?.startFocusAndMetering(action)
                        }
                        true
                    }
                    previewView
                },
                update = { previewView ->
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        if (!isActive) {
                            cameraProvider.unbindAll()
                            return@addListener
                        }

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                            
                        onImageCaptureCreated(capture)

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                            cameraControl = camera.cameraControl
                            onCameraControlCreated(camera.cameraControl)
                        } catch (e: Exception) {
                            Log.e("CameraBackground", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        }
    }
}
