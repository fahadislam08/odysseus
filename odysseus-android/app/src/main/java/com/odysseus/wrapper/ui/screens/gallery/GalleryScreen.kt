package com.odysseus.wrapper.ui.screens.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.odysseus.wrapper.core.NetworkClient
import com.odysseus.wrapper.data.remote.GalleryImage
import com.odysseus.wrapper.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(vm: GalleryViewModel = viewModel()) {
    val images       by vm.images.collectAsStateWithLifecycle()
    val albums       by vm.albums.collectAsStateWithLifecycle()
    val stats        by vm.stats.collectAsStateWithLifecycle()
    val loading      by vm.loading.collectAsStateWithLifecycle()
    val uploading    by vm.uploading.collectAsStateWithLifecycle()
    val error        by vm.error.collectAsStateWithLifecycle()
    val selectedAlbum by vm.selectedAlbum.collectAsStateWithLifecycle()

    var showNewAlbum  by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<GalleryImage?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.uploadFromUri(it) } }

    if (showNewAlbum) {
        var albumName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewAlbum = false },
            title = { Text("New Album") },
            text = {
                OutlinedTextField(value = albumName, onValueChange = { albumName = it },
                    label = { Text("Album name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = { vm.createAlbum(albumName); showNewAlbum = false },
                    enabled = albumName.isNotBlank()) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewAlbum = false }) { Text("Cancel") } }
        )
    }

    // Full-screen image detail
    selectedImage?.let { img ->
        ImageDetailSheet(img,
            baseUrl = NetworkClient.baseUrl,
            onClose  = { selectedImage = null },
            onFav    = { vm.favorite(img.id) },
            onAiTag  = { vm.aiTag(img.id) },
            onDelete = { vm.delete(img.id); selectedImage = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery", fontWeight = FontWeight.Bold) },
                actions = {
                    stats?.let {
                        Text("${it.total_images} imgs", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                            modifier = Modifier.padding(end = 8.dp))
                    }
                    if (uploading) CircularProgressIndicator(Modifier.size(20.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    IconButton(onClick = { vm.aiTagAll() }) { Icon(Icons.Default.AutoAwesome, "AI Tag all") }
                    IconButton(onClick = { vm.load(selectedAlbum) }) { Icon(Icons.Default.Refresh, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Upload")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            error?.let {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(it, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            // Albums row
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = selectedAlbum == null, onClick = { vm.setAlbum(null) },
                        label = { Text("All") })
                }
                items(albums, key = { it.id }) { album ->
                    FilterChip(
                        selected = selectedAlbum == album.id,
                        onClick  = { vm.setAlbum(album.id) },
                        label    = { Text(album.name) },
                        trailingIcon = {
                            IconButton(onClick = { vm.deleteAlbum(album.id) }, Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, null, Modifier.size(12.dp))
                            }
                        }
                    )
                }
                item {
                    IconButton(onClick = { showNewAlbum = true }) {
                        Icon(Icons.Default.Add, "New album",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            when {
                loading         -> LoadingBox()
                images.isEmpty() -> EmptyBox("No images. Tap + to upload one.")
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement   = Arrangement.spacedBy(4.dp)
                ) {
                    items(images, key = { it.id }) { img ->
                        GalleryTile(img, baseUrl = NetworkClient.baseUrl,
                            onTap = { selectedImage = img })
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryTile(img: GalleryImage, baseUrl: String, onTap: () -> Unit) {
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)).clickable { onTap() }
        .background(MaterialTheme.colorScheme.surface)) {
        AsyncImage(
            model = "${baseUrl}${img.url.trimStart('/')}",
            contentDescription = img.filename,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (img.is_favorite) {
            Icon(Icons.Default.Favorite, null, Modifier.size(16.dp).align(Alignment.TopEnd).padding(4.dp),
                tint = MaterialTheme.colorScheme.error)
        }
        if (img.ai_tags.isNotEmpty()) {
            Box(Modifier.align(Alignment.BottomStart).padding(4.dp)
                .background(MaterialTheme.colorScheme.background.copy(0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(img.ai_tags.first(), fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailSheet(
    img: GalleryImage,
    baseUrl: String,
    onClose: () -> Unit,
    onFav: () -> Unit,
    onAiTag: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(img.filename, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) } },
                actions = {
                    IconButton(onClick = onFav) {
                        Icon(if (img.is_favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null, tint = if (img.is_favorite) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onAiTag) { Icon(Icons.Default.AutoAwesome, "AI Tag") }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AsyncImage(
                model = "${baseUrl}${img.url.trimStart('/')}",
                contentDescription = img.filename,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Column(Modifier.padding(16.dp)) {
                Text("${img.width}×${img.height} px", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
                Text(img.created_at.take(10), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                val allTags = img.tags + img.ai_tags
                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Tags:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(allTags) { tag -> SuggestionChip(onClick = {}, label = { Text(tag, fontSize = 11.sp) }) }
                    }
                }
            }
        }
    }
}
