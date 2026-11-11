package com.myiptv.clean.ui.compose

import android.net.Uri
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myiptv.clean.CleanIPTVApp
import com.myiptv.clean.ads.SamplayBanner
import com.myiptv.clean.ads.AdsRules
import com.myiptv.clean.ads.AdSection
import com.myiptv.clean.ads.CoinPurchaseStatus
import com.myiptv.clean.ads.CoinRules
import com.myiptv.clean.ads.PendingRewardDecision
import com.myiptv.clean.ads.canOfferLiveChannelAds
import com.myiptv.clean.ads.pendingRewardDecision
import com.myiptv.clean.data.repository.ChannelCatalogRepository
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.MediaPlaylistRepository
import com.myiptv.clean.data.repository.TmdbRepository
import com.myiptv.clean.data.repository.loadPlaylistMetadata
import com.myiptv.clean.notifications.CatalogNotificationTarget
import com.myiptv.clean.navigation.SettingsRoutes
import com.myiptv.clean.update.AppUpdateManager
import com.myiptv.clean.update.AppUpdateSyncScheduler
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object IptvRoute {
    const val HOME = "home"
    const val CHANNELS = "channels"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val KIDS = "kids"
    const val ANIME = "anime"
    const val FAVORITES = "favorites"
    const val SEARCH = "search"
    const val DOWNLOADS = "downloads"
    const val HISTORY = "history"
    const val SETTINGS = SettingsRoutes.SETTINGS
    const val SETTINGS_COINS = SettingsRoutes.COINS
    const val CHANNEL_DETAIL = "channel/{id}"
    const val MEDIA_DETAIL = "media/{id}?kids={kids}"
    const val PLAYER = "player/{id}"

    fun channel(id: String) = "channel/$id"
    fun media(id: String, kids: Boolean = false) = "media/$id?kids=$kids"
    fun player(id: String) = "player/$id"
}

private data class AppDestination(val route: String, val label: String, val icon: ImageVector)

private class PendingRewardPlayback(
    val offer: PlaybackRewardOffer,
    val origin: NavBackStackEntry,
    val isStillCurrent: () -> Boolean,
    val canAdvertise: () -> Boolean,
    val pauseForAd: () -> (() -> Unit),
    val proceed: () -> Unit,
)

private data class DeferredRewardPlayback(val request: PendingRewardPlayback, val spendCoins: Boolean)

private val sidebarDestinations = listOf(
    AppDestination(IptvRoute.CHANNELS, "TV", Icons.Filled.LiveTv),
    AppDestination(IptvRoute.HOME, "DESTACADO", Icons.Filled.ThumbUp),
    AppDestination(IptvRoute.MOVIES, "PELÍCULAS", Icons.Filled.Movie),
    AppDestination(IptvRoute.SERIES, "SERIES", Icons.Filled.Theaters),
    AppDestination(IptvRoute.KIDS, "KIDS", Icons.Filled.ChildCare),
    AppDestination(IptvRoute.ANIME, "ANIME", Icons.Filled.Animation),
    AppDestination(IptvRoute.SEARCH, "EXPLORAR", Icons.Filled.Explore),
    AppDestination(IptvRoute.FAVORITES, "MI LISTA", Icons.Filled.Favorite),
    AppDestination(IptvRoute.DOWNLOADS, "DESCARGAS", Icons.Filled.Download),
    AppDestination(IptvRoute.HISTORY, "HISTORIAL", Icons.Filled.History),
    AppDestination(IptvRoute.SETTINGS, "AJUSTES", Icons.Filled.Settings)
)

@Composable
internal fun IptvApp(
    notificationTarget: CatalogNotificationTarget? = null,
    onNotificationHandled: (Long) -> Unit = {},
    updateRequest: Long? = null,
    onUpdateRequestHandled: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val app = appContext as CleanIPTVApp
    val ads = app.ads
    val coins = app.coins
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: IptvRoute.HOME
    val isPlayer = currentRoute == IptvRoute.PLAYER
    val isDetail = currentRoute == IptvRoute.CHANNEL_DETAIL || currentRoute == IptvRoute.MEDIA_DETAIL
    var liveFullscreen by remember { mutableStateOf(false) }
    var phoneChannelSearchOpen by remember { mutableStateOf(false) }
    var pendingPhoneSettingsUpdate by rememberSaveable { mutableStateOf<Long?>(null) }
    var phoneSettingsUpdateSequence by rememberSaveable { mutableLongStateOf(0L) }
    var tvChannelRequest by remember { mutableStateOf<TvChannelRequest?>(null) }
    var tvChannelRequestSequence by remember { mutableLongStateOf(0L) }
    val tvChannelKeys = remember { TvChannelKeys() }
    val uiMode = LocalConfiguration.current.uiMode
    val isTelevision = remember(context, uiMode) { context.isTelevisionDevice(uiMode) }
    // Mobile search is a dedicated page with its own back and cancel controls, as in the reference.
    // TV keeps its existing header and sidebar, and the five-item phone bar returns on leaving search.
    val showChrome = !isPlayer && !isDetail && !(currentRoute == IptvRoute.CHANNELS && liveFullscreen) &&
        !usePhoneSearchLayout(isTelevision, currentRoute)
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(currentRoute) {
        if (currentRoute != IptvRoute.CHANNELS) phoneChannelSearchOpen = false
    }
    RequestCatalogNotificationsOnce()
    // Refresh expiry even if the user remains on the same screen for 24 hours.
    LaunchedEffect(ads, coins, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                coins.refreshExpired()
                ads.refreshAdFreeExpiry()
                delay(15_000L)
            }
        }
    }
    val isKidsDetail = when (currentRoute) {
        IptvRoute.MEDIA_DETAIL -> backStackEntry?.arguments?.getBoolean("kids") == true
        // Include every provider category; missing metadata is treated conservatively too.
        IptvRoute.CHANNEL_DETAIL -> {
            val channel = ChannelCatalogRepository.cachedChannel(backStackEntry?.arguments?.getString("id").orEmpty())
            channel == null || !canOfferLiveChannelAds(channel.sourceCategories)
        }
        else -> false
    }
    // Cache before playback, so accepting an offer never makes the user wait for a network load.
    val canPreloadRewards = AdsRules.canPreloadRewards(isTelevision, isKidsDetail, currentRoute)
    LaunchedEffect(ads, lifecycleOwner, canPreloadRewards, ads.canShowAds, ads.fullscreenShowing, currentRoute) {
        if (canPreloadRewards && ads.canShowAds && !ads.fullscreenShowing) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { ads.loadRewards() }
        }
    }
    // If a consent update finishes while video/live/Kids is active, defer its form
    // and SDK initialization until a safe browsing screen is resumed.
    val canPresentAdConsent = AdsRules.canPresentConsent(
        isTvDevice = isTelevision,
        route = currentRoute,
        notificationPending = notificationTarget != null,
    )
    LaunchedEffect(ads, lifecycleOwner, canPresentAdConsent) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            ads.setContentAllowsConsentUi(canPresentAdConsent)
            try { awaitCancellation() } finally { ads.setContentAllowsConsentUi(false) }
        }
    }
    AdAudienceDialog(
        ads = ads,
        canPrompt = !isTelevision && currentRoute == IptvRoute.HOME && notificationTarget == null && updateRequest == null,
    )
    val playlist by produceState(initialValue = MediaPlaylist.Loading, key1 = appContext) {
        MediaPlaylistRepository.observe(appContext).collectLatest { local ->
            value = local // Videos remain available while TMDb requests run in the background.
            if (TmdbRepository.isConfigured) {
                loadPlaylistMetadata(local, TmdbRepository::playlistMetadata) { value = it }
            }
        }
    }
    LaunchedEffect(appContext, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                MediaPlaylistRepository.refresh(appContext)
                delay(MediaPlaylistRepository.REFRESH_INTERVAL_MS)
            }
        }
    }
    // Play does not start a release-checking coroutine or contact the GitHub updater.
    if (AppUpdateSyncScheduler.ENABLED) {
        LaunchedEffect(appContext, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    AppUpdateManager.check(appContext)
                    delay(AppUpdateSyncScheduler.CHECK_INTERVAL_MS)
                }
            }
        }
    }
    LaunchedEffect(updateRequest) {
        val request = updateRequest ?: return@LaunchedEffect
        // Phone settings has collapsible sections; keep this event until the update card opens.
        // Use our own saved sequence: Activity request IDs can restart after rotation.
        if (!isTelevision) {
            phoneSettingsUpdateSequence += 1
            pendingPhoneSettingsUpdate = phoneSettingsUpdateSequence
        }
        navigateTopLevel(navController, IptvRoute.SETTINGS)
        onUpdateRequestHandled(request)
    }
    LaunchedEffect(playlist.items) { favoritesViewModel.canonicalize(playlist) }
    LaunchedEffect(notificationTarget, playlist.loaded) {
        val target = notificationTarget ?: return@LaunchedEffect
        // A newer catalog notification supersedes any unfinished update-panel request.
        pendingPhoneSettingsUpdate = null
        // On a cold start the atomic remote cache may still be loading. Wait for it so the
        // destination can resolve the exact M3U title, including a series with new episodes.
        if (!playlist.loaded) return@LaunchedEffect
        navController.navigate(IptvRoute.media(Uri.encode(target.mediaId))) { launchSingleTop = true }
        onNotificationHandled(target.sequence)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { event ->
                val code = event.nativeKeyEvent.keyCode
                val step = tvChannelKeys.onStep
                if (currentRoute == IptvRoute.CHANNELS && step != null && event.type == KeyEventType.KeyDown &&
                    (code == AndroidKeyEvent.KEYCODE_CHANNEL_UP || code == AndroidKeyEvent.KEYCODE_CHANNEL_DOWN)
                ) {
                    if (event.nativeKeyEvent.repeatCount == 0) step(if (code == AndroidKeyEvent.KEYCODE_CHANNEL_UP) 1 else -1)
                    true
                } else false
            }
    ) {
        // Keep the redesigned catalog on touch devices even in a wide window; Android TV
        // continues to use exactly the same sidebar, home and remote-focused layout as before.
        val phoneCatalog = usePhoneCatalogLayout(isTelevision, currentRoute)
        val isTvLayout = useSamplayTvLayout(
            isTvDevice = isTelevision,
            route = currentRoute,
            isPlayer = isPlayer,
            isChannelDestination = currentRoute == IptvRoute.CHANNELS || currentRoute == IptvRoute.CHANNEL_DETAIL,
            widthDp = maxWidth.value
        )
        val compactSidebar = maxWidth < 600.dp
        // Keep one NavHost at the same composition location as the window rotates. Replacing the
        // graph here would dispose VLC/WebView and restart playback on a landscape phone.
        // Android 15+ enforces edge-to-edge for the Play target. Keep the same black background,
        // but protect browsing controls; full-screen playback still occupies the entire window.
        Row(modifier = Modifier.fillMaxSize().background(Color.Black).safeAreaWhenEnforced(
            isTelevision = isTelevision,
            immersive = isPlayer || (currentRoute == IptvRoute.CHANNELS && liveFullscreen)
        )) {
            if (showChrome && isTvLayout) {
                TvSidebar(
                    currentRoute = currentRoute,
                    compact = compactSidebar,
                    onNavigate = { navigateTopLevel(navController, it) }
                )
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Top-edge placement avoids sandwiching an ad between content and the menu.
                if (AdsRules.canPlaceBanner(isTelevision, isTvLayout, currentRoute)) {
                    AdSection.forBannerRoute(currentRoute)?.let { section -> SamplayBanner(ads, section) }
                }
                if (showChrome) {
                    AppTopBar(
                        onSearch = {
                            if (!isTvLayout && currentRoute == IptvRoute.CHANNELS) phoneChannelSearchOpen = !phoneChannelSearchOpen
                            else navigateFromChrome(navController, IptvRoute.SEARCH, isTelevision)
                        },
                        onHome = { navigateTopLevel(navController, IptvRoute.HOME) },
                        onFavorites = { navigateTopLevel(navController, IptvRoute.FAVORITES) },
                        onSettings = { navigateTopLevel(navController, IptvRoute.SETTINGS) },
                        onHistory = { navigateTopLevel(navController, IptvRoute.HISTORY) },
                        onNavigate = { navigateFromChrome(navController, it, isTelevision) },
                        isTv = isTvLayout
                    )
                    if (phoneCatalog) {
                        PhoneCatalogTabs(currentRoute, onNavigate = { navigateTopLevel(navController, it) })
                    }
                }
                IptvNavigationGraph(
                    navController = navController,
                    isTv = if (isPlayer) isTelevision else isTvLayout,
                    isTelevisionDevice = isTelevision,
                    favoritesViewModel = favoritesViewModel,
                    playlist = playlist,
                    pendingPhoneSettingsUpdate = pendingPhoneSettingsUpdate,
                    onPhoneSettingsUpdateHandled = { handled ->
                        pendingPhoneSettingsUpdate = phoneSettingsUpdateAfterHandled(pendingPhoneSettingsUpdate, handled)
                    },
                    liveFullscreen = liveFullscreen,
                    onLiveFullscreenChange = { liveFullscreen = it },
                    phoneChannelSearchOpen = phoneChannelSearchOpen,
                    onClosePhoneChannelSearch = { phoneChannelSearchOpen = false },
                    tvChannelKeys = tvChannelKeys,
                    tvChannelRequest = tvChannelRequest,
                    onTvChannelRequestHandled = { tvChannelRequest = null },
                    onOpenTvChannel = { id ->
                        tvChannelRequestSequence++
                        tvChannelRequest = TvChannelRequest(id, tvChannelRequestSequence)
                        navigateTopLevel(navController, IptvRoute.CHANNELS)
                        liveFullscreen = true
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                if (showChrome && !isTvLayout) {
                    PhoneBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { navigateFromChrome(navController, it, isTelevision) }
                    )
                }
            }
        }
    }
}

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(IptvRoute.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Opening phone search retains the actual previous page so Back/Cancel can return to it. */
private fun navigateFromChrome(navController: NavHostController, route: String, isTelevision: Boolean) {
    if (usePhoneSearchLayout(isTelevision, route)) {
        navController.navigate(route) { launchSingleTop = true }
    } else {
        navigateTopLevel(navController, route)
    }
}

@Composable
private fun IptvNavigationGraph(
    navController: NavHostController,
    isTv: Boolean,
    isTelevisionDevice: Boolean,
    favoritesViewModel: FavoritesViewModel,
    playlist: MediaPlaylist,
    pendingPhoneSettingsUpdate: Long?,
    onPhoneSettingsUpdateHandled: (Long) -> Unit,
    liveFullscreen: Boolean,
    onLiveFullscreenChange: (Boolean) -> Unit,
    phoneChannelSearchOpen: Boolean,
    onClosePhoneChannelSearch: () -> Unit,
    tvChannelKeys: TvChannelKeys,
    tvChannelRequest: TvChannelRequest?,
    onTvChannelRequestHandled: () -> Unit,
    onOpenTvChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIsTv = rememberUpdatedState(isTv)
    val currentIsTelevisionDevice = rememberUpdatedState(isTelevisionDevice)
    val currentPlaylist = rememberUpdatedState(playlist)
    val currentPhoneSettingsUpdate = rememberUpdatedState(pendingPhoneSettingsUpdate)
    val currentOnPhoneSettingsUpdateHandled = rememberUpdatedState(onPhoneSettingsUpdateHandled)
    val currentLiveFullscreen = rememberUpdatedState(liveFullscreen)
    val currentOnLiveFullscreenChange = rememberUpdatedState(onLiveFullscreenChange)
    val currentPhoneChannelSearchOpen = rememberUpdatedState(phoneChannelSearchOpen)
    val currentOnClosePhoneChannelSearch = rememberUpdatedState(onClosePhoneChannelSearch)
    val currentTvChannelRequest = rememberUpdatedState(tvChannelRequest)
    val currentOnTvChannelRequestHandled = rememberUpdatedState(onTvChannelRequestHandled)
    val currentOnOpenTvChannel = rememberUpdatedState(onOpenTvChannel)
    val context = LocalContext.current
    val app = context.applicationContext as CleanIPTVApp
    val ads = app.ads
    val coins = app.coins
    val activity = context.adActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingOffer by remember { mutableStateOf<PendingRewardPlayback?>(null) }
    var waitingForReward by remember { mutableStateOf(false) }
    var deferredPlayback by remember { mutableStateOf<DeferredRewardPlayback?>(null) }
    val currentEntry by navController.currentBackStackEntryAsState()

    fun finish(request: PendingRewardPlayback, spendCoins: Boolean = false) {
        if (pendingOffer !== request) return // A callback from an older impression must do nothing.
        pendingOffer = null
        waitingForReward = false
        deferredPlayback = DeferredRewardPlayback(request, spendCoins)
    }
    fun cancel(request: PendingRewardPlayback) {
        if (pendingOffer !== request) return
        pendingOffer = null
        waitingForReward = false
    }
    fun decision(request: PendingRewardPlayback): PendingRewardDecision {
        val offer = request.offer
        val pass = if (offer.isChannel) coins.isChannelOfferFree(offer.id) else coins.isSectionAdFree(offer.section)
        val mayOffer = AdsRules.canOfferReward(
            // A wide phone/tablet may use the TV layout without being an Android TV device.
            isTv = context.isTelevisionDevice(),
            isKids = !request.canAdvertise(),
            hasActivity = activity != null,
            hasPass = pass,
            canShowAds = ads.canShowAdsIn(offer.section) && !ads.fullscreenShowing,
        )
        return pendingRewardDecision(
            sameOrigin = navController.currentBackStackEntry === request.origin,
            sameContent = request.isStillCurrent(),
            eligibleForOffer = mayOffer,
        )
    }
    // SDK callbacks can arrive before the activity resumes. A new destination/channel wins.
    LaunchedEffect(deferredPlayback, lifecycleOwner) {
        val deferred = deferredPlayback ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            deferredPlayback = null
            val target = deferred.request
            if (navController.currentBackStackEntry === target.origin && target.isStillCurrent()) {
                // Charge only when this exact playback can actually continue, and only if the
                // invitation remains eligible. Changed consent/passes/metadata continue for free.
                if (deferred.spendCoins && decision(target) == PendingRewardDecision.AVAILABLE) {
                    val offer = target.offer
                    coins.buyPlaybackPass(offer.section, if (offer.isChannel) offer.id else null)
                }
                target.proceed()
            }
        }
    }
    val requestOnScreen = pendingOffer
    val requestDecision = if (requestOnScreen == null || waitingForReward) null
    else if (currentEntry !== requestOnScreen.origin) PendingRewardDecision.CANCEL
    else decision(requestOnScreen)
    LaunchedEffect(requestOnScreen, requestDecision, waitingForReward) {
        if (requestOnScreen != null && !waitingForReward) {
            when (requestDecision) {
                PendingRewardDecision.CANCEL -> cancel(requestOnScreen)
                PendingRewardDecision.CONTINUE_FREE -> cancel(requestOnScreen)
                else -> Unit
            }
        }
    }

    fun requestOffer(
        offer: PlaybackRewardOffer,
        isStillCurrent: () -> Boolean,
        canAdvertise: () -> Boolean,
        pauseForAd: () -> (() -> Unit),
        proceed: () -> Unit,
    ) {
        // Ignore double taps while an earlier request or its lifecycle continuation is pending.
        if (pendingOffer != null || deferredPlayback != null) return
        val origin = navController.currentBackStackEntry
        if (origin == null) {
            if (isStillCurrent()) proceed()
            return
        }
        val request = PendingRewardPlayback(offer, origin, isStillCurrent, canAdvertise, pauseForAd, proceed)
        when (decision(request)) {
            PendingRewardDecision.CANCEL -> Unit
            PendingRewardDecision.CONTINUE_FREE -> proceed()
            PendingRewardDecision.AVAILABLE -> pendingOffer = request
        }
    }
    fun requestPlayback(id: String, section: AdSection, kids: Boolean, canStillAdvertise: () -> Boolean) {
        requestOffer(
            offer = PlaybackRewardOffer(id, section, section == AdSection.CHANNELS),
            isStillCurrent = { true },
            canAdvertise = {
                !kids && canStillAdvertise() && (section != AdSection.CHANNELS ||
                    ChannelCatalogRepository.cachedChannel(id)?.sourceCategories?.let(::canOfferLiveChannelAds) == true)
            },
            pauseForAd = { {} },
            proceed = { navController.navigate(IptvRoute.player(id)) },
        )
    }
    fun requestPhoneFullscreen(request: PhoneLiveFullscreenRequest) {
        requestOffer(
            offer = PlaybackRewardOffer(request.channelId, AdSection.CHANNELS, isChannel = true),
            isStillCurrent = request.isStillCurrent,
            canAdvertise = request.canAdvertise,
            pauseForAd = request.pauseForAd,
            proceed = request.enterFullscreen,
        )
    }
    val currentRequestPlayback = rememberUpdatedState(::requestPlayback)
    val currentRequestPhoneFullscreen = rememberUpdatedState(::requestPhoneFullscreen)
    // The graph itself must not be rebuilt when metadata or screen dimensions change.
    val graphBuilder: NavGraphBuilder.() -> Unit = remember(navController, favoritesViewModel, tvChannelKeys) {
        {
            composable(IptvRoute.HOME) {
                HomeScreen(
                    isTv = currentIsTv.value,
                    playlist = currentPlaylist.value,
                    onOpenChannels = { navigateTopLevel(navController, IptvRoute.CHANNELS) },
                    onOpenMovies = { navigateTopLevel(navController, IptvRoute.MOVIES) },
                    onOpenSeries = { navigateTopLevel(navController, IptvRoute.SERIES) },
                    onOpenSettings = { navigateTopLevel(navController, IptvRoute.SETTINGS) },
                    onOpenChannel = { navController.navigate(IptvRoute.channel(it)) },
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                    favorites = currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds),
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(IptvRoute.CHANNELS) {
                ChannelsScreen(
                    // The outgoing entry can overlap another destination during navigation; use
                    // the device type, not the other destination's width-based layout, for VLC.
                    isTv = context.isTelevisionDevice(),
                    onOpenChannel = { navController.navigate(IptvRoute.channel(it)) },
                    fullscreen = currentLiveFullscreen.value,
                    onFullscreenChange = { currentOnLiveFullscreenChange.value(it) },
                    channelKeys = tvChannelKeys,
                    requestedChannel = currentTvChannelRequest.value,
                    onRequestHandled = { currentOnTvChannelRequestHandled.value() },
                    favorites = currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds),
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) },
                    phoneSearchOpen = currentPhoneChannelSearchOpen.value,
                    onClosePhoneSearch = { currentOnClosePhoneChannelSearch.value() },
                    onRequestPhoneFullscreen = { currentRequestPhoneFullscreen.value(it) },
                )
            }
            composable(IptvRoute.MOVIES) {
                BrowseMediaScreen(
                    isTv = currentIsTv.value,
                    kind = MediaKind.MOVIE,
                    playlist = currentPlaylist.value,
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                    favorites = currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds),
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(IptvRoute.SERIES) {
                BrowseMediaScreen(
                    isTv = currentIsTv.value,
                    kind = MediaKind.SERIES,
                    playlist = currentPlaylist.value,
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                    favorites = currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds),
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(IptvRoute.KIDS) {
                MediaCollectionScreen(
                    collection = MediaCollection.KIDS,
                    isTv = currentIsTv.value,
                    onOpenMedia = { navController.navigate(IptvRoute.media(it, kids = true)) },
                    favorites = currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds),
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(IptvRoute.ANIME) {
                MediaCollectionScreen(
                    collection = MediaCollection.ANIME,
                    isTv = currentIsTv.value,
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                    favorites = currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds),
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(IptvRoute.FAVORITES) {
                FavoritesScreen(
                    isTv = currentIsTv.value,
                    playlist = currentPlaylist.value,
                    savedIds = currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds),
                    onToggle = { favoritesViewModel.toggle(it, currentPlaylist.value) },
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                    onOpenChannel = { navController.navigate(IptvRoute.channel(it)) },
                    onExplore = { navigateTopLevel(navController, IptvRoute.HOME) }
                )
            }
            composable(IptvRoute.SEARCH) {
                SearchScreen(
                    isTv = currentIsTv.value,
                    playlist = currentPlaylist.value,
                    onBack = {
                        if (!navController.popBackStack()) navigateTopLevel(navController, IptvRoute.HOME)
                    },
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                    onOpenChannel = { navController.navigate(IptvRoute.channel(it)) }
                )
            }
            composable(IptvRoute.DOWNLOADS) {
                DownloadsScreen(currentIsTv.value)
            }
            composable(IptvRoute.HISTORY) {
                HistoryScreen(currentIsTv.value)
            }
            composable(IptvRoute.SETTINGS) { entry ->
                SettingsScreen(
                    // Route transitions may overlap layouts; form factor belongs to the device.
                    isTv = currentIsTelevisionDevice.value,
                    playlist = currentPlaylist.value,
                    onOpenCoins = { navController.navigate(IptvRoute.SETTINGS_COINS) { launchSingleTop = true } },
                    // Only the visible entry may consume an update while navigation animates.
                    openUpdateRequest = phoneSettingsUpdateForEntry(currentPhoneSettingsUpdate.value, entry === currentEntry),
                    onUpdateRequestHandled = { currentOnPhoneSettingsUpdateHandled.value(it) }
                )
            }
            composable(IptvRoute.SETTINGS_COINS) {
                SamplayCoinsScreen(isTv = currentIsTelevisionDevice.value, onBack = { navController.popBackStack() })
            }
            composable(
                route = IptvRoute.CHANNEL_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                ChannelDetailScreen(
                    channelId = entry.arguments?.getString("id").orEmpty(),
                    isTv = currentIsTv.value,
                    onBack = { navController.popBackStack() },
                    onPlay = {
                        if (currentIsTv.value) currentOnOpenTvChannel.value(it)
                        else currentRequestPlayback.value(it, AdSection.CHANNELS, false) { true }
                    },
                    isFavorite = { it in currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds) },
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(
                route = IptvRoute.MEDIA_DETAIL,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("kids") { type = NavType.BoolType; defaultValue = false },
                )
            ) { entry ->
                val fromKids = entry.arguments?.getBoolean("kids") == true
                MediaDetailScreen(
                    mediaId = entry.arguments?.getString("id").orEmpty(),
                    isTv = currentIsTv.value,
                    playlist = currentPlaylist.value,
                    fromKids = fromKids,
                    onBack = { navController.popBackStack() },
                    onOpenMedia = { navController.navigate(IptvRoute.media(it, kids = fromKids)) },
                    onPlay = { id, kind, childDirected, canStillAdvertise ->
                        val section = if (kind == MediaKind.MOVIE) AdSection.MOVIES else AdSection.SERIES
                        currentRequestPlayback.value(id, section, fromKids || childDirected, canStillAdvertise)
                    },
                    isFavorite = { it in currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds) },
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(
                route = IptvRoute.PLAYER,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                PlayerScreen(
                    contentId = entry.arguments?.getString("id").orEmpty(),
                    isTv = currentIsTv.value,
                    playlist = currentPlaylist.value,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    NavHost(navController = navController, startDestination = IptvRoute.HOME, modifier = modifier, builder = graphBuilder)
    val request = pendingOffer
    if (request != null && requestDecision == PendingRewardDecision.AVAILABLE && !waitingForReward) {
        val offer = request.offer
        PlaybackRewardOfferDialog(
            offer = offer,
            balance = coins.state.balance,
            onCancel = { cancel(request) },
            onWatch = {
                when (decision(request)) {
                    PendingRewardDecision.CANCEL, PendingRewardDecision.CONTINUE_FREE -> cancel(request)
                    PendingRewardDecision.AVAILABLE -> {
                        val resume = runCatching { request.pauseForAd() }.getOrNull()
                        if (resume == null) {
                            cancel(request)
                        } else {
                            var resumed = false
                            fun resumeOnce() {
                                if (!resumed) {
                                    resumed = true
                                    runCatching { resume() }
                                }
                            }
                            val useInterstitial = !ads.hasFreshReward(false) && ads.hasFreshReward(true)
                            waitingForReward = true
                            val started = activity?.let {
                                ads.showRewarded(
                                    activity = it,
                                    interstitial = useInterstitial,
                                    onFinished = {
                                        resumeOnce()
                                        finish(request)
                                    },
                                    onFailedToShow = {
                                        resumeOnce()
                                        cancel(request)
                                    },
                                )
                            } == true
                            if (!started) {
                                resumeOnce()
                                cancel(request)
                            }
                        }
                    }
                }
            },
            onSpend = {
                when (decision(request)) {
                    PendingRewardDecision.CANCEL -> { cancel(request); null }
                    PendingRewardDecision.CONTINUE_FREE -> { cancel(request); null }
                    PendingRewardDecision.AVAILABLE -> {
                        if (coins.state.balance < CoinRules.PLAYBACK_PRICE) CoinPurchaseStatus.INSUFFICIENT
                        else {
                            finish(request, spendCoins = true)
                            CoinPurchaseStatus.ACTIVATED
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun AppTopBar(
    onSearch: () -> Unit,
    onHome: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
    onNavigate: (String) -> Unit,
    isTv: Boolean
) {
    if (!isTv) {
        PhoneTopBar(onHome = onHome, onSearch = onSearch, onHistory = onHistory, onNavigate = onNavigate)
        return
    }
    var now by remember { androidx.compose.runtime.mutableStateOf(Date()) }
    LaunchedEffect(isTv) {
        while (isTv) {
            now = Date()
            delay(15_000)
        }
    }
    val time = remember(now) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(if (isTv) 72.dp else 64.dp)) {
        val compactTv = isTv && maxWidth < 550.dp
        val showClock = isTv && maxWidth >= 330.dp
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (isTv) 24.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isTv) TvBrandLogo(Modifier.width(110.dp))
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusableHeaderAction(Icons.Filled.Search, "Buscar", onSearch)
                if (!isTv) {
                    var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
                    Box {
                        FocusableHeaderAction(Icons.Filled.Menu, "Abrir menú", { menuExpanded = true })
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(TvSurface)
                        ) {
                            sidebarDestinations.forEach { destination ->
                                DropdownMenuItem(
                                    text = { Text(destination.label, color = Color.White) },
                                    leadingIcon = { Icon(destination.icon, contentDescription = null, tint = TvAccent) },
                                    onClick = { menuExpanded = false; onNavigate(destination.route) }
                                )
                            }
                        }
                    }
                }
                if (isTv && !compactTv) {
                    FocusableHeaderAction(Icons.Filled.GridView, "Destacado", onHome)
                    FocusableHeaderAction(Icons.Filled.Favorite, "Mi lista", onFavorites)
                    FocusableHeaderAction(Icons.Filled.Settings, "Ajustes", onSettings)
                }
                if (showClock) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.width(1.dp).height(22.dp).background(Color.White.copy(alpha = 0.3f)))
                    Text(
                        time,
                        modifier = Modifier.padding(start = 12.dp),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private val phoneCatalogDestinations = listOf(
    AppDestination(IptvRoute.HOME, "Recomendado", Icons.Filled.Home),
    AppDestination(IptvRoute.MOVIES, "Películas", Icons.Filled.Movie),
    AppDestination(IptvRoute.SERIES, "Series", Icons.Filled.Theaters),
    AppDestination(IptvRoute.KIDS, "Kids", Icons.Filled.ChildCare),
    AppDestination(IptvRoute.ANIME, "Anime", Icons.Filled.Animation)
)

@Composable
private fun PhoneTopBar(
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onHistory: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClickLabel = "Ir a inicio", onClick = onHome)) {
            TvBrandLogo(Modifier.width(108.dp))
        }
        Spacer(Modifier.weight(1f))
        PhoneHeaderAction(Icons.Filled.Search, "Buscar", onSearch)
        PhoneHeaderAction(Icons.Filled.History, "Historial", onHistory)
        var expanded by remember { mutableStateOf(false) }
        Box {
            PhoneHeaderAction(Icons.Filled.MoreVert, "Abrir menú", { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(TvSurface)) {
                sidebarDestinations.forEach { destination ->
                    DropdownMenuItem(
                        text = { Text(destination.label, color = Color.White) },
                        leadingIcon = { Icon(destination.icon, contentDescription = null, tint = TvAccent) },
                        onClick = { expanded = false; onNavigate(destination.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneHeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun PhoneCatalogTabs(currentRoute: String, onNavigate: (String) -> Unit) {
    val state = rememberLazyListState()
    val selectedIndex = phoneCatalogDestinations.indexOfFirst { it.route == currentRoute }
    LaunchedEffect(currentRoute, state) {
        if (selectedIndex >= 0) {
            val item = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
            val completelyVisible = item != null && item.offset >= state.layoutInfo.viewportStartOffset &&
                item.offset + item.size <= state.layoutInfo.viewportEndOffset
            if (!completelyVisible) state.animateScrollToItem(selectedIndex)
        }
    }
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(phoneCatalogDestinations, key = AppDestination::route) { destination ->
            val active = currentRoute == destination.route
            Column(
                modifier = Modifier.fillMaxHeight().selectable(
                    selected = active,
                    role = Role.Tab,
                    onClick = { onNavigate(destination.route) }
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        destination.label,
                        color = if (active) PhoneChannelAccent else Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Box(Modifier.width(38.dp).height(3.dp).background(if (active) PhoneChannelAccent else Color.Transparent, CircleShape))
            }
        }
    }
}

@Composable
private fun FocusableHeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (focused) Color.White else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (focused) Color.Black else Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TvSidebar(currentRoute: String, compact: Boolean, onNavigate: (String) -> Unit) {
    val initialFocus = remember { FocusRequester() }
    val selectedRoute = SettingsRoutes.topLevel(currentRoute)
    Column(
        modifier = Modifier
            .width(if (compact) 136.dp else 180.dp)
            .fillMaxHeight()
            .background(Color.Black)
            .padding(start = if (compact) 12.dp else 24.dp, end = 8.dp, top = 14.dp, bottom = 12.dp)
    ) {
        Box(Modifier.padding(start = 6.dp, bottom = 25.dp)) { TvBrandLogo(Modifier.width(if (compact) 102.dp else 112.dp)) }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sidebarDestinations.forEach { destination ->
                SidebarItem(
                    destination = destination,
                    selected = selectedRoute == destination.route,
                    onClick = { onNavigate(destination.route) },
                    modifier = if (selectedRoute == destination.route) Modifier.focusRequester(initialFocus) else Modifier
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(180)
        runCatching { initialFocus.requestFocus() }
    }
}

@Composable
private fun SidebarItem(destination: AppDestination, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(41.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color.White.copy(alpha = 0.14f) else if (selected) Color.White.copy(alpha = 0.07f) else Color.Transparent)
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val tint = if (selected || focused) Color.White else Color.White.copy(alpha = 0.52f)
        Icon(destination.icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Text(
            destination.label,
            color = tint,
            fontSize = 12.sp,
            letterSpacing = 0.3.sp,
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PhoneBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val selectedRoute = phoneBottomCatalogRoute(SettingsRoutes.topLevel(currentRoute))
    // These are the same five destinations on home and channels; labels remain accessible to TalkBack.
    val destinations = listOf(
        AppDestination(IptvRoute.HOME, "Inicio", Icons.Filled.Home),
        AppDestination(IptvRoute.CHANNELS, "En vivo", Icons.Filled.LiveTv),
        AppDestination(IptvRoute.SEARCH, "Buscar", Icons.Filled.Search),
        AppDestination(IptvRoute.FAVORITES, "Mi lista", Icons.Filled.Favorite),
        AppDestination(IptvRoute.SETTINGS, "Ajustes", Icons.Filled.Settings)
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(58.dp).background(Color(0xFF101012)).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        destinations.forEach { destination ->
            val selected = selectedRoute == destination.route
            var focused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .semantics { this.selected = selected }
                    .onFocusChanged { focused = it.isFocused }
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = destination.label) { onNavigate(destination.route) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    destination.icon,
                    contentDescription = destination.label,
                    tint = if (selected || focused) PhoneChannelAccent else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}
