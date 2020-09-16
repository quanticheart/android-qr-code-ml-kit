@file:Suppress("DEPRECATION")

package com.quanticheart.qrcodezxing

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.internal.Objects
import com.quanticheart.qrcodezxing.barcodeConfirminGraphic.BarcodeProcessor
import com.quanticheart.qrcodezxing.custonView.bottomSheetResult.BarcodeResultFragment
import com.quanticheart.qrcodezxing.custonView.bottomSheetResult.BarcodeResultViewModel
import com.quanticheart.qrcodezxing.custonView.bottomSheetResult.adapter.BarcodeField
import com.quanticheart.qrcodezxing.custonView.camera.CameraSource
import com.quanticheart.qrcodezxing.utils.Utils
import kotlinx.android.synthetic.main.activity_live_barcode_kotlin.*
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val tag = "LiveBarcodeActivity"
    private var cameraSource: CameraSource? = null
    private var promptChipAnimator: AnimatorSet? = null
    private var barcodeResultViewModel: BarcodeResultViewModel? = null
    private var currentBarcodeResultViewState: BarcodeResultViewModel.WorkflowState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_barcode_kotlin)

        camera_preview_graphic_overlay.apply {
            cameraSource =
                CameraSource(this)
        }

        close_button.setOnClickListener {
            onBackPressed()
        }

        flash_button.setOnClickListener {
            if (it.isSelected) {
                it.isSelected = false
                cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
            } else {
                it.isSelected = true
                cameraSource!!.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
            }
        }

        promptChipAnimator = (AnimatorInflater.loadAnimator(
            this,
            R.animator.bottom_prompt_chip_enter
        ) as AnimatorSet).apply {
            setTarget(bottom_prompt_chip)
        }

        setUpResultViewModel()
    }

    /**
     * Camera Status
     */

    private fun startCameraPreview() {
        val workflowModel = this.barcodeResultViewModel ?: return
        val cameraSource = this.cameraSource ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                camera_preview?.start(cameraSource)
            } catch (e: IOException) {
                Log.e(tag, "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        val workflowModel = this.barcodeResultViewModel ?: return
        if (workflowModel.isCameraLive) {
            workflowModel.markCameraFrozen()
            flash_button?.isSelected = false
            camera_preview?.stop()
        }
    }

    /**
     * BottomSheet Result ViewModel
     */
    private fun setUpResultViewModel() {
        barcodeResultViewModel = ViewModelProviders.of(this).get(BarcodeResultViewModel::class.java)

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        barcodeResultViewModel!!.workflowState.observe(this, Observer { workflowState ->
            if (workflowState == null || Objects.equal(currentBarcodeResultViewState, workflowState)
            ) {
                return@Observer
            }

            currentBarcodeResultViewState = workflowState
            Log.d(tag, "Current workflow state: ${currentBarcodeResultViewState!!.name}")

            val wasPromptChipGone = bottom_prompt_chip?.visibility == View.GONE

            when (workflowState) {
                BarcodeResultViewModel.WorkflowState.DETECTING -> {
                    bottom_prompt_chip?.visibility = View.VISIBLE
                    bottom_prompt_chip?.setText(R.string.prompt_point_at_a_barcode)
                    startCameraPreview()
                }
                BarcodeResultViewModel.WorkflowState.CONFIRMING -> {
                    bottom_prompt_chip?.visibility = View.VISIBLE
                    bottom_prompt_chip?.setText(R.string.prompt_move_camera_closer)
                    startCameraPreview()
                }
                BarcodeResultViewModel.WorkflowState.SEARCHING -> {
                    bottom_prompt_chip?.visibility = View.VISIBLE
                    bottom_prompt_chip?.setText(R.string.prompt_searching)
                    stopCameraPreview()
                }
                BarcodeResultViewModel.WorkflowState.DETECTED, BarcodeResultViewModel.WorkflowState.SEARCHED -> {
                    bottom_prompt_chip?.visibility = View.GONE
                    stopCameraPreview()
                }
                else -> bottom_prompt_chip?.visibility = View.GONE
            }

            val shouldPlayPromptChipEnteringAnimation =
                wasPromptChipGone && bottom_prompt_chip?.visibility == View.VISIBLE
            promptChipAnimator?.let {
                if (shouldPlayPromptChipEnteringAnimation && !it.isRunning) it.start()
            }
        })

        barcodeResultViewModel?.detectedBarcode?.observe(this, Observer { barcode ->
            if (barcode != null) {
                val barcodeFieldList = ArrayList<BarcodeField>()
                barcodeFieldList.add(BarcodeField("Raw Value", barcode.rawValue ?: ""))
                BarcodeResultFragment.show(supportFragmentManager, barcodeFieldList)
            }
        })
    }

    /**
     * Activity's Status
     */

    override fun onResume() {
        super.onResume()

        barcodeResultViewModel?.markCameraFrozen()
        currentBarcodeResultViewState = BarcodeResultViewModel.WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(
            BarcodeProcessor(camera_preview_graphic_overlay, barcodeResultViewModel!!)
        )
        barcodeResultViewModel?.setWorkflowState(BarcodeResultViewModel.WorkflowState.DETECTING)

        if (!Utils.allPermissionsGranted(this)) {
            Utils.requestRuntimePermissions(this)
        }
    }

    override fun onPause() {
        super.onPause()
        currentBarcodeResultViewState = BarcodeResultViewModel.WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
    }

    override fun onPostResume() {
        super.onPostResume()
        BarcodeResultFragment.dismiss(supportFragmentManager)
    }
}
