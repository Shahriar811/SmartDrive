package com.shuvo.SmartDrive.ui.screens.viewers

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.shuvo.SmartDrive.data.model.UnifiedCloudFile
import com.shuvo.SmartDrive.ui.theme.*
import com.shuvo.SmartDrive.ui.viewmodel.MainStorageViewModel

/**
 * In-App Cloud Photo Viewer (Zero Local Storage Footprint).
 *
 * Utilizes Coil with memory-only cache (disk cache strictly disabled)
 * and authenticated Bearer token header. Supports pinch-to-zoom and pan gestures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudImageViewerScreen(
    file: UnifiedCloudFile,
    viewModel: MainStorageViewModel,
    onNavigateBack: () -> Unit,
    onDownloadClicked: (UnifiedCloudFile) -> Unit,
    onDeleteClicked: (UnifiedCloudFile) -> Unit
) {
    val context = LocalContext.current
    var bearerToken by remember { mutableStateOf<String?>(null) }
    var isLoadingToken by remember { mutableStateOf(true) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(file.ownerAccountEmail) {
        isLoadingToken = true
        val token = viewModel.getAccessTokenForAccount(file.ownerAccountEmail)
        bearerToken = token
        isLoadingToken = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Cloud View • ${file.formattedSize}",
                            fontSize = 11.sp,
                            color = Color(0xB3FFFFFF)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onDownloadClicked(file) }) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download",
                            tint = AppleTeal
                        )
                    }
                    IconButton(onClick = { onDeleteClicked(file) }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Permanent Delete",
                            tint = StatusDanger
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OceanHeaderTop)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            val maxOffsetX = (size.width * (scale - 1)) / 2f
                            val maxOffsetY = (size.height * (scale - 1)) / 2f
                            val newX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            val newY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            offset = Offset(newX, newY)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingToken) {
                CircularProgressIndicator(color = DiscordBlurple, modifier = Modifier.size(36.dp))
            } else if (!bearerToken.isNullOrBlank()) {
                val imageUrl = "https://www.googleapis.com/drive/v3/files/${file.driveFileId}?alt=media"

                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .addHeader("Authorization", "Bearer $bearerToken")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.DISABLED) // CRITICAL: Zero local storage footprint
                    .crossfade(true)
                    .build()

                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = file.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    val state = painter.state
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            CircularProgressIndicator(color = DiscordBlurple, modifier = Modifier.size(36.dp))
                        }
                        is AsyncImagePainter.State.Error -> {
                            Text("Failed to render cloud image", color = DangerRed, fontSize = 13.sp)
                        }
                        else -> {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }
            } else {
                Text("Authentication failed for image stream", color = DangerRed, fontSize = 13.sp)
            }
        }
    }
}

