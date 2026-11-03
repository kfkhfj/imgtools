package com.example.idcardmerger

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class ImageEditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val frontMatrix = Matrix()
    private val backMatrix = Matrix()
    private var frontBitmap: Bitmap? = null
    private var backBitmap: Bitmap? = null
    private var refWidthMm = 85.6f
    private var refHeightMm = 54.0f
    private var pxPerMm = 1f
    private var brightness = 0
    private var contrast = 100
    private var sharpness = 0
    private var cropRect: RectF? = null
    private var isCropping = false
    private var activeMatrix: Matrix? = null
    private var lastX = 0f
    private var lastY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rulerPaint = Paint().apply {
        color = Color.RED; strokeWidth = 2f; style = Paint.Style.STROKE
        textSize = 28f; textAlign = Paint.Align.CENTER
    }

    init { pxPerMm = context.resources.displayMetrics.xdpi / 25.4f }

    fun setReferenceSizeMm(w: Float, h: Float) {
        refWidthMm = w; refHeightMm = h; invalidate()
    }

    fun setImages(front: Uri?, back: Uri?) {
        val loader = ImageLoader(context)
        CoroutineScope(Dispatchers.Main).launch {
            front?.let {
                val bmp = loader.execute(ImageRequest.Builder(context).data(it).build()).drawable?.toBitmap()
                frontBitmap = bmp; resetMatrix(frontMatrix, bmp, 0f)
            }
            back?.let {
                val bmp = loader.execute(ImageRequest.Builder(context).data(it).build()).drawable?.toBitmap()
                backBitmap = bmp; resetMatrix(backMatrix, bmp, height / 2f)
            }
            invalidate()
        }
    }

    private fun resetMatrix(matrix: Matrix, bmp: Bitmap?, yOffset: Float) {
        bmp ?: return; matrix.reset()
        val scale = (width * 0.45f) / max(bmp.width, bmp.height)
        matrix.postScale(scale, scale)
        matrix.postTranslate(width * 0.25f, yOffset + 50f)
    }

    fun setBrightness(v: Int) { brightness = v; invalidate() }
    fun setContrast(v: Int) { contrast = v; invalidate() }
    fun setSharpness(v: Int) { sharpness = v; invalidate() }
    fun startCrop(rect: RectF) { cropRect = rect; isCropping = true; invalidate() }

    fun exportBitmap(): Bitmap? {
        val w = width; val h = height
        if (w == 0 || h == 0) return null
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawOnCanvas(Canvas(out), forExport = true)
        return out
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); drawOnCanvas(canvas, forExport = false)
    }

    private fun drawOnCanvas(canvas: Canvas, forExport: Boolean) {
        canvas.drawColor(Color.WHITE)
        if (!forExport) drawRuler(canvas)
        val cf = ColorMatrixColorFilter(floatArrayOf(
            contrast / 100f, 0f, 0f, 0f, brightness.toFloat(),
            0f, contrast / 100f, 0f, 0f, brightness.toFloat(),
            0f, 0f, contrast / 100f, 0f, brightness.toFloat(),
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = cf
        frontBitmap?.let { canvas.drawBitmap(it, frontMatrix, paint) }
        backBitmap?.let { canvas.drawBitmap(it, backMatrix, paint) }
        if (!forExport && isCropping) {
            cropRect?.let {
                val cp = Paint().apply {
                    color = Color.GREEN; strokeWidth = 3f; style = Paint.Style.STROKE
                }
                canvas.drawRect(it, cp)
            }
        }
    }

    private fun drawRuler(canvas: Canvas) {
        val stepPx = pxPerMm * 10
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, 20f, rulerPaint)
            canvas.drawText("${(x / pxPerMm).toInt()}mm", x, 45f, rulerPaint)
            x += stepPx
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, 20f, y, rulerPaint)
            y += stepPx
        }
        val rw = refWidthMm * pxPerMm
        val rh = refHeightMm * pxPerMm
        val rp = Paint().apply {
            color = Color.BLUE; strokeWidth = 2f; style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }
        canvas.drawRect(50f, 50f, 50f + rw, 50f + rh, rp)
        canvas.drawText("85.6×54.0 mm", 50f + rw / 2, 45f, rulerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y
                activeMatrix = if (event.y < height / 2f) frontMatrix else backMatrix
            }
            MotionEvent.ACTION_MOVE -> {
                activeMatrix?.postTranslate(event.x - lastX, event.y - lastY)
                lastX = event.x; lastY = event.y; invalidate()
            }
        }
        return true
    }
}
