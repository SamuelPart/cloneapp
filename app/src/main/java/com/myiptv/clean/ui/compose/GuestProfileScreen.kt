package com.myiptv.clean.ui.compose

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.myiptv.clean.BuildConfig
import com.myiptv.clean.data.repository.GuestProfile
import com.myiptv.clean.data.repository.GuestProfileRepository
import kotlinx.coroutines.launch

@Composable
internal fun GuestProfileScreen(
    username: String? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { GuestProfileRepository(context) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context).components { add(GifDecoder.Factory()) }.build()
    }
    val ownerMode = username.isNullOrBlank()
    var profile by remember { mutableStateOf<GuestProfile?>(null) }
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var coverAnimated by remember { mutableStateOf(false) }
    var removeAvatar by remember { mutableStateOf(false) }
    var removeCover by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, username) {
        loading = true
        if (ownerMode) {
            profile = repository.myProfile()
            profile?.let {
                name = it.displayName
                handle = it.username
                coverAnimated = it.coverAnimated
            }
        } else {
            runCatching { repository.find(username.orEmpty()) }
                .onSuccess { profile = it }
                .onFailure { message = it.message ?: "No se encontró el perfil." }
        }
        loading = false
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { avatarUri = uri; removeAvatar = false; message = null }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coverUri = uri
            removeCover = false
            coverAnimated = context.contentResolver.getType(uri)?.equals("image/gif", ignoreCase = true) == true
            message = null
        }
    }
    val profileLink = profile?.let { "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/guest-profiles?username=${Uri.encode(it.username)}" }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0D0E12)).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (!ownerMode) IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) }
            Text(if (ownerMode) "Mi perfil" else "Perfil", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (profile != null) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    val link = profileLink ?: return@TextButton
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Mira el perfil @${profile?.username} en SamPlay: $link")
                    }, "Compartir perfil"))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Color(0xFFFF5678))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir", color = Color(0xFFFF8BA0))
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1.9f).clip(RoundedCornerShape(20.dp))
                .background(BrushProfileCover)
                .clickable(enabled = ownerMode) { coverPicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            val coverModel: Any? = coverUri ?: profile?.coverUrl
            if (coverModel != null && !removeCover) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(coverModel).crossfade(true).build(),
                    imageLoader = imageLoader,
                    contentDescription = "Portada del perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(36.dp))
                    Text(if (ownerMode) "Añade una portada · foto o GIF" else "Aún no hay portada", color = Color.White.copy(alpha = 0.82f))
                }
            }
            if (ownerMode) {
                Text("Cambiar portada", modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(82.dp).clip(CircleShape).background(Color(0xFF242631))
                    .clickable(enabled = ownerMode) { avatarPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val avatarModel: Any? = avatarUri ?: profile?.avatarUrl
                if (avatarModel != null && !removeAvatar) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(avatarModel).crossfade(true).build(),
                        imageLoader = imageLoader,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(38.dp))
            }
            Column {
                Text(profile?.displayName ?: if (ownerMode) "Tu nombre visible" else "", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                profile?.let { Text("@${it.username}", color = Color(0xFFB3B4BF)) }
                if (ownerMode) Text(if (avatarUri != null) "Foto seleccionada" else "Toca el círculo para elegir foto", color = Color(0xFFB3B4BF), style = MaterialTheme.typography.bodySmall)
            }
        }

        if (ownerMode) {
            Text("Sin cuenta ni contraseña: el perfil se guarda en este dispositivo y se publica para compartirlo.", color = Color(0xFFB3B4BF), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text("Nombre visible") }, singleLine = true)
            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it.removePrefix("@").lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' || ch == '.' }.take(20) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("@usuario") },
                prefix = { Text("@") },
                supportingText = { Text("3–20 caracteres · letras, números, punto o guion bajo") },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { avatarPicker.launch("image/*") }) { Text("Elegir foto de perfil") }
                TextButton(onClick = { coverPicker.launch("image/*") }) { Text("Elegir portada") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profile?.avatarUrl != null) TextButton(onClick = { avatarUri = null; removeAvatar = true }) { Text("Quitar avatar") }
                if (profile?.coverUrl != null) TextButton(onClick = { coverUri = null; removeCover = true; coverAnimated = false }) { Text("Quitar portada") }
                if (coverAnimated) Text("GIF animado", color = Color(0xFFB3B4BF), modifier = Modifier.align(Alignment.CenterVertically))
            }
            Button(
                onClick = {
                    saving = true
                    message = null
                    scope.launch {
                        runCatching { repository.save(name, handle, avatarUri, coverUri, removeAvatar, removeCover, coverAnimated) }
                            .onSuccess { profile = it; avatarUri = null; coverUri = null; removeAvatar = false; removeCover = false; message = "Perfil guardado. Ya puedes compartirlo." }
                            .onFailure { message = it.message ?: "No se pudo guardar el perfil." }
                        saving = false
                    }
                },
                enabled = !saving && !loading && name.isNotBlank() && handle.length >= 3,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (saving) "Guardando…" else if (profile == null) "Crear perfil" else "Guardar cambios")
            }
        } else if (profile != null) {
            Surface(color = Color(0xFF171920), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(profile!!.displayName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("@${profile!!.username}", color = Color(0xFFFF8BA0), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (profile != null) {
            Surface(color = Color(0xFF171920), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ideas para compartir", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ShareIdea("1", "Enlace directo", "Manda tu perfil por WhatsApp, mensajes o redes.")
                    ShareIdea("2", "Tarjeta para historias", "Una imagen con tu avatar, portada y @usuario, lista para publicar.")
                    ShareIdea("3", "Código QR", "Pon un QR en una invitación o muéstralo en persona para abrir tu perfil.")
                    if (profileLink != null) TextButton(onClick = { clipboard.setText(AnnotatedString(profileLink)); message = "Enlace copiado." }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar enlace", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Color(0xFFFF5678))
        message?.let { Text(it, color = if (it.startsWith("Perfil guardado") || it.contains("copiado")) Color(0xFF8DE6AE) else Color(0xFFFFA0A0), style = MaterialTheme.typography.bodyMedium) }
        if (ownerMode && profile == null && !loading) Text("Tu perfil aún no se comparte. Elige un nombre y un @usuario para empezar.", color = Color(0xFFB3B4BF), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ShareIdea(number: String, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = Color(0xFF38212C), modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(number, color = Color(0xFFFF8BA0), fontWeight = FontWeight.Bold) }
        }
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Color(0xFFB3B4BF), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val BrushProfileCover = androidx.compose.ui.graphics.Brush.linearGradient(
    listOf(Color(0xFF442336), Color(0xFF171A2A), Color(0xFF28234D))
)
