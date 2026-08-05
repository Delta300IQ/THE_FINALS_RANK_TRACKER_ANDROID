package com.example.finalsranktracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import android.app.Activity
import android.view.ViewGroup
import java.io.File
import java.io.FileOutputStream

internal fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "wrapped_season.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Season Wrapped"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

internal fun generateWrappedBitmap(
    context: Context,
    entries: List<RankEntry>,
    palette: Palette,
    isEnglish: Boolean,
    playerProfile: PlayerProfile?,
    season: Int,
    onBitmapGenerated: (Bitmap) -> Unit
) {
    val activity = context as? Activity ?: return
    val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

    val width = 1080
    val height = 1920

    val composeView = androidx.compose.ui.platform.ComposeView(context).apply {
        // Use INVISIBLE so it doesn't show up, but still gets measured/laid out
        visibility = View.INVISIBLE
        setContent {
            WrappedUI(
                entries = entries,
                palette = palette,
                isEnglish = isEnglish,
                playerProfile = playerProfile,
                season = season
            )
        }
    }

    val layoutParams = ViewGroup.LayoutParams(width, height)
    rootView.addView(composeView, layoutParams)

    // Wait a bit for compose to render and images to load
    composeView.postDelayed({
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Draw background explicitly to avoid transparent artifacts if any
            canvas.drawColor(android.graphics.Color.BLACK)
            composeView.draw(canvas)
            onBitmapGenerated(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            rootView.removeView(composeView)
        }
    }, 800)
}
