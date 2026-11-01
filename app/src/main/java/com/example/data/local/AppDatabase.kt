package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Announcement
import com.example.data.model.AppNotification
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.JimpitanStatus
import com.example.data.model.RtConfig
import com.example.data.model.SickResidentAid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        RtConfig::class,
        House::class,
        JimpitanRecord::class,
        CashIncome::class,
        CashExpense::class,
        Announcement::class,
        SickResidentAid::class,
        AppNotification::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jimpitanDao(): JimpitanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jimpitan_kita_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.let { populateInitialData(it.jimpitanDao()) }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialData(dao: JimpitanDao) {
            // RT 02 RW 03 Configuration: Rp 500 per hari default
            val defaultConfig = RtConfig(
                id = "MAIN_CONFIG",
                dailyJimpitanRate = 500L,
                rtName = "RT 02 RW 03",
                neighborhoodName = "Desa Kesugihan Muktisari",
                treasurerUsername = "bendahara",
                treasurerPassword = "rt0203",
                treasurerName = "Bpk Akmal (Bendahara RT)",
                officerUsername = "petugas",
                officerPassword = "ronda02",
                standardSickAidAmount = 100000L
            )
            dao.insertConfig(defaultConfig)

            // Monthly target = 30 hari x Rp 500 = Rp 15.000
            val houses = listOf(
                House("RT02-A01", "Blok A", "No. 01", "Bpk. Budi Santoso", "Ibu Sri Wahyuni", "081234567801", "1234", 4, 15000L, "JIMPITAN:RT02:A01"),
                House("RT02-A02", "Blok A", "No. 02", "Bpk. Joko Anwar", "Ibu Siti Fatimah", "081234567802", "1234", 3, 15000L, "JIMPITAN:RT02:A02"),
                House("RT02-A03", "Blok A", "No. 03", "Bpk. Hendra Wijaya", "Ibu Maya Pertiwi", "081234567803", "1234", 5, 15000L, "JIMPITAN:RT02:A03"),
                House("RT02-A04", "Blok A", "No. 04", "Bpk. Bambang Pamungkas", "Ibu Dewi Lestari", "081234567804", "1234", 4, 15000L, "JIMPITAN:RT02:A04"),
                House("RT02-A05", "Blok A", "No. 05", "Bpk. Ahmad Fauzi", "Ibu Rina Marlina", "081234567805", "1234", 2, 15000L, "JIMPITAN:RT02:A05"),
                House("RT02-A06", "Blok A", "No. 06", "Bpk. Slamet Riyadi", "Ibu Nurul Aini", "081234567806", "1234", 4, 15000L, "JIMPITAN:RT02:A06"),
                House("RT02-B01", "Blok B", "No. 01", "Bpk. Agus Prasetyo", "Ibu Indah Sari", "081234567807", "1234", 3, 15000L, "JIMPITAN:RT02:B01"),
                House("RT02-B02", "Blok B", "No. 02", "Bpk. Deni Kurniawan", "Ibu Ratna Susanti", "081234567808", "1234", 4, 15000L, "JIMPITAN:RT02:B02"),
                House("RT02-B03", "Blok B", "No. 03", "Bpk. Eko Supriyanto", "Ibu Anisa Rahma", "081234567809", "1234", 3, 15000L, "JIMPITAN:RT02:B03"),
                House("RT02-B04", "Blok B", "No. 04", "Bpk. Yudi Gunawan", "Ibu Tri Handayani", "081234567810", "1234", 5, 15000L, "JIMPITAN:RT02:B04"),
                House("RT02-B05", "Blok B", "No. 05", "Bpk. Arif Wibowo", "Ibu Fitriana", "081234567811", "1234", 4, 15000L, "JIMPITAN:RT02:B05")
            )
            dao.insertHouses(houses)

            // Initial database starts clean with registered RT Houses and Official Config.
            // All transactions, incomes, expenses, and records are recorded in real-time.
            val now = System.currentTimeMillis()
            val welcomeNotification = AppNotification(
                houseId = "ALL",
                title = "JimpitanKita RT 02 RW 03 Aktif",
                message = "Aplikasi resmi Jimpitan & Kas RT siap digunakan secara riil. Transparansi dan kemudahan dalam genggaman warga.",
                timestamp = now,
                isRead = false
            )
            dao.insertNotification(welcomeNotification)
        }
    }
}
