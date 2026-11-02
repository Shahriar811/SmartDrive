package com.shuvo.SmartDrive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.shuvo.SmartDrive.R
import com.shuvo.SmartDrive.data.model.FileCategory
import com.shuvo.SmartDrive.data.model.StorageCategorySummary
import com.shuvo.SmartDrive.data.model.UnifiedCloudFile
import com.shuvo.SmartDrive.data.model.UnifiedStoragePoolState
import com.shuvo.SmartDrive.ui.theme.AppleIndigo
import com.shuvo.SmartDrive.ui.theme.AppleTeal
import com.shuvo.SmartDrive.ui.theme.AppleTealDark
import com.shuvo.SmartDrive.ui.theme.AppleTealGlow
import com.shuvo.SmartDrive.ui.theme.AppleTealSoft
import com.shuvo.SmartDrive.ui.theme.CatAudioGradEnd
import com.shuvo.SmartDrive.ui.theme.CatAudioGradStart
import com.shuvo.SmartDrive.ui.theme.CatDocGradEnd
import com.shuvo.SmartDrive.ui.theme.CatDocGradStart
import com.shuvo.SmartDrive.ui.theme.CatImageGradEnd
import com.shuvo.SmartDrive.ui.theme.CatImageGradStart
import com.shuvo.SmartDrive.ui.theme.CatOtherGradEnd
import com.shuvo.SmartDrive.ui.theme.CatOtherGradStart
import com.shuvo.SmartDrive.ui.theme.CatVideoGradEnd
import com.shuvo.SmartDrive.ui.theme.CatVideoGradStart
import com.shuvo.SmartDrive.ui.theme.DayNightSwitchButton
import com.shuvo.SmartDrive.ui.theme.OceanGlassBorder
import com.shuvo.SmartDrive.ui.theme.OceanGlassCard
import com.shuvo.SmartDrive.ui.theme.OceanHeaderBottom
import com.shuvo.SmartDrive.ui.theme.OceanHeaderMid
import com.shuvo.SmartDrive.ui.theme.OceanHeaderTop
import com.shuvo.SmartDrive.ui.theme.StatusDanger
import com.shuvo.SmartDrive.ui.theme.appColors
import com.shuvo.SmartDrive.ui.viewmodel.MainStorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainStorageViewModel,
    onNavigateToAccountLinker: () -> Unit,
    onOpenFileViewer: (file: UnifiedCloudFile) -> Unit,
    onLaunchBatchUploadPicker: () -> Unit
) {
    val poolState by viewModel.storagePoolState.collectAsState()
    val filteredFiles by viewModel.filteredFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val filePendingDeletion by viewModel.filePendingDeletion.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    val colors = MaterialTheme.appColors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            snackbarHostState.showSnackbar(uiMessage!!)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Iconic Apple-style glowing turquoise FAB
            FloatingActionButton(
                onClick = onLaunchBatchUploadPicker,
                containerColor = AppleTeal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 12.dp, end = 8.dp)
                    .size(60.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = AppleTealGlow,
                        spotColor = AppleTeal
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload Files",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Signature Deep Oceanic Header with Storage Donut Gauge
            item {
                OceanicHeaderSection(
                    poolState = poolState,
                    isDark = isDark,
                    onToggleTheme = { viewModel.toggleTheme() },
                    onNavigateToAccountLinker = onNavigateToAccountLinker,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) }
                )
            }

            // 2. Recent Files Header with Grid/List Toggle
            item {
                FilesSectionHeader(
                    selectedCategory = selectedCategory,
                    fileCount = filteredFiles.size,
                    isGridView = isGridView,
                    onToggleView = { viewModel.setGridView(it) }
                )
            }

            // 3. Animated File Items (Grid vs List)
            if (filteredFiles.isEmpty()) {
                item {
                    EmptyFilesPlaceholder(
                        isFiltered = searchQuery.isNotBlank() || selectedCategory != null
                    )
                }
            } else {
                if (isGridView) {
                    // 2-column paired grid items
                    val pairedFiles = filteredFiles.chunked(2)
                    items(pairedFiles, key = { pair -> pair.first().id }) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val first = pair[0]
                            Box(modifier = Modifier.weight(1f)) {
                                FileGridCard(
                                    file = first,
                                    onItemClicked = { onOpenFileViewer(first) },
                                    onDownloadClicked = { viewModel.downloadFile(first) },
                                    onDeleteClicked = { viewModel.promptPermanentDelete(first) }
                                )
                            }

                            if (pair.size > 1) {
                                val second = pair[1]
                                Box(modifier = Modifier.weight(1f)) {
                                    FileGridCard(
                                        file = second,
                                        onItemClicked = { onOpenFileViewer(second) },
                                        onDownloadClicked = { viewModel.downloadFile(second) },
                                        onDeleteClicked = { viewModel.promptPermanentDelete(second) }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    // Sleek horizontal list items
                    items(filteredFiles, key = { it.id }) { file ->
                        FileListItemRow(
                            file = file,
                            onItemClicked = { onOpenFileViewer(file) },
                            onDownloadClicked = { viewModel.downloadFile(file) },
                            onDeleteClicked = { viewModel.promptPermanentDelete(file) }
                        )
                    }
                }
            }
        }
    }

    // Batch Upload Progress Dialog
    if (uploadState.isUploading || uploadState.errorMessage != null || uploadState.isSuccess) {
        UploadProgressDialog(
            uploadState = uploadState,
            onDismiss = { viewModel.dismissUploadProgressDialog() }
        )
    }

    // Permanent Deletion Confirmation Dialog
    if (filePendingDeletion != null) {
        PermanentDeleteConfirmDialog(
            file = filePendingDeletion!!,
            isDeleting = isDeleting,
            onConfirm = { viewModel.executePermanentDelete() },
            onDismiss = { viewModel.dismissPermanentDeleteDialog() }
        )
    }
}

/**
 * Top Oceanic Gradient Header with Donut Storage Arc Gauge, Search Bar, and Filter Chips.
 */
@Composable
fun OceanicHeaderSection(
    poolState: UnifiedStoragePoolState,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToAccountLinker: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedCategory: FileCategory?,
    onCategorySelected: (FileCategory?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(OceanHeaderTop, OceanHeaderMid, OceanHeaderBottom)
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App Logo & Status Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "SmartDrive Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "SmartDrive",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "75 GB Unified Pool",
                            color = Color(0xB3FFFFFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Controls: Day/Night Switch + Account Linker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Satisfying Day / Night Shift Button
                    DayNightSwitchButton(
                        isDark = isDark,
                        onToggle = onToggleTheme
                    )

                    // Account Linker Hub Icon
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable(onClick = onNavigateToAccountLinker),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = "Manage Drives",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recreated Circular Storage Donut Gauge Card
            StorageDonutProgressCard(
                poolState = poolState,
                onManageClicked = onNavigateToAccountLinker
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Recreated Search Bar & Turquoise Filter Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Frosted Glass Search Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x2EFFFFFF))
                        .border(1.dp, OceanGlassBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xB3FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = {
                                Text(
                                    text = "Search anything here",
                                    color = Color(0x99FFFFFF),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AppleTeal
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { onSearchQueryChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Vibrant Turquoise Filter Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppleTeal)
                        .clickable { onCategorySelected(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = OceanHeaderTop,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Type Capsule Pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAllSelected = selectedCategory == null
                    CategoryFilterPill(
                        title = "All Files",
                        isSelected = isAllSelected,
                        onClick = { onCategorySelected(null) }
                    )
                }

                items(FileCategory.entries) { cat ->
                    val isSelected = selectedCategory == cat
                    CategoryFilterPill(
                        title = cat.displayName,
                        isSelected = isSelected,
                        onClick = { onCategorySelected(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/**
 * Recreated Circular Donut Gauge Card (matching the first phone screen from the uploaded UI).
 */
@Composable
fun StorageDonutProgressCard(
    poolState: UnifiedStoragePoolState,
    onManageClicked: () -> Unit
) {
    val usedFraction = if (poolState.totalQuotaBytes > 0) {
        (poolState.usedQuotaBytes.toFloat() / poolState.totalQuotaBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val freeFraction = (1f - usedFraction).coerceIn(0f, 1f)

    val animatedUsedFraction by animateFloatAsState(
        targetValue = usedFraction,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donutProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(OceanGlassCard)
            .border(1.dp, OceanGlassBorder, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular Pie / Donut Chart with glowing turquoise and marine arcs
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background track (Free pool arc in glowing turquoise)
                    drawArc(
                        color = AppleTeal,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Used pool arc in deep oceanic navy
                    val usedSweep = animatedUsedFraction * 360f
                    if (usedSweep > 0f) {
                        drawArc(
                            color = Color(0xFF162D50),
                            startAngle = -90f,
                            sweepAngle = usedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth + 2f, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center percentage labels
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val freePercent = (freeFraction * 100).toInt()
                    Text(
                        text = "Free",
                        color = Color(0xB3FFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$freePercent%",
                        color = AppleTeal,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Storage Details & Manage Button
            Column(modifier = Modifier.weight(1f)) {
                // Free Pool Stat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppleTeal)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Free: ${poolState.formattedFree}",
                        color = Color(0xCCFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Used Pool Stat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF60A5FA))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Used: ${poolState.formattedUsed}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Max Single File Info
                Text(
                    text = "Max File: ${poolState.formattedMaxSingleFile}",
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // "Manage ↗" Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(12.dp))
                        .clickable(onClick = onManageClicked)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Manage ↗",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter Pill matching the capsule style in the design images.
 */
@Composable
fun CategoryFilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) AppleTeal else Color(0x26FFFFFF))
            .border(
                1.dp,
                if (isSelected) AppleTeal else Color(0x33FFFFFF),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) OceanHeaderTop else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Section Header with "Recent File", Subtitle, and Grid/List toggle pills.
 */
@Composable
fun FilesSectionHeader(
    selectedCategory: FileCategory?,
    fileCount: Int,
    isGridView: Boolean,
    onToggleView: (Boolean) -> Unit
) {
    val colors = MaterialTheme.appColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (selectedCategory == null) "Recent File" else "${selectedCategory.displayName} Files",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "Cloud Storage / $fileCount items",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }

        // View Mode Switcher [ ⊞  ☰ ]
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.card)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grid Icon Toggle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isGridView) AppleTeal else Color.Transparent)
                    .clickable { onToggleView(true) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Grid View",
                    tint = if (isGridView) OceanHeaderTop else colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // List Icon Toggle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (!isGridView) AppleTeal else Color.Transparent)
                    .clickable { onToggleView(false) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                    contentDescription = "List View",
                    tint = if (!isGridView) OceanHeaderTop else colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 2-Column Grid Card recreated directly from the reference UI.
 */
@Composable
fun FileGridCard(
    file: UnifiedCloudFile,
    onItemClicked: () -> Unit,
    onDownloadClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val colors = MaterialTheme.appColors
    var menuExpanded by remember { mutableStateOf(false) }

    val (gradStart, gradEnd, iconVector) = getCategoryGradientAndIcon(file.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClicked),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: 3D Gradient Icon + 3-Dot Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
                        .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = gradStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FileActionDropdownMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onOpen = onItemClicked,
                        onDownload = onDownloadClicked,
                        onDelete = onDeleteClicked
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // File Name
            Text(
                text = file.fileName,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata: Size • Extension / Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = file.formattedSize,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = file.formattedDate,
                    color = colors.textTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Sleek List Item Row with soft iOS card styling.
 */
@Composable
fun FileListItemRow(
    file: UnifiedCloudFile,
    onItemClicked: () -> Unit,
    onDownloadClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val colors = MaterialTheme.appColors
    var menuExpanded by remember { mutableStateOf(false) }
    val (gradStart, gradEnd, iconVector) = getCategoryGradientAndIcon(file.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onItemClicked),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(gradStart, gradEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.formattedSize,
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = " • ",
                        color = colors.textTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = file.formattedDate,
                        color = colors.textTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = colors.textTertiary
                    )
                }

                FileActionDropdownMenu(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onOpen = onItemClicked,
                    onDownload = onDownloadClicked,
                    onDelete = onDeleteClicked
                )
            }
        }
    }
}

/**
 * Dropdown Menu for file operations.
 */
@Composable
fun FileActionDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.appColors

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(colors.card)
    ) {
        DropdownMenuItem(
            text = { Text("Open in Cloud Viewer", color = colors.textPrimary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AppleTeal) },
            onClick = {
                onDismiss()
                onOpen()
            }
        )
        DropdownMenuItem(
            text = { Text("Download to Device", color = colors.textPrimary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AppleIndigo) },
            onClick = {
                onDismiss()
                onDownload()
            }
        )
        DropdownMenuItem(
            text = { Text("Permanent Delete", color = StatusDanger, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusDanger) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}

/**
 * Returns Category Gradient Colors and Icon Vector.
 */
fun getCategoryGradientAndIcon(category: FileCategory): Triple<Color, Color, ImageVector> {
    return when (category) {
        FileCategory.PHOTOS -> Triple(CatImageGradStart, CatImageGradEnd, Icons.Default.Image)
        FileCategory.VIDEOS -> Triple(CatVideoGradStart, CatVideoGradEnd, Icons.Default.Videocam)
        FileCategory.DOCUMENTS -> Triple(CatDocGradStart, CatDocGradEnd, Icons.Default.Description)
        FileCategory.TEXT_AND_CODE -> Triple(AppleTeal, AppleTealDark, Icons.Default.Code)
        FileCategory.AUDIO_AND_OTHERS -> Triple(CatAudioGradStart, CatAudioGradEnd, Icons.Default.AudioFile)
    }
}

@Composable
fun EmptyFilesPlaceholder(isFiltered: Boolean) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = colors.textTertiary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isFiltered) "No matching files found" else "No files in your 75 GB pool yet",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap the '+' turquoise button below to upload files with zero local disk footprint.",
                color = colors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UploadProgressDialog(
    uploadState: MainStorageViewModel.BatchUploadUiState,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors

    AlertDialog(
        onDismissRequest = {
            if (!uploadState.isUploading) onDismiss()
        },
        containerColor = colors.card,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = when {
                    uploadState.errorMessage != null -> "Upload Failed"
                    uploadState.isSuccess -> "Upload Complete"
                    else -> "Streaming to Cloud (${uploadState.currentFileIndex}/${uploadState.totalFiles})"
                },
                color = when {
                    uploadState.errorMessage != null -> StatusDanger
                    uploadState.isSuccess -> AppleTeal
                    else -> colors.textPrimary
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (uploadState.errorMessage != null) {
                    Text(
                        text = uploadState.errorMessage,
                        color = StatusDanger,
                        fontSize = 13.sp
                    )
                } else if (uploadState.isSuccess) {
                    Text(
                        text = "All files successfully uploaded and indexed into the virtual pool with zero local storage footprint.",
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = uploadState.currentFileName,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current file: ${uploadState.currentFileProgressPercent.toInt()}%",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    LinearProgressIndicator(
                        progress = { uploadState.currentFileProgressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AppleTeal,
                        trackColor = colors.cardBorder
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Overall batch progress: ${uploadState.overallProgressPercent.toInt()}%",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    LinearProgressIndicator(
                        progress = { uploadState.overallProgressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AppleIndigo,
                        trackColor = colors.cardBorder
                    )
                }
            }
        },
        confirmButton = {
            if (!uploadState.isUploading) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTeal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = OceanHeaderTop, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun PermanentDeleteConfirmDialog(
    file: UnifiedCloudFile,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.appColors

    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        containerColor = colors.card,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = StatusDanger,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Permanent Deletion Warning",
                color = StatusDanger,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = "Permanent Deletion Warning: If you delete this file, it will be permanently erased from your cloud storage and will no longer exist anywhere. It will not go to the Trash and cannot be recovered. Do you want to proceed?",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "File: ${file.fileName} (${file.formattedSize})",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = StatusDanger),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Proceed", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = colors.textPrimary)
            }
        }
    )
}
