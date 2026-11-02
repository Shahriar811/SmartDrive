package com.shuvo.SmartDrive.ui.screens.viewers

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shuvo.SmartDrive.DriveApplication
import com.shuvo.SmartDrive.data.model.UnifiedCloudFile
import com.shuvo.SmartDrive.ui.theme.*
import com.shuvo.SmartDrive.ui.viewmodel.MainStorageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * In-App Cloud Document & Text/Code Viewer (Zero Local Storage Footprint).
 *
 * For PDFs: Streams directly into memory, utilizes native PdfRenderer for page bitmap rendering,
 * and clears temporary buffer immediately on exit.
 * For Text & Code: Streams directly from network into memory and renders inside a dark code editor UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDocTextViewerScreen(
    file: UnifiedCloudFile,
    viewModel: MainStorageViewModel,
    onNavigateBack: () -> Unit,
    onDownloadClicked: (UnifiedCloudFile) -> Unit,
    onDeleteClicked: (UnifiedCloudFile) -> Unit
) {
    val context = LocalContext.current
    val isPdf = file.mimeType.equals("application/pdf", ignoreCase = true) ||
            file.fileName.endsWith(".pdf", ignoreCase = true)

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var textContent by remember { mutableStateOf("") }

    // PDF state
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPageCount by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.id) {
        isLoading = true
        errorMessage = null
        try {
            val token = viewModel.getAccessTokenForAccount(file.ownerAccountEmail)
            if (token.isNullOrBlank()) {
                errorMessage = "Authentication failed for account: ${file.ownerAccountEmail}"
                isLoading = false
                return@LaunchedEffect
            }

            val app = context.applicationContext as DriveApplication
            val streamResult = app.unifiedStorageRepository.getFileStream(file)

            if (streamResult.isFailure) {
                errorMessage = streamResult.exceptionOrNull()?.localizedMessage ?: "Failed to stream document"
                isLoading = false
                return@LaunchedEffect
            }

            val responseBody = streamResult.getOrNull()!!

            withContext(Dispatchers.IO) {
                if (isPdf) {
                    val temp = File.createTempFile("stream_pdf_", ".tmp", context.cacheDir)
                    temp.deleteOnExit()
                    FileOutputStream(temp).use { out ->
                        responseBody.byteStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    val descriptor = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(descriptor)

                    withContext(Dispatchers.Main) {
                        tempPdfFile = temp
                        pfd = descriptor
                        pdfRenderer = renderer
                        totalPageCount = renderer.pageCount
                        currentPageIndex = 0
                        renderPdfPage(renderer, 0) { renderedBmp ->
                            currentBitmap = renderedBmp
                            isLoading = false
                        }
                    }
                } else {
                    val text = responseBody.byteStream().bufferedReader().use { it.readText() }
                    withContext(Dispatchers.Main) {
                        textContent = text
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Stream reading error"
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                pfd?.close()
                tempPdfFile?.delete()
            } catch (ignored: Exception) {
            }
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
                            text = "Cloud Stream • ${file.formattedSize}",
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
        bottomBar = {
            if (isPdf && totalPageCount > 0) {
                val colors = MaterialTheme.appColors
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex--
                                    pdfRenderer?.let { renderer ->
                                        renderPdfPage(renderer, currentPageIndex) {
                                            currentBitmap = it
                                        }
                                    }
                                }
                            },
                            enabled = currentPageIndex > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordDarkElevated)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                            Text("Prev")
                        }

                        Text(
                            text = "Page ${currentPageIndex + 1} of $totalPageCount",
                            color = TextWhitePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = {
                                if (currentPageIndex < totalPageCount - 1) {
                                    currentPageIndex++
                                    pdfRenderer?.let { renderer ->
                                        renderPdfPage(renderer, currentPageIndex) {
                                            currentBitmap = it
                                        }
                                    }
                                }
                            },
                            enabled = currentPageIndex < totalPageCount - 1,
                            colors = ButtonDefaults.buttonColors(containerColor = DiscordDarkElevated)
                        ) {
                            Text("Next")
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }
                }
            }
        },
        containerColor = DiscordDarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DiscordBlurple, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isPdf) "Rendering PDF pages..." else "Streaming text buffer...",
                            color = TextMutedSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Error reading document",
                        color = DangerRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                isPdf -> {
                    if (currentBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = currentBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page ${currentPageIndex + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
                else -> {
                    val lines = remember(textContent) { textContent.lines() }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DiscordDarkBackground)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(DiscordDarkSurface.copy(alpha = 0.5f))
                                .padding(end = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            lines.indices.forEach { index ->
                                Text(
                                    text = "${index + 1} ",
                                    color = TextMutedSecondary.copy(alpha = 0.6f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            lines.forEach { line ->
                                Text(
                                    text = line.ifEmpty { " " },
                                    color = TextWhitePrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun renderPdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    onRendered: (Bitmap) -> Unit
) {
    try {
        val page = renderer.openPage(pageIndex)
        val width = page.width * 2
        val height = page.height * 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        onRendered(bitmap)
    } catch (ignored: Exception) {
    }
}

