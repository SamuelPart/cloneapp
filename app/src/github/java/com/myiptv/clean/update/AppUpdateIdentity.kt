package com.myiptv.clean.update

/** Package metadata used in addition to Android's own package-signature verification. */
internal data class AppPackageIdentity(
    val packageName: String,
    val versionCode: Long,
    val versionName: String?,
    val signerSha256: Set<String>,
)

internal fun isMatchingUpdate(
    installed: AppPackageIdentity,
    archive: AppPackageIdentity,
    release: AppUpdateRelease,
): Boolean = installed.packageName == archive.packageName &&
    archive.versionCode == release.versionCode && archive.versionCode > installed.versionCode &&
    archive.versionName == release.versionName &&
    installed.signerSha256.isNotEmpty() && installed.signerSha256 == archive.signerSha256
