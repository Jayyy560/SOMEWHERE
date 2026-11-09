package com.somewhere.app.ui.component

import android.view.MotionEvent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/** Own only this preview's use cases; never unbind another screen's camera. */
@Composable
fun CameraBackground(
    isActive: Boolean,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCameraControlCreated: (CameraControl) -> Unit,
    onCameraError: (String?) -> Unit = {},
    retryKey: Int = 0
) {
    if (!isActive) return
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val captureCallback by rememberUpdatedState(onImageCaptureCreated)
    val controlCallback by rememberUpdatedState(onCameraControlCreated)
    val errorCallback by rememberUpdatedState(onCameraError)
    PermissionGate(
        title = "Camera Access",
        description = "We need camera access to capture drops.",
        permissions = listOf(android.Manifest.permission.CAMERA)
    ) {
        key(retryKey, owner) {
            val binding = remember { CaptureCameraBinding() }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                onRelease = { binding.release() },
                factory = { ctx ->
                    val view = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    view.setOnTouchListener { touched, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val point = view.meteringPointFactory.createPoint(event.x, event.y)
                            binding.control?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                        }
                        if (event.action == MotionEvent.ACTION_UP) touched.performClick()
                        true
                    }
                    val future = ProcessCameraProvider.getInstance(context)
                    future.addListener(listener@{
                        if (binding.released) return@listener
                        try {
                            val provider = future.get()
                            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                            binding.provider = provider
                            binding.preview = preview
                            binding.capture = capture
                            val camera = provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                            binding.control = camera.cameraControl
                            captureCallback(capture)
                            controlCallback(camera.cameraControl)
                            errorCallback(null)
                        } catch (_: Exception) {
                            errorCallback("Camera is unavailable. Close other camera apps and retry.")
                        }
                    }, ContextCompat.getMainExecutor(context))
                    view
                }
            )
        }
    }
}

private class CaptureCameraBinding {
    var released = false
    var provider: ProcessCameraProvider? = null
    var preview: Preview? = null
    var capture: ImageCapture? = null
    var control: CameraControl? = null
    fun release() {
        released = true
        val cases = listOfNotNull(preview, capture).toTypedArray()
        if (cases.isNotEmpty()) provider?.unbind(*cases)
        control = null
    }
}
