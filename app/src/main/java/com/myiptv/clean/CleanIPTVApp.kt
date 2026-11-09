package com.myiptv.clean

import android.app.Application
import com.myiptv.clean.ads.AdsController

class CleanIPTVApp : Application() {
    // Constructed lazily; background catalog jobs never initialize advertising.
    internal val ads: AdsController by lazy { AdsController(this) }
}
