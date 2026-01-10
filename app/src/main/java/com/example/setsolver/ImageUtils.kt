package com.example.setsolver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Utility class for image processing operations
 */
object ImageUtils {
    
    /**
     * Converts YUV_420_888 image data to a Bitmap
     */
    fun yuv420ToBitmap(data: ByteArray, width: Int, height: Int): Bitmap {
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    /**
     * Converts an Android Image to a Bitmap
     */
    fun imageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
