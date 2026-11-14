package com.myiptv.clean.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

/** Reject a different app, version or signing certificate before asking Android to install. */
internal object InstalledApkVerifier {
    @Suppress("DEPRECATION")
    fun matches(context: Context, file: File, release: AppUpdateRelease): Boolean {
        return try {
            val manager = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            val installed = manager.getPackageInfo(context.packageName, flags)
            val archive = manager.getPackageArchiveInfo(file.absolutePath, flags) ?: return false
            isMatchingUpdate(identity(installed), identity(archive), release)
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun identity(info: PackageInfo): AppPackageIdentity {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        val certs = signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).toHex()
        }.toSet()
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        return AppPackageIdentity(info.packageName.orEmpty(), code, info.versionName, certs)
    }
}
