package com.example.ui.screens.bendahara

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.RtConfig
import com.example.data.model.SickResidentAid
import com.example.ui.components.EditTreasurerSecurityDialog
import com.example.ui.components.HouseQrCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.JimpitanViewModel
import com.example.util.Formatters
import com.example.util.MonthInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BendaharaHomeScreen(
    viewModel: JimpitanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val balance by viewModel.currentBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalJimpitanIncome by viewModel.totalJimpitanIncome.collectAsState()
    val totalOtherIncomes by viewModel.totalOtherIncomes.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val cashIncomes by viewModel.cashIncomes.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val houses by viewModel.houses.collectAsState()
    val allSickAids by viewModel.allSickResidentAids.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val rtConfig by viewModel.rtConfig.collectAsState()
    val selectedMonthInfo by viewModel.selectedMonthInfo.collectAsState()
    val selectedMonthRecords by viewModel.selectedMonthRecords.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Arus Kas RT", "Pemasukan", "Pengeluaran", "Bantuan Sakit", "Warga & QR", "Pengaturan")

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showExportReportDialog by remember { mutableStateOf(false) }
    var showAddHouseDialog by remember { mutableStateOf(false) }
    var showAddAnnouncementDialog by remember { mutableStateOf(false) }
    var showEditRateDialog by remember { mutableStateOf(false) }
    var showEditSickAidDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var viewingQrHouse by remember { mutableStateOf<House?>(null) }
    var houseToDelete by remember { mutableStateOf<House?>(null) }
    var showResetDummyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("bendahara_home_screen")
    ) {
        // Month Selector Bar (Januari s/d Desember)
        MonthSelectorBar(
            currentMonthInfo = selectedMonthInfo,
            onSelectMonth = { year, month -> viewModel.setSelectedMonth(year, month) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 12.dp
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("bendahara_tab_$index")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> DashboardKasTab(
                    rtName = rtConfig.rtName,
                    selectedMonthInfo = selectedMonthInfo,
                    balance = balance,
                    totalIncome = totalIncome,
                    totalJimpitanIncome = totalJimpitanIncome,
                    totalOtherIncomes = totalOtherIncomes,
                    totalExpenses = totalExpenses,
                    expenses = expenses,
                    onDownloadPdf = { viewModel.generateAndSharePdf(context, selectedMonthInfo) },
                    onAddIncomeClick = { showAddIncomeDialog = true },
                    onAddExpenseClick = { showAddExpenseDialog = true },
                    onExportReportClick = { showExportReportDialog = true }
                )
                1 -> DaftarPemasukanTab(
                    cashIncomes = cashIncomes,
                    totalJimpitan = totalJimpitanIncome,
                    onAddIncomeClick = { showAddIncomeDialog = true },
                    onDeleteIncome = { id -> viewModel.deleteCashIncome(id) }
                )
                2 -> DaftarPengeluaranTab(
                    expenses = expenses,
                    onAddExpenseClick = { showAddExpenseDialog = true },
                    onDeleteExpense = { id -> viewModel.deleteExpense(id) }
                )
                3 -> KelolaBantuanSakitTab(
                    sickAids = allSickAids,
                    onUpdateStatus = { aid, status -> viewModel.updateSickAidStatus(aid, status) }
                )
                4 -> KelolaWargaTab(
                    houses = houses,
                    selectedMonthInfo = selectedMonthInfo,
                    dailyRate = rtConfig.dailyJimpitanRate,
                    records = selectedMonthRecords,
                    onViewQr = { viewingQrHouse = it },
                    onAddNewHouse = { showAddHouseDialog = true },
                    onDeleteHouse = { houseToDelete = it }
                )
                5 -> PengaturanRtTab(
                    config = rtConfig,
                    onEditRate = { showEditRateDialog = true },
                    onEditSickAidAmount = { showEditSickAidDialog = true },
                    onEditSecurity = { showSecurityDialog = true },
                    onAddAnnouncement = { showAddAnnouncementDialog = true },
                    onResetDummyData = { showResetDummyDialog = true },
                    onLogoutBendahara = { viewModel.logoutBendahara() },
                    onShowMessage = { viewModel.showMessage(it) }
                )
            }
        }
    }

    // Modal Add Expense
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { title, cat, amt, note ->
                viewModel.addExpense(title, cat, amt, note)
                showAddExpenseDialog = false
            }
        )
    }

    // Modal Add Income (Pemasukan Kas Lainnya)
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            onDismiss = { showAddIncomeDialog = false },
            onSave = { title, cat, amt, from, note ->
                viewModel.addCashIncome(title, cat, amt, from, note)
                showAddIncomeDialog = false
            }
        )
    }

    // Modal Edit Rate
    if (showEditRateDialog) {
        EditDailyRateDialog(
            currentRate = rtConfig.dailyJimpitanRate,
            onDismiss = { showEditRateDialog = false },
            onSave = { newRate ->
                viewModel.updateDailyJimpitanRate(newRate)
                showEditRateDialog = false
            }
        )
    }

    // Modal Edit Standar Bantuan Warga Sakit
    if (showEditSickAidDialog) {
        EditStandardSickAidDialog(
            currentAmount = rtConfig.standardSickAidAmount,
            onDismiss = { showEditSickAidDialog = false },
            onSave = { newAmount ->
                viewModel.updateStandardSickAidAmount(newAmount)
                showEditSickAidDialog = false
            }
        )
    }

    // Modal Security / Password Bendahara
    if (showSecurityDialog) {
        EditTreasurerSecurityDialog(
            config = rtConfig,
            onDismiss = { showSecurityDialog = false },
            onSave = { oldPass, newUser, newPass, name ->
                viewModel.updateBendaharaCredentials(oldPass, newUser, newPass, name)
            }
        )
    }

    // Modal Export Text Report
    if (showExportReportDialog) {
        ExportReportDialog(
            rtName = rtConfig.rtName,
            monthInfo = selectedMonthInfo,
            balance = balance,
            totalIncome = totalIncome,
            totalJimpitan = totalJimpitanIncome,
            totalOtherIncome = totalOtherIncomes,
            totalExpenses = totalExpenses,
            expenses = expenses,
            onDismiss = { showExportReportDialog = false },
            onCopySuccess = { viewModel.showMessage("Laporan RT disalin! Siap dikirim ke WhatsApp warga.") }
        )
    }

    // Modal Add House
    if (showAddHouseDialog) {
        AddHouseDialog(
            existingCount = houses.size,
            onDismiss = { showAddHouseDialog = false },
            onSave = { id, block, no, head, phone ->
                viewModel.addHouse(id, block, no, head, phone)
                showAddHouseDialog = false
            }
        )
    }

    // Modal Add Announcement
    if (showAddAnnouncementDialog) {
        AddAnnouncementDialog(
            onDismiss = { showAddAnnouncementDialog = false },
            onSave = { title, content, priority ->
                viewModel.addAnnouncement(title, content, priority)
                showAddAnnouncementDialog = false
            }
        )
    }

    // Modal View House QR
    viewingQrHouse?.let { house ->
        AlertDialog(
            onDismissRequest = { viewingQrHouse = null },
            confirmButton = {
                Button(
                    onClick = { viewingQrHouse = null },
                    modifier = Modifier.fillMaxWidth().testTag("close_house_qr_button")
                ) {
                    Text("Tutup")
                }
            },
            text = {
                HouseQrCard(
                    house = house,
                    onShareClick = {
                        viewModel.showMessage("Tautan kartu QR ${house.headOfFamily} disalin.")
                    }
                )
            }
        )
    }

    // Modal Konfirmasi Hapus Warga / KK (Warga Pindah)
    houseToDelete?.let { house ->
        AlertDialog(
            onDismissRequest = { houseToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Hapus KK Warga (Pindah)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin menghapus data warga ${house.headOfFamily} (${house.block} - ${house.houseNumber})?\n\n• Data warga & QR akan otomatis terhapus dari penyimpanan lokal dan database Firebase Cloud.\n• Seluruh riwayat uang & jimpitan yang pernah disetorkan tetap aman di kas RT dan tidak akan hilang."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHouse(house.id)
                        houseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_house_btn")
                ) {
                    Text("Hapus KK")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { houseToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_house_btn")
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Konfirmasi Pembersihan Data Uji / Dummy
    if (showResetDummyDialog) {
        AlertDialog(
            onDismissRequest = { showResetDummyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = StatusAmber,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Bersihkan Seluruh Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Seluruh riwayat transaksi jimpitan, kas masuk, kas keluar, pengumuman, dan permohonan bantuan sakit akan dibersihkan bersih dari 0 (baik lokal maupun Cloud Firebase).\n\nDaftar KK warga dan akun bendahara tetap aman tersimpan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDummyData()
                        showResetDummyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAmber),
                    modifier = Modifier.testTag("confirm_reset_dummy_btn")
                ) {
                    Text("Bersihkan Seluruh Data")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetDummyDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun DashboardKasTab(
    rtName: String,
    selectedMonthInfo: MonthInfo,
    balance: Long,
    totalIncome: Long,
    totalJimpitanIncome: Long,
    totalOtherIncomes: Long,
    totalExpenses: Long,
    expenses: List<CashExpense>,
    onDownloadPdf: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onExportReportClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Balance Card - Ultra High Contrast (Bersih & Elegan tanpa banner bertumpuk)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_balance_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF022C22) // Rich Deep Forest Emerald, maximum contrast
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SALDO KAS RUKUN TETANGGA ($rtName)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFFFFF) // Pure 100% white
                        )
                        FilledTonalButton(
                            onClick = onDownloadPdf,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF065F46),
                                contentColor = Color(0xFF86EFAC)
                            ),
                            modifier = Modifier.testTag("download_pdf_button")
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = Formatters.formatRupiah(balance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFFFFF)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Total Pemasukan",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Formatters.formatRupiah(totalIncome),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4ADE80)
                            )
                            Text(
                                text = "Jimpitan: ${Formatters.formatRupiah(totalJimpitanIncome)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Total Pengeluaran",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Formatters.formatRupiah(totalExpenses),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF87171)
                            )
                            Text(
                                text = "Kas Lain: ${Formatters.formatRupiah(totalOtherIncomes)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddIncomeClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_income_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Pemasukan", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_expense_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Pengeluaran", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onExportReportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_text_report_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salin Laporan Format WhatsApp")
            }
        }

        // Expense Category Breakdown Visual Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Breakdown Pengeluaran per Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val categories = listOf(
                        "Keamanan & Ronda",
                        "Santunan Warga Sakit",
                        "Kebersihan",
                        "Konsumsi Kegiatan",
                        "Infrastruktur"
                    )

                    categories.forEach { cat ->
                        val catTotal = expenses.filter { it.category.contains(cat, ignoreCase = true) }.sumOf { it.amount }
                        val pct = if (totalExpenses > 0) (catTotal.toFloat() / totalExpenses.toFloat()) else 0f
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "${Formatters.formatRupiah(catTotal)} (${(pct * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = EmeraldPrimary,
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaftarPemasukanTab(
    cashIncomes: List<CashIncome>,
    totalJimpitan: Long,
    onAddIncomeClick: () -> Unit,
    onDeleteIncome: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Jimpitan Stream Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Arus Pemasukan Jimpitan Warga",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Otomatis dari pencatatan tarikan ronda warga",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                    Text(
                        text = "+${Formatters.formatRupiah(totalJimpitan)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pemasukan Kas Lainnya",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddIncomeClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Pemasukan", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (cashIncomes.isEmpty()) {
            item {
                Text(
                    text = "Belum ada catatan pemasukan kas lainnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(cashIncomes.distinctBy { it.id }, key = { "bend_inc_${it.id}" }) { inc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = inc.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sumber: ${inc.receivedFrom} • ${inc.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${inc.dateString} ${if (inc.receiptNote.isNotEmpty()) "• " + inc.receiptNote else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${Formatters.formatRupiah(inc.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                            IconButton(
                                onClick = { onDeleteIncome(inc.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaftarPengeluaranTab(
    expenses: List<CashExpense>,
    onAddExpenseClick: () -> Unit,
    onDeleteExpense: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Pengeluaran Kas RT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Catat Pengeluaran", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                Text(
                    text = "Belum ada catatan pengeluaran.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(expenses.distinctBy { it.id }, key = { "bend_exp_${it.id}" }) { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exp.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${exp.category} • ${exp.dateString}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (exp.receiptNote.isNotEmpty()) {
                                Text(
                                    text = "Bukti: ${exp.receiptNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-${Formatters.formatRupiah(exp.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626)
                            )
                            IconButton(
                                onClick = { onDeleteExpense(exp.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KelolaBantuanSakitTab(
    sickAids: List<SickResidentAid>,
    onUpdateStatus: (SickResidentAid, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Persetujuan Bantuan Warga Sakit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Permohonan dana santunan kesehatan warga yang memerlukan persetujuan Ketua RT & Bendahara.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sickAids.isEmpty()) {
            item {
                Text(
                    text = "Tidak ada permohonan bantuan warga sakit saat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sickAids.distinctBy { it.id }, key = { "bend_aid_${it.id}" }) { aid ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pasien: ${aid.patientName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (aid.status) {
                                    "DISETUJUI", "DICAIRKAN" -> StatusGreen.copy(alpha = 0.15f)
                                    "MENUNGGU" -> StatusAmber.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Text(
                                    text = aid.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (aid.status) {
                                        "DISETUJUI", "DICAIRKAN" -> StatusGreen
                                        "MENUNGGU" -> StatusAmber
                                        else -> MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }

                        Text(
                            text = "Pemohon: ${aid.applicantName} (${aid.houseId}) • ${Formatters.formatDate(aid.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Diagnosa: ${aid.diagnosisOrCondition}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (aid.hospitalOrNeeds.isNotEmpty()) {
                            Text(
                                text = "Lokasi Rawat: ${aid.hospitalOrNeeds}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Nominal Santunan: ${Formatters.formatRupiah(aid.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (aid.status == "MENUNGGU") {
                                Button(
                                    onClick = { onUpdateStatus(aid, "DISETUJUI") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Setujui", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { onUpdateStatus(aid, "DITOLAK") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Tolak", style = MaterialTheme.typography.labelMedium)
                                }
                            } else if (aid.status == "DISETUJUI") {
                                Button(
                                    onClick = { onUpdateStatus(aid, "DICAIRKAN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                                ) {
                                    Text("Cairkan Santunan Kas RT", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KelolaWargaTab(
    houses: List<House>,
    selectedMonthInfo: MonthInfo,
    dailyRate: Long,
    records: List<com.example.data.model.JimpitanRecord>,
    onViewQr: (House) -> Unit,
    onAddNewHouse: () -> Unit,
    onDeleteHouse: (House) -> Unit
) {
    val targetPerHouse = selectedMonthInfo.daysInMonth * dailyRate
    val housePaidMap = remember(records) {
        records.groupBy { it.houseId }.mapValues { (_, recs) -> recs.sumOf { it.amount } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Data Warga & QR Code RT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Periode ${selectedMonthInfo.monthName} (Target: ${Formatters.formatRupiah(targetPerHouse)}/KK)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddNewHouse,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_house_button")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah KK", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        items(houses.distinctBy { it.id }, key = { "bend_house_${it.id}" }) { house ->
            val paid = housePaidMap[house.id] ?: 0L
            val shortage = (targetPerHouse - paid).coerceAtLeast(0L)
            val isLunas = shortage == 0L

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${house.block} - ${house.houseNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = house.headOfFamily,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${selectedMonthInfo.monthName}: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isLunas) "Lunas (${Formatters.formatRupiah(paid)})"
                                       else "Kurang ${Formatters.formatRupiah(shortage)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLunas) StatusGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { onViewQr(house) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("view_qr_${house.id}")
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("QR", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = { onDeleteHouse(house) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("delete_house_${house.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus KK", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Hapus", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PengaturanRtTab(
    config: RtConfig,
    onEditRate: () -> Unit,
    onEditSickAidAmount: () -> Unit,
    onEditSecurity: () -> Unit,
    onAddAnnouncement: () -> Unit,
    onResetDummyData: () -> Unit,
    onLogoutBendahara: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Pengaturan & Keamanan Pengurus RT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Kelola tarif dasar, keamanan login bendahara, dan publikasi pengumuman warga.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Card 1: Status Koneksi Database Firebase Cloud (Real Production)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF064E3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF86EFAC), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Database Firebase Terhubung",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF16A34A).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "RIIL AKTIF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Project ID: Jimpitan kita • Dimas Bani Agung Frandika",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Aplikasi siap beroperasi dengan sinkronisasi database cloud.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

    
          // Card 1: Tarif Jimpitan Harian
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tarif Iuran Jimpitan Harian",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tarif aktif: ${Formatters.formatRupiah(config.dailyJimpitanRate)} / hari",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "30 hari: ${Formatters.formatRupiah(30 * config.dailyJimpitanRate)} | 31 hari: ${Formatters.formatRupiah(31 * config.dailyJimpitanRate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Button(onClick = onEditRate) {
                        Text("Ubah")
                    }
                }
            }
        }

        // Card 2: Standar Nominal Bantuan Warga Sakit
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nominal Bantuan yang Diberikan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Standar RT: ${Formatters.formatRupiah(config.standardSickAidAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hanya Bendahara & Pengurus RT yang dapat merubahnya. Warga hanya dapat melihat nominal ini.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Button(onClick = onEditSickAidAmount) {
                        Text("Ubah")
                    }
                }
            }
        }

        // Card 2: Keamanan Akun Bendahara
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keamanan Akun & Sandi Bendahara",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Username: ${config.treasurerUsername}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pejabat: ${config.treasurerName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Button(onClick = onEditSecurity) {
                        Text("Ganti Sandi")
                    }
                }
            }
        }

        // Card 3: Buat Pengumuman Baru
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Publikasi Pengumuman Warga",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kirim pesan informasi kegiatan RT ke aplikasi seluruh warga",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onAddAnnouncement) {
                        Text("Kirim Info")
                    }
                }
            }
        }

              // Card 2: Pembersihan Data Uji / Dummy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusAmber.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bersihkan Semua Data Transaksi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Menghapus seluruh riwayat jimpitan & kas (Lokal & Cloud) kembali ke Rp 0. Data KK warga tetap tersimpan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onResetDummyData,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bersihkan")
                    }
                }
            }
        }


        // Card 4: Logout Bendahara
        item {
            OutlinedButton(
                onClick = onLogoutBendahara,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kunci & Keluar dari Mode Bendahara")
            }
        }
    }
}

// Dialogs
@Composable
fun EditDailyRateDialog(
    currentRate: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var rateText by remember { mutableStateOf(currentRate.toString()) }
    val presets = listOf(500L, 1000L, 2000L, 5000L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ubah Tarif Jimpitan Harian RT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tarif jimpitan harian ini menjadi acuan hitung iuran warga (misal 30/31 hari per bulan):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Tarif per Hari (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("daily_rate_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Preset Cepat:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = rateText == preset.toString(),
                            onClick = { rateText = preset.toString() },
                            label = { Text(Formatters.formatRupiah(preset)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateText.toLongOrNull() ?: 500L
                    onSave(rate)
                }
            ) {
                Text("Simpan Tarif")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun EditStandardSickAidDialog(
    currentAmount: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf(currentAmount.toString()) }
    val presets = listOf(100000L, 200000L, 300000L, 500000L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Atur Nominal Bantuan yang Diberikan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tentukan nominal bantuan sosial resmi RT untuk warga yang sakit atau dirawat. Nominal ini otomatis terkunci dan tidak dapat diubah oleh warga saat mengajukan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Bantuan yang Diberikan (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_standard_sick_aid_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Pilihan Nominal Standar Cepat:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = amountText == preset.toString(),
                            onClick = { amountText = preset.toString() },
                            label = { Text(Formatters.formatRupiah(preset), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 200000L
                    onSave(amt)
                },
                modifier = Modifier.testTag("save_standard_sick_aid_button")
            ) {
                Text("Simpan Nominal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AddIncomeDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, amount: Long, receivedFrom: String, receiptNote: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Donasi Warga") }
    var amountText by remember { mutableStateOf("") }
    var receivedFrom by remember { mutableStateOf("") }
    var receiptNote by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val categories = listOf(
        "Donasi Warga",
        "Sewa Tenda & Fasum",
        "Bantuan Pemerintah/Kelurahan",
        "Swadaya Kegiatan",
        "Lainnya"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Catat Pemasukan Kas RT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Sumber Pemasukan") },
                    placeholder = { Text("Misal: Sumbangan Donasi Warga Peduli") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("income_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Pemasukan (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("income_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = receivedFrom,
                    onValueChange = { receivedFrom = it },
                    label = { Text("Diterima Dari") },
                    placeholder = { Text("Misal: Bpk. Hendra (A3)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Kategori Pemasukan:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))

                categories.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = receiptNote,
                    onValueChange = { receiptNote = it },
                    label = { Text("Catatan / No. Kuitansi (Opsional)") },
                    placeholder = { Text("Misal: Kuitansi No. 12") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Judul dan nominal harus diisi dengan benar.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    if (title.isBlank() || amt <= 0) {
                        isError = true
                    } else {
                        onSave(title, selectedCategory, amt, receivedFrom, receiptNote)
                    }
                },
                modifier = Modifier.testTag("save_income_button")
            ) {
                Text("Simpan Pemasukan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, amount: Long, receiptNote: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Keamanan & Ronda") }
    var amountText by remember { mutableStateOf("") }
    var receiptNote by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val categories = listOf(
        "Keamanan & Ronda",
        "Santunan Warga Sakit",
        "Kebersihan",
        "Infrastruktur / Fasum",
        "Konsumsi Kegiatan",
        "Lainnya"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Catat Pengeluaran Kas RT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Pengeluaran / Keperluan") },
                    placeholder = { Text("Misal: Beli Lampu LED Pos Ronda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal Pengeluaran (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Kategori Pengeluaran:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))

                categories.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = receiptNote,
                    onValueChange = { receiptNote = it },
                    label = { Text("Keterangan Struk / Nota Toko") },
                    placeholder = { Text("Misal: Nota Toko Berkah No. 12") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Judul dan nominal harus diisi.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    if (title.isBlank() || amt <= 0) {
                        isError = true
                    } else {
                        onSave(title, selectedCategory, amt, receiptNote)
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Simpan Pengeluaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ExportReportDialog(
    rtName: String,
    monthInfo: MonthInfo,
    balance: Long,
    totalIncome: Long,
    totalJimpitan: Long,
    totalOtherIncome: Long,
    totalExpenses: Long,
    expenses: List<CashExpense>,
    onDismiss: () -> Unit,
    onCopySuccess: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val reportText = remember(rtName, monthInfo, balance, totalIncome, totalExpenses, expenses) {
        buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📋 LAPORAN KEUANGAN KAS & JIMPITAN $rtName")
            appendLine("Periode: ${monthInfo.monthName} ${monthInfo.year} (${monthInfo.daysInMonth} Hari)")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 Total Pemasukan : ${Formatters.formatRupiah(totalIncome)}")
            appendLine("   - Dari Jimpitan  : ${Formatters.formatRupiah(totalJimpitan)}")
            appendLine("   - Kas Lainnya    : ${Formatters.formatRupiah(totalOtherIncome)}")
            appendLine("💸 Total Pengeluaran: ${Formatters.formatRupiah(totalExpenses)}")
            appendLine("🏦 Sisa Saldo Kas RT : ${Formatters.formatRupiah(balance)}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Rincian Pengeluaran Terakhir:")
            if (expenses.isEmpty()) {
                appendLine("- Belum ada catatan pengeluaran.")
            } else {
                expenses.take(6).forEachIndexed { idx, exp ->
                    appendLine("${idx + 1}. ${exp.title} (${exp.category})")
                    appendLine("   Nominal: ${Formatters.formatRupiah(exp.amount)} | Bukti: ${exp.receiptNote.ifEmpty { "Terverifikasi" }}")
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Laporan resmi disiarkan otomatis via JimpitanKita.")
            appendLine("Transparan, Amanah, Guyub Rukun.")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Laporan Teks Format WhatsApp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Format teks siap salin untuk disiarkan ke grup WhatsApp warga:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    Text(
                        text = reportText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(reportText))
                    onCopySuccess()
                    onDismiss()
                },
                modifier = Modifier.testTag("copy_report_button")
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin Teks WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun AddHouseDialog(
    existingCount: Int,
    onDismiss: () -> Unit,
    onSave: (id: String, block: String, houseNumber: String, headOfFamily: String, phoneNumber: String) -> Unit
) {
    var block by remember { mutableStateOf("Blok A") }
    var number by remember { mutableStateOf("No. ${String.format("%02d", existingCount + 1)}") }
    var headOfFamily by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("08") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daftarkan Warga / KK Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = block,
                        onValueChange = { block = it },
                        label = { Text("Blok") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("No. Rumah") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = headOfFamily,
                    onValueChange = { headOfFamily = it },
                    label = { Text("Nama Kepala Keluarga") },
                    placeholder = { Text("Misal: Bpk. Bambang Pamungkas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_house_head_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Nomor WhatsApp / HP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Otomatis tersimpan ke database lokal & Firebase Cloud real-time.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF047857)
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nama kepala keluarga harus diisi.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (headOfFamily.isBlank()) {
                        isError = true
                    } else {
                        val cleanBlock = block.replace(" ", "").takeLast(1).uppercase()
                        val cleanNum = number.filter { it.isDigit() }
                        val id = "RT02-$cleanBlock${cleanNum.padStart(2, '0')}"
                        onSave(id, block, number, headOfFamily, phoneNumber)
                    }
                },
                modifier = Modifier.testTag("save_new_house_button")
            ) {
                Text("Daftarkan & Buat QR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AddAnnouncementDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("NORMAL") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Buat Pengumuman Warga RT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Pengumuman") },
                    placeholder = { Text("Misal: Kerja Bakti Hari Minggu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("announcement_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Isi Pesan Pengumuman") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("announcement_content_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = priority == "NORMAL",
                        onClick = { priority = "NORMAL" },
                        label = { Text("Normal") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = priority == "PENTING",
                        onClick = { priority = "PENTING" },
                        label = { Text("Penting / Urgent") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Judul dan isi pengumuman tidak boleh kosong.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || content.isBlank()) {
                        isError = true
                    } else {
                        onSave(title, content, priority)
                    }
                },
                modifier = Modifier.testTag("broadcast_button")
            ) {
                Text("Sebarkan Pengumuman")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
