package com.shuvo.SmartDrive.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shuvo.SmartDrive.data.auth.MultiDriveOAuthManager
import com.shuvo.SmartDrive.data.model.LinkedDriveAccount
import com.shuvo.SmartDrive.ui.theme.AppleIndigo
import com.shuvo.SmartDrive.ui.theme.AppleTeal
import com.shuvo.SmartDrive.ui.theme.DayNightSwitchButton
import com.shuvo.SmartDrive.ui.theme.OceanGlassBorder
import com.shuvo.SmartDrive.ui.theme.OceanGlassCard
import com.shuvo.SmartDrive.ui.theme.OceanHeaderBottom
import com.shuvo.SmartDrive.ui.theme.OceanHeaderTop
import com.shuvo.SmartDrive.ui.theme.StatusDanger
import com.shuvo.SmartDrive.ui.theme.appColors
import com.shuvo.SmartDrive.ui.viewmodel.MainStorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountLinkerScreen(
    viewModel: MainStorageViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onLaunchAccountPicker: () -> Unit,
    onPoolMerged: () -> Unit
) {
    val linkedAccounts by viewModel.linkedAccounts.collectAsState()
    val isMerging by viewModel.isMergingPool.collectAsState()
    val mergeStatusText by viewModel.mergeStatusText.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    val colors = MaterialTheme.appColors
    val maxSlots = MultiDriveOAuthManager.MAX_SLOTS

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Account Linker Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            val totalFreeBytes = linkedAccounts.sumOf { it.freeQuotaBytes }
                            val formattedTotalFree = LinkedDriveAccount.formatBytes(totalFreeBytes)
                            Text(
                                text = if (linkedAccounts.isEmpty()) {
                                    "Merge up to 5 Google Accounts"
                                } else {
                                    "$formattedTotalFree Free Available across ${linkedAccounts.size} drive${if (linkedAccounts.size > 1) "s" else ""}"
                                },
                                fontSize = 12.sp,
                                color = Color(0xB3FFFFFF)
                            )
                        }
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        // Satisfying Day / Night switch button
                        DayNightSwitchButton(
                            isDark = isDark,
                            onToggle = { viewModel.toggleTheme() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OceanHeaderTop
                    )
                )
            },
            containerColor = colors.background,
            bottomBar = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        val totalFreeBytes = linkedAccounts.sumOf { it.freeQuotaBytes }
                        val totalQuotaBytes = linkedAccounts.sumOf { it.totalQuotaBytes }
                        val formattedTotalFree = LinkedDriveAccount.formatBytes(totalFreeBytes)
                        val formattedTotalQuota = LinkedDriveAccount.formatBytes(totalQuotaBytes)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Connected: ${linkedAccounts.size} / $maxSlots slots",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (linkedAccounts.isNotEmpty()) {
                                    Text(
                                        text = "Total Space Available: $formattedTotalFree Free",
                                        color = AppleTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                text = if (linkedAccounts.isEmpty()) "0.0 GB" else "$formattedTotalFree / $formattedTotalQuota",
                                color = AppleTeal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.mergeAndAnalyzeStoragePool(onPoolMerged)
                            },
                            enabled = linkedAccounts.isNotEmpty() && !isMerging,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleTeal,
                                disabledContainerColor = colors.cardBorder
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = OceanHeaderTop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Merge Storage Pool",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = OceanHeaderTop
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 5 Account Slots
                items(maxSlots) { slotIndex ->
                    val slotNumber = slotIndex + 1
                    val account = linkedAccounts.getOrNull(slotIndex)

                    AccountSlotCard(
                        slotNumber = slotNumber,
                        account = account,
                        onAddAccountClicked = onLaunchAccountPicker,
                        onRemoveAccountClicked = { email ->
                            viewModel.removeLinkedAccount(email)
                        }
                    )
                }
            }
        }

        // Animated Merging & Analyzing Full-Screen Overlay
        AnimatedVisibility(visible = isMerging) {
            MergingOverlay(statusText = mergeStatusText)
        }
    }
}

@Composable
fun AccountSlotCard(
    slotNumber: Int,
    account: LinkedDriveAccount?,
    onAddAccountClicked: () -> Unit,
    onRemoveAccountClicked: (String) -> Unit
) {
    val colors = MaterialTheme.appColors

    if (account != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (account.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = account.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AppleTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (account.displayName.firstOrNull() ?: 'U').uppercase(),
                                    color = OceanHeaderTop,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Slot $slotNumber",
                                    fontSize = 11.sp,
                                    color = AppleTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = AppleTeal,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = account.displayName.ifBlank { "Google Account" },
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = account.accountEmail,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { onRemoveAccountClicked(account.accountEmail) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Unlink",
                            tint = StatusDanger.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${account.formattedUsedStorage} used of ${account.formattedTotalStorage}",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleTeal.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${account.formattedFreeStorage} left",
                            color = AppleTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { account.usageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AppleTeal,
                    trackColor = colors.cardBorder,
                )
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.card),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(colors.cardBorder, colors.cardBorder)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.cardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$slotNumber",
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Slot $slotNumber (Available)",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = onAddAccountClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = OceanHeaderTop,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        color = OceanHeaderTop,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MergingOverlay(statusText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OceanHeaderTop.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(OceanGlassCard)
                    .border(1.dp, OceanGlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Syncing",
                    tint = AppleTeal,
                    modifier = Modifier
                        .size(54.dp)
                        .rotate(rotation)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Merging Storage Pool",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                color = Color(0xB3FFFFFF),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = AppleTeal,
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
