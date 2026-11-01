package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Announcement
import com.example.data.model.AppNotification
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.RtConfig
import com.example.data.model.SickResidentAid
import kotlinx.coroutines.flow.Flow

@Dao
interface JimpitanDao {
    // --- Config ---
    @Query("SELECT * FROM rt_config WHERE id = 'MAIN_CONFIG' LIMIT 1")
    fun getConfig(): Flow<RtConfig?>

    @Query("SELECT * FROM rt_config WHERE id = 'MAIN_CONFIG' LIMIT 1")
    suspend fun getConfigOnce(): RtConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: RtConfig)

    @Update
    suspend fun updateConfig(config: RtConfig)

    // --- Houses ---
    @Query("SELECT * FROM houses ORDER BY block ASC, houseNumber ASC")
    fun getAllHouses(): Flow<List<House>>

    @Query("SELECT * FROM houses ORDER BY block ASC, houseNumber ASC")
    suspend fun getAllHousesOnce(): List<House>

    @Query("SELECT * FROM houses WHERE id = :id LIMIT 1")
    suspend fun getHouseById(id: String): House?

    @Query("SELECT * FROM houses WHERE id = :id LIMIT 1")
    fun getHouseFlow(id: String): Flow<House?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouses(houses: List<House>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouse(house: House)

    @Update
    suspend fun updateHouse(house: House)

    @Query("DELETE FROM houses WHERE id = :id")
    suspend fun deleteHouse(id: String)

    // --- Jimpitan Records ---
    @Query("SELECT * FROM jimpitan_records ORDER BY timestamp DESC")
    fun getAllJimpitanRecords(): Flow<List<JimpitanRecord>>

    @Query("SELECT * FROM jimpitan_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsOnce(): List<JimpitanRecord>

    @Query("SELECT * FROM jimpitan_records WHERE houseId = :houseId ORDER BY timestamp DESC")
    fun getRecordsForHouse(houseId: String): Flow<List<JimpitanRecord>>

    @Query("SELECT * FROM jimpitan_records WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getRecordsForDate(dateString: String): Flow<List<JimpitanRecord>>

    @Query("SELECT * FROM jimpitan_records WHERE houseId = :houseId AND (monthKey = :monthKey OR dateString LIKE :monthKey || '%') ORDER BY timestamp DESC, id DESC")
    fun getRecordsForHouseAndMonth(houseId: String, monthKey: String): Flow<List<JimpitanRecord>>

    @Query("SELECT * FROM jimpitan_records WHERE monthKey = :monthKey OR dateString LIKE :monthKey || '%' ORDER BY timestamp DESC, id DESC")
    fun getRecordsForMonth(monthKey: String): Flow<List<JimpitanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJimpitanRecord(record: JimpitanRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJimpitanRecords(records: List<JimpitanRecord>)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM jimpitan_records")
    fun getTotalJimpitanIncome(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM jimpitan_records WHERE houseId = :houseId")
    fun getTotalJimpitanByHouse(houseId: String): Flow<Long>

    @Query("DELETE FROM jimpitan_records WHERE houseId = :houseId")
    suspend fun deleteRecordsForHouse(houseId: String)

    @Query("DELETE FROM jimpitan_records")
    suspend fun clearAllJimpitanRecords()

    // --- Cash Incomes (Pemasukan Kas Lainnya) ---
    @Query("SELECT * FROM cash_incomes ORDER BY timestamp DESC")
    fun getAllCashIncomes(): Flow<List<CashIncome>>

    @Query("SELECT * FROM cash_incomes ORDER BY timestamp DESC")
    suspend fun getAllIncomesOnce(): List<CashIncome>

    @Query("SELECT * FROM cash_incomes WHERE dateString LIKE :monthPrefix || '%' ORDER BY timestamp DESC")
    fun getIncomesForMonth(monthPrefix: String): Flow<List<CashIncome>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashIncome(income: CashIncome): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashIncomes(incomes: List<CashIncome>)

    @Query("DELETE FROM cash_incomes WHERE id = :id")
    suspend fun deleteCashIncome(id: Long)

    @Query("DELETE FROM cash_incomes")
    suspend fun clearAllCashIncomes()

    @Query("SELECT COALESCE(SUM(amount), 0) FROM cash_incomes")
    fun getTotalOtherIncomes(): Flow<Long>

    // --- Cash Expenses (Pengeluaran Kas RT) ---
    @Query("SELECT * FROM cash_expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<CashExpense>>

    @Query("SELECT * FROM cash_expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesOnce(): List<CashExpense>

    @Query("SELECT * FROM cash_expenses WHERE dateString LIKE :monthPrefix || '%' ORDER BY timestamp DESC")
    fun getExpensesForMonth(monthPrefix: String): Flow<List<CashExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: CashExpense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<CashExpense>)

    @Query("DELETE FROM cash_expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("DELETE FROM cash_expenses")
    suspend fun clearAllCashExpenses()

    @Query("SELECT COALESCE(SUM(amount), 0) FROM cash_expenses")
    fun getTotalExpenses(): Flow<Long>

    // --- Announcements ---
    @Query("SELECT * FROM announcements ORDER BY id DESC")
    fun getAllAnnouncements(): Flow<List<Announcement>>

    @Query("SELECT * FROM announcements ORDER BY id DESC")
    suspend fun getAllAnnouncementsOnce(): List<Announcement>

    @Query("DELETE FROM announcements WHERE timestamp < :cutoffTime")
    suspend fun deleteExpiredAnnouncements(cutoffTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<Announcement>)

    @Query("DELETE FROM announcements")
    suspend fun clearAllAnnouncements()

    // --- Bantuan Warga Sakit ---
    @Query("SELECT * FROM sick_resident_aids ORDER BY timestamp DESC")
    fun getAllSickResidentAids(): Flow<List<SickResidentAid>>

    @Query("SELECT * FROM sick_resident_aids ORDER BY timestamp DESC")
    suspend fun getAllSickAidsOnce(): List<SickResidentAid>

    @Query("SELECT * FROM sick_resident_aids WHERE houseId = :houseId ORDER BY timestamp DESC")
    fun getSickAidsForHouse(houseId: String): Flow<List<SickResidentAid>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSickResidentAid(aid: SickResidentAid): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSickResidentAids(aids: List<SickResidentAid>)

    @Update
    suspend fun updateSickResidentAid(aid: SickResidentAid)

    @Query("DELETE FROM sick_resident_aids")
    suspend fun clearAllSickResidentAids()

    // --- Notifications ---
    @Query("SELECT * FROM app_notifications WHERE houseId = :houseId OR houseId = 'ALL' ORDER BY timestamp DESC")
    fun getNotificationsForHouse(houseId: String): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Query("UPDATE app_notifications SET isRead = 1 WHERE houseId = :houseId OR houseId = 'ALL'")
    suspend fun markAllAsRead(houseId: String)

    @Query("DELETE FROM app_notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteNotificationsOlderThan(cutoffTime: Long)

    @Query("DELETE FROM app_notifications")
    suspend fun clearAllNotifications()
}
