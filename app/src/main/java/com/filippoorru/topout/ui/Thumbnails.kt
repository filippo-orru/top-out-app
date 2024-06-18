package com.filippoorru.topout.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi
import java.io.File
import kotlin.math.max

// Copied from ThumbnailUtils to support desiredTimestamp

fun createVideoThumbnail(file: File, size: Size, desiredTimestamp: Long?): Bitmap? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val resizer =
                Resizer(size, null)
            MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(file.absolutePath)
                // Try to retrieve thumbnail from metadata
                val raw = mmr.embeddedPicture
                if (raw != null) {
                    return ImageDecoder.decodeBitmap(ImageDecoder.createSource(raw), resizer)
                }

                val params = MediaMetadataRetriever.BitmapParams()
                params.preferredConfig = Bitmap.Config.ARGB_8888

                val width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!.toInt()
                val height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!.toInt()
                // Fall back to middle of video
                // Note: METADATA_KEY_DURATION unit is in ms, not us.
                val videoDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
                val timestamp = desiredTimestamp?.coerceIn(0, videoDuration) ?: (videoDuration / 2)
                val thumbnailTimeUs = timestamp * 1000

                // If we're okay with something larger than native format, just
                // return a frame without up-scaling it
                return if (size.width > width && size.height > height) {
                    mmr.getFrameAtTime(thumbnailTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, params)
                } else {
                    mmr.getScaledFrameAtTime(
                        thumbnailTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        size.width, size.height, params
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            return ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND)
        }
    } catch (e: RuntimeException) {
        //throw IOException("Failed to create thumbnail", e)
        return null
    }
}

@RequiresApi(Build.VERSION_CODES.P)
internal class Resizer(private val size: Size, private val signal: CancellationSignal?) : ImageDecoder.OnHeaderDecodedListener {
    override fun onHeaderDecoded(decoder: ImageDecoder, info: ImageDecoder.ImageInfo, source: ImageDecoder.Source) {
        // One last-ditch check to see if we've been canceled.
        signal?.throwIfCanceled()

        // We don't know how clients will use the decoded data, so we have
        // to default to the more flexible "software" option.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

        // We requested a rough thumbnail size, but the remote size may have
        // returned something giant, so defensively scale down as needed.
        val widthSample = info.size.width / size.width
        val heightSample = info.size.height / size.height
        val sample = max(widthSample.toDouble(), heightSample.toDouble()).toInt()
        if (sample > 1) {
            decoder.setTargetSampleSize(sample)
        }
    }
}
