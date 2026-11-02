package com.shuvo.SmartDrive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.shuvo.SmartDrive.data.model.FileCategory
import com.shuvo.SmartDrive.ui.screens.AccountLinkerScreen
import com.shuvo.SmartDrive.ui.screens.AuthScreen
import com.shuvo.SmartDrive.ui.screens.DashboardScreen
import com.shuvo.SmartDrive.ui.screens.viewers.CloudDocTextViewerScreen
import com.shuvo.SmartDrive.ui.screens.viewers.CloudImageViewerScreen
import com.shuvo.SmartDrive.ui.screens.viewers.CloudVideoPlayerScreen
import com.shuvo.SmartDrive.ui.theme.DiscordDarkBackground
import com.shuvo.SmartDrive.ui.theme.Drive75Theme
import com.shuvo.SmartDrive.ui.viewmodel.MainStorageViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainStorageViewModel by viewModels()

    // 1. Google Account Picker Launcher for Multi-Drive Linker (Layer 2 OAuth)
    private val driveAccountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // User dismissed or canceled account picker dialog, no error
            return@registerForActivityResult
        }
        val app = application as DriveApplication
        val parseResult = app.oAuthManager.parseAccountFromIntent(result.data)
        parseResult.fold(
            onSuccess = { linkedAccount ->
                viewModel.onGoogleAccountLinked(linkedAccount)
                Toast.makeText(
                    this,
                    "Linking '${linkedAccount.accountEmail}'...",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onFailure = { error ->
                Toast.makeText(
                    this,
                    "Failed to link Google Drive account: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // 1b. Google Drive User-Recoverable OAuth Consent Launcher
    private val driveAuthConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Google Drive access authorized!", Toast.LENGTH_SHORT).show()
            viewModel.refreshStoragePool()
        }
    }

    // 2. Google Sign-In Launcher for Master Account (Layer 1 Auth)
    private val masterGoogleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                viewModel.signInWithGoogle(idToken) {
                    // Handled reactively via currentUser Flow
                }
            } else {
                Toast.makeText(this, "Unable to obtain Google ID token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. System File Picker Launcher for Smart Batch Upload
    private val batchFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.handleSelectedUrisForUpload(uris, contentResolver)
        }
    }

    // 4. Notification Permission Launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Listen for Google Drive OAuth consent prompts across accounts
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recoverableAuthIntent.collect { intent ->
                    driveAuthConsentLauncher.launch(intent)
                }
            }
        }

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            Drive75Theme(darkTheme = isDark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onLaunchGoogleSignIn = {
                            val app = application as DriveApplication
                            masterGoogleSignInLauncher.launch(app.oAuthManager.googleSignInClient.signInIntent)
                        },
                        onLaunchDriveAccountPicker = {
                            val app = application as DriveApplication
                            driveAccountPickerLauncher.launch(app.oAuthManager.getAccountPickerIntent())
                        },
                        onLaunchBatchUploadPicker = {
                            batchFilePickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AppNavigation(
    viewModel: MainStorageViewModel,
    onLaunchGoogleSignIn: () -> Unit,
    onLaunchDriveAccountPicker: () -> Unit,
    onLaunchBatchUploadPicker: () -> Unit
) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()
    val isGuestMode by viewModel.isGuestMode.collectAsState()
    val linkedAccounts by viewModel.linkedAccounts.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()

    val startDestination = when {
        currentUser == null && !isGuestMode -> "auth"
        linkedAccounts.isEmpty() -> "account_linker"
        else -> "dashboard"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("auth") {
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    if (linkedAccounts.isEmpty()) {
                        navController.navigate("account_linker") {
                            popUpTo("auth") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                },
                onGoogleSignInClicked = onLaunchGoogleSignIn,
                onBypassLoginClicked = {
                    if (linkedAccounts.isEmpty()) {
                        navController.navigate("account_linker") {
                            popUpTo("auth") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("account_linker") {
            AccountLinkerScreen(
                viewModel = viewModel,
                onNavigateBack = if (linkedAccounts.isNotEmpty()) {
                    { navController.popBackStack() }
                } else null,
                onLaunchAccountPicker = onLaunchDriveAccountPicker,
                onPoolMerged = {
                    navController.navigate("dashboard") {
                        popUpTo("account_linker") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAccountLinker = {
                    navController.navigate("account_linker")
                },
                onOpenFileViewer = { file ->
                    when (file.category) {
                        FileCategory.VIDEOS -> navController.navigate("viewer_video/${file.id}")
                        FileCategory.PHOTOS -> navController.navigate("viewer_image/${file.id}")
                        else -> navController.navigate("viewer_doc/${file.id}")
                    }
                },
                onLaunchBatchUploadPicker = onLaunchBatchUploadPicker
            )
        }

        composable(
            route = "viewer_video/{fileId}",
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId")
            val file = allFiles.find { it.id == fileId }
            if (file != null) {
                CloudVideoPlayerScreen(
                    file = file,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDownloadClicked = { viewModel.downloadFile(it) },
                    onDeleteClicked = {
                        viewModel.promptPermanentDelete(it)
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(
            route = "viewer_image/{fileId}",
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId")
            val file = allFiles.find { it.id == fileId }
            if (file != null) {
                CloudImageViewerScreen(
                    file = file,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDownloadClicked = { viewModel.downloadFile(it) },
                    onDeleteClicked = {
                        viewModel.promptPermanentDelete(it)
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(
            route = "viewer_doc/{fileId}",
            arguments = listOf(navArgument("fileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getString("fileId")
            val file = allFiles.find { it.id == fileId }
            if (file != null) {
                CloudDocTextViewerScreen(
                    file = file,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDownloadClicked = { viewModel.downloadFile(it) },
                    onDeleteClicked = {
                        viewModel.promptPermanentDelete(it)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

