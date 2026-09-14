package com.parkspot.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

/**
 * [ActivityResultContracts.TakePicture] with the URI permission the camera app needs.
 *
 * The plain contract passes the target as an extra, and grant flags only apply to an intent's data
 * or clip data — so without putting the URI in the clip data too, camera apps get a
 * `SecurityException` when they try to write the picture.
 */
class CapturePhotoContract : ActivityResultContracts.TakePicture() {

    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).apply {
            clipData = ClipData.newRawUri(null, input)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
