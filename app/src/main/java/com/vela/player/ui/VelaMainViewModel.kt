package com.vela.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vela.player.VelaPlayerApp
import com.vela.player.data.downloader.AnalyzedMedia
import com.vela.player.data.downloader.DownloadJob
import com.vela.player.data.downloader.DownloadOption
import com.vela.player.data.local.MediaItemEntity
import com.vela.player.data.local.MediaType
import com.vela.player.data.local.UserSettings
import com.vela.player.ui.theme.ThemeMode
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class GalleryScopeFilter(val label: String) {
    ALL("All"),
    STARRED("Starred")
}

enum class GalleryTypeFilter(val label: String) {
    ALL("Everything"),
    VIDEO("Videos"),
    AUDIO("Audio"),
    IMAGE("Images")
}

data class DownloaderUiState(
    val urlInput: String = "",
    val isAnalyzing: Boolean = false,
    val analyzedMedia: AnalyzedMedia? = null,
    val selectedOption: DownloadOption = DownloadOption.VIDEO_720P,
    val showInstagramLogin: Boolean = false,
    val snackbarMessage: String? = null
)

class VelaMainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VelaPlayerApp
    private val mediaDao = app.database.mediaDao()
    private val prefs = app.preferences

    val engineReady: StateFlow<Boolean> = app.engineReady
    val engineError: StateFlow<String?> = app.engineError
    val downloadJobs: StateFlow<List<DownloadJob>> = app.downloadRepository.jobs

    val settings: StateFlow<UserSettings> = prefs.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    val totalCount: StateFlow<Int> = mediaDao.observeTotalCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )

    val starredCount: StateFlow<Int> = mediaDao.observeStarredCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )

    // Gallery filters
    private val _galleryScope = MutableStateFlow(GalleryScopeFilter.STARRED)
    val galleryScope: StateFlow<GalleryScopeFilter> = _galleryScope.asStateFlow()

    private val _galleryType = MutableStateFlow(GalleryTypeFilter.ALL)
    val galleryType: StateFlow<GalleryTypeFilter> = _galleryType.asStateFlow()

    private val _gallerySearch = MutableStateFlow("")
    val gallerySearch: StateFlow<String> = _gallerySearch.asStateFlow()

    private val _isScanningDevice = MutableStateFlow(false)
    val isScanningDevice: StateFlow<Boolean> = _isScanningDevice.asStateFlow()

    val filteredGalleryItems: StateFlow<List<MediaItemEntity>> = combine(
        mediaDao.observeAll(),
        _galleryScope,
        _galleryType,
        _gallerySearch
    ) { items, scopeFilter, typeFilter, query ->
        val trimmed = query.trim().lowercase()
        items.filter { item ->
            val matchesScope = scopeFilter == GalleryScopeFilter.ALL || item.isStarred
            val matchesType = when (typeFilter) {
                GalleryTypeFilter.ALL -> true
                GalleryTypeFilter.VIDEO -> item.parsedMediaType == MediaType.VIDEO
                GalleryTypeFilter.AUDIO -> item.parsedMediaType == MediaType.AUDIO
                GalleryTypeFilter.IMAGE -> item.parsedMediaType == MediaType.IMAGE
            }
            val matchesSearch = trimmed.isEmpty() ||
                item.title.lowercase().contains(trimmed) ||
                item.sourcePlatform.lowercase().contains(trimmed)
            matchesScope && matchesType && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Downloader UI state
    private val _downloaderState = MutableStateFlow(DownloaderUiState())
    val downloaderState: StateFlow<DownloaderUiState> = _downloaderState.asStateFlow()

    // Settings status feedback
    private val _isUpdatingEngine = MutableStateFlow(false)
    val isUpdatingEngine: StateFlow<Boolean> = _isUpdatingEngine.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val cookieStore get() = app.instagramCookieStore

    init {
        // Sync persisted Instagram login badge with actual cookie store on startup
        viewModelScope.launch {
            prefs.setIgLoggedIn(app.instagramCookieStore.hasValidSession())
        }
    }

    // --- Gallery Controls ---
    fun setGalleryScope(scope: GalleryScopeFilter) {
        _galleryScope.value = scope
    }

    fun setGalleryType(type: GalleryTypeFilter) {
        _galleryType.value = type
    }

    fun setGallerySearch(query: String) {
        _gallerySearch.value = query
    }

    fun toggleStar(item: MediaItemEntity) {
        viewModelScope.launch {
            mediaDao.setStarred(item.id, !item.isStarred)
        }
    }

    fun deleteMediaItem(item: MediaItemEntity, deleteFileOnDisk: Boolean = false) {
        viewModelScope.launch {
            mediaDao.deleteById(item.id)
            if (deleteFileOnDisk && !item.filePath.startsWith("content://")) {
                runCatching { File(item.filePath).delete() }
            }
            _statusBanner.value = "Removed \"${item.title}\""
        }
    }

    fun scanDeviceVideos() {
        if (_isScanningDevice.value) return
        viewModelScope.launch {
            _isScanningDevice.value = true
            val added = runCatching { app.localMediaScanner.scanDeviceVideos() }.getOrDefault(0)
            _isScanningDevice.value = false
            _statusBanner.value = if (added > 0) {
                "Imported $added offline video${if (added > 1) "s" else ""} from device"
            } else {
                "Library is up to date"
            }
        }
    }

    // --- Downloader Controls ---
    fun updateUrlInput(input: String) {
        _downloaderState.value = _downloaderState.value.copy(
            urlInput = input,
            snackbarMessage = null
        )
    }

    fun handleSharedLink(sharedText: String) {
        val extracted = app.linkAnalyzer.extractSupportedUrl(sharedText) ?: sharedText.trim()
        _downloaderState.value = _downloaderState.value.copy(urlInput = extracted)
        analyzeCurrentLink()
    }

    fun analyzeCurrentLink() {
        val raw = _downloaderState.value.urlInput.trim()
        if (raw.isEmpty()) return

        viewModelScope.launch {
            _downloaderState.value = _downloaderState.value.copy(
                isAnalyzing = true,
                analyzedMedia = null,
                snackbarMessage = null
            )
            val result = app.linkAnalyzer.analyze(raw)
            val defaultOpt = DownloadOption.fromId(settings.value.defaultQuality)
            val chosen = when {
                result.availableOptions.contains(defaultOpt) -> defaultOpt
                result.availableOptions.isNotEmpty() -> result.availableOptions.first()
                else -> DownloadOption.VIDEO_720P
            }
            _downloaderState.value = _downloaderState.value.copy(
                isAnalyzing = false,
                analyzedMedia = result,
                selectedOption = chosen,
                showInstagramLogin = false
            )
        }
    }

    fun selectDownloadOption(option: DownloadOption) {
        _downloaderState.value = _downloaderState.value.copy(selectedOption = option)
    }

    fun startSelectedDownload() {
        val state = _downloaderState.value
        val analyzed = state.analyzedMedia ?: return
        if (analyzed.availableOptions.isEmpty()) return

        app.downloadRepository.enqueueDownload(analyzed, state.selectedOption)
        _statusBanner.value = "Downloading ${analyzed.title} (${state.selectedOption.label})"
    }

    fun cancelDownload(jobId: String) {
        app.downloadRepository.cancelJob(jobId)
    }

    fun dismissDownload(jobId: String) {
        app.downloadRepository.dismissJob(jobId)
    }

    fun openInstagramLogin() {
        _downloaderState.value = _downloaderState.value.copy(showInstagramLogin = true)
    }

    fun closeInstagramLogin() {
        _downloaderState.value = _downloaderState.value.copy(showInstagramLogin = false)
    }

    fun onInstagramAuthenticated() {
        viewModelScope.launch {
            prefs.setIgLoggedIn(true)
            _downloaderState.value = _downloaderState.value.copy(showInstagramLogin = false)
            _statusBanner.value = "Instagram connected — retrying link"
            if (_downloaderState.value.urlInput.isNotBlank()) {
                analyzeCurrentLink()
            }
        }
    }

    // --- Settings Controls ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setDefaultQuality(option: DownloadOption) {
        viewModelScope.launch { prefs.setDefaultQuality(option.id) }
    }

    fun logoutInstagram() {
        viewModelScope.launch {
            app.instagramCookieStore.clearSession()
            prefs.setIgLoggedIn(false)
            _statusBanner.value = "Signed out of Instagram and cleared local cookies"
        }
    }

    fun updateYtDlpEngine() {
        if (_isUpdatingEngine.value) return
        viewModelScope.launch {
            _isUpdatingEngine.value = true
            val result = runCatching { app.downloadRepository.updateYtDlpEngine() }
            _isUpdatingEngine.value = false
            result.onSuccess { status ->
                prefs.setLastYtdlpUpdateMs(System.currentTimeMillis())
                _statusBanner.value = when (status) {
                    YoutubeDL.UpdateStatus.DONE -> "yt-dlp engine updated to latest release"
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp engine is already up to date"
                    null -> "Update check finished"
                }
            }.onFailure { err ->
                _statusBanner.value = "Engine update failed: ${err.message?.take(80) ?: "network error"}"
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }
}
