package com.vela.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vela.player.ui.VelaMainViewModel
import com.vela.player.ui.components.VelaIcon
import com.vela.player.ui.components.VelaIcons
import com.vela.player.ui.downloader.DownloaderScreen
import com.vela.player.ui.downloader.InstagramLoginDialog
import com.vela.player.ui.gallery.GalleryScreen
import com.vela.player.ui.player.PlayerActivity
import com.vela.player.ui.settings.SettingsScreen
import com.vela.player.ui.theme.ThemeMode
import com.vela.player.ui.theme.VelaRadius
import com.vela.player.ui.theme.VelaSize
import com.vela.player.ui.theme.VelaSpacing
import com.vela.player.ui.theme.VelaTheme

private enum class VelaTab(val label: String, val iconRes: Int) {
    GALLERY("Gallery", VelaIcons.Film),
    DOWNLOADER("Download", VelaIcons.Download),
    SETTINGS("Settings", VelaIcons.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: VelaMainViewModel by viewModels()
    private var incomingShareTabTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeShareIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            VelaTheme(themeMode = settings.themeMode) {
                VelaPlayerRoot(
                    viewModel = viewModel,
                    shareTrigger = incomingShareTabTrigger
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeShareIntent(intent)
    }

    private fun consumeShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val viewed = intent.dataString
            if (!viewed.isNullOrBlank()) {
                viewModel.handleSharedLink(viewed)
                incomingShareTabTrigger++
            }
            return
        }
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleSharedLink(sharedText)
                incomingShareTabTrigger++
            }
        }
    }
}

@Composable
private fun VelaPlayerRoot(
    viewModel: VelaMainViewModel,
    shareTrigger: Int
) {
    val context = LocalContext.current
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableStateOf(VelaTab.GALLERY) }

    val settings by viewModel.settings.collectAsState()
    val engineReady by viewModel.engineReady.collectAsState()
    val engineError by viewModel.engineError.collectAsState()
    val downloaderState by viewModel.downloaderState.collectAsState()
    val downloadJobs by viewModel.downloadJobs.collectAsState()
    val galleryItems by viewModel.filteredGalleryItems.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val starredCount by viewModel.starredCount.collectAsState()
    val galleryScope by viewModel.galleryScope.collectAsState()
    val galleryType by viewModel.galleryType.collectAsState()
    val gallerySearch by viewModel.gallerySearch.collectAsState()
    val isScanning by viewModel.isScanningDevice.collectAsState()
    val isUpdatingEngine by viewModel.isUpdatingEngine.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()

    // Switch straight to Downloader tab when receiving a YouTube/Instagram Share Sheet intent
    LaunchedEffect(shareTrigger) {
        if (shareTrigger > 0) {
            activeTab = VelaTab.DOWNLOADER
        }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    // Request Android 13+ Notification permission (for Foreground Download Progress) cleanly
    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Runtime permission launcher for scanning local device videos
    val mediaScanPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.scanDeviceVideos()
        }
    }

    val requestScanWithPermission: () -> Unit = {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.scanDeviceVideos()
        } else {
            mediaScanPermissionLauncher.launch(perm)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgCanvas),
        containerColor = colors.bgCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            VelaTopBrandBar(
                themeMode = settings.themeMode,
                onCycleTheme = {
                    val next = when (settings.themeMode) {
                        ThemeMode.DARK -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                    }
                    viewModel.setThemeMode(next)
                }
            )
        },
        bottomBar = {
            VelaBottomNavBar(
                activeTab = activeTab,
                onSelectTab = { activeTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                VelaTab.GALLERY -> GalleryScreen(
                    items = galleryItems,
                    totalCount = totalCount,
                    starredCount = starredCount,
                    scopeFilter = galleryScope,
                    typeFilter = galleryType,
                    searchQuery = gallerySearch,
                    isScanning = isScanning,
                    onScopeChange = viewModel::setGalleryScope,
                    onTypeChange = viewModel::setGalleryType,
                    onSearchChange = viewModel::setGallerySearch,
                    onScanDevice = requestScanWithPermission,
                    onPlayMedia = { mediaItem ->
                        val queueIds = galleryItems
                            .filter { it.parsedMediaType != com.vela.player.data.local.MediaType.IMAGE }
                            .map { it.id }
                            .toLongArray()
                        context.startActivity(PlayerActivity.createIntent(context, mediaItem, queueIds))
                    },
                    onToggleStar = viewModel::toggleStar,
                    onDeleteMedia = { item -> viewModel.deleteMediaItem(item, deleteFileOnDisk = false) },
                    onGoToDownloader = { activeTab = VelaTab.DOWNLOADER }
                )

                VelaTab.DOWNLOADER -> DownloaderScreen(
                    state = downloaderState,
                    jobs = downloadJobs,
                    engineReady = engineReady,
                    engineError = engineError,
                    igLoggedIn = settings.igLoggedIn,
                    onUrlChange = viewModel::updateUrlInput,
                    onAnalyze = viewModel::analyzeCurrentLink,
                    onSelectOption = viewModel::selectDownloadOption,
                    onStartDownload = viewModel::startSelectedDownload,
                    onStreamNow = { media -> context.startActivity(PlayerActivity.createStreamIntent(context, media)) },
                    onOpenInstagramLogin = viewModel::openInstagramLogin,
                    onCancelJob = viewModel::cancelDownload,
                    onDismissJob = viewModel::dismissDownload,
                    onOpenGallery = { activeTab = VelaTab.GALLERY }
                )

                VelaTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    isUpdatingEngine = isUpdatingEngine,
                    onSelectTheme = viewModel::setThemeMode,
                    onSelectDefaultQuality = viewModel::setDefaultQuality,
                    onOpenInstagramLogin = viewModel::openInstagramLogin,
                    onLogoutInstagram = viewModel::logoutInstagram,
                    onUpdateEngine = viewModel::updateYtDlpEngine
                )
            }
        }
    }

    // In-App Instagram Login Modal (triggered from Downloader or Settings)
    if (downloaderState.showInstagramLogin) {
        InstagramLoginDialog(
            cookieStore = viewModel.cookieStore,
            onAuthenticated = viewModel::onInstagramAuthenticated,
            onDismiss = viewModel::closeInstagramLogin
        )
    }
}

@Composable
private fun VelaTopBrandBar(
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSidebar)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VelaSpacing.s16, vertical = VelaSpacing.s12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VelaRadius.control))
                        .background(colors.brandPrimary)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VelaIcon(
                        iconRes = VelaIcons.Play,
                        contentDescription = null,
                        size = VelaSize.iconSm,
                        tint = colors.actionPrimaryFg
                    )
                }
                Text(
                    text = "VelaPlayer",
                    style = typography.section,
                    color = colors.textPrimary
                )
            }

            // Quick Light/Dark Toggle right in the top bar
            Row(
                modifier = Modifier
                    .heightIn(min = VelaSize.minTouchTarget)
                    .clip(RoundedCornerShape(VelaRadius.pill))
                    .background(colors.bgSurface)
                    .border(BorderStroke(1.dp, colors.borderSubtle), RoundedCornerShape(VelaRadius.pill))
                    .clickable(onClick = onCycleTheme)
                    .padding(horizontal = VelaSpacing.s12, vertical = VelaSpacing.s8),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
            ) {
                VelaIcon(
                    iconRes = if (colors.isDark) VelaIcons.Sun else VelaIcons.Moon,
                    contentDescription = "Switch theme",
                    size = VelaSize.iconSm,
                    tint = colors.brandPrimary
                )
                Text(
                    text = if (colors.isDark) "Light" else "Dark",
                    style = typography.caption,
                    color = colors.textPrimary
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.borderSubtle)
    }
}

@Composable
private fun VelaBottomNavBar(
    activeTab: VelaTab,
    onSelectTab: (VelaTab) -> Unit
) {
    val colors = VelaTheme.colors
    val typography = VelaTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSidebar)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.borderSubtle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VelaSpacing.s12, vertical = VelaSpacing.s8),
            horizontalArrangement = Arrangement.spacedBy(VelaSpacing.s8)
        ) {
            VelaTab.entries.forEach { tab ->
                val selected = tab == activeTab
                val shape = RoundedCornerShape(VelaRadius.card)
                val bg = if (selected) colors.brandPrimary.copy(alpha = 0.14f) else Color.Transparent
                val fg = if (selected) colors.brandPrimary else colors.textSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = VelaSize.minTouchTarget)
                        .clip(shape)
                        .background(bg)
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = VelaSpacing.s8),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(VelaSpacing.s4)
                ) {
                    VelaIcon(
                        iconRes = tab.iconRes,
                        contentDescription = tab.label,
                        size = VelaSize.iconMd,
                        tint = fg
                    )
                    Text(
                        text = tab.label,
                        style = typography.caption,
                        color = fg,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
