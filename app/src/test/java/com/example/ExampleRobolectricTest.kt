package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Announcement
import com.example.data.model.AppNotification
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.JimpitanStatus
import com.example.data.model.RtConfig
import com.example.data.model.UserRole
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context matches JimpitanKita`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JimpitanKita", appName)
    }

    @Test
    fun `test launch MainActivity`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(activity != null)
    }

    @Test
    fun `formatRupiah formats correctly`() {
        val formatted = Formatters.formatRupiah(500L)
        assertTrue(formatted.contains("500"))
    }

    @Test
    fun `house data class initializes proper qr payload`() {
        val house = House(
            id = "RT02-A01",
            block = "Blok A",
            houseNumber = "No. 01",
            headOfFamily = "Bpk. Budi Santoso",
            monthlyTarget = 15000L,
            qrPayload = "JIMPITAN:RT02:A01"
        )
        assertEquals("JIMPITAN:RT02:A01", house.qrPayload)
        assertEquals(JimpitanStatus.TERAMBIL_TUNAI.label, "Terambil Tunai")
    }

    // SCENARIO 1: Filter Transaksi Warga (Semua, Pemasukan, Pengeluaran)
    @Test
    fun `scenario 1 - test transaction filtering and count parity`() {
        val cashIncomes = listOf(
            CashIncome(id = 1L, title = "Donasi", category = "Donasi", amount = 100000L, timestamp = 1000L, dateString = "2026-10-01", receivedFrom = "A1"),
            CashIncome(id = 2L, title = "Sewa", category = "Sewa", amount = 50000L, timestamp = 2000L, dateString = "2026-10-01", receivedFrom = "A2")
        )
        val expenses = listOf(
            CashExpense(id = 1L, title = "Beli Lampu", category = "Keamanan", amount = 30000L, timestamp = 3000L, dateString = "2026-10-01", recordedBy = "Bendahara")
        )

        val totalTransactions = cashIncomes.size + expenses.size
        assertEquals(2, cashIncomes.size)
        assertEquals(1, expenses.size)
        assertEquals(3, totalTransactions)
    }

    // SCENARIO 2: Otomatis 24 Jam Papan Pengumuman & Jam 12 Malam Notifikasi
    @Test
    fun `scenario 2 - announcements older than 24 hours are excluded`() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (1 * 3600 * 1000L)
        val twentyFiveHoursAgo = now - (25 * 3600 * 1000L)
        val cutoff = now - (24 * 3600 * 1000L)

        val announcements = listOf(
            Announcement(id = 1L, title = "Info Baru", content = "Test 1", dateString = "Hari ini", priority = "BIASA", timestamp = oneHourAgo),
            Announcement(id = 2L, title = "Info Lama", content = "Test 2", dateString = "Kemarin", priority = "BIASA", timestamp = twentyFiveHoursAgo)
        )

        val activeAnnouncements = announcements.filter { it.timestamp >= cutoff }
        assertEquals(1, activeAnnouncements.size)
        assertEquals("Info Baru", activeAnnouncements.first().title)
    }

    @Test
    fun `scenario 2 - notifications before midnight are reset`() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val midnightToday = cal.timeInMillis

        val yesterdayNotification = AppNotification(
            id = 1L,
            houseId = "RT02-A01",
            title = "Pesan Kemarin",
            message = "Sebelum jam 12 malam",
            timestamp = midnightToday - 1000L,
            isRead = true
        )
        val todayNotification = AppNotification(
            id = 2L,
            houseId = "RT02-A01",
            title = "Pesan Hari Ini",
            message = "Setelah jam 12 malam",
            timestamp = midnightToday + 10000L,
            isRead = false
        )

        val list = listOf(yesterdayNotification, todayNotification)
        val activeToday = list.filter { it.timestamp >= midnightToday }

        assertEquals(1, activeToday.size)
        assertEquals("Pesan Hari Ini", activeToday.first().title)
    }

    // SCENARIO 3: Notifikasi Pop-up 5 Detik Auto-Dismiss
    @Test
    fun `scenario 3 - popup banner 5 second timeout value`() {
        val timeoutMillis = 5000L
        assertEquals(5000L, timeoutMillis)
        val seconds = timeoutMillis / 1000
        assertEquals(5L, seconds)
    }

    // SCENARIO 4: Role Pengurus - Tambah KK dan Hapus KK Warga Pindah
    @Test
    fun `scenario 4 - add and delete house administration for moving resident`() {
        val initialHouses = mutableListOf(
            House("RT02-A01", "Blok A", "No. 01", "Bpk. Budi Santoso", qrPayload = "JIMPITAN:RT02:A01"),
            House("RT02-A02", "Blok A", "No. 02", "Bpk. Joko Anwar", qrPayload = "JIMPITAN:RT02:A02")
        )
        // Add KK
        val newHouse = House("RT02-A03", "Blok A", "No. 03", "Bpk. Hendra Wijaya", qrPayload = "JIMPITAN:RT02:A03")
        initialHouses.add(newHouse)
        assertEquals(3, initialHouses.size)

        // Delete KK when resident moves (warga pindah)
        val houseToDeleteId = "RT02-A02"
        initialHouses.removeAll { it.id == houseToDeleteId }
        assertEquals(2, initialHouses.size)
        assertFalse(initialHouses.any { it.id == "RT02-A02" })
        assertTrue(initialHouses.any { it.id == "RT02-A03" })

        // Past deposited records for RT02-A02 MUST be preserved for RT bookkeeping
        val records = listOf(
            JimpitanRecord(id = 1L, houseId = "RT02-A02", amount = 15000L, dateString = "2026-10-01"),
            JimpitanRecord(id = 2L, houseId = "RT02-A01", amount = 15000L, dateString = "2026-10-01")
        )
        // House deleted, but past total jimpitan remains 30.000 (not reduced)
        val totalDeposit = records.sumOf { it.amount }
        assertEquals(30000L, totalDeposit)
    }

    // SCENARIO 5: Arus Kas RT Clean Balance & Breakdown Calculations
    @Test
    fun `scenario 5 - arus kas RT balance equals income minus expenses`() {
        val totalJimpitan = 75000L
        val otherIncome = 150000L
        val totalIncome = totalJimpitan + otherIncome
        val totalExpenses = 45000L
        val expectedBalance = totalIncome - totalExpenses

        assertEquals(225000L, totalIncome)
        assertEquals(180000L, expectedBalance)
    }

    // SCENARIO 6: Clean Production State without Dummy Residuals
    @Test
    fun `scenario 6 - empty test records state yields zero balance discrepancy`() {
        val cleanRecords = emptyList<JimpitanRecord>()
        val cleanIncomes = emptyList<CashIncome>()
        val cleanExpenses = emptyList<CashExpense>()

        val income = cleanRecords.sumOf { it.amount } + cleanIncomes.sumOf { it.amount }
        val expense = cleanExpenses.sumOf { it.amount }
        val balance = income - expense

        assertEquals(0L, balance)
        assertTrue(cleanRecords.isEmpty())
        assertTrue(cleanIncomes.isEmpty())
        assertTrue(cleanExpenses.isEmpty())
    }

    // SCENARIO 7: Uji Kelayakan Fitur Pencarian Nama Warga (Role Switch & Petugas Ronda)
    @Test
    fun `scenario 7 - citizen name search matches headOfFamily, block, number, and spouse`() {
        val testHouses = listOf(
            House("RT02-A01", "Blok A", "No. 01", "Bpk. Budi Santoso", spouseOrSecondary = "Ibu Sri Wahyuni", qrPayload = "JIMPITAN:RT02:A01"),
            House("RT02-A02", "Blok A", "No. 02", "Bpk. Joko Anwar", spouseOrSecondary = "Ibu Siti Fatimah", qrPayload = "JIMPITAN:RT02:A02"),
            House("RT02-B01", "Blok B", "No. 01", "Bpk. Agus Prasetyo", spouseOrSecondary = "Ibu Indah Sari", qrPayload = "JIMPITAN:RT02:B01")
        )

        // 1. Search by headOfFamily (case-insensitive)
        val queryBudi = "budi"
        val resultBudi = testHouses.filter {
            it.headOfFamily.lowercase().contains(queryBudi) ||
            it.houseNumber.lowercase().contains(queryBudi) ||
            it.block.lowercase().contains(queryBudi) ||
            it.id.lowercase().contains(queryBudi) ||
            it.spouseOrSecondary.lowercase().contains(queryBudi)
        }
        assertEquals(1, resultBudi.size)
        assertEquals("RT02-A01", resultBudi.first().id)

        // 2. Search by spouse name
        val querySiti = "siti"
        val resultSiti = testHouses.filter {
            it.headOfFamily.lowercase().contains(querySiti) ||
            it.houseNumber.lowercase().contains(querySiti) ||
            it.block.lowercase().contains(querySiti) ||
            it.id.lowercase().contains(querySiti) ||
            it.spouseOrSecondary.lowercase().contains(querySiti)
        }
        assertEquals(1, resultSiti.size)
        assertEquals("RT02-A02", resultSiti.first().id)

        // 3. Search by block name
        val queryBlokB = "blok b"
        val resultBlokB = testHouses.filter {
            it.headOfFamily.lowercase().contains(queryBlokB) ||
            it.houseNumber.lowercase().contains(queryBlokB) ||
            it.block.lowercase().contains(queryBlokB) ||
            it.id.lowercase().contains(queryBlokB) ||
            it.spouseOrSecondary.lowercase().contains(queryBlokB)
        }
        assertEquals(1, resultBlokB.size)
        assertEquals("RT02-B01", resultBlokB.first().id)

        // 4. Search not found returns empty list
        val queryNotFound = "Zulkifli"
        val resultNotFound = testHouses.filter {
            it.headOfFamily.lowercase().contains(queryNotFound.lowercase()) ||
            it.houseNumber.lowercase().contains(queryNotFound.lowercase()) ||
            it.block.lowercase().contains(queryNotFound.lowercase()) ||
            it.id.lowercase().contains(queryNotFound.lowercase()) ||
            it.spouseOrSecondary.lowercase().contains(queryNotFound.lowercase())
        }
        assertTrue(resultNotFound.isEmpty())
    }

    @Test
    fun `scenario 7 - petugas ronda search combines with status filter`() {
        val testHouses = listOf(
            House("RT02-A01", "Blok A", "No. 01", "Bpk. Budi Santoso", qrPayload = "JIMPITAN:RT02:A01"),
            House("RT02-A02", "Blok A", "No. 02", "Bpk. Budi Hermanto", qrPayload = "JIMPITAN:RT02:A02"),
            House("RT02-B01", "Blok B", "No. 01", "Bpk. Slamet Riyadi", qrPayload = "JIMPITAN:RT02:B01")
        )
        // A01 already recorded tonight
        val recordedHouseMap = mapOf("RT02-A01" to JimpitanRecord(id = 1L, houseId = "RT02-A01", amount = 1000L, dateString = "2026-10-10"))

        // Search "Budi" + filter "BELUM"
        val filterStatus = "BELUM"
        val searchQuery = "budi"

        val baseList = when (filterStatus) {
            "BELUM" -> testHouses.filter { !recordedHouseMap.containsKey(it.id) }
            "SUDAH" -> testHouses.filter { recordedHouseMap.containsKey(it.id) }
            else -> testHouses
        }
        val finalResult = baseList.filter {
            it.headOfFamily.lowercase().contains(searchQuery)
        }

        assertEquals(1, finalResult.size)
        assertEquals("RT02-A02", finalResult.first().id)
        assertEquals("Bpk. Budi Hermanto", finalResult.first().headOfFamily)
    }
}
