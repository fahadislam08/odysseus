package com.odysseus.wrapper.ui.screens.gallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odysseus.wrapper.data.remote.*
import com.odysseus.wrapper.data.repository.GalleryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GalleryRepository()
    private val ctx  = app

    private val _images  = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _images

    private val _albums  = MutableStateFlow<List<GalleryAlbum>>(emptyList())
    val albums: StateFlow<List<GalleryAlbum>> = _albums

    private val _stats   = MutableStateFlow<GalleryStats?>(null)
    val stats: StateFlow<GalleryStats?> = _stats

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    private val _error   = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum

    init { load() }

    fun load(albumId: String? = null) = viewModelScope.launch {
        _loading.value = true
        repo.library(albumId = albumId).fold(onSuccess = { _images.value = it }, onFailure = { _error.value = it.message })
        repo.albums().fold(onSuccess = { _albums.value = it }, onFailure = {})
        repo.stats().fold(onSuccess = { _stats.value = it }, onFailure = {})
        _loading.value = false
    }

    fun setAlbum(albumId: String?) { _selectedAlbum.value = albumId; load(albumId) }

    fun uploadFromUri(uri: Uri) = viewModelScope.launch {
        _uploading.value = true
        try {
            val inputStream = ctx.contentResolver.openInputStream(uri)
            val tmpFile = File.createTempFile("upload_", ".jpg", ctx.cacheDir)
            tmpFile.outputStream().use { out -> inputStream?.copyTo(out) }
            repo.upload(tmpFile).fold(
                onSuccess = { _images.value = listOf(it) + _images.value },
                onFailure = { _error.value = it.message }
            )
            tmpFile.delete()
        } catch (e: Exception) {
            _error.value = e.message
        }
        _uploading.value = false
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id).fold(
            onSuccess = { _images.value = _images.value.filter { it.id != id } },
            onFailure = { _error.value = it.message }
        )
    }

    fun favorite(id: String) = viewModelScope.launch {
        repo.favorite(id).fold(onSuccess = { load(_selectedAlbum.value) }, onFailure = { _error.value = it.message })
    }

    fun aiTag(id: String) = viewModelScope.launch {
        repo.aiTag(id).fold(
            onSuccess = { updated -> _images.value = _images.value.map { if (it.id == id) updated else it } },
            onFailure = { _error.value = it.message }
        )
    }

    fun aiTagAll() = viewModelScope.launch {
        val ids = _images.value.map { it.id }
        if (ids.isEmpty()) return@launch
        repo.aiTagBatch(ids).fold(
            onSuccess = { load(_selectedAlbum.value) },
            onFailure = { _error.value = it.message }
        )
    }

    fun createAlbum(name: String) = viewModelScope.launch {
        repo.createAlbum(name).fold(
            onSuccess = { _albums.value = _albums.value + it },
            onFailure = { _error.value = it.message }
        )
    }

    fun deleteAlbum(id: String) = viewModelScope.launch {
        repo.deleteAlbum(id).fold(
            onSuccess = { _albums.value = _albums.value.filter { it.id != id }; if (_selectedAlbum.value == id) setAlbum(null) },
            onFailure = { _error.value = it.message }
        )
    }

    fun clearError() { _error.value = null }
}
