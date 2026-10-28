package com.myiptv.clean.ui.compose

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay

private object IptvRoute {
    const val HOME = "home"
    const val CHANNELS = "channels"
    const val MOVIES = "movies"
    const val SERIES = "series"
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
    AppDestination(IptvRoute.HOME, "Inicio", Icons.Filled.Home),
    AppDestination(IptvRoute.CHANNELS, "Canales", Icons.Filled.LiveTv),
    AppDestination(IptvRoute.MOVIES, "Películas", Icons.Filled.Movie),
    AppDestination(IptvRoute.SERIES, "Series", Icons.Filled.Theaters),
    AppDestination(IptvRoute.FAVORITES, "Mi lista", Icons.Filled.Favorite),
    AppDestination(IptvRoute.DOWNLOADS, "Descargas", Icons.Filled.Download),
    AppDestination(IptvRoute.HISTORY, "Historial", Icons.Filled.History),
    AppDestination(IptvRoute.SETTINGS, "Ajustes", Icons.Filled.Settings)
)

@Composable
fun IptvApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: IptvRoute.HOME
    val isPlayer = currentRoute == IptvRoute.PLAYER
    val isDetail = currentRoute == IptvRoute.CHANNEL_DETAIL || currentRoute == IptvRoute.MEDIA_DETAIL
    val showChrome = !isPlayer && !isDetail
    val isLeanback = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        val isTvLayout = isLeanback || maxWidth >= 840.dp
        if (isTvLayout) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (showChrome) {
                    TvSidebar(
                        currentRoute = currentRoute,
                        onNavigate = { navigateTopLevel(navController, it) }
                    )
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (showChrome) {
                        AppTopBar(onSearch = { navigateTopLevel(navController, IptvRoute.SEARCH) }, isTv = true)
                    }
                    IptvNavigationGraph(
                        navController = navController,
                        isTv = isTvLayout,
                        favoritesViewModel = favoritesViewModel,
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showChrome) {
                    AppTopBar(onSearch = { navigateTopLevel(navController, IptvRoute.SEARCH) }, isTv = false)
                }
                IptvNavigationGraph(
                    navController = navController,
                    isTv = false,
                    favoritesViewModel = favoritesViewModel,
                    settingsViewModel = settingsViewModel,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                if (showChrome) {
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
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = IptvRoute.HOME, modifier = modifier) {
        composable(IptvRoute.HOME) {
            HomeScreen(
                isTv = isTv,
                onOpenChannels = { navigateTopLevel(navController, IptvRoute.CHANNELS) },
                onOpenMovies = { navigateTopLevel(navController, IptvRoute.MOVIES) },
                onOpenSeries = { navigateTopLevel(navController, IptvRoute.SERIES) },
                onOpenChannel = { navController.navigate(IptvRoute.channel(it)) },
                onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                onOpenPlayer = { navController.navigate(IptvRoute.player(it)) },
                favorites = favoritesViewModel.savedIds,
                onToggleFavorite = favoritesViewModel::toggle
            )
        }
        composable(IptvRoute.CHANNELS) {
            ChannelsScreen(isTv, onOpenChannel = { navController.navigate(IptvRoute.channel(it)) })
        }
        composable(IptvRoute.MOVIES) {
            BrowseMediaScreen(
                isTv = isTv,
                kind = MediaKind.MOVIE,
                onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                favorites = favoritesViewModel.savedIds,
                onToggleFavorite = favoritesViewModel::toggle
            )
        }
        composable(IptvRoute.SERIES) {
            BrowseMediaScreen(
                isTv = isTv,
                kind = MediaKind.SERIES,
                onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                favorites = favoritesViewModel.savedIds,
                onToggleFavorite = favoritesViewModel::toggle
            )
        }
        composable(IptvRoute.FAVORITES) {
            FavoritesScreen(
                isTv = isTv,
                savedIds = favoritesViewModel.savedIds,
                onToggle = favoritesViewModel::toggle,
                onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                onOpenChannel = { navController.navigate(IptvRoute.channel(it)) },
                onExplore = { navigateTopLevel(navController, IptvRoute.HOME) }
            )
        }
        composable(IptvRoute.SEARCH) {
            SearchScreen(
                isTv = isTv,
                onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                onOpenChannel = { navController.navigate(IptvRoute.channel(it)) }
            )
        }
        composable(IptvRoute.DOWNLOADS) {
            DownloadsScreen(isTv, onOpenMedia = { navController.navigate(IptvRoute.media(it)) })
        }
        composable(IptvRoute.HISTORY) {
            HistoryScreen(isTv, onOpenMedia = { navController.navigate(IptvRoute.media(it)) }, onOpenPlayer = { navController.navigate(IptvRoute.player(it)) })
        }
        composable(IptvRoute.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onOpenDownloads = { navigateTopLevel(navController, IptvRoute.DOWNLOADS) },
                onOpenHistory = { navigateTopLevel(navController, IptvRoute.HISTORY) }
            )
        }
        composable(
            route = IptvRoute.CHANNEL_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            ChannelDetailScreen(
                channelId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
                onPlay = { navController.navigate(IptvRoute.player(it)) },
                isFavorite = { it in favoritesViewModel.savedIds },
                onToggleFavorite = favoritesViewModel::toggle
            )
        }
        composable(
            route = IptvRoute.MEDIA_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            MediaDetailScreen(
                mediaId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
                onOpenMedia = { navController.navigate(IptvRoute.media(it)) },
                onPlay = { navController.navigate(IptvRoute.player(it)) },
                isFavorite = { it in favoritesViewModel.savedIds },
                onToggleFavorite = favoritesViewModel::toggle
            )
        }
        composable(
            route = IptvRoute.PLAYER,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            PlayerScreen(
                contentId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun AppTopBar(onSearch: () -> Unit, isTv: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTv) 78.dp else 68.dp)
            .padding(horizontal = if (isTv) 34.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(
                    androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, Color(0xFF7054D2)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(21.dp))
            }
            Column {
                Text("CLEAN IPTV", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("ENTRETENIMIENTO", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FocusableHeaderAction(icon = Icons.Filled.Search, description = "Buscar", onClick = onSearch)
            if (isTv) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
            .size(42.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (focused) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun TvSidebar(currentRoute: String, onNavigate: (String) -> Unit) {
    val initialFocus = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .width(238.dp)
            .fillMaxHeight()
            .background(Color(0xFF0D131D))
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(start = 7.dp, bottom = 25.dp)) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column {
                Text("CLEAN IPTV", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
                Text("TV", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp)
            }
        }
        sidebarDestinations.forEachIndexed { index, destination ->
            SidebarItem(
                destination = destination,
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                modifier = if (index == 0) Modifier.focusRequester(initialFocus) else Modifier
            )
            Spacer(Modifier.height(5.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(
            "NAVEGA CON TU CONTROL REMOTO",
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.6.sp
        )
        Text("Interfaz de demostración", modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
    }
    LaunchedEffect(Unit) {
        delay(180)
        initialFocus.requestFocus()
    }
}

@Composable
private fun SidebarItem(destination: AppDestination, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(
                when {
                    focused -> MaterialTheme.colorScheme.surfaceVariant
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.colorScheme.secondary else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Icon(destination.icon, contentDescription = null, tint = if (selected || focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(destination.label, color = if (selected || focused) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Normal)
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
        modifier = Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF0D131D)).padding(horizontal = 7.dp, vertical = 5.dp),
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
