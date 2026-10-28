package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myiptv.clean.data.repository.MediaPlaylist
import com.myiptv.clean.data.repository.MediaPlaylistRepository
import com.myiptv.clean.data.repository.TmdbRepository
import com.myiptv.clean.data.repository.loadPlaylistMetadata
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
    const val SETTINGS = "settings"
    const val CHANNEL_DETAIL = "channel/{id}"
    const val MEDIA_DETAIL = "media/{id}"
    const val PLAYER = "player/{id}"

    fun channel(id: String) = "channel/$id"
    fun media(id: String) = "media/$id"
    fun player(id: String) = "player/$id"
}

private data class AppDestination(val route: String, val label: String, val icon: ImageVector)

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
fun IptvApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: IptvRoute.HOME
    val isPlayer = currentRoute == IptvRoute.PLAYER
    val isDetail = currentRoute == IptvRoute.CHANNEL_DETAIL || currentRoute == IptvRoute.MEDIA_DETAIL
    val showChrome = !isPlayer && !isDetail
    val uiMode = LocalConfiguration.current.uiMode
    val isTelevision = remember(context, uiMode) { context.isTelevisionDevice(uiMode) }
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    RequestCatalogNotificationsOnce()
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
    LaunchedEffect(playlist.items) { favoritesViewModel.canonicalize(playlist) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        val isTvLayout = isTelevision || (!isPlayer && maxWidth >= 840.dp)
        val compactSidebar = maxWidth < 600.dp
        // Keep one NavHost at the same composition location as the window rotates. Replacing the
        // graph here would dispose VLC/WebView and restart playback on a landscape phone.
        Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (showChrome && isTvLayout) {
                TvSidebar(
                    currentRoute = currentRoute,
                    compact = compactSidebar,
                    onNavigate = { navigateTopLevel(navController, it) }
                )
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (showChrome) {
                    AppTopBar(
                        onSearch = { navigateTopLevel(navController, IptvRoute.SEARCH) },
                        onHome = { navigateTopLevel(navController, IptvRoute.HOME) },
                        onFavorites = { navigateTopLevel(navController, IptvRoute.FAVORITES) },
                        onSettings = { navigateTopLevel(navController, IptvRoute.SETTINGS) },
                        onNavigate = { navigateTopLevel(navController, it) },
                        isTv = isTvLayout
                    )
                }
                IptvNavigationGraph(
                    navController = navController,
                    isTv = if (isPlayer) isTelevision else isTvLayout,
                    favoritesViewModel = favoritesViewModel,
                    playlist = playlist,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                if (showChrome && !isTvLayout) {
                    PhoneBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { navigateTopLevel(navController, it) }
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

@Composable
private fun IptvNavigationGraph(
    navController: NavHostController,
    isTv: Boolean,
    favoritesViewModel: FavoritesViewModel,
    playlist: MediaPlaylist,
    modifier: Modifier = Modifier
) {
    val currentIsTv = rememberUpdatedState(isTv)
    val currentPlaylist = rememberUpdatedState(playlist)
    // The graph itself must not be rebuilt when metadata or screen dimensions change.
    val graphBuilder: NavGraphBuilder.() -> Unit = remember(navController, favoritesViewModel) {
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
                ChannelsScreen(currentIsTv.value, onOpenChannel = { navController.navigate(IptvRoute.channel(it)) })
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
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
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
            composable(IptvRoute.SETTINGS) {
                SettingsScreen(currentIsTv.value, currentPlaylist.value)
            }
            composable(
                route = IptvRoute.CHANNEL_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                ChannelDetailScreen(
                    channelId = entry.arguments?.getString("id").orEmpty(),
                    isTv = currentIsTv.value,
                    onBack = { navController.popBackStack() },
                    onPlay = { navController.navigate(IptvRoute.player(it)) },
                    isFavorite = { it in currentPlaylist.value.expandedMediaIds(favoritesViewModel.savedIds) },
                    onToggleFavorite = { favoritesViewModel.toggle(it, currentPlaylist.value) }
                )
            }
            composable(
                route = IptvRoute.MEDIA_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                MediaDetailScreen(
                    mediaId = entry.arguments?.getString("id").orEmpty(),
                    isTv = currentIsTv.value,
                    playlist = currentPlaylist.value,
                    onBack = { navController.popBackStack() },
                    onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                    onPlay = { navController.navigate(IptvRoute.player(it)) },
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
}

@Composable
private fun AppTopBar(
    onSearch: () -> Unit,
    onHome: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
    onNavigate: (String) -> Unit,
    isTv: Boolean
) {
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
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination.route) },
                    modifier = if (currentRoute == destination.route) Modifier.focusRequester(initialFocus) else Modifier
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
    val destinations = listOf(
        AppDestination(IptvRoute.HOME, "Inicio", Icons.Filled.Home),
        AppDestination(IptvRoute.CHANNELS, "En vivo", Icons.Filled.LiveTv),
        AppDestination(IptvRoute.SEARCH, "Buscar", Icons.Filled.Search),
        AppDestination(IptvRoute.FAVORITES, "Mi lista", Icons.Filled.Favorite),
        AppDestination(IptvRoute.SETTINGS, "Ajustes", Icons.Filled.Settings)
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF101012)).padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onFocusChanged { focused = it.isFocused }
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected || focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onNavigate(destination.route) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(destination.icon, contentDescription = destination.label, tint = if (selected || focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
                Spacer(Modifier.height(3.dp))
                Text(destination.label, color = if (selected || focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
