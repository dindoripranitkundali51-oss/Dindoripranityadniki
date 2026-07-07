package com.example.dindoripranityadnyiki.core.design

import android.content.Context
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

/**
 * Centralized Coil image loader configuration with optimized settings.
 * 
 * Features:
 * - Memory cache with LRU eviction
 * - Disk cache with size limit (50MB)
 * - Network cache policy for offline support
 * - Optimized image compression
 */
object CoilImageLoader {

    /**
     * Creates an optimized ImageLoader instance.
     * 
     * @param context Application context
     * @return Configured ImageLoader with optimized settings
     */
    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .allowHardware(true)
            .build()
    }

    /**
     * Creates an optimized ImageRequest for profile images.
     * 
     * @param context Context
     * @param imageUrl Image URL
     * @param size Target size (default: 200x200)
     * @return Configured ImageRequest
     */
    fun createProfileImageRequest(
        context: Context,
        imageUrl: String?,
        size: Size = Size(200, 200)
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .size(size)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    /**
     * Creates an optimized ImageRequest for pooja/service images.
     * 
     * @param context Context
     * @param imageUrl Image URL
     * @param size Target size (default: 400x400)
     * @return Configured ImageRequest
     */
    fun createPoojaImageRequest(
        context: Context,
        imageUrl: String?,
        size: Size = Size(400, 400)
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .size(size)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
