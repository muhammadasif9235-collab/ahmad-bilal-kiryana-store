package com.example.ui.components

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private object DecodedDrawableCache {
    private val cache = ConcurrentHashMap<Int, ImageBitmap>()

    fun get(resId: Int): ImageBitmap? = cache[resId]

    fun put(resId: Int, bitmap: ImageBitmap) {
        cache[resId] = bitmap
    }
}

/**
 * Decodes a bundled raster drawable off the Main Thread with downsampling so the first frame's
 * SurfaceSyncGroup never stalls waiting on synchronous JPEG decoding or oversized GPU uploads.
 */
@Composable
fun AsyncBannerImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxWidthPx: Int = 720,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val cached = DecodedDrawableCache.get(resId)
    val imageBitmap by produceState<ImageBitmap?>(initialValue = cached, key1 = resId) {
        if (value == null) {
            val decoded = withContext(Dispatchers.IO) {
                decodeSampledDrawable(context.resources, resId, maxWidthPx)
            }
            if (decoded != null) {
                DecodedDrawableCache.put(resId, decoded)
                value = decoded
            }
        }
    }

    val bitmap = imageBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0A3A24), Color(0xFF0F5132), Color(0xFF196B45))
                )
            )
        )
    }
}

private fun decodeSampledDrawable(
    resources: Resources,
    @DrawableRes resId: Int,
    maxWidthPx: Int
): ImageBitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, resId, boundsOpts)
        var sampleSize = 1
        val srcWidth = boundsOpts.outWidth
        if (srcWidth > maxWidthPx && maxWidthPx > 0) {
            while ((srcWidth / (sampleSize * 2)) >= maxWidthPx) {
                sampleSize *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeResource(resources, resId, decodeOpts)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
