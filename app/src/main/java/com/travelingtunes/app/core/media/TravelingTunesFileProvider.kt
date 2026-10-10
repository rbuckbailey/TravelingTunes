package com.travelingtunes.app.core.media

import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.FileProvider

class TravelingTunesFileProvider : FileProvider() {

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val callingUid = Binder.getCallingUid()
        val ctx = context
        val callingPkg = try {
            ctx?.packageManager?.getPackagesForUid(callingUid)?.firstOrNull() ?: "uid:$callingUid"
        } catch (_: Exception) {
            "uid:$callingUid"
        }
        Log.d("AutoArtworkDiag", "FileProvider openFile uri=$uri mode=$mode callingPkg=$callingPkg")

        if (ctx != null) {
            val pkgs = ctx.packageManager?.getPackagesForUid(callingUid)
            if (!pkgs.isNullOrEmpty()) {
                for (pkg in pkgs) {
                    try {
                        ctx.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {}
                }
            }
        }

        return try {
            super.openFile(uri, mode)
        } catch (e: SecurityException) {
            Log.w("AutoArtworkDiag", "FileProvider openFile SecurityException for uri=$uri callingPkg=$callingPkg, attempting retry", e)
            if (ctx != null) {
                val pkgs = ctx.packageManager?.getPackagesForUid(callingUid)
                if (!pkgs.isNullOrEmpty()) {
                    for (pkg in pkgs) {
                        try {
                            ctx.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (_: Exception) {}
                    }
                    try {
                        return super.openFile(uri, mode)
                    } catch (retryEx: Exception) {
                        Log.e("AutoArtworkDiag", "FileProvider retry openFile failed for uri=$uri callingPkg=$callingPkg", retryEx)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("AutoArtworkDiag", "FileProvider openFile failed for uri=$uri callingPkg=$callingPkg", e)
            null
        }
    }
}
