package com.myiptv.clean.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val TvSurface = Color(0xFF171719)
internal val TvMuted = Color.White.copy(alpha = 0.66f)
internal val TvAccent = Color(0xFF54C3F2)

/** A text wordmark keeps the same brand sharp on phones and TV displays. */
@Composable
internal fun TvBrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Sam", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp, maxLines = 1)
        Text("Play", color = TvAccent, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp, maxLines = 1)
    }
}

@Composable
internal fun TvPageHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title.uppercase(), color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = TvMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        action?.invoke()
    }
}

@Composable
internal fun TvActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val light = focused || primary
    val shape = RoundedCornerShape(3.dp)
    Row(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (light) Color.White else TvSurface)
            .border(if (focused) 2.dp else 1.dp, if (focused) TvAccent else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (light) Color.Black else Color.White, modifier = Modifier.size(18.dp))
        Text(label, color = if (light) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun TvSearchBox(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        shape = RoundedCornerShape(3.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = TvSurface,
            unfocusedContainerColor = TvSurface,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedLeadingIconColor = Color.White,
            unfocusedLeadingIconColor = TvMuted,
            cursorColor = TvAccent
        )
    )
}

@Composable
internal fun TvFilterRow(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.heightIn(min = 42.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(options.distinct(), key = { it }) { option ->
            var focused by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(3.dp)
            Box(
                modifier = Modifier.onFocusChanged { focused = it.isFocused }
                    .clip(shape)
                    .background(if (focused) Color.White else if (option == selected) Color.White.copy(alpha = 0.2f) else TvSurface)
                    .border(1.dp, if (focused) TvAccent else Color.White.copy(alpha = 0.1f), shape)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(option, color = if (focused) Color.Black else Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun TvCatalogControls(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    filters: List<String>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 520.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TvSearchBox(query, onQueryChange, placeholder)
                TvFilterRow(filters, selectedFilter, onSelectFilter, Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvSearchBox(query, onQueryChange, placeholder, Modifier.weight(0.42f))
                TvFilterRow(filters, selectedFilter, onSelectFilter, Modifier.weight(0.58f))
            }
        }
    }
}

@Composable
internal fun TvStatus(message: String, modifier: Modifier = Modifier) {
    if (message.isNotBlank()) {
        Text(message, modifier = modifier.padding(vertical = 6.dp), color = TvMuted, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun TvEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(32.dp))
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(message, color = TvMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            if (action != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                TvActionButton(action, onAction, primary = true)
            }
        }
    }
}

/** Read-only panels are focusable so the remote can scroll long settings pages. */
@Composable
internal fun TvInfoPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
            .background(TvSurface, shape)
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color.White.copy(alpha = 0.08f), shape)
            .focusable()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun TvFavoriteButton(saved: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.size(34.dp).onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (focused) Color.White else Color.Black.copy(alpha = 0.7f))
            .border(if (focused) 2.dp else 1.dp, if (focused) TvAccent else Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (saved) "Quitar de mi lista" else "Agregar a mi lista",
            tint = if (focused) Color.Black else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun TvCatalogMediaCard(
    item: MediaItem,
    featured: Boolean,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        TvMediaArtwork(item, Modifier.fillMaxSize(), featured = featured, onClick = onClick)
        if (onToggleFavorite != null) TvFavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd).padding(5.dp))
    }
}

@Composable
internal fun TvCatalogChannelCard(
    channel: LiveChannel,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        TvChannelArtwork(channel, Modifier.fillMaxSize(), onClick)
        if (onToggleFavorite != null) TvFavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd).padding(5.dp))
    }
}

@Composable
internal fun TvMediaGrid(
    items: List<MediaItem>,
    onOpenMedia: (String) -> Unit,
    modifier: Modifier = Modifier,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: ((String) -> Unit)? = null
) {
    val titles = remember(items) { items.distinctBy(MediaItem::id) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = tvCatalogColumnCount(maxWidth.value)
        val featuredHeight = tvFeaturedTileHeight(maxWidth.value, maxHeight.value, columns).dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            itemsIndexed(
                titles,
                key = { _, item -> item.id },
                span = { index, _ -> GridItemSpan(tvCatalogItemSpan(columns, index, titles.size)) }
            ) { index, item ->
                val featured = tvCatalogItemSpan(columns, index, titles.size) > 1
                TvCatalogMediaCard(
                    item = item,
                    featured = featured,
                    modifier = if (featured) Modifier.fillMaxWidth().height(featuredHeight) else Modifier.fillMaxWidth().aspectRatio(1.8f),
                    isFavorite = item.id in favorites,
                    onToggleFavorite = onToggleFavorite?.let { toggle -> { toggle(item.id) } },
                    onClick = { onOpenMedia(item.id) }
                )
            }
        }
    }
}

@Composable
internal fun TvChannelGrid(channels: List<LiveChannel>, onOpenChannel: (String) -> Unit, modifier: Modifier = Modifier) {
    val titles = remember(channels) { channels.distinctBy(LiveChannel::id) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = tvCatalogColumnCount(maxWidth.value)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            itemsIndexed(titles, key = { _, channel -> channel.id }) { _, channel ->
                TvCatalogChannelCard(channel, Modifier.fillMaxWidth().aspectRatio(1.8f), onClick = { onOpenChannel(channel.id) })
            }
        }
    }
}

@Composable
internal fun TvMediaRail(
    items: List<MediaItem>,
    onOpenMedia: (String) -> Unit,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: ((String) -> Unit)? = null
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        items(items.distinctBy(MediaItem::id), key = MediaItem::id) { item ->
            TvCatalogMediaCard(
                item = item,
                featured = false,
                modifier = Modifier.width(205.dp).aspectRatio(1.8f),
                isFavorite = item.id in favorites,
                onToggleFavorite = onToggleFavorite?.let { toggle -> { toggle(item.id) } },
                onClick = { onOpenMedia(item.id) }
            )
        }
    }
}

@Composable
internal fun TvChannelRail(
    channels: List<LiveChannel>,
    onOpenChannel: (String) -> Unit,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: ((String) -> Unit)? = null
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        items(channels.distinctBy(LiveChannel::id), key = LiveChannel::id) { channel ->
            TvCatalogChannelCard(
                channel = channel,
                modifier = Modifier.width(205.dp).aspectRatio(1.8f),
                isFavorite = channel.id in favorites,
                onToggleFavorite = onToggleFavorite?.let { toggle -> { toggle(channel.id) } },
                onClick = { onOpenChannel(channel.id) }
            )
        }
    }
}
