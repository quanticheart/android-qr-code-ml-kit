/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quanticheart.qrcodezxing.barcodeConfirminGraphic

import android.animation.ValueAnimator
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.quanticheart.qrcodezxing.camera.CameraReticleAnimator
import com.quanticheart.qrcodezxing.camera.FrameProcessorBase
import com.quanticheart.qrcodezxing.custonView.bottomSheetResult.BarcodeResultViewModel
import com.quanticheart.qrcodezxing.custonView.camera.GraphicOverlay
import com.quanticheart.qrcodezxing.custonView.camera.effects.BarcodeLoadingGraphic
import com.quanticheart.qrcodezxing.custonView.camera.effects.RippleEffectCamera
import java.io.IOException

/** A processor to run the barcode detector.  */
class BarcodeProcessor(
    graphicOverlay: GraphicOverlay,
    private val barcodeResultViewModel: BarcodeResultViewModel
) : FrameProcessorBase<List<FirebaseVisionBarcode>>() {

    private val detector = FirebaseVision.getInstance().visionBarcodeDetector
    private val cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(graphicOverlay)

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionBarcode>> =
        detector.detectInImage(image)

    @MainThread
    override fun onSuccess(
        image: FirebaseVisionImage,
        results: List<FirebaseVisionBarcode>,
        graphicOverlay: GraphicOverlay
    ) {

        if (!barcodeResultViewModel.isCameraLive) return

        Log.d(TAG, "Barcode result size: ${results.size}")

        // Picks the barcode, if exists, that covers the center of graphic overlay.

        val barcodeInCenter = results.firstOrNull { barcode ->
            val boundingBox = barcode.boundingBox ?: return@firstOrNull false
            val box = graphicOverlay.translateRect(boundingBox)
            box.contains(graphicOverlay.width / 2f, graphicOverlay.height / 2f)
        }

        graphicOverlay.clear()
        if (barcodeInCenter == null) {
            cameraReticleAnimator.start()
            graphicOverlay.add(
                RippleEffectCamera(
                    graphicOverlay,
                    cameraReticleAnimator
                )
            )
            barcodeResultViewModel.setWorkflowState(BarcodeResultViewModel.WorkflowState.DETECTING)
        } else {
            cameraReticleAnimator.cancel()
            // Barcode size in the camera view is sufficient.
            val loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter)
            loadingAnimator.start()
            graphicOverlay.add(
                BarcodeLoadingGraphic(
                    graphicOverlay,
                    loadingAnimator
                )
            )
            barcodeResultViewModel.setWorkflowState(BarcodeResultViewModel.WorkflowState.SEARCHING)
        }
        graphicOverlay.invalidate()
    }

    private fun createLoadingAnimator(
        graphicOverlay: GraphicOverlay,
        barcode: FirebaseVisionBarcode
    ): ValueAnimator {
        val endProgress = 1.1f
        return ValueAnimator.ofFloat(0f, endProgress).apply {
            duration = 2000
            addUpdateListener {
                if ((animatedValue as Float).compareTo(endProgress) >= 0) {
                    graphicOverlay.clear()
                    barcodeResultViewModel.setWorkflowState(BarcodeResultViewModel.WorkflowState.SEARCHED)
                    barcodeResultViewModel.detectedBarcode.setValue(barcode)
                } else {
                    graphicOverlay.invalidate()
                }
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Barcode detection failed!", e)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close barcode detector!", e)
        }
    }

    companion object {
        private const val TAG = "BarcodeProcessor"
    }
}
