package com.example.data.repository

import com.example.data.local.JimpitanDao
import com.example.data.model.Announcement
import com.example.data.model.AppNotification
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.RtConfig
import com.example.data.model.SickResidentAid
import com.example.data.firebase.FirebaseSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class JimpitanRepository(private val dao: JimpitanDao) {
    val syncManager: FirebaseSyncManager = FirebaseSyncManager(dao)

    fun startRealtimeSync(scope: CoroutineScope) {
        syncManager.startRealtimeSync(scope)
    }

    // Config
    val rtConfig: Flow<RtConfig?> = dao.getConfig()
    suspend fun getConfigOnce(): RtConfig? = dao.getConfigOnce()
    suspend fun saveConfig(config: RtConfig) {
        dao.insertConfig(config)
        syncManager.uploadConfig(config)
    }
    suspend fun updateConfig(config: RtConfig) {
        dao.updateConfig(config)
        syncManager.uploadConfig(config)
    }

    // Houses
    val allHouses: Flow<List<House>> = dao.getAllHouses()
    suspend fun getHouseById(id: String): House? = dao.getHouseById(id)
    fun getHouseFlow(id: String): Flow<House?> = dao.getHouseFlow(id)
    suspend fun insertHouse(house: House) {
        dao.insertHouse(house)
        syncManager.uploadHouse(house)
    }
    suspend fun updateHouse(house: House) {
        dao.updateHouse(house)
        syncManager.uploadHouse(house)
    }
    suspend fun deleteHouse(houseId: String) {
        // Hanya hapus KK dari daftar warga aktif (warga pindah domisili).
        // Riwayat uang yang pernah disetorkan tetap aman di kas RT dan tidak akan hilang
        // kecuali jika pengurus memilih menu "Bersihkan Semua Data".
        dao.deleteHouse(houseId)
        syncManager.deleteHouseFromCloud(houseId)
    }

    // Jimpitan Records
    val allJimpitanRecords: Flow<List<JimpitanRecord>> = dao.getAllJimpitanRecords()
    val totalJimpitanIncome: Flow<Long> = dao.getTotalJimpitanIncome()

    fun getRecordsForHouse(houseId: String): Flow<List<JimpitanRecord>> = dao.getRecordsForHouse(houseId)
    fun getRecordsForDate(dateString: String): Flow<List<JimpitanRecord>> = dao.getRecordsForDate(dateString)
    fun getTotalJimpitanByHouse(houseId: String): Flow<Long> = dao.getTotalJimpitanByHouse(houseId)

    fun getRecordsForHouseAndMonth(houseId: String, monthKey: String): Flow<List<JimpitanRecord>> =
        dao.getRecordsForHouseAndMonth(houseId, monthKey)

    fun getRecordsForMonth(monthKey: String): Flow<List<JimpitanRecord>> =
        dao.getRecordsForMonth(monthKey)

    suspend fun recordJimpitan(record: JimpitanRecord): Long {
        val id = dao.insertJimpitanRecord(record)
        syncManager.uploadRecord(if (record.id == 0L) record.copy(id = id) else record)
        return id
    }

    // Cash Incomes (Pemasukan Kas Lainnya)
    val allCashIncomes: Flow<List<CashIncome>> = dao.getAllCashIncomes()
    val totalOtherIncomes: Flow<Long> = dao.getTotalOtherIncomes()
    fun getIncomesForMonth(monthPrefix: String): Flow<List<CashIncome>> = dao.getIncomesForMonth(monthPrefix)

    suspend fun addCashIncome(income: CashIncome): Long {
        val id = dao.insertCashIncome(income)
        syncManager.uploadIncome(if (income.id == 0L) income.copy(id = id) else income)
        return id
    }
    suspend fun deleteCashIncome(id: Long) {
        dao.deleteCashIncome(id)
        syncManager.deleteIncomeFromCloud(id)
    }

    // Cash Expenses
    val allExpenses: Flow<List<CashExpense>> = dao.getAllExpenses()
    val totalExpenses: Flow<Long> = dao.getTotalExpenses()
    fun getExpensesForMonth(monthPrefix: String): Flow<List<CashExpense>> = dao.getExpensesForMonth(monthPrefix)

    suspend fun addExpense(expense: CashExpense): Long {
        val id = dao.insertExpense(expense)
        syncManager.uploadExpense(if (expense.id == 0L) expense.copy(id = id) else expense)
        return id
    }
    suspend fun deleteExpense(id: Long) {
        dao.deleteExpense(id)
        syncManager.deleteExpenseFromCloud(id)
    }

    // Announcements
    val allAnnouncements: Flow<List<Announcement>> = dao.getAllAnnouncements()
    suspend fun addAnnouncement(announcement: Announcement): Long {
        val id = dao.insertAnnouncement(announcement)
        syncManager.uploadAnnouncement(if (announcement.id == 0L) announcement.copy(id = id) else announcement)
        return id
    }
    suspend fun cleanupExpiredAnnouncements(cutoffTime: Long) = dao.deleteExpiredAnnouncements(cutoffTime)

    // Bantuan Warga Sakit
    val allSickResidentAids: Flow<List<SickResidentAid>> = dao.getAllSickResidentAids()
    fun getSickAidsForHouse(houseId: String): Flow<List<SickResidentAid>> = dao.getSickAidsForHouse(houseId)
    suspend fun applySickAid(aid: SickResidentAid): Long {
        val id = dao.insertSickResidentAid(aid)
        syncManager.uploadSickAid(if (aid.id == 0L) aid.copy(id = id) else aid)
        return id
    }
    suspend fun updateSickAid(aid: SickResidentAid) {
        dao.updateSickResidentAid(aid)
        syncManager.uploadSickAid(aid)
    }

    // Notifications
    fun getNotificationsForHouse(houseId: String): Flow<List<AppNotification>> = dao.getNotificationsForHouse(houseId)
    suspend fun sendNotification(notification: AppNotification): Long = dao.insertNotification(notification)
    suspend fun markAllNotificationsRead(houseId: String) = dao.markAllAsRead(houseId)
    suspend fun cleanupNotificationsOlderThan(cutoffTime: Long) = dao.deleteNotificationsOlderThan(cutoffTime)
    suspend fun clearAllJimpitanRecords() {
        dao.clearAllJimpitanRecords()
        syncManager.clearCollection("jimpitan_records")
    }
    suspend fun clearAllCashIncomes() {
        dao.clearAllCashIncomes()
        syncManager.clearCollection("cash_incomes")
    }
    suspend fun clearAllCashExpenses() {
        dao.clearAllCashExpenses()
        syncManager.clearCollection("cash_expenses")
    }
    suspend fun clearAllNotifications() = dao.clearAllNotifications()
    suspend fun clearAllDataCompletely() {
        syncManager.clearAllCloudData(dao)
    }
}
