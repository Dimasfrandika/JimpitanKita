package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val label: String, val badge: String) {
    WARGA("Warga RT", "Warga"),
    PETUGAS("Petugas Ronda / Lapangan", "Petugas"),
    BENDAHARA("Bendahara / Pengurus RT", "Pengurus")
}

@Entity(tableName = "rt_config")
data class RtConfig(
    @PrimaryKey val id: String = "MAIN_CONFIG",
    val dailyJimpitanRate: Long = 500L, // Default Rp 500/hari
    val rtName: String = "RT 02 RW 03",
    val neighborhoodName: String = "Desa Kesugihan Muktisari",
    val treasurerUsername: String = "bendahara",
    val treasurerPassword: String = "rt0203",
    val treasurerName: String = "Bpk Akmal (Bendahara RT)",
    val officerUsername: String = "petugas",
    val officerPassword: String = "ronda02",
    val standardSickAidAmount: Long = 200000L // Bantuan yang Diberikan resmi oleh Pengurus RT
)

@Entity(tableName = "houses")
data class House(
    @PrimaryKey val id: String, // e.g. "RT02-A01"
    val block: String,          // e.g. "Blok A"
    val houseNumber: String,    // e.g. "No. 01"
    val headOfFamily: String,   // e.g. "Bpk. Budi Santoso"
    val spouseOrSecondary: String = "", // e.g. "Ibu Siti Aminah"
    val phoneNumber: String = "081234567890",
    val pin: String = "1234",
    val familyMembersCount: Int = 4,
    val monthlyTarget: Long = 15000L, // Default 30 hari x Rp 500 = Rp 15.000
    val qrPayload: String = "JIMPITAN:RT02:A01"
)

enum class JimpitanStatus(val label: String, val shortDesc: String) {
    TERAMBIL_TUNAI("Terambil Tunai", "Uang koin/kertas di kaleng jimpitan"),
    KOSONG_ABSEN("Kosong / Absen", "Warga tidak meletakkan jimpitan"),
    LUNAS_TRANSFER("Transfer Bank", "Iuran dibayar transfer bulanan"),
    QRIS_DIGITAL("QRIS Non-Tunai", "Dibayar digital langsung ke kas RT")
}

@Entity(tableName = "jimpitan_records")
data class JimpitanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val houseId: String,
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // "YYYY-MM-DD"
    val status: JimpitanStatus = JimpitanStatus.TERAMBIL_TUNAI,
    val officerName: String = "Petugas Ronda",
    val notes: String = "",
    val billingPeriodDays: Int = 1, // 1 (harian) atau 30/31 (bulanan)
    val periodType: String = "HARIAN", // "HARIAN" atau "BULANAN"
    val monthKey: String = "" // "YYYY-MM"
)

@Entity(tableName = "cash_incomes")
data class CashIncome(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Donasi Warga", "Sewa Tratag", "Bantuan Pemerintah/Kelurahan", "Swadaya Kegiatan", "Lainnya"
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val receivedFrom: String = "Warga RT 02",
    val receiptNote: String = ""
)

@Entity(tableName = "cash_expenses")
data class CashExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Keamanan & Ronda", "Kebersihan", "Infrastruktur", "Santunan Warga Sakit", "Konsumsi", "Lainnya"
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val recordedBy: String = "Bendahara RT 02",
    val receiptNote: String = ""
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val dateString: String,
    val author: String = "Ketua RT 02 RW 03",
    val priority: String = "NORMAL", // "NORMAL", "PENTING", "DARURAT"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sick_resident_aids")
data class SickResidentAid(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val houseId: String,
    val applicantName: String,
    val patientName: String,
    val diagnosisOrCondition: String,
    val amount: Long,
    val hospitalOrNeeds: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "MENUNGGU" // "MENUNGGU", "DISETUJUI", "DICAIRKAN", "DITOLAK"
)

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val houseId: String, // or "ALL"
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
