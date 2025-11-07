package com.movieroulette.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    /**
     * Compress an image URI to a ByteArray
     * @param context Android context
     * @param uri Image URI from gallery picker
     * @param maxSizeKB Maximum size in KB (default 500KB)
     * @param quality Initial JPEG quality (default 90)
     * @return Compressed image as ByteArray
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        maxSizeKB: Int = 500,
        quality: Int = 90
    ): Result<ByteArray> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return Result.failure(Exception("No se pudo abrir la imagen"))
            }

            // Decode image
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return Result.failure(Exception("Formato de imagen no válido"))
            }

            // Fix orientation
            val rotatedBitmap = fixOrientation(context, uri, originalBitmap)

            // Resize if too large
            val resizedBitmap = resizeIfNeeded(rotatedBitmap, 1200, 1200)

            // Compress to target size
            var currentQuality = quality
            var compressedData: ByteArray

            do {
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                compressedData = outputStream.toByteArray()
                currentQuality -= 5
            } while (compressedData.size > maxSizeKB * 1024 && currentQuality > 10)

            // Clean up
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
            resizedBitmap.recycle()
            originalBitmap.recycle()

            Result.success(compressedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resize bitmap if it exceeds max dimensions
     */
    private fun resizeIfNeeded(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
