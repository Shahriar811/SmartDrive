package com.shuvo.SmartDrive.ui.screens.viewers

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.shuvo.SmartDrive.data.model.UnifiedCloudFile
import com.shuvo.SmartDrive.ui.theme.*
import com.shuvo.SmartDrive.ui.viewmodel.MainStorageViewModel

/**
 * In-App Cloud Video Streaming Viewer (Zero Local Storage Footprint).
 *
 * Uses AndroidX Media3 ExoPlayer with DefaultHttpDataSource injecting Authorization: Bearer <TOKEN>
 * headers for direct Google Drive network byte-range streaming and seeking.
 */
@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun CloudVideoPlayerScreen(
    file: UnifiedCloudFile,
    viewModel: MainStorageViewModel,
    onNavigateBack: () -> Unit,
    onDownloadClicked: (UnifiedCloudFile) -> Unit,
    onDeleteClicked: (UnifiedCloudFile) -> Unit
) {
    val context = LocalContext.current
    var bearerToken by remember { mutableStateOf<String?>(null) }
    var isLoadingToken by remember { mutableStateOf(true) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(file.ownerAccountEmail) {
        isLoadingToken = true
        val token = viewModel.getAccessTokenForAccount(file.ownerAccountEmail)
        bearerToken = token
        isLoadingToken = false
    }

    DisposableEffect(bearerToken) {
        val token = bearerToken
        if (!token.isNullOrBlank()) {
            val streamingUrl = "https://www.googleapis.com/drive/v3/files/${file.driveFileId}?alt=media"

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))

            val mediaSource = ProgressiveMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(streamingUrl)))

            val player = ExoPlayer.Builder(context).build().apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
            exoPlayer = player
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
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
                            text = "Streaming • ${file.formattedSize}",
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
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingToken) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DiscordBlurple, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.size(16.dp))
                    Text("Connecting to cloud stream...", color = TextMutedSecondary, fontSize = 13.sp)
                }
            } else if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Failed to authenticate stream session.", color = DangerRed, fontSize = 13.sp)
            }
        }
    }
}

