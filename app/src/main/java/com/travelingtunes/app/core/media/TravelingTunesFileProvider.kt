package com.travelingtunes.app.core.media

import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.FileProvider

class TravelingTunesFileProvider : FileProvider() {

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val callingUid = Binder.getCallingUid()
        val callingPkg = try {
            context?.packageManager?.getPackagesForUid(callingUid)?.firstOrNull() ?: "uid:$callingUid"
        } catch (_: Exception) {
            "uid:$callingUid"
        }
        Log.d("AutoArtworkDiag", "FileProvider openFile uri=$uri mode=$mode callingPkg=$callingPkg")
        return try {
            super.openFile(uri, mode)
        } catch (e: Exception) {
            Log.e("AutoArtworkDiag", "FileProvider openFile failed for uri=$uri callingPkg=$callingPkg", e)
            throw e
        }
    }
}
