package com.myiptv.clean.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

/** Creates a branded vertical SamPlay image and a QR that opens Shorts in the app. */
internal fun createShortPromoCard(context: Context, title: String, clipId: String): Uri {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(android.graphics.Color.rgb(11, 12, 20))
    paint.shader = android.graphics.LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        intArrayOf(android.graphics.Color.rgb(112, 26, 66), android.graphics.Color.rgb(37, 29, 90), android.graphics.Color.rgb(11, 12, 20)),
        null, android.graphics.Shader.TileMode.CLAMP
    )
    canvas.drawRoundRect(RectF(42f, 42f, 1038f, 1878f), 56f, 56f, paint)
    paint.shader = null

    paint.color = android.graphics.Color.WHITE
    paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    paint.textSize = 96f
    canvas.drawText("SamPlay", 90f, 178f, paint)
    paint.color = android.graphics.Color.rgb(255, 91, 133)
    paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    paint.textSize = 36f
    canvas.drawText("SHORTS · MOMENTOS QUE ENGANCHAN", 94f, 238f, paint)

    // Decorative playback mark gives the card a recognizable Shorts identity.
    paint.color = android.graphics.Color.rgb(255, 65, 113)
    canvas.drawRoundRect(RectF(90f, 350f, 990f, 1030f), 42f, 42f, paint)
    paint.color = android.graphics.Color.WHITE
    val triangle = android.graphics.Path().apply {
        moveTo(475f, 555f); lineTo(475f, 825f); lineTo(700f, 690f); close()
    }
    canvas.drawPath(triangle, paint)

    paint.color = android.graphics.Color.WHITE
    paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    paint.textSize = 66f
    val safeTitle = title.trim().ifBlank { "Descubre este Short" }.take(24)
    canvas.drawText(safeTitle, 94f, 1160f, paint)
    paint.color = android.graphics.Color.rgb(212, 213, 226)
    paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    paint.textSize = 39f
    canvas.drawText("Escanea y sigue disfrutando en", 94f, 1230f, paint)
    paint.color = android.graphics.Color.rgb(255, 91, 133)
    paint.typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
    canvas.drawText("SamPlay", 94f, 1288f, paint)

    val target = Uri.Builder().scheme("samplay").authority("shorts")
        .appendQueryParameter("clip", clipId).build().toString()
    val qrMatrix = QRCodeWriter().encode(target, BarcodeFormat.QR_CODE, 380, 380)
    val qr = Bitmap.createBitmap(380, 380, Bitmap.Config.ARGB_8888)
    for (y in 0 until 380) for (x in 0 until 380) {
        qr.setPixel(x, y, if (qrMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    paint.color = android.graphics.Color.WHITE
    canvas.drawRoundRect(RectF(94f, 1370f, 530f, 1806f), 26f, 26f, paint)
    canvas.drawBitmap(qr, 122f, 1398f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 34f
    paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    canvas.drawText("ESCANEA EL CÓDIGO", 580f, 1510f, paint)
    paint.color = android.graphics.Color.rgb(202, 204, 221)
    paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    paint.textSize = 30f
    canvas.drawText("Abre SamPlay y descubre", 580f, 1570f, paint)
    canvas.drawText("más videos para ti.", 580f, 1615f, paint)
    paint.color = android.graphics.Color.rgb(255, 91, 133)
    paint.textSize = 28f
    canvas.drawText("@SamPlay  ·  samplay", 94f, 1850f, paint)

    val folder = File(context.cacheDir, "samplay-share").apply { mkdirs() }
    val file = File(folder, "short-${clipId.hashCode().toUInt().toString(16)}.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    qr.recycle()
    return FileProvider.getUriForFile(context, "${context.packageName}.shares", file)
}
