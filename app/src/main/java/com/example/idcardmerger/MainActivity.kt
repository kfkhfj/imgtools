package com.example.idcardmerger

import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.idcardmerger.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var frontUri: Uri? = null
    private var backUri: Uri? = null
    private val CARD_WIDTH_MM = 85.6f
    private val CARD_HEIGHT_MM = 54.0f

    private val pickFront = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> frontUri = uri; updatePreview() }

    private val pickBack = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> backUri = uri; updatePreview() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnPickFront.setOnClickListener { pickFront.launch("image/*") }
        binding.btnPickBack.setOnClickListener { pickBack.launch("image/*") }
        setupAdjustmentControls()
        binding.btnExportPdf.setOnClickListener { exportPdf() }
        binding.canvasView.setReferenceSizeMm(CARD_WIDTH_MM, CARD_HEIGHT_MM)
    }

    private fun setupAdjustmentControls() {
        binding.seekBrightness.setOnSeekBarChangeListener(createListener {
            binding.canvasView.setBrightness(it)
        })
        binding.seekContrast.setOnSeekBarChangeListener(createListener {
            binding.canvasView.setContrast(it)
        })
        binding.seekSharpness.setOnSeekBarChangeListener(createListener {
            binding.canvasView.setSharpness(it)
        })
    }

    private fun createListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
            if (fromUser) onChange(p)
        }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    }

    private fun updatePreview() {
        binding.canvasView.setImages(frontUri, backUri)
    }

    private fun exportPdf() {
        val bitmap = binding.canvasView.exportBitmap() ?: return
        PdfExporter.export(this, bitmap, "id_card_merged.pdf")
    }
}
