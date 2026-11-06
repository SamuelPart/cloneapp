package com.myiptv.clean.update

import androidx.core.content.FileProvider

/** A dedicated provider avoids conflicts with providers used by other libraries. */
class AppUpdateFileProvider : FileProvider()
