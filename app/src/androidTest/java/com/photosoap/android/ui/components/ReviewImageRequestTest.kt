package com.photosoap.android.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.request.SuccessResult
import com.photosoap.android.domain.model.Photo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ReviewImageRequestTest {
    @Test fun displayReusesPrefetchedFullResolutionPhotoWithoutDecodingAgain(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "swipe-cache-test.jpg")
        val bitmap = Bitmap.createBitmap(4032, 3024, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(50, 120, 180))
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        val photo = Photo(1, file.toURI().toString(), file.name, "image/jpeg", 0, 0, file.length(), 4032, 3024)
        val loader = ImageLoader.Builder(context).build()
        try {
            val dimensions = IntSize(984, 1400)
            val start = SystemClock.elapsedRealtimeNanos()
            val preload = loader.execute(reviewImageRequest(context, photo, dimensions))
            val prepared = SystemClock.elapsedRealtimeNanos()
            val display = loader.execute(reviewImageRequest(context, photo, dimensions))
            val displayed = SystemClock.elapsedRealtimeNanos()
            assertTrue(preload is SuccessResult)
            assertTrue(display is SuccessResult)
            assertEquals(DataSource.MEMORY_CACHE, (display as SuccessResult).dataSource)
            assertSame((preload as SuccessResult).image, display.image)
            Log.i("SwipeImageQA", "12MP preload=${(prepared-start)/1_000_000}ms cachedDisplay=${(displayed-prepared)/1_000_000}ms")
        } finally {
            loader.shutdown()
            file.delete()
        }
    }
}
