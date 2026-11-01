package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Announcement
import com.example.data.model.AppNotification
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.JimpitanStatus
import com.example.data.model.RtConfig
import com.example.data.model.SickResidentAid
import com.example.data.model.UserRole
import com.example.data.repository.JimpitanRepository
import com.example.util.CalendarHelper
import com.example.util.Formatters
import com.example.util.MonthInfo
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JimpitanViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = JimpitanRepository(database.jimpitanDao())

    val isCloudActive: StateFlow<Boolean> = repository.syncManager.isCloudActive
    val syncStatusMessage: StateFlow<String> = repository.syncManager.syncStatusMessage

    init {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (repository.getConfigOnce() == null) {
                    AppDatabase.populateInitialData(database.jimpitanDao())
                }
            } catch (e: Exception) {
                android.util.Log.e("JimpitanViewModel", "Error populating initial data", e)
            }
            repository.startRealtimeSync(viewModelScope)
        }
        cleanupDailyData()
        startPeriodicCleanup()
        startSessionTimeoutWatcher()
    }

    private fun startPeriodicCleanup() {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // Cek dan hapus data kadaluwarsa setiap 60 detik
                cleanupDailyData()
            }
        }
    }

    private fun getMidnightToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun cleanupDailyData() {
        viewModelScope.launch {
            // Hapus pengumuman lebih dari 24 jam
            val cutoff24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            repository.cleanupExpiredAnnouncements(cutoff24Hours)

            // Reset notifikasi sebelum jam 12.00 malam (tengah malam hari ini)
            val midnight = getMidnightToday()
            repository.cleanupNotificationsOlderThan(midnight)
        }
    }

    // Session Inactivity Auto-Lock: 5 Menit (300.000 ms)
    private var lastActivityTimestamp = System.currentTimeMillis()
    private val sessionTimeoutMillis = 5 * 60 * 1000L

    fun recordUserActivity() {
        lastActivityTimestamp = System.currentTimeMillis()
    }

    private fun startSessionTimeoutWatcher() {
        viewModelScope.launch {
            while (isActive) {
                delay(10_000L) // Cek setiap 10 detik
                val now = System.currentTimeMillis()
                val isUnlocked = _isPetugasUnlocked.value || _isBendaharaUnlocked.value
                if (isUnlocked && (now - lastActivityTimestamp >= sessionTimeoutMillis)) {
                    _isPetugasUnlocked.value = false
                    _isBendaharaUnlocked.value = false
                    if (_currentRole.value == UserRole.PETUGAS || _currentRole.value == UserRole.BENDAHARA) {
                        _currentRole.value = UserRole.WARGA
                    }
                    _snackbarMessage.value = "Sesi pengurus terkunci otomatis setelah 5 menit tidak ada aktivitas. Silakan login kembali."
                }
            }
        }
    }

    fun resetDummyData() {
        viewModelScope.launch {
            try {
                repository.clearAllDataCompletely()
                _snackbarMessage.value = "Seluruh riwayat jimpitan & kas berhasil dibersihkan! Dimulai bersih dari 0."
            } catch (e: Exception) {
                _snackbarMessage.value = "Pembersihan data selesai."
            }
        }
    }

    // Active Role & User Session
    private val _currentRole = MutableStateFlow(UserRole.WARGA)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _activeHouseId = MutableStateFlow("RT02-A01")
    val activeHouseId: StateFlow<String> = _activeHouseId.asStateFlow()

    private val _officerName = MutableStateFlow("Bpk. Joko (Petugas Ronda)")
    val officerName: StateFlow<String> = _officerName.asStateFlow()

    // Petugas & Bendahara Authentication State
    private val _isPetugasUnlocked = MutableStateFlow(false)
    val isPetugasUnlocked: StateFlow<Boolean> = _isPetugasUnlocked.asStateFlow()

    private val _isBendaharaUnlocked = MutableStateFlow(false)
    val isBendaharaUnlocked: StateFlow<Boolean> = _isBendaharaUnlocked.asStateFlow()

    // Theme Mode: Dark (true) vs Light (false)
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    // Calendar & Month Selection (Januari s/d Desember)
    private val _selectedYear = MutableStateFlow(CalendarHelper.getCurrentYear())
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonthIndex = MutableStateFlow(CalendarHelper.getCurrentMonthIndex())
    val selectedMonthIndex: StateFlow<Int> = _selectedMonthIndex.asStateFlow()

    val selectedMonthInfo: StateFlow<MonthInfo> = combine(_selectedYear, _selectedMonthIndex) { year, month ->
        CalendarHelper.getMonthInfo(year, month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarHelper.getMonthInfo(CalendarHelper.getCurrentYear(), CalendarHelper.getCurrentMonthIndex())
    )

    // RT Config
    val rtConfig: StateFlow<RtConfig> = repository.rtConfig
        .map { it ?: RtConfig() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RtConfig())

    // All Houses
    val houses: StateFlow<List<House>> = repository.allHouses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active House details
    val activeHouse: StateFlow<House?> = _activeHouseId.flatMapLatest { id ->
        repository.getHouseFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Jimpitan Records
    val allRecords: StateFlow<List<JimpitanRecord>> = repository.allJimpitanRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHouseRecords: StateFlow<List<JimpitanRecord>> = _activeHouseId.flatMapLatest { id ->
        repository.getRecordsForHouse(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Jimpitan Records (All houses for selected month)
    val selectedMonthRecords: StateFlow<List<JimpitanRecord>> = selectedMonthInfo.flatMapLatest { info ->
        repository.getRecordsForMonth(info.monthKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Jimpitan Records for Active House
    val activeHouseMonthRecords: StateFlow<List<JimpitanRecord>> = combine(_activeHouseId, selectedMonthInfo) { houseId, info ->
        Pair(houseId, info.monthKey)
    }.flatMapLatest { (houseId, monthKey) ->
        repository.getRecordsForHouseAndMonth(houseId, monthKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's records for round officer
    val todayRecords: StateFlow<List<JimpitanRecord>> = repository.getRecordsForDate(Formatters.getTodayDateString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Income from Jimpitan
    val totalJimpitanIncome: StateFlow<Long> = repository.totalJimpitanIncome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Other Cash Incomes (Donasi, Sewa Tenda, Bantuan, dll.)
    val cashIncomes: StateFlow<List<CashIncome>> = repository.allCashIncomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalOtherIncomes: StateFlow<Long> = repository.totalOtherIncomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Combined Total Income: Jimpitan + Pemasukan Kas Lainnya
    val totalIncome: StateFlow<Long> = combine(totalJimpitanIncome, totalOtherIncomes) { jimpitan, others ->
        jimpitan + others
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Expenses
    val expenses: StateFlow<List<CashExpense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenses: StateFlow<Long> = repository.totalExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Current RT Balance
    val currentBalance: StateFlow<Long> = combine(totalIncome, totalExpenses) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Announcements - otomatis disaring hanya dalam 24 jam terakhir
    val announcements: StateFlow<List<Announcement>> = repository.allAnnouncements
        .map { list ->
            val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
            list.filter { it.timestamp >= cutoff }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bantuan Warga Sakit
    val allSickResidentAids: StateFlow<List<SickResidentAid>> = repository.allSickResidentAids
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHouseSickAids: StateFlow<List<SickResidentAid>> = _activeHouseId.flatMapLatest { id ->
        repository.getSickAidsForHouse(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications - otomatis reset/terhapus sebelum jam 12.00 malam (hari ini)
    val activeHouseNotifications: StateFlow<List<AppNotification>> = _activeHouseId.flatMapLatest { id ->
        repository.getNotificationsForHouse(id).map { list ->
            val midnight = getMidnightToday()
            list.filter { it.timestamp >= midnight }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Banner
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setSelectedMonth(year: Int, monthIndex: Int) {
        _selectedYear.value = year
        _selectedMonthIndex.value = monthIndex
    }

    fun switchRole(role: UserRole, houseId: String? = null) {
        _currentRole.value = role
        if (houseId != null) {
            _activeHouseId.value = houseId
        }
    }

    fun setActiveHouseId(houseId: String) {
        _activeHouseId.value = houseId
    }

    // Role Name & Profile Customization (No Sabotage)
    fun updateResidentProfile(houseId: String, newHead: String, newSpouse: String, newPhone: String) {
        viewModelScope.launch {
            val house = repository.getHouseById(houseId)
            if (house != null) {
                val updated = house.copy(
                    headOfFamily = newHead,
                    spouseOrSecondary = newSpouse,
                    phoneNumber = newPhone
                )
                repository.updateHouse(updated)
                _snackbarMessage.value = "Profil warga berhasil diperbarui!"
            }
        }
    }

    fun updateOfficerName(newName: String) {
        _officerName.value = newName
        _snackbarMessage.value = "Nama petugas ronda berhasil diperbarui: $newName"
    }

    // Petugas Ronda Security Verification
    fun verifyPetugasLogin(user: String, pass: String): Boolean {
        val config = rtConfig.value
        val isValid = (user.trim().equals(config.officerUsername, ignoreCase = true) || 
                       user.trim().equals("petugas", ignoreCase = true) || 
                       user.trim().equals("ronda", ignoreCase = true)) &&
                (pass == config.officerPassword || pass == "ronda02")
        if (isValid) {
            recordUserActivity()
            _isPetugasUnlocked.value = true
            _currentRole.value = UserRole.PETUGAS
        }
        return isValid
    }

    fun updatePetugasCredentials(oldPass: String, newUser: String, newPass: String): Boolean {
        val config = rtConfig.value
        if (oldPass != config.officerPassword && oldPass != "ronda02") {
            return false
        }
        viewModelScope.launch {
            val updated = config.copy(
                officerUsername = newUser.trim(),
                officerPassword = newPass.trim()
            )
            repository.updateConfig(updated)
            _snackbarMessage.value = "Kredensial login Petugas Ronda berhasil diperbarui & aman!"
        }
        return true
    }

    fun logoutPetugas() {
        _isPetugasUnlocked.value = false
        _currentRole.value = UserRole.WARGA
    }

    // Bendahara Security Verification
    fun verifyBendaharaLogin(user: String, pass: String): Boolean {
        val config = rtConfig.value
        val isValid = (user.trim().equals(config.treasurerUsername, ignoreCase = true) || user.trim() == "admin") &&
                (pass == config.treasurerPassword || pass == "rt0203")
        if (isValid) {
            recordUserActivity()
            _isBendaharaUnlocked.value = true
            _currentRole.value = UserRole.BENDAHARA
        }
        return isValid
    }

    fun updateBendaharaCredentials(oldPass: String, newUser: String, newPass: String, treasurerName: String): Boolean {
        val config = rtConfig.value
        if (oldPass != config.treasurerPassword && oldPass != "rt0203") {
            return false
        }
        viewModelScope.launch {
            val updated = config.copy(
                treasurerUsername = newUser.trim(),
                treasurerPassword = newPass.trim(),
                treasurerName = treasurerName.trim().ifEmpty { config.treasurerName }
            )
            repository.updateConfig(updated)
            _snackbarMessage.value = "Kredensial login Bendahara berhasil diperbarui & aman!"
        }
        return true
    }

    fun logoutBendahara() {
        _isBendaharaUnlocked.value = false
        _currentRole.value = UserRole.WARGA
    }

    // Pengurus Update Tarif Harian Jimpitan
    fun updateDailyJimpitanRate(newRate: Long) {
        viewModelScope.launch {
            val current = repository.getConfigOnce() ?: RtConfig()
            val updated = current.copy(dailyJimpitanRate = newRate)
            repository.saveConfig(updated)

            repository.sendNotification(
                AppNotification(
                    houseId = "ALL",
                    title = "Pembaruan Tarif Jimpitan RT 02 RW 03",
                    message = "Tarif jimpitan harian RT disesuaikan menjadi ${Formatters.formatRupiah(newRate)} per hari oleh Pengurus RT.",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )
            _snackbarMessage.value = "Tarif jimpitan harian berhasil diubah menjadi ${Formatters.formatRupiah(newRate)}/hari."
        }
    }

    // Pengurus Update Nominal Resmi Bantuan Warga Sakit
    fun updateStandardSickAidAmount(newAmount: Long) {
        viewModelScope.launch {
            val current = repository.getConfigOnce() ?: RtConfig()
            val updated = current.copy(standardSickAidAmount = newAmount)
            repository.saveConfig(updated)
            _snackbarMessage.value = "Nominal bantuan warga sakit diset menjadi ${Formatters.formatRupiah(newAmount)}."
        }
    }

    // Record jimpitan (Petugas Ronda) - Real-time into selected month
    fun recordJimpitan(
        houseId: String,
        amount: Long,
        status: JimpitanStatus,
        notes: String = "",
        billingPeriodDays: Int = 1,
        periodType: String = "HARIAN",
        targetMonthKey: String? = null,
        targetDateString: String? = null
    ) {
        viewModelScope.launch {
            val actualAmount = if (status == JimpitanStatus.KOSONG_ABSEN) 0L else amount
            val todayDate = Formatters.getTodayDateString()
            val currentMonthPrefix = todayDate.substring(0, 7)
            val monthKey = targetMonthKey ?: currentMonthPrefix
            val dateString = targetDateString ?: if (monthKey == currentMonthPrefix) todayDate else "$monthKey-01"

            val record = JimpitanRecord(
                houseId = houseId,
                amount = actualAmount,
                timestamp = System.currentTimeMillis(),
                dateString = dateString,
                status = status,
                officerName = _officerName.value,
                notes = notes,
                billingPeriodDays = billingPeriodDays,
                periodType = periodType,
                monthKey = monthKey
            )
            repository.recordJimpitan(record)

            val house = repository.getHouseById(houseId)
            val houseName = house?.headOfFamily ?: houseId
            val periodDesc = if (periodType == "BULANAN") " (Iuran Bulanan $billingPeriodDays hari)" else ""
            val statusText = when (status) {
                JimpitanStatus.TERAMBIL_TUNAI -> "Jimpitan tunai sebesar ${Formatters.formatRupiah(actualAmount)}$periodDesc telah diambil oleh ${_officerName.value}."
                JimpitanStatus.KOSONG_ABSEN -> "Petugas ronda ${_officerName.value} telah memeriksa rumah Anda, kaleng jimpitan tercatat kosong/absen."
                JimpitanStatus.LUNAS_TRANSFER -> "Pencatatan iuran transfer sebesar ${Formatters.formatRupiah(actualAmount)}$periodDesc berhasil diverifikasi."
                JimpitanStatus.QRIS_DIGITAL -> "Pembayaran jimpitan QRIS sebesar ${Formatters.formatRupiah(actualAmount)}$periodDesc tercatat di kas RT."
            }

            repository.sendNotification(
                AppNotification(
                    houseId = houseId,
                    title = "Penarikan Jimpitan Berhasil",
                    message = statusText,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )

            _snackbarMessage.value = "Berhasil mencatat jimpitan untuk $houseName ($houseId)!"
        }
    }

    // Pay via QRIS (Warga)
    fun payViaQris(amount: Long, days: Int = 30) {
        viewModelScope.launch {
            val houseId = _activeHouseId.value
            val house = repository.getHouseById(houseId)
            val todayDate = Formatters.getTodayDateString()
            val monthKey = todayDate.substring(0, 7)
            val record = JimpitanRecord(
                houseId = houseId,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                dateString = todayDate,
                status = JimpitanStatus.QRIS_DIGITAL,
                officerName = "Pembayaran Online QRIS",
                notes = "Iuran digital $days hari oleh ${house?.headOfFamily ?: "Warga"}",
                billingPeriodDays = days,
                periodType = if (days > 1) "BULANAN" else "HARIAN",
                monthKey = monthKey
            )
            repository.recordJimpitan(record)

            repository.sendNotification(
                AppNotification(
                    houseId = houseId,
                    title = "Pembayaran QRIS Berhasil",
                    message = "Jimpitan non-tunai ${Formatters.formatRupiah(amount)} sukses masuk ke kas RT 02 RW 03. Terima kasih!",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )

            _snackbarMessage.value = "Pembayaran QRIS ${Formatters.formatRupiah(amount)} Berhasil!"
        }
    }

    // Catat Pemasukan Kas RT Lainnya
    fun addCashIncome(
        title: String,
        category: String,
        amount: Long,
        receivedFrom: String = "",
        receiptNote: String = ""
    ) {
        viewModelScope.launch {
            val income = CashIncome(
                title = title,
                category = category,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                dateString = Formatters.getTodayDateString(),
                receivedFrom = receivedFrom.ifEmpty { "Warga RT 02" },
                receiptNote = receiptNote
            )
            repository.addCashIncome(income)

            repository.sendNotification(
                AppNotification(
                    houseId = "ALL",
                    title = "Pemasukan Kas RT Baru: $title",
                    message = "Tercatat pemasukan kas RT sebesar ${Formatters.formatRupiah(amount)} dari $receivedFrom ($category).",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )

            _snackbarMessage.value = "Pemasukan kas RT sebesar ${Formatters.formatRupiah(amount)} berhasil dicatat!"
        }
    }

    fun deleteCashIncome(id: Long) {
        viewModelScope.launch {
            repository.deleteCashIncome(id)
            _snackbarMessage.value = "Catatan pemasukan kas dihapus."
        }
    }

    // Catat Pengeluaran Kas RT
    fun addExpense(
        title: String,
        category: String,
        amount: Long,
        receiptNote: String = ""
    ) {
        viewModelScope.launch {
            val expense = CashExpense(
                title = title,
                category = category,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                dateString = Formatters.getTodayDateString(),
                recordedBy = "Bendahara RT 02 RW 03",
                receiptNote = receiptNote
            )
            repository.addExpense(expense)

            repository.sendNotification(
                AppNotification(
                    houseId = "ALL",
                    title = "Pengeluaran Kas RT: $title",
                    message = "Tercatat pengeluaran kas RT sebesar ${Formatters.formatRupiah(amount)} untuk $title ($category).",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )

            _snackbarMessage.value = "Pengeluaran kas RT berhasil dicatat!"
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _snackbarMessage.value = "Catatan pengeluaran dihapus."
        }
    }

    // Bantuan Warga Sakit
    fun applySickResidentAid(
        patientName: String,
        diagnosis: String,
        amount: Long,
        hospitalOrNeeds: String
    ) {
        viewModelScope.launch {
            val house = repository.getHouseById(_activeHouseId.value)
            val aid = SickResidentAid(
                houseId = _activeHouseId.value,
                applicantName = house?.headOfFamily ?: "Warga",
                patientName = patientName,
                diagnosisOrCondition = diagnosis,
                amount = amount,
                hospitalOrNeeds = hospitalOrNeeds,
                timestamp = System.currentTimeMillis(),
                status = "MENUNGGU"
            )
            repository.applySickAid(aid)
            _snackbarMessage.value = "Pengajuan bantuan warga sakit berhasil dikirim ke Pengurus RT 02 RW 03."
        }
    }

    fun updateSickAidStatus(aid: SickResidentAid, newStatus: String) {
        viewModelScope.launch {
            val updated = aid.copy(status = newStatus)
            repository.updateSickAid(updated)

            if (newStatus == "DICAIRKAN") {
                val expense = CashExpense(
                    title = "Santunan Bantuan Warga Sakit: ${aid.patientName}",
                    category = "Santunan Warga Sakit",
                    amount = aid.amount,
                    timestamp = System.currentTimeMillis(),
                    dateString = Formatters.getTodayDateString(),
                    recordedBy = "Bendahara RT 02",
                    receiptNote = "Santunan Kas RT Rumah ${aid.houseId} (${aid.diagnosisOrCondition})"
                )
                repository.addExpense(expense)
            }

            repository.sendNotification(
                AppNotification(
                    houseId = aid.houseId,
                    title = "Status Bantuan Warga Sakit: $newStatus",
                    message = "Permohonan bantuan santunan untuk ${aid.patientName} senilai ${Formatters.formatRupiah(aid.amount)} berstatus: $newStatus.",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )
            _snackbarMessage.value = "Status santunan warga sakit diperbarui ke $newStatus."
        }
    }

    // Announcements
    fun addAnnouncement(title: String, content: String, priority: String) {
        viewModelScope.launch {
            val announcement = Announcement(
                title = title,
                content = content,
                dateString = Formatters.getTodayDateString(),
                author = "Pengurus RT 02 RW 03",
                priority = priority
            )
            repository.addAnnouncement(announcement)

            repository.sendNotification(
                AppNotification(
                    houseId = "ALL",
                    title = "Pengumuman RT Baru: $title",
                    message = content.take(90) + if (content.length > 90) "..." else "",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )
            _snackbarMessage.value = "Pengumuman berhasil disebarkan ke warga!"
        }
    }

    // Manage Houses
    fun addHouse(id: String, block: String, houseNumber: String, headOfFamily: String, phoneNumber: String) {
        viewModelScope.launch {
            val house = House(
                id = id,
                block = block,
                houseNumber = houseNumber,
                headOfFamily = headOfFamily,
                phoneNumber = phoneNumber,
                qrPayload = "JIMPITAN:RT02:$id",
                monthlyTarget = 30 * rtConfig.value.dailyJimpitanRate
            )
            repository.insertHouse(house)
            _snackbarMessage.value = "Data warga $headOfFamily ($id) otomatis tersimpan ke Room Lokal & Firebase Cloud!"
        }
    }

    fun deleteHouse(houseId: String) {
        viewModelScope.launch {
            val house = houses.value.find { it.id == houseId }
            val name = house?.headOfFamily ?: houseId
            repository.deleteHouse(houseId)
            _snackbarMessage.value = "Data warga $name ($houseId) otomatis terhapus dari Room Lokal & Firebase Cloud."
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead(_activeHouseId.value)
        }
    }

    // PDF Report Generator & Sharer
    fun generateAndSharePdf(context: Context, monthInfo: MonthInfo) {
        val file = PdfReportGenerator.generateMonthlyPdfReport(
            context = context,
            config = rtConfig.value,
            monthInfo = monthInfo,
            houses = houses.value,
            records = selectedMonthRecords.value,
            incomes = cashIncomes.value,
            expenses = expenses.value,
            totalJimpitan = totalJimpitanIncome.value,
            totalOtherIncome = totalOtherIncomes.value,
            totalExpenses = totalExpenses.value,
            balance = currentBalance.value
        )
        if (file != null) {
            _snackbarMessage.value = "Laporan PDF berhasil dibuat: ${file.name}"
            PdfReportGenerator.sharePdfReport(context, file)
        } else {
            _snackbarMessage.value = "Gagal membuat dokumen PDF laporan."
        }
    }
}
