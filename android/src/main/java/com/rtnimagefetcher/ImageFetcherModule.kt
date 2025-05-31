package com.rtnimagefetcher

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap

// Import the generated Spec interface
import com.rtnimagefetcher.NativeImageFetcherSpec

class ImageFetcherModule(private val reactContext: ReactApplicationContext) : NativeImageFetcherSpec(reactContext) {

    companion object {
        const val NAME = "RTNImageFetcher"
    }

    override fun getName() = NAME

    @ReactMethod
    override fun getPhotos(options: ReadableMap, promise: Promise) {
        // Extract options
        val limit = if (options.hasKey("limit")) options.getInt("limit") else 20
        val offset = if (options.hasKey("offset")) options.getInt("offset") else 0
        val sortBy = if (options.hasKey("sortBy")) options.getString("sortBy") else "creationDate" // Corresponds to DATE_ADDED or DATE_MODIFIED
        val sortOrder = if (options.hasKey("sortOrder")) options.getString("sortOrder") else "desc"
        // val mediaTypeFilter = if (options.hasKey("mediaType")) options.getString("mediaType") else "all" // e.g. "photo", "video"

        // Permissions Check
        val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        // For videos, you might need READ_MEDIA_VIDEO as well on API 33+

        if (ContextCompat.checkSelfPermission(reactContext, readPermission) != PackageManager.PERMISSION_GRANTED) {
            promise.reject("PERMISSION_DENIED", "Read external storage permission denied.")
            return
        }

        val assets = WritableNativeArray()
        val contentResolver = reactContext.contentResolver

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DATE_ADDED, // For creationDate
            MediaStore.MediaColumns.DATE_MODIFIED, // For modificationDate
            MediaStore.MediaColumns.MIME_TYPE, // To determine if photo or video
            MediaStore.Video.Media.DURATION // For video duration
        )

        val sortColumn = when (sortBy) {
            "creationDate" -> MediaStore.MediaColumns.DATE_ADDED
            "modificationDate" -> MediaStore.MediaColumns.DATE_MODIFIED
            else -> MediaStore.MediaColumns.DATE_ADDED
        }
        val sortDirection = if (sortOrder == "asc") "ASC" else "DESC"
        val sortOrderClause = "$sortColumn $sortDirection"

        // Query for both images and videos
        val queryUri = MediaStore.Files.getContentUri("external")
        
        // Example filter for mediaType (can be expanded)
        // var selection: String? = null
        // var selectionArgs: Array<String>? = null
        // if (mediaTypeFilter == "photo") {
        //     selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        //     selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
        // } else if (mediaTypeFilter == "video") {
        //     selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        //     selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        // }

        // Adding LIMIT and OFFSET for pagination
        // Note: MediaStore limit/offset is best handled by cursors, but for simplicity using SQL-like syntax if available or manual skip.
        // For Android 10 (API 29) and above, you can use `MediaStore.createSqlQuery()` with `limit` and `offset` clauses.
        // For older versions, you'd typically iterate the cursor and skip `offset` items, then take `limit` items.
        // This example uses cursor iteration for compatibility, which is less performant for large offsets.

        val cursor = contentResolver.query(
            queryUri,
            projection,
            null, // Pass selection here
            null, // Pass selectionArgs here
            sortOrderClause
        )

        if (cursor == null) {
            promise.reject("QUERY_FAILED", "Failed to query MediaStore.")
            return
        }

        var currentIndex = 0
        var itemsAdded = 0

        cursor.use { c ->
            if (c.moveToPosition(offset)) {
                do {
                    if (itemsAdded >= limit) break

                    val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val width = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH))
                    val height = c.getInt(c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT))
                    val dateAdded = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED))
                    val dateModified = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                    val mimeType = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))

                    val assetMap = WritableNativeMap()
                    val assetUri = ContentUris.withAppendedId(
                        if (mimeType.startsWith("image/")) MediaStore.Images.Media.EXTERNAL_CONTENT_URI 
                        else if (mimeType.startsWith("video/")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI 
                        else queryUri, // Fallback, though specific URIs are better
                        id
                    ).toString()

                    assetMap.putString("uri", assetUri)
                    assetMap.putString("id", id.toString())
                    assetMap.putString("filename", displayName)
                    assetMap.putInt("width", width)
                    assetMap.putInt("height", height)
                    assetMap.putDouble("creationDate", dateAdded.toDouble()) // Timestamps are usually in seconds
                    assetMap.putDouble("modificationDate", dateModified.toDouble())
                    
                    if (mimeType.startsWith("video/")) {
                        assetMap.putString("mediaType", "video")
                        val duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                        assetMap.putDouble("duration", duration / 1000.0) // Duration is in ms
                    } else if (mimeType.startsWith("image/")){
                        assetMap.putString("mediaType", "photo")
                    } else {
                        assetMap.putString("mediaType", "unknown") // Or skip this asset
                    }
                    
                    // Only add if mediaType is known (photo or video)
                    if (assetMap.getString("mediaType") != "unknown") {
                         assets.pushMap(assetMap)
                         itemsAdded++
                    }
                    currentIndex++
                } while (c.moveToNext())
            }
        }

        val totalCount = cursor.count // Total items matched by query before pagination
        val hasNextPage = (offset + itemsAdded) < totalCount 
        val nextOffset = if (hasNextPage) offset + itemsAdded else offset + itemsAdded // or totalCount if it's the absolute end
        
        val result = WritableNativeMap()
        result.putArray("assets", assets)
        result.putBoolean("hasNextPage", hasNextPage)
        result.putInt("nextOffset", nextOffset)

        promise.resolve(result)
    }

    // Add other overridden methods from your Spec here
    /*
    @ReactMethod
    override fun requestPermissions(promise: Promise) {
        promise.reject("UNIMPLEMENTED", "requestPermissions has not been implemented yet.")
    }
    */
} 