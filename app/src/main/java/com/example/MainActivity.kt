package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.ui.components.JimpitanTopBar
import com.example.ui.components.NotificationsBottomSheet
import com.example.ui.components.RoleSwitchDialog
import com.example.ui.screens.bendahara.BendaharaHomeScreen
import com.example.ui.screens.petugas.PetugasHomeScreen
import com.example.ui.screens.warga.WargaHomeScreen
import com.example.ui.theme.JimpitanKitaTheme
import com.example.ui.viewmodel.JimpitanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.util.CrashlyticsHelper.log("Aplikasi JimpitanKita aktif")
        com.example.util.CrashlyticsHelper.setCustomKey("app_package", "com.aistudio.jimpitankita.rtapp")
        setContent {
            val viewModel: JimpitanViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            JimpitanKitaTheme(darkTheme = isDarkMode) {
                JimpitanApp(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleTheme = { viewModel.toggleTheme() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JimpitanApp(
    viewModel: JimpitanViewModel = viewModel(),
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val activeHouseId by viewModel.activeHouseId.collectAsState()
    val activeHouse by viewModel.activeHouse.collectAsState()
    val houses by viewModel.houses.collectAsState()
    val notifications by viewModel.activeHouseNotifications.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val rtConfig by viewModel.rtConfig.collectAsState()
    val officerName by viewModel.officerName.collectAsState()
    val isBendaharaUnlocked by viewModel.isBendaharaUnlocked.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val unreadCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    var activeBannerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            com.example.util.NotificationSoundHelper.playNotificationAlert(context)
            activeBannerMessage = msg
            viewModel.clearSnackbar()
            kotlinx.coroutines.delay(5000L) // Hilang otomatis setelah dibiarkan 5 detik
            if (activeBannerMessage == msg) {
                activeBannerMessage = null
            }
        }
    }

    val subtitle = when (currentRole) {
        UserRole.WARGA -> "${activeHouse?.headOfFamily ?: "Warga"} (${activeHouse?.block ?: "Blok A"} ${activeHouse?.houseNumber ?: "No. 01"})"
        UserRole.PETUGAS -> "$officerName • ${rtConfig.rtName}"
        UserRole.BENDAHARA -> "Administrasi & Pembukuan Kas ${rtConfig.rtName}"
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        viewModel.recordUserActivity()
                    }
                }
            }
            .testTag("app_root_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            JimpitanTopBar(
                currentRole = currentRole,
                subtitle = subtitle,
                unreadNotificationsCount = unreadCount,
                isDarkMode = isDarkMode,
                onToggleTheme = onToggleTheme,
                onSwitchRoleClick = { showRoleDialog = true },
                onNotificationsClick = { showNotificationsSheet = true }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("app_snackbar_host")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (currentRole) {
                UserRole.WARGA -> WargaHomeScreen(viewModel = viewModel)
                UserRole.PETUGAS -> PetugasHomeScreen(viewModel = viewModel)
                UserRole.BENDAHARA -> BendaharaHomeScreen(viewModel = viewModel)
            }

            // Custom Floating Woro-Woro JimpitanKita Notification Banner
            androidx.compose.animation.AnimatedVisibility(
                visible = activeBannerMessage != null,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                activeBannerMessage?.let { msg ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isDarkMode) Color(0xFF10B981) else Color(0xFF059669)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeBannerMessage = null
                                showNotificationsSheet = true
                            }
                            .testTag("jimpitan_notification_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0xFF047857) else Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Woro-woro JimpitanKita",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• RT 02/03",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF047857)
                                    )
                                }
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { activeBannerMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val isPetugasUnlocked by viewModel.isPetugasUnlocked.collectAsState()

    // Role Switch Dialog
    if (showRoleDialog) {
        RoleSwitchDialog(
            currentRole = currentRole,
            activeHouseId = activeHouseId,
            houses = houses,
            config = rtConfig,
            isBendaharaUnlocked = isBendaharaUnlocked,
            isPetugasUnlocked = isPetugasUnlocked,
            onDismiss = { showRoleDialog = false },
            onSelectRole = { role, houseId ->
                viewModel.switchRole(role, houseId)
            },
            onVerifyBendahara = { user, pass ->
                viewModel.verifyBendaharaLogin(user, pass)
            },
            onVerifyPetugas = { user, pass ->
                viewModel.verifyPetugasLogin(user, pass)
            }
        )
    }

    // Notifications Bottom Sheet
    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            notifications = notifications,
            onDismiss = { showNotificationsSheet = false },
            onMarkAllRead = {
                viewModel.markNotificationsRead()
            }
        )
    }
}
