package com.example.ui.screens.petugas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.JimpitanStatus
import com.example.data.model.RtConfig
import com.example.ui.components.CameraScanOverlay
import com.example.ui.components.EditOfficerNameDialog
import com.example.ui.components.EditOfficerSecurityDialog
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.viewmodel.JimpitanViewModel
import com.example.util.Formatters
import com.example.util.MonthInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetugasHomeScreen(
    viewModel: JimpitanViewModel,
    modifier: Modifier = Modifier
) {
    val houses by viewModel.houses.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val officerName by viewModel.officerName.collectAsState()
    val rtConfig by viewModel.rtConfig.collectAsState()
    val selectedMonthInfo by viewModel.selectedMonthInfo.collectAsState()
    val selectedMonthRecords by viewModel.selectedMonthRecords.collectAsState()

    var showScannerDialog by remember { mutableStateOf(false) }
    var selectedHouseForInput by remember { mutableStateOf<House?>(null) }
    var showEditOfficerDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf("SEMUA") } // "SEMUA", "BELUM", "SUDAH"
    var searchQuery by remember { mutableStateOf("") }

    // Group records by house for selected month
    val targetPerHouse = remember(selectedMonthInfo, rtConfig) {
        selectedMonthInfo.daysInMonth * rtConfig.dailyJimpitanRate
    }
    val houseMonthPaidMap = remember(selectedMonthRecords) {
        selectedMonthRecords.groupBy { it.houseId }
            .mapValues { (_, recs) -> recs.sumOf { it.amount } }
    }

    // Map today's collected houses
    val recordedHouseMap = remember(todayRecords) {
        todayRecords.associateBy { it.houseId }
    }

    val totalCollectedTonight = remember(todayRecords) {
        todayRecords.sumOf { it.amount }
    }
    val housesCollectedCount = remember(todayRecords) {
        todayRecords.count { it.status != JimpitanStatus.KOSONG_ABSEN }
    }
    val housesEmptyCount = remember(todayRecords) {
        todayRecords.count { it.status == JimpitanStatus.KOSONG_ABSEN }
    }

    val filteredHouses = remember(houses, recordedHouseMap, filterStatus, searchQuery) {
        val baseList = when (filterStatus) {
            "BELUM" -> houses.filter { !recordedHouseMap.containsKey(it.id) }
            "SUDAH" -> houses.filter { recordedHouseMap.containsKey(it.id) }
            else -> houses
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.headOfFamily.lowercase().contains(q) ||
                it.houseNumber.lowercase().contains(q) ||
                it.block.lowercase().contains(q) ||
                it.id.lowercase().contains(q) ||
                it.spouseOrSecondary.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("petugas_home_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Officer Banner with Edit Name button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                .border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_petugas_ronda),
                                contentDescription = "Foto Profil Petugas Ronda",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Petugas Ronda Lapangan • ${rtConfig.rtName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = officerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Tarif Jimpitan: ${Formatters.formatRupiah(rtConfig.dailyJimpitanRate)}/hari",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                            )
                        }

                        Row {
                            FilledTonalIconButton(
                                onClick = { showEditOfficerDialog = true },
                                modifier = Modifier.testTag("edit_officer_name_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ganti Nama Petugas",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            FilledTonalIconButton(
                                onClick = { showSecurityDialog = true },
                                modifier = Modifier.testTag("edit_officer_security_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Ganti Sandi Petugas",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // Month Selector Bar for checking month dues
            item {
                MonthSelectorBar(
                    currentMonthInfo = selectedMonthInfo,
                    onSelectMonth = { year, month -> viewModel.setSelectedMonth(year, month) }
                )
            }

            // Quick Stats Tonight
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Total Tarikan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatRupiah(totalCollectedTonight),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Terambil",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$housesCollectedCount / ${houses.size} KK",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusGreen
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Absen / Kosong",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$housesEmptyCount Rumah",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusAmber
                            )
                        }
                    }
                }
            }

            // 1-Click Scan Hero Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showScannerDialog = true }
                        .testTag("scan_banner_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pindai QR Code Jimpitan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Arahkan kamera ke stiker pintu warga untuk input cepat harian / bulanan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Fitur Pencarian Nama Warga & Filter Status (Menggantikan Rute Rumah & Cek Tunggakan)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pencarian Nama Warga",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Bulan: ${selectedMonthInfo.monthName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Ketik nama warga, blok, atau no...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cari Warga",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Pencarian"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("petugas_citizen_search_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterStatus == "SEMUA",
                                onClick = { filterStatus = "SEMUA" },
                                label = { Text("Semua (${houses.size})") }
                            )
                            FilterChip(
                                selected = filterStatus == "BELUM",
                                onClick = { filterStatus = "BELUM" },
                                label = { Text("Belum (${houses.size - recordedHouseMap.size})") }
                            )
                            FilterChip(
                                selected = filterStatus == "SUDAH",
                                onClick = { filterStatus = "SUDAH" },
                                label = { Text("Sudah (${recordedHouseMap.size})") }
                            )
                        }

                        if (searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hasil pencarian \"$searchQuery\": ${filteredHouses.size} warga ditemukan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (filteredHouses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Tidak ada warga dengan nama \"$searchQuery\""
                                else
                                    "Tidak ada data rumah pada kategori ini",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reset Pencarian")
                                }
                            }
                        }
                    }
                }
            }

            // Houses List with Month Shortage tracker!
            items(filteredHouses.distinctBy { it.id }, key = { "petugas_house_${it.id}" }) { house ->
                val todayRecord = recordedHouseMap[house.id]
                val paidInMonth = houseMonthPaidMap[house.id] ?: 0L
                val monthShortage = (targetPerHouse - paidInMonth).coerceAtLeast(0L)
                val isMonthLunas = monthShortage == 0L

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedHouseForInput = house }
                        .testTag("house_item_${house.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayRecord != null)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.surface
                    ),
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (todayRecord != null) {
                                        when (todayRecord.status) {
                                            JimpitanStatus.KOSONG_ABSEN -> StatusAmber.copy(alpha = 0.15f)
                                            else -> StatusGreen.copy(alpha = 0.15f)
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (todayRecord != null) {
                                    when (todayRecord.status) {
                                        JimpitanStatus.KOSONG_ABSEN -> Icons.Default.Cancel
                                        else -> Icons.Default.CheckCircle
                                    }
                                } else {
                                    Icons.Default.HourglassEmpty
                                },
                                contentDescription = null,
                                tint = if (todayRecord != null) {
                                    when (todayRecord.status) {
                                        JimpitanStatus.KOSONG_ABSEN -> StatusAmber
                                        else -> StatusGreen
                                    }
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Status Bulan Terpilih (Kekurangan iuran)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedMonthInfo.monthName}: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isMonthLunas) "LUNAS (${Formatters.formatRupiah(paidInMonth)})"
                                           else "Kurang ${Formatters.formatRupiah(monthShortage)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMonthLunas) StatusGreen else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Button(
                            onClick = { selectedHouseForInput = house },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isMonthLunas) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDCFCE7),
                                    contentColor = Color(0xFF166534)
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                            modifier = Modifier.testTag("record_btn_${house.id}")
                        ) {
                            Text(
                                text = if (isMonthLunas) "Lunas" else if (todayRecord == null) "Tarik" else "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isMonthLunas) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Modal Edit Officer Name
    if (showEditOfficerDialog) {
        EditOfficerNameDialog(
            currentName = officerName,
            onDismiss = { showEditOfficerDialog = false },
            onSave = { newName ->
                viewModel.updateOfficerName(newName)
                showEditOfficerDialog = false
            }
        )
    }

    // Modal Edit Officer Security (Username & Password)
    if (showSecurityDialog) {
        EditOfficerSecurityDialog(
            config = rtConfig,
            onDismiss = { showSecurityDialog = false },
            onSave = { oldPass, newUser, newPass ->
                viewModel.updatePetugasCredentials(oldPass, newUser, newPass)
            }
        )
    }

    // Dedicated Camera Scanning Overlay for QR Code Reader
    if (showScannerDialog) {
        CameraScanOverlay(
            houses = houses,
            onDismiss = { showScannerDialog = false },
            onHouseScanned = { house ->
                showScannerDialog = false
                selectedHouseForInput = house
            }
        )
    }

    // Modal Input Jimpitan Form
    selectedHouseForInput?.let { house ->
        val paidInMonth = houseMonthPaidMap[house.id] ?: 0L
        val monthShortage = (targetPerHouse - paidInMonth).coerceAtLeast(0L)
        val isMonthLunas = monthShortage == 0L

        InputJimpitanDialog(
            house = house,
            existingRecord = recordedHouseMap[house.id],
            dailyRate = rtConfig.dailyJimpitanRate,
            selectedMonthInfo = selectedMonthInfo,
            isMonthLunas = isMonthLunas,
            paidInMonth = paidInMonth,
            onDismiss = { selectedHouseForInput = null },
            onSave = { amount, status, notes, billingDays, periodType, monthKey ->
                viewModel.recordJimpitan(
                    houseId = house.id,
                    amount = amount,
                    status = status,
                    notes = notes,
                    billingPeriodDays = billingDays,
                    periodType = periodType,
                    targetMonthKey = monthKey
                )
                selectedHouseForInput = null
            }
        )
    }
}



@Composable
fun InputJimpitanDialog(
    house: House,
    existingRecord: JimpitanRecord?,
    dailyRate: Long,
    selectedMonthInfo: MonthInfo,
    isMonthLunas: Boolean = false,
    paidInMonth: Long = 0L,
    onDismiss: () -> Unit,
    onSave: (amount: Long, status: JimpitanStatus, notes: String, billingDays: Int, periodType: String, monthKey: String) -> Unit
) {
    var periodType by remember { mutableStateOf(existingRecord?.periodType ?: "HARIAN") } // "HARIAN" / "BULANAN"
    var billingDays by remember { mutableStateOf(existingRecord?.billingPeriodDays ?: 1) }
    var selectedStatus by remember { mutableStateOf(existingRecord?.status ?: JimpitanStatus.TERAMBIL_TUNAI) }
    var notesText by remember { mutableStateOf(existingRecord?.notes ?: "") }

    // Customizable Nominal Input (allows > 500 or any custom amount in cash can)
    var nominalInputText by remember {
        mutableStateOf(
            if (existingRecord != null && existingRecord.amount > 0) existingRecord.amount.toString()
            else dailyRate.toString()
        )
    }

    val finalAmount = remember(nominalInputText, selectedStatus) {
        if (selectedStatus == JimpitanStatus.KOSONG_ABSEN) 0L
        else nominalInputText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isMonthLunas) "Status Iuran Jimpitan" else "Catat Jimpitan Warga",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${house.block} - ${house.houseNumber} (${house.headOfFamily})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isMonthLunas) {
                    // Status tagihan sudah lunas - petugas lapangan tidak bisa menginput nominal lagi
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF16A34A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tagihan Bulan ${selectedMonthInfo.monthName} Sudah Lunas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF14532D)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Iuran warga ${house.headOfFamily} (${house.block} - ${house.houseNumber}) periode ${selectedMonthInfo.monthName} ${selectedMonthInfo.year} sebesar ${Formatters.formatRupiah(paidInMonth)} telah terbayar lunas. Petugas lapangan tidak dapat menginput nominal lagi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Keterangan Periode:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Periode: ${selectedMonthInfo.monthName} ${selectedMonthInfo.year} (${selectedMonthInfo.daysInMonth} Hari)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Total Terbayar: ${Formatters.formatRupiah(paidInMonth)} (LUNAS)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                            if (existingRecord != null && existingRecord.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Catatan: ${existingRecord.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Info Target Periode Bulan
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Masuk ke Periode: Bulan ${selectedMonthInfo.monthName} ${selectedMonthInfo.year}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                Spacer(modifier = Modifier.height(10.dp))

                // Period Mode: Harian vs Bulanan
                Text(
                    text = "Pilih Jenis Penarikan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = periodType == "HARIAN",
                        onClick = {
                            periodType = "HARIAN"
                            billingDays = 1
                            nominalInputText = dailyRate.toString()
                        },
                        label = { Text("Harian (${Formatters.formatRupiah(dailyRate)})") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = periodType == "BULANAN",
                        onClick = {
                            periodType = "BULANAN"
                            billingDays = selectedMonthInfo.daysInMonth
                            nominalInputText = (selectedMonthInfo.daysInMonth * dailyRate).toString()
                        },
                        label = { Text("Bulanan (${selectedMonthInfo.daysInMonth} hr)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // If Bulanan, choose 30 or 31 or custom days
                if (periodType == "BULANAN") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Jumlah Hari Bulan ${selectedMonthInfo.monthName}:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(28, 29, 30, 31).forEach { d ->
                            FilterChip(
                                selected = billingDays == d,
                                onClick = {
                                    billingDays = d
                                    nominalInputText = (d * dailyRate).toString()
                                },
                                label = { Text("$d Hari") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Nominal Input (allows > 500 or any amount in coin/cash can)
                Text(
                    text = "Nominal Uang Jimpitan di Rumah (Rp):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Jika warga memberi lebih dari Rp 500 (uang koin/kertas), ketik jumlah pastinya di sini:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = nominalInputText,
                    onValueChange = { input ->
                        nominalInputText = input.filter { it.isDigit() }
                        if (selectedStatus == JimpitanStatus.KOSONG_ABSEN && nominalInputText.isNotEmpty()) {
                            selectedStatus = JimpitanStatus.TERAMBIL_TUNAI
                        }
                    },
                    enabled = selectedStatus != JimpitanStatus.KOSONG_ABSEN,
                    label = { Text("Nominal Uang (Rp)") },
                    placeholder = { Text("Contoh: 500, 1000, 2000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("jimpitan_custom_nominal_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Chips for fast selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(500L, 1000L, 2000L, 5000L, 10000L, 15000L, 15500L).forEach { chipVal ->
                        AssistChip(
                            onClick = {
                                nominalInputText = chipVal.toString()
                                if (selectedStatus == JimpitanStatus.KOSONG_ABSEN) {
                                    selectedStatus = JimpitanStatus.TERAMBIL_TUNAI
                                }
                            },
                            label = { Text(Formatters.formatRupiah(chipVal), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Pilih Status Penarikan:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                JimpitanStatus.values().filter { it != JimpitanStatus.QRIS_DIGITAL }.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedStatus = status
                                if (status == JimpitanStatus.KOSONG_ABSEN) {
                                    nominalInputText = "0"
                                } else if (nominalInputText == "0") {
                                    nominalInputText = dailyRate.toString()
                                }
                            }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = {
                                selectedStatus = status
                                if (status == JimpitanStatus.KOSONG_ABSEN) {
                                    nominalInputText = "0"
                                } else if (nominalInputText == "0") {
                                    nominalInputText = dailyRate.toString()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedStatus == status) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = status.shortDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Tercatat Masuk Kas:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Formatters.formatRupiah(finalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Catatan Ronda (Opsional)") },
                    placeholder = { Text("Misal: Uang pas, uang lebih, kondisi aman") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("jimpitan_notes_input")
                )
                }
            }
        },
        confirmButton = {
            if (!isMonthLunas) {
                Button(
                    onClick = {
                        onSave(
                            finalAmount,
                            selectedStatus,
                            notesText,
                            billingDays,
                            periodType,
                            selectedMonthInfo.monthKey
                        )
                    },
                    modifier = Modifier.testTag("save_jimpitan_button")
                ) {
                    Text("Simpan ${Formatters.formatRupiah(finalAmount)}")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_jimpitan_dialog_button")
            ) {
                Text(if (isMonthLunas) "Tutup" else "Batal")
            }
        }
    )
}
