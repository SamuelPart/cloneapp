package com.myiptv.clean

import android.app.Application
import com.myiptv.clean.ads.AdsController
import com.myiptv.clean.ads.CoinWallet

class CleanIPTVApp : Application() {
    // Constructed lazily; background catalog jobs never initialize advertising.
    internal val ads: AdsController by lazy { AdsController(this) }
    // Coins are stored only on this installation and never sent to an ad provider.
    internal val coins: CoinWallet by lazy { CoinWallet(this) }
}
