package com.example.ui.screens.warga

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.JimpitanStatus
import com.example.data.model.RtConfig
import com.example.data.model.SickResidentAid
import com.example.ui.components.HouseQrCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.JimpitanViewModel
import com.example.util.Formatters
import com.example.util.MonthInfo
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WargaHomeScreen(
    viewModel: JimpitanViewModel,
    modifier: Modifier = Modifier
) {
    val activeHouse by viewModel.activeHouse.collectAsState()
    val activeHouseMonthRecords by viewModel.activeHouseMonthRecords.collectAsState()
    val allActiveRecords by viewModel.activeHouseRecords.collectAsState()
    val balance by viewModel.currentBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val cashIncomes by viewModel.cashIncomes.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val sickAids by viewModel.activeHouseSickAids.collectAsState()
    val rtConfig by viewModel.rtConfig.collectAsState()
    val selectedMonthInfo by viewModel.selectedMonthInfo.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Iuran Bulanan", "Kas RT 02/03", "Pengumuman", "Lapor warga Sakit")

    var showQrModal by remember { mutableStateOf(false) }
    var showApplySickAidDialog by remember { mutableStateOf(false) }

    val house = activeHouse ?: House("RT02-A01", "Blok A", "No. 01", "Bpk. Budi Santoso", qrPayload = "JIMPITAN:RT02:A01")

    // Monthly obligation for selected month
    val monthlyTarget = remember(selectedMonthInfo, rtConfig) {
        selectedMonthInfo.daysInMonth * rtConfig.dailyJimpitanRate
    }
    val paidThisMonth = remember(activeHouseMonthRecords) {
        activeHouseMonthRecords.sumOf { it.amount }
    }
    val shortage = remember(monthlyTarget, paidThisMonth) {
        (monthlyTarget - paidThisMonth).coerceAtLeast(0L)
    }
    val isLunas = shortage == 0L
    val progress = if (monthlyTarget > 0L) (paidThisMonth.toFloat() / monthlyTarget.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("warga_home_screen")
    ) {
        // Resident Welcome Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Warga ${rtConfig.rtName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Tarif: ${Formatters.formatRupiah(rtConfig.dailyJimpitanRate)}/hr",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = house.headOfFamily,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${house.block} ${house.houseNumber} • ID: ${house.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { showQrModal = true },
                        modifier = Modifier.testTag("open_my_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Lihat Kartu QR",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Secondary Tabs - Scrollable to ensure "Pengumuman" is always shown fully
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
                    modifier = Modifier.testTag("warga_tab_$index")
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> IuranKalenderTab(
                    selectedMonthInfo = selectedMonthInfo,
                    onSelectMonth = { year, month -> viewModel.setSelectedMonth(year, month) },
                    monthlyTarget = monthlyTarget,
                    paidThisMonth = paidThisMonth,
                    shortage = shortage,
                    isLunas = isLunas,
                    progress = progress,
                    records = activeHouseMonthRecords,
                    dailyRate = rtConfig.dailyJimpitanRate,
                    onOpenQr = { showQrModal = true }
                )
                1 -> KasRtTransparanTab(
                    rtName = rtConfig.rtName,
                    balance = balance,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    cashIncomes = cashIncomes,
                    expenses = expenses,
                    isDark = isDarkMode
                )
                2 -> PengumumanTab(announcements = announcements)
                3 -> BantuanWargaSakitTab(
                    sickAids = sickAids,
                    standardAidAmount = rtConfig.standardSickAidAmount,
                    onApplyClick = { showApplySickAidDialog = true }
                )
            }
        }
    }

    // Modal QR Code Card
    if (showQrModal) {
        AlertDialog(
            onDismissRequest = { showQrModal = false },
            confirmButton = {
                Button(
                    onClick = { showQrModal = false },
                    modifier = Modifier.fillMaxWidth().testTag("close_qr_modal_button")
                ) {
                    Text("Tutup Kartu QR")
                }
            },
            text = {
                HouseQrCard(
                    house = house,
                    onShareClick = {
                        viewModel.showMessage("Tautan kartu QR disalin ke papan klip.")
                    }
                )
            }
        )
    }

    // Modal Apply Sick Resident Aid Dialog
    if (showApplySickAidDialog) {
        ApplySickAidDialog(
            standardAmount = rtConfig.standardSickAidAmount,
            onDismiss = { showApplySickAidDialog = false },
            onSubmit = { patient, diag, amount, hospital ->
                viewModel.applySickResidentAid(patient, diag, amount, hospital)
                showApplySickAidDialog = false
            }
        )
    }
}

@Composable
private fun IuranKalenderTab(
    selectedMonthInfo: MonthInfo,
    onSelectMonth: (year: Int, monthIndex: Int) -> Unit,
    monthlyTarget: Long,
    paidThisMonth: Long,
    shortage: Long,
    isLunas: Boolean,
    progress: Float,
    records: List<JimpitanRecord>,
    dailyRate: Long,
    onOpenQr: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sortedRecords = remember(records) {
        records.sortedWith(compareByDescending<JimpitanRecord> { it.timestamp }.thenByDescending { it.id })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Selector Bar (Januari s/d Desember)
        item {
            MonthSelectorBar(
                currentMonthInfo = selectedMonthInfo,
                onSelectMonth = onSelectMonth
            )
        }

        // Monthly Status Card
        item {
            val statusCardBg = if (isDark) {
                if (isLunas) Color(0xFF064E3B) else Color(0xFF451A03)
            } else {
                if (isLunas) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
            }
            val statusCardBorder = if (isDark) {
                if (isLunas) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
            } else {
                if (isLunas) Color(0xFF86EFAC) else Color(0xFFFDE68A)
            }
            val statusTitleColor = if (isDark) {
                if (isLunas) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
            } else {
                if (isLunas) Color(0xFF14532D) else Color(0xFF78350F)
            }
            val statusSubtitleColor = if (isDark) Color(0xFFE2E8F0) else if (isLunas) Color(0xFF166534) else Color(0xFF92400E)
            val statusPaidColor = if (isDark) Color(0xFF4ADE80) else if (isLunas) Color(0xFF14532D) else Color(0xFF78350F)
            val statusShortageColor = if (isDark) (if (isLunas) Color(0xFF4ADE80) else Color(0xFFF87171)) else (if (isLunas) Color(0xFF15803D) else Color(0xFFDC2626))
            val statusWarningColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = statusCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Status Iuran Bulan ${selectedMonthInfo.monthName} ${selectedMonthInfo.year}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusTitleColor
                            )
                            Text(
                                text = "${selectedMonthInfo.daysInMonth} Hari x ${Formatters.formatRupiah(dailyRate)} = ${Formatters.formatRupiah(monthlyTarget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusSubtitleColor
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLunas) Color(0xFF16A34A) else Color(0xFFD97706)
                        ) {
                            Text(
                                text = if (isLunas) "LUNAS" else "BELUM LUNAS",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Terbayar: ${Formatters.formatRupiah(paidThisMonth)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusPaidColor
                        )
                        Text(
                            text = if (isLunas) "Tidak ada tunggakan" else "Kurang: ${Formatters.formatRupiah(shortage)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusShortageColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isLunas) Color(0xFF16A34A) else Color(0xFFD97706),
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )

                    if (!isLunas) {
                        val daysShort = if (dailyRate > 0) (shortage / dailyRate) else 0
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ Terdeteksi belum setor sekitar $daysShort hari pada periode ${selectedMonthInfo.monthName}. Anda dapat memasukkan uang ke wadah/kaleng jimpitan atau titipkan ke petugas ronda.",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusWarningColor
                        )
                    }
                }
            }
        }

        // Action Buttons
        item {
            Button(
                onClick = onOpenQr,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_qr_code_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lihat Kartu QR Jimpitan Rumah", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "💡 Petugas ronda malam akan memeriksa dan mengambil uang jimpitan dari kaleng di depan rumah Anda serta memindai kartu QR ini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text(
                text = "Catatan Tarikan Jimpitan ${selectedMonthInfo.monthName} ${selectedMonthInfo.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (sortedRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada catatan transaksi jimpitan di bulan ${selectedMonthInfo.monthName} ${selectedMonthInfo.year}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(sortedRecords.distinctBy { it.id }, key = { "jimpitan_${it.id}" }) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when (record.status) {
                                        JimpitanStatus.TERAMBIL_TUNAI, JimpitanStatus.LUNAS_TRANSFER -> Color(0xFFDCFCE7)
                                        JimpitanStatus.QRIS_DIGITAL -> Color(0xFFDBEAFE)
                                        JimpitanStatus.KOSONG_ABSEN -> Color(0xFFFEF3C7)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (record.status) {
                                    JimpitanStatus.TERAMBIL_TUNAI -> Icons.Default.Savings
                                    JimpitanStatus.QRIS_DIGITAL -> Icons.Default.QrCode
                                    JimpitanStatus.LUNAS_TRANSFER -> Icons.Default.CheckCircle
                                    JimpitanStatus.KOSONG_ABSEN -> Icons.Default.Cancel
                                },
                                contentDescription = null,
                                tint = when (record.status) {
                                    JimpitanStatus.TERAMBIL_TUNAI, JimpitanStatus.LUNAS_TRANSFER -> Color(0xFF16A34A)
                                    JimpitanStatus.QRIS_DIGITAL -> Color(0xFF2563EB)
                                    JimpitanStatus.KOSONG_ABSEN -> Color(0xFFD97706)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.status.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "${record.dateString} • ${record.officerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                            if (record.notes.isNotEmpty()) {
                                Text(
                                    text = record.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        Text(
                            text = if (record.amount > 0) "+${Formatters.formatRupiah(record.amount)}" else "Rp 0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (record.amount > 0) Color(0xFF16A34A) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KasRtTransparanTab(
    rtName: String,
    balance: Long,
    totalIncome: Long,
    totalExpenses: Long,
    cashIncomes: List<CashIncome>,
    expenses: List<CashExpense>,
    isDark: Boolean = true
) {
    var cashFlowFilter by remember { mutableStateOf("SEMUA") } // "SEMUA", "MASUK", "KELUAR"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Main High-Contrast Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF022C22)), // Rich Deep Forest Emerald
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "TOTAL SALDO KAS RT 02/03 ($rtName)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF) // Pure white high contrast
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.formatRupiah(balance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFFFFF) // Pure white
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
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
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Text(
                                text = Formatters.formatRupiah(totalIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF86EFAC)
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
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Text(
                                text = Formatters.formatRupiah(totalExpenses),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }
        }

        // Section Title & Filter Chips Card - Clear Boundary & High Contrast
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Rincian Transparansi Arus Kas $rtName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Laporan terbuka untuk seluruh warga : pemasukan & pengeluaran kas lingkungan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF334155),
                        fontWeight = if (isDark) FontWeight.Medium else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val totalTransCount = cashIncomes.size + expenses.size

                        // 1. SEMUA TRANSAKSI - Tema Biru Royal / Indigo
                        val isSemua = cashFlowFilter == "SEMUA"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { cashFlowFilter = "SEMUA" }
                                .testTag("filter_semua_transaksi"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSemua) Color(0xFF2563EB) else (if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSemua) Color(0xFF1D4ED8) else (if (isDark) Color(0xFF3B82F6).copy(alpha = 0.6f) else Color(0xFF93C5FD))
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSemua) 3.dp else 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Semua",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSemua) Color.White else (if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "($totalTransCount Transaksi)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSemua) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSemua) Color.White.copy(alpha = 0.9f) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. PEMASUKAN - Tema Hijau Emerald
                        val isMasuk = cashFlowFilter == "MASUK"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { cashFlowFilter = "MASUK" }
                                .testTag("filter_pemasukan"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMasuk) Color(0xFF16A34A) else (if (isDark) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFF0FDF4))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isMasuk) Color(0xFF15803D) else (if (isDark) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF86EFAC))
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isMasuk) 3.dp else 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Pemasukan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMasuk) Color.White else (if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "(${cashIncomes.size} Pos Masuk)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isMasuk) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isMasuk) Color.White.copy(alpha = 0.9f) else (if (isDark) Color(0xFFA7F3D0) else Color(0xFF166534)),
                                    maxLines = 1
                                )
                            }
                        }

                        // 3. PENGELUARAN - Tema Merah Crimson
                        val isKeluar = cashFlowFilter == "KELUAR"
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { cashFlowFilter = "KELUAR" }
                                .testTag("filter_pengeluaran"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isKeluar) Color(0xFFDC2626) else (if (isDark) Color(0xFF450A0A).copy(alpha = 0.6f) else Color(0xFFFEF2F2))
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isKeluar) Color(0xFFB91C1C) else (if (isDark) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFFFCA5A5))
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isKeluar) 3.dp else 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Pengeluaran",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKeluar) Color.White else (if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "(${expenses.size} Pos Keluar)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isKeluar) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isKeluar) Color.White.copy(alpha = 0.9f) else (if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Combine items for listing
        if (cashFlowFilter == "SEMUA" || cashFlowFilter == "MASUK") {
            item {
                Text(
                    text = "🟢 Rincian Pemasukan Kas RT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                )
            }

            if (cashIncomes.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada catatan pemasukan kas lainnya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                }
            } else {
                items(cashIncomes.distinctBy { it.id }, key = { "inc_${it.id}" }) { income ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = income.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Dari: ${income.receivedFrom} • ${income.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                )
                                Text(
                                    text = "${income.dateString} ${if (income.receiptNote.isNotEmpty()) "• " + income.receiptNote else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                            Text(
                                text = "+${Formatters.formatRupiah(income.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            )
                        }
                    }
                }
            }
        }

        if (cashFlowFilter == "SEMUA" || cashFlowFilter == "KELUAR") {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔴 Rincian Pengeluaran Kas RT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                )
            }

            if (expenses.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada catatan pengeluaran kas RT.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                }
            } else {
                items(expenses.distinctBy { it.id }, key = { "exp_${it.id}" }) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF451A03) else Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exp.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${exp.category} • ${exp.dateString}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                )
                                if (exp.receiptNote.isNotEmpty()) {
                                    Text(
                                        text = "Bukti: ${exp.receiptNote}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                    )
                                }
                            }
                            Text(
                                text = "-${Formatters.formatRupiah(exp.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PengumumanTab(announcements: List<Announcement>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Papan Pengumuman & Informasi RT 02 RW 03",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pesan & pengumuman otomatis terhapus setelah 24 jam tayang",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (announcements.isEmpty()) {
            item {
                Text(
                    text = "Tidak ada pengumuman saat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(announcements.distinctBy { it.id }, key = { "ann_${it.id}" }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.priority == "PENTING")
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.priority == "PENTING") {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "PENTING",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.author} • ${item.dateString}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BantuanWargaSakitTab(
    sickAids: List<SickResidentAid>,
    standardAidAmount: Long,
    onApplyClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Santunan & Bantuan Warga Sakit RT 02 RW 03",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fasilitas dana sosial kemanusiaan kas RT untuk membantu warga yang sedang dirawat sakit atau memerlukan bantuan kesehatan darurat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Bantuan yang Diberikan: ${Formatters.formatRupiah(standardAidAmount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nominal resmi ditetapkan dan hanya dapat diubah oleh Pengurus & Bendahara RT.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onApplyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_sick_aid_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajukan Bantuan Warga Sakit")
                    }
                }
            }
        }

        item {
            Text(
                text = "Riwayat Pengajuan Bantuan Rumah Anda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (sickAids.isEmpty()) {
            item {
                Text(
                    text = "Belum ada pengajuan bantuan warga sakit dari rumah ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sickAids.distinctBy { it.id }, key = { "aid_${it.id}" }) { aid ->
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
                                text = "Pasien: ${aid.patientName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kondisi: ${aid.diagnosisOrCondition}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (aid.hospitalOrNeeds.isNotEmpty()) {
                                Text(
                                    text = "Lokasi/RS: ${aid.hospitalOrNeeds}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Bantuan yang Diberikan: ${Formatters.formatRupiah(aid.amount)} • ${Formatters.formatDate(aid.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (aid.status) {
                                "DISETUJUI", "DICAIRKAN" -> StatusGreen.copy(alpha = 0.15f)
                                "MENUNGGU" -> StatusAmber.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Text(
                                text = aid.status,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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
                }
            }
        }
    }
}

@Composable
fun ApplySickAidDialog(
    standardAmount: Long,
    onDismiss: () -> Unit,
    onSubmit: (patient: String, diagnosis: String, amount: Long, hospital: String) -> Unit
) {
    var patientName by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pengajuan Bantuan Warga Sakit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Bantuan sosial kesehatan disalurkan dari kas RT 02 RW 03 atas verifikasi Ketua RT & Bendahara.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Nama Anggota Keluarga yang Sakit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sick_patient_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("Diagnosa / Sakit yang Diderita") },
                    placeholder = { Text("Contoh: Demam Berdarah / Rawat Inap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sick_diagnosis_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Tempat Perawatan / Rumah Sakit") },
                    placeholder = { Text("Contoh: RSUD / Puskesmas / Rawat Jalan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bantuan yang Diberikan (Read-only for citizen as requested)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Bantuan yang Diberikan:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatRupiah(standardAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Nominal bantuan kas RT telah ditetapkan resmi oleh Pengurus & Bendahara RT.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nama pasien dan diagnosa sakit harus diisi.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (patientName.isBlank() || diagnosis.isBlank()) {
                        isError = true
                    } else {
                        onSubmit(patientName.trim(), diagnosis.trim(), standardAmount, hospital.trim())
                    }
                },
                modifier = Modifier.testTag("submit_sick_aid_button")
            ) {
                Text("Kirim Permohonan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
