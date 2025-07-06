package com.yelp.cursair.presentation.common

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * A "dumb" composable that either shows the camera preview or a permission request button.
 * It does NOT manage its own state.
 */
@Composable
fun QRCodeScannerView(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQRCodeScanned: (String) -> Unit
) {
    if (hasPermission) {
        CameraPreview(onQRCodeScanned = onQRCodeScanned)
    } else {
        PermissionRequestScreen(onGrantPermissionClick = onRequestPermission)
    }
}

@Composable
private fun PermissionRequestScreen(onGrantPermissionClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Camera permission is required to scan the QR code.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = onGrantPermissionClick) {
                Text("Grant Permission")
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onQRCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                        val scanner = BarcodeScanning.getClient(options)
                        val image = imageProxy.image
                        if (image != null) {
                            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        barcodes.firstNotNullOfOrNull { it.rawValue }?.let { qrValue ->
                                            onQRCodeScanned(qrValue)
                                            cameraProvider.unbindAll()
                                        }
                                    }
                                }
                                .addOnFailureListener { e -> Log.e("QRCodeScanner", "Error scanning", e) }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("QRCodeScanner", "Camera bind failed", e)
            }
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}