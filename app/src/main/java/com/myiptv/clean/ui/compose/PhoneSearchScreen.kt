package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myiptv.clean.data.repository.MediaPlaylist

private val SearchAccent = Color(0xFFF40B43)
private val SearchBarSurface = Color(0xFF17171C)
private val SearchFieldSurface = Color(0xFF27272B)

/** A phone-only search page; Android TV continues using its original search screen. */
@Composable
internal fun PhoneSearchScreen(
    playlist: MediaPlaylist,
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    viewModel: PhoneSearchViewModel = viewModel()
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    fun dismissKeyboard() {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val query = viewModel.query
    val term = normalizePhoneSearch(query)
    val searching = term.isNotEmpty()
    val mediaIndex = remember(playlist.items) { PhoneMediaSearchIndex(playlist.items) }
    val channelIndex = remember(viewModel.channels) { PhoneChannelSearchIndex(viewModel.channels) }
    val mediaResults = remember(mediaIndex, term, viewModel.remoteResults) {
        phoneSearchMedia(mediaIndex, term, viewModel.remoteResults)
    }
    val channelResults = remember(channelIndex, term) { channelIndex.matches(term) }
    val suggestions = remember(playlist, viewModel.popularMedia) {
        phoneSearchSuggestions(playlist, viewModel.popularMedia)
    }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val visibleSuggestions = remember(suggestions, page) { phoneSuggestionPage(suggestions, page) }
    val canCycleSuggestions = suggestions.size > PHONE_SEARCH_SUGGESTIONS_PER_PAGE
    val canRetryPopular = viewModel.canRefreshPopular && !viewModel.popularLoading
    val listState = rememberLazyListState()
    LaunchedEffect(term, page) { listState.scrollToItem(0) }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()
    ) {
        PhoneSearchBar(
            query = query,
            onQueryChange = viewModel::updateQuery,
            onBack = { dismissKeyboard(); onBack() },
            onSearchDone = { dismissKeyboard() }
        )
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when {
                    searching -> "Resultados de búsqueda"
                    viewModel.popularMedia.isNotEmpty() -> "Populares en TMDb"
                    else -> "Sugerencias del catálogo"
                },
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!searching) {
                IconButton(
                    enabled = canCycleSuggestions || canRetryPopular,
                    onClick = {
                        if (canCycleSuggestions) page = if (page == Int.MAX_VALUE) 0 else page + 1
                        // A failed/offline TMDb request can be retried while browsing local titles.
                        if (viewModel.popularMedia.isEmpty() || !canCycleSuggestions) viewModel.refreshPopular()
                    }
                ) {
                    if (viewModel.popularLoading && visibleSuggestions.isEmpty()) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = if (canCycleSuggestions) "Mostrar más sugerencias" else "Actualizar sugerencias",
                            tint = Color.White.copy(alpha = if (canCycleSuggestions || canRetryPopular) 0.92f else 0.35f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (!searching) {
                if (visibleSuggestions.isEmpty()) {
                    item(key = "suggestions-empty") {
                        if (!playlist.loaded || viewModel.popularLoading) {
                            SamplayLoadingPane(message = "Cargando títulos…")
                        } else {
                            PhoneSearchMessage("No hay títulos disponibles", viewModel.popularError ?: "Comprueba el catálogo y la conexión.")
                        }
                    }
                } else {
                    items(visibleSuggestions, key = { "suggestion-${it.item.id}" }) { suggestion ->
                        PhoneSearchRow(
                            title = suggestion.item.title,
                            rank = suggestion.position,
                            onClick = { dismissKeyboard(); onOpenMedia(suggestion.item.id) }
                        )
                    }
                }
            } else {
                if (mediaResults.isEmpty() && channelResults.isEmpty()) {
                    item(key = "search-empty") {
                        val loading = !playlist.loaded || viewModel.searchPending || viewModel.channelsLoading
                        if (loading) {
                            SamplayLoadingPane(message = "Buscando…")
                        } else {
                            PhoneSearchMessage("No encontramos resultados", viewModel.searchError ?: "Prueba otro título, género o canal.")
                        }
                    }
                } else {
                    if (mediaResults.isNotEmpty()) {
                        item(key = "media-label") { PhoneSearchSection("Películas y series") }
                        items(mediaResults, key = { "media-${it.id}" }) { item ->
                            val kind = if (item.kind == MediaKind.MOVIE) "Película" else "Serie"
                            val subtitle = if (item.year > 0) "$kind · ${item.year}" else kind
                            PhoneSearchRow(
                                title = item.title,
                                subtitle = subtitle,
                                icon = if (item.kind == MediaKind.MOVIE) Icons.Filled.Movie else Icons.Filled.Theaters,
                                onClick = { dismissKeyboard(); onOpenMedia(item.id) }
                            )
                        }
                    }
                    if (channelResults.isNotEmpty()) {
                        item(key = "channels-label") { PhoneSearchSection("Canales") }
                        items(channelResults, key = { "channel-${it.id}" }) { channel ->
                            PhoneSearchRow(
                                title = channel.name,
                                subtitle = channel.category,
                                icon = Icons.Filled.LiveTv,
                                onClick = { dismissKeyboard(); onOpenChannel(channel.id) }
                            )
                        }
                    }
                    if (viewModel.searchPending) {
                        item(key = "remote-loading") { PhoneSearchStatus("Buscando también en TMDb…") }
                    } else if (viewModel.searchError != null) {
                        item(key = "remote-error") { PhoneSearchStatus("TMDb no está disponible; se muestran los resultados locales.") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearchDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(SearchBarSurface).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(25.dp))
        }
        val shape = RoundedCornerShape(6.dp)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                .clip(shape).background(SearchFieldSurface).border(1.dp, SearchAccent, shape)
                .padding(start = 8.dp, end = 2.dp)
                .semantics { contentDescription = "Buscar películas, series o canales" },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 14.sp),
            cursorBrush = SolidColor(SearchAccent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Buscar por título o canal",
                                color = Color.White.copy(alpha = 0.56f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        )
        Box(
            modifier = Modifier.heightIn(min = 48.dp).clip(shape).background(SearchAccent)
                .clickable(onClickLabel = "Cancelar búsqueda", onClick = onBack)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Cancelar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun PhoneSearchRow(
    title: String,
    onClick: () -> Unit,
    rank: Int? = null,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(onClickLabel = "Abrir $title", onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            val badge = when (rank) {
                1 -> Color(0xFFED1645)
                2 -> Color(0xFFDF4027)
                3 -> Color(0xFFE88A23)
                else -> Color(0xFF77777B)
            }
            Box(Modifier.heightIn(min = 23.dp).widthIn(min = 23.dp).clip(RoundedCornerShape(5.dp)).background(badge), contentAlignment = Alignment.Center) {
                Text(rank.toString(), modifier = Modifier.padding(horizontal = 3.dp), color = Color.White,
                    fontSize = if (rank < 100) 15.sp else 11.sp, fontWeight = FontWeight.Bold)
            }
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(23.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PhoneSearchSection(label: String) {
    Text(label, modifier = Modifier.padding(start = 17.dp, top = 10.dp, bottom = 4.dp),
        color = Color.White.copy(alpha = 0.64f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PhoneSearchStatus(message: String) {
    Text(message, modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
        color = Color.White.copy(alpha = 0.64f), fontSize = 13.sp)
}

@Composable
private fun PhoneSearchMessage(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (message.isNotBlank()) {
            Text(message, modifier = Modifier.padding(top = 7.dp), color = Color.White.copy(alpha = 0.64f),
                fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
