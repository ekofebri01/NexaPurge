package com.example.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.StorageCircularDashboard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entities.CleanHistory
import com.example.data.entities.QuarantinedFile
import com.example.data.repository.JunkCategory
import com.example.data.repository.ScannedJunkFile
import kotlinx.coroutines.delay
import com.example.data.repository.StorageStats
import com.example.viewmodel.NexaPurgeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexaPurgeApp(
    viewModel: NexaPurgeViewModel = viewModel(),
    darkThemeState: MutableState<Boolean>
) {
    val context = LocalContext.current
    val currentPage by viewModel.currentSelectedPage.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Toast watcher
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentPage != "scan_active") {
                NexaBottomBar(
                    currentPage = currentPage,
                    onPageSelected = { viewModel.selectPage(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentPage) {
                "dashboard" -> DashboardScreen(viewModel = viewModel)
                "trash" -> TrashBinScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel, darkThemeState = darkThemeState)
                else -> DashboardScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NexaBottomBar(
    currentPage: String,
    onPageSelected: (String) -> Unit
) {
    val items = listOf(
        Triple("dashboard", "Dasbor", Icons.Default.Refresh),
        Triple("trash", "Karantina", Icons.Default.Delete),
        Triple("settings", "Setelan", Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        items.forEach { (route, label, icon) ->
            val isSelected = currentPage == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onPageSelected(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(viewModel: NexaPurgeViewModel) {
    val stats by viewModel.storageStats.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isCleaning by viewModel.isCleaning.collectAsStateWithLifecycle()
    val scannedFiles by viewModel.scannedFiles.collectAsStateWithLifecycle()
    val selectedFileIds by viewModel.selectedFileIds.collectAsStateWithLifecycle()
    val lastCleanedSummary by viewModel.lastCleanedSummary.collectAsStateWithLifecycle()
    val rootEnabled by viewModel.rootAccessEnabled.collectAsStateWithLifecycle()
    val autoCleanEnabled by viewModel.autoCleanEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadStorageStats()
    }

    AnimatedContent(
        targetState = scanState,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
        },
        label = "DashboardFlow"
    ) { state ->
        when (state) {
            "idle" -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // App Header matching Sophisticated Dark
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEXAPURGE PRO",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                style = LocalTextStyle.current.copy(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF10B981),
                                            Color(0xFF06B6D4)
                                        )
                                    )
                                ),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SYSTEM OPTIMIZER V4.2",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 2.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (rootEnabled) Color(0xFF10B981).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, if (rootEnabled) Color(0xFF10B981).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (rootEnabled) Color(0xFF10B981) else Color(0xFF64748B), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (rootEnabled) "ROOT ACTIVE" else "STANDARD ACCESS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rootEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.selectPage("settings") },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Setelan",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Dashboard Component: Storage Usage Visualization (Used vs. Free Space)
                    stats?.let {
                        StorageCircularDashboard(
                            stats = it,
                            onOptimizeClick = { viewModel.startScan() },
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    // Quick Action Grid matching Sophisticated Dark design
                    DashboardQuickActionGrid(
                        onCacheClick = { viewModel.startScan() },
                        onRecoveryClick = { viewModel.selectPage("trash") },
                        onAutoSweepClick = { viewModel.selectPage("settings") },
                        onDeepRootClick = { viewModel.selectPage("settings") },
                        autoCleanEnabled = autoCleanEnabled,
                        rootEnabled = rootEnabled,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }

            "scanning" -> {
                ScanningProgressOverlay()
            }

            "scanned" -> {
                ScanResultsBoard(
                    scannedFiles = scannedFiles,
                    selectedFileIds = selectedFileIds,
                    onToggleFile = { viewModel.toggleFileSelection(it) },
                    onToggleAll = { viewModel.toggleSelectAll(it) },
                    onCleanClick = { viewModel.executeCleanUp() },
                    onBackClick = { viewModel.resetStateToIdle() },
                    isCleaning = isCleaning,
                    rootEnabled = rootEnabled
                )
            }

            "cleaned" -> {
                CleanSuccessSplash(
                    summary = lastCleanedSummary,
                    onBackButtonClick = { viewModel.resetStateToIdle() }
                )
            }
        }
    }
}

@Composable
fun StorageRingGauge(stats: StorageStats) {
    StorageCircularDashboard(stats = stats)
}

@Composable
fun DashboardQuickActionGrid(
    onCacheClick: () -> Unit,
    onRecoveryClick: () -> Unit,
    onAutoSweepClick: () -> Unit,
    onDeepRootClick: () -> Unit,
    autoCleanEnabled: Boolean,
    rootEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "FITUR PINTAR OPTIMASI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Action 1: Cache
            QuickActionTile(
                icon = Icons.Default.Delete,
                iconColor = Color(0xFF06B6D4),
                iconBg = Color(0xFF06B6D4).copy(alpha = 0.15f),
                title = "Cache & Temp",
                subtitle = "Pindai & Bersihkan",
                onClick = onCacheClick,
                modifier = Modifier.weight(1f)
            )

            // Quick Action 2: Recovery / Karantina
            QuickActionTile(
                icon = Icons.Default.Refresh,
                iconColor = Color(0xFFF97316),
                iconBg = Color(0xFFF97316).copy(alpha = 0.15f),
                title = "Karantina File",
                subtitle = "Pulihkan Berkas",
                onClick = onRecoveryClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Action 3: Auto-Sweep
            QuickActionTile(
                icon = Icons.Default.PlayArrow,
                iconColor = Color(0xFFA855F7),
                iconBg = Color(0xFFA855F7).copy(alpha = 0.15f),
                title = "Auto-Sweep",
                subtitle = if (autoCleanEnabled) "Aktif Terjadwal" else "Nonaktif",
                onClick = onAutoSweepClick,
                modifier = Modifier.weight(1f)
            )

            // Quick Action 4: Deep Root
            QuickActionTile(
                icon = Icons.Default.Lock,
                iconColor = Color(0xFFF43F5E),
                iconBg = Color(0xFFF43F5E).copy(alpha = 0.15f),
                title = "Deep Root",
                subtitle = if (rootEnabled) "Superuser Aktif" else "Konfigurasi",
                onClick = onDeepRootClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun StorageSmallBar(
    title: String,
    sizeText: String,
    color: Color,
    weight: Float,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            Text(
                sizeText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun ScanningProgressOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanRadar")
    val primaryColor = MaterialTheme.colorScheme.primary

    // Pulsing core scan glow
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Rotating scanning vector degree
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Active filename carousel cycle simulating audits
    val auditFiles = listOf(
        "com.google.android.gms/cache/temp_perf_data_v1.log",
        "system/dalvik-cache/dexopt/indexing_active_services.odex",
        "cache/nexapurge_mock_junk/uninstalled_residual_assets.old",
        "data/local/tmp/crash_dump_com.android.system.core",
        "app_cache/profiles/analytics_temp_markers_882.tmp",
        "system/lost_and_found/residual_fragments_node.sid"
    )
    var currentAuditingIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            currentAuditingIndex = (currentAuditingIndex + 1) % auditFiles.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Radar Graphics Board
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .drawBehind {
                        // Background sweeping boundaries concentric circles
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.05f),
                            radius = size.minDimension / 2
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.10f),
                            radius = size.minDimension / 3
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.15f),
                            radius = size.minDimension / 4
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Radar sweep line
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseGlow)
                ) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.08f),
                        radius = size.minDimension / 5
                    )
                }

                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Radar audit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(pulseGlow)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "MEMINDAI MEMORI...",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                modifier = Modifier
                    .width(180.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Auditing file name indicator carousel
            Text(
                text = "Memeriksa:\n${auditFiles[currentAuditingIndex]}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(36.dp)
            )
        }
    }
}

@Composable
fun ScanResultsBoard(
    scannedFiles: List<ScannedJunkFile>,
    selectedFileIds: Set<String>,
    onToggleFile: (String) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onCleanClick: () -> Unit,
    onBackClick: () -> Unit,
    isCleaning: Boolean,
    rootEnabled: Boolean
) {
    val totalCheckedSize = scannedFiles
        .filter { selectedFileIds.contains(it.id) }
        .sumOf { it.size }

    val allSelected = scannedFiles.isNotEmpty() && selectedFileIds.size == scannedFiles.size

    if (isCleaning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 6.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "MEMBERSIHKAN PENGOPTIMAL...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Memindahkan sampah ke Karantina Aman",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Elegant Header Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Hasil Pemindaian",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Select all control action
                Text(
                    text = if (allSelected) "Lepas Semua" else "Pilih Semua",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onToggleAll(!allSelected) }
                )
            }

            // Storage breakdown bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "TOTAL SAMPAH DITEMUKAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = formatSizeStatic(scannedFiles.sumOf { it.size }),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${scannedFiles.size} Item",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Categorized Items LazyColumn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val grouped = scannedFiles.groupBy { it.category }
                grouped.forEach { (category, files) ->
                    item {
                        val headerLabel = when (category) {
                            JunkCategory.APP_CACHE -> "Cache Residu Aplikasi"
                            JunkCategory.TEMP_FILES -> "Berkas Log & Temporary"
                            JunkCategory.LARGE_FILES -> "Unduhan & Installer APK Lama"
                            JunkCategory.DEEP_SYSTEM -> "Berkas Sistem Dalam (Butuh Root)"
                        }
                        val headerColor = when (category) {
                            JunkCategory.DEEP_SYSTEM -> MaterialTheme.colorScheme.tertiary
                            JunkCategory.LARGE_FILES -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(headerColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = headerLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    items(files, key = { it.id }) { item ->
                        ScannedFileCard(
                            scanned = item,
                            isSelected = selectedFileIds.contains(item.id),
                            onToggle = { onToggleFile(item.id) },
                            categoryColor = when (item.category) {
                                JunkCategory.APP_CACHE -> MaterialTheme.colorScheme.primary
                                JunkCategory.TEMP_FILES -> MaterialTheme.colorScheme.secondary
                                JunkCategory.LARGE_FILES -> Color(0xFFFF9F43)
                                JunkCategory.DEEP_SYSTEM -> MaterialTheme.colorScheme.tertiary
                            }
                        )
                    }
                }
            }

            // Bottom sticky Clean CTA action block
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TERPILIH UNTUK KARANTINA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = formatSizeStatic(totalCheckedSize),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Button(
                        onClick = onCleanClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(14.dp),
                        enabled = totalCheckedSize > 0,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "AMANKAN ${formatSizeStatic(totalCheckedSize)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScannedFileCard(
    scanned: ScannedJunkFile,
    isSelected: Boolean,
    onToggle: () -> Unit,
    categoryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = categoryColor
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = scanned.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = scanned.detailInfo,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = scanned.path,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatSizeStatic(scanned.size),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CleanSuccessSplash(
    summary: Pair<Long, Int>?,
    onBackButtonClick: () -> Unit
) {
    val totalSizeText = summary?.first?.let { formatSizeStatic(it) } ?: "0 B"
    val fileCount = summary?.second ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "PERANGKAT BERSIH & OPTIMAL!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Sebanyak $fileCount jenis sampah berhasil dipindahkan dengan aman ke kontainer Karantina NexaPurge.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Total saved statistics board
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL KAPASITAS DIAMANKAN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalSizeText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onBackButtonClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "KEMBALI KE DASBOR",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun TrashBinScreen(viewModel: NexaPurgeViewModel) {
    val quarantined by viewModel.quarantinedFiles.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Karantina Memori",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Pulihkan file yang tidak sengaja terhapus",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            if (quarantined.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearTrashBin() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kosongkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (quarantined.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Empty quarantine",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Karantina Bersih",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Setiap berkas sampah yang dibersihkan dienkapsulasi sementara di folder ini agar bisa dipulihkan kembali saat sewaktu-waktu dibutuhkan.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Kontainer ini menyaring sampah. Memulihkan file akan mengembalikannya ke folder aslinya seketika.",
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                items(quarantined, key = { it.id }) { file ->
                    QuarantineItemCard(
                        file = file,
                        onRestore = { viewModel.restoreFile(file) },
                        onDelete = { viewModel.permanentlyDeleteQuarantinedFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuarantineItemCard(
    file: QuarantinedFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.filename,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatSizeStatic(file.size),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Restore & Delete mini buttons
                Row {
                    FilledIconButton(
                        onClick = onRestore,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Permanently Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            // Sub paths
            Text(
                text = "Sumber: ${file.originalPath}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: NexaPurgeViewModel,
    darkThemeState: MutableState<Boolean>
) {
    val autoCleanVal by viewModel.autoCleanEnabled.collectAsStateWithLifecycle()
    val autoIntervalVal by viewModel.autoCleanIntervalHours.collectAsStateWithLifecycle()
    val rootVal by viewModel.rootAccessEnabled.collectAsStateWithLifecycle()
    val alertThresholdVal by viewModel.alertThresholdPercent.collectAsStateWithLifecycle()
    val cleanHistories by viewModel.cleanHistory.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Setelan Pro NexaPurge",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Section: System & Theme Look
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ANTARMUKA & FITUR PRO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Dark theme toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Mode Gelap Mata Nyaman", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Kurangi lelah mata di malam hari", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                        Switch(
                            checked = darkThemeState.value,
                            onCheckedChange = { darkThemeState.value = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Deep system root deep scan option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = if (rootVal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Aktivasi Kedalaman Root", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Membuka scan dalvik, kernel log & trace kernel dalam.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                        Switch(
                            checked = rootVal,
                            onCheckedChange = { viewModel.toggleRootAccess(it) }
                        )
                    }
                }
            }
        }

        // Section: Scheduling parameters
        item {
            Text(
                text = "PEMBERSIHAN BERKALA OTOMATIS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Periodic clean toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Pembersihan Otomatis", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Bersihkan background cache rutin", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                        Switch(
                            checked = autoCleanVal,
                            onCheckedChange = { viewModel.toggleAutoClean(it) }
                        )
                    }

                    if (autoCleanVal) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Setiap Berapa Jam Pembersihan Selesai?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Horiz choices
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(6, 12, 24, 48).forEach { hrs ->
                                val active = autoIntervalVal == hrs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.setAutoCleanIntervalHours(hrs) }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$hrs Jam",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Storage threshold Alert notifications
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Notifikasi Penyimpanan Penuh", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Peringatan jika sisa ruang < $alertThresholdVal%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(10, 15, 20, 25).forEach { percent ->
                                val active = alertThresholdVal == percent
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.setAlertThresholdPercent(percent) }
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "< $percent%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Audit Logs histories
        item {
            Text(
                text = "RIWAYAT OPTIMALISASI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }

        if (cleanHistories.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = "Riwayat pembersihan kosong. Jalankan scan untuk mengoptimalkan perangkat.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(cleanHistories, key = { it.id }) { log ->
                HistoryLogCard(log = log)
            }
        }
    }
}

@Composable
fun HistoryLogCard(log: CleanHistory) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (log.isAutoClean) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.isAutoClean) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (log.isAutoClean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (log.isAutoClean) "Pembersihan Otomatis" else "Manual Deep Clean",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTimestamp(log.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+" + formatSizeStatic(log.cleanedSize),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${log.filesCount} file diamankan",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Global Static File Size and Date Formatter Helpers to support compile & unit-testing with zero platform dependencies
private fun formatSizeStatic(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val date = Date(timestamp)
        sdf.format(date)
    } catch (e: Exception) {
        "Baru saja"
    }
}
