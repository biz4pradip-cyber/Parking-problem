package com.parkspot.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Stores spot photos in app-private storage and hands out `content://` URIs through
 * [FileProvider], so the camera app can write into them without any storage permission.
 */
class PhotoStore(private val context: Context) {

    private val photoDir: File
        get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    private val authority: String
        get() = "${context.packageName}.fileprovider"

    /** Creates an empty file for a new capture and returns the URI to hand to the camera. */
    fun createPhotoUri(): Uri {
        val file = File(photoDir, "spot_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, authority, file)
    }

    /** Deletes the backing file of a URI created by [createPhotoUri]. Safe to call with null. */
    fun delete(uriString: String?) {
        val name = uriString?.let { Uri.parse(it).lastPathSegment } ?: return
        // lastPathSegment of the FileProvider URI is the file name inside photoDir.
        val file = File(photoDir, File(name).name)
        if (file.exists()) file.delete()
    }

    /** Removes a capture file that was created but never used (user cancelled the camera). */
    fun deleteIfEmpty(uri: Uri?) {
        val name = uri?.lastPathSegment ?: return
        val file = File(photoDir, File(name).name)
        if (file.exists() && file.length() == 0L) file.delete()
    }

    private companion object {
        const val DIR_NAME = "photos"
    }
}
