package com.example.idcardmerger

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PdfExporter {
    fun export(context: Context, bitmap: Bitmap, fileName: String) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val scaleX = 595f / bitmap.width
        val scaleY = 842f / bitmap.height
        val scale = min(scaleX, scaleY) * 0.9f
        val dx = (595f - bitmap.width * scale) / 2f
        val dy = (842f - bitmap.height * scale) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.restore()
        pdf.finishPage(page)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Documents/")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os -> pdf.writeTo(os) }
                }
            } else {
                @Suppress("DEPRECATION")
                val file = File(context.getExternalFilesDir(null), fileName)
                FileOutputStream(file).use { pdf.writeTo(it) }
            }
            Toast.makeText(context, "PDF已保存至Documents", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally { pdf.close() }
    }
}
