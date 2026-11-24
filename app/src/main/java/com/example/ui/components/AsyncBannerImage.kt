package com.example.ui.components

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private object DecodedDrawableCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(key: String): ImageBitmap? = cache[key]

    fun put(key: String, bitmap: ImageBitmap) {
        cache[key] = bitmap
    }
}

@Composable
fun AsyncBannerImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxWidthPx: Int = 720,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val cacheKey = "banner_$resId"
    val cached = DecodedDrawableCache.get(cacheKey)
    val imageBitmap by produceState<ImageBitmap?>(initialValue = cached, key1 = cacheKey) {
        if (value == null) {
            val decoded = withContext(Dispatchers.IO) {
                decodeSampledDrawable(context.resources, resId, maxWidthPx)
            }
            if (decoded != null) {
                DecodedDrawableCache.put(cacheKey, decoded)
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

/**
 * Displays the official Ahmad Bilal Kiryana Store circular emblem (or custom shopkeeper photo)
 * zoomed slightly with ContentScale.Crop so white outer padding disappears completely.
 */
@Composable
fun ShopLogoBadge(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    maxWidthPx: Int = 512,
    customPhotoPath: String? = null
) {
    val context = LocalContext.current
    val validCustomFile = customPhotoPath?.takeIf { it.isNotBlank() }?.let { path ->
        if (path.startsWith("content://") || path.startsWith("file://")) path
        else File(path).takeIf { it.exists() }?.absolutePath
    }
    val cacheKey = if (validCustomFile != null) "custom_$validCustomFile" else "logo_$resId"
    val cached = DecodedDrawableCache.get(cacheKey)
    val imageBitmap by produceState<ImageBitmap?>(initialValue = cached, key1 = cacheKey) {
        if (value == null) {
            val decoded = withContext(Dispatchers.IO) {
                if (validCustomFile != null) {
                    decodeCustomPhoto(context, validCustomFile, maxWidthPx)
                } else {
                    decodeSampledDrawable(
                        resources = context.resources,
                        resId = resId,
                        maxWidthPx = maxWidthPx,
                        preserveAlpha = true
                    )
                }
            }
            if (decoded != null) {
                DecodedDrawableCache.put(cacheKey, decoded)
                value = decoded
            }
        }
    }

    val bitmap = imageBitmap
    if (bitmap != null) {
        val zoomFactor = if (validCustomFile != null) 1.02f else 1.22f
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.graphicsLayer(
                scaleX = zoomFactor,
                scaleY = zoomFactor
            )
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF084C36))
        )
    }
}

private fun decodeCustomPhoto(
    context: android.content.Context,
    pathOrUri: String,
    maxWidthPx: Int
): ImageBitmap? {
    return try {
        val bytes = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
            context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use { it.readBytes() }
        } else {
            File(pathOrUri).takeIf { it.exists() }?.readBytes()
        } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        if (bounds.outWidth > maxWidthPx && maxWidthPx > 0) {
            while ((bounds.outWidth / (sample * 2)) >= maxWidthPx) sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun decodeSampledDrawable(
    resources: Resources,
    @DrawableRes resId: Int,
    maxWidthPx: Int,
    preserveAlpha: Boolean = false
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
            inPreferredConfig = if (preserveAlpha) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeResource(resources, resId, decodeOpts)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
