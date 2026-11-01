package com.example.data.firebase

import android.util.Log
import com.example.data.local.JimpitanDao
import com.example.data.model.Announcement
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.JimpitanStatus
import com.example.data.model.RtConfig
import com.example.data.model.SickResidentAid
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manajer Sinkronisasi Dua Arah Real-time antara Room Database lokal (AppDatabase.kt)
 * dan Cloud Firebase Firestore.
 *
 * Pola Offline-First:
 * 1. Setiap input disimpan seketika di Room DB (0ms, 100% responsif).
 * 2. Data otomatis terunggah ke Firebase Firestore saat online.
 * 3. Perubahan dari perangkat warga/pengurus lain diterima secara real-time via SnapshotListener
 *    dan otomatis disinkronkan ke Room DB lokal.
 */
class FirebaseSyncManager(private val dao: JimpitanDao) {

    private val tag = "FirebaseSync"
    private var firestore: FirebaseFirestore? = null

    @Volatile
    private var isClearingCloud = false

    private val _isCloudActive = MutableStateFlow(false)
    val isCloudActive: StateFlow<Boolean> = _isCloudActive.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Menghubungkan ke Firebase...")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
            firestore = db
            _isCloudActive.value = true
            _syncStatusMessage.value = "Terhubung Real-time dengan Firebase Firestore"
            Log.i(tag, "Firebase Firestore initialized successfully with offline persistence.")
        } catch (e: Exception) {
            _isCloudActive.value = false
            _syncStatusMessage.value = "Mode Lokal Aktif (Offline-First)"
            Log.w(tag, "Firestore init notice: Running with local persistence. ${e.message}")
        }
    }

    /**
     * Memulai sinkronisasi real-time dua arah.
     * Menerima pembaruan data dari Firestore dan menyimpannya ke database lokal Room.
     */
    fun startRealtimeSync(scope: CoroutineScope) {
        val db = firestore ?: return

        scope.launch(Dispatchers.IO) {
            try {
                // 1. Sinkronisasi RT Config
                val configReg = db.collection("rt_configs").document("MAIN_CONFIG")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(tag, "Config listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val data = snapshot.data ?: return@launch
                                    val config = RtConfig(
                                        id = "MAIN_CONFIG",
                                        dailyJimpitanRate = (data["dailyJimpitanRate"] as? Number)?.toLong() ?: 500L,
                                        rtName = data["rtName"] as? String ?: "RT 02 RW 03",
                                        neighborhoodName = data["neighborhoodName"] as? String ?: "Desa Kesugihan Muktisari",
                                        treasurerUsername = data["treasurerUsername"] as? String ?: "bendahara",
                                        treasurerPassword = data["treasurerPassword"] as? String ?: "rt0203",
                                        treasurerName = data["treasurerName"] as? String ?: "Bpk Akmal (Bendahara RT)",
                                        officerUsername = data["officerUsername"] as? String ?: "petugas",
                                        officerPassword = data["officerPassword"] as? String ?: "ronda02",
                                        standardSickAidAmount = (data["standardSickAidAmount"] as? Number)?.toLong() ?: 200000L
                                    )
                                    dao.insertConfig(config)
                                    Log.d(tag, "RT Config synced from Firebase")
                                } catch (e: Exception) {
                                    Log.w(tag, "Error parsing cloud config: ${e.message}")
                                }
                            }
                        }
                    }
                listeners.add(configReg)

                // 2. Sinkronisasi Data Warga (Houses)
                val houseReg = db.collection("houses")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(tag, "Houses listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    // 1. Hapus KK lokal jika dokumen dihapus dari Firebase
                                    for (change in snapshot.documentChanges) {
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                            dao.deleteHouse(change.document.id)
                                            Log.d(tag, "Warga/KK ${change.document.id} otomatis terhapus dari Room DB lokal (sinkronisasi Cloud)")
                                        }
                                    }

                                    // 2. Sinkronkan KK aktif dari Firebase ke database lokal
                                    val houses = snapshot.documents.mapNotNull { doc ->
                                        val d = doc.data ?: return@mapNotNull null
                                        House(
                                            id = doc.id,
                                            block = d["block"] as? String ?: "",
                                            houseNumber = d["houseNumber"] as? String ?: "",
                                            headOfFamily = d["headOfFamily"] as? String ?: "",
                                            spouseOrSecondary = d["spouseOrSecondary"] as? String ?: "",
                                            phoneNumber = d["phoneNumber"] as? String ?: "",
                                            pin = d["pin"] as? String ?: "1234",
                                            familyMembersCount = (d["familyMembersCount"] as? Number)?.toInt() ?: 4,
                                            monthlyTarget = (d["monthlyTarget"] as? Number)?.toLong() ?: 15000L,
                                            qrPayload = d["qrPayload"] as? String ?: "JIMPITAN:RT02:${doc.id}"
                                        )
                                    }
                                    if (houses.isNotEmpty()) {
                                        dao.insertHouses(houses)
                                        Log.d(tag, "Synced ${houses.size} houses from Firebase")
                                    }
                                } catch (e: Exception) {
                                    Log.w(tag, "Error syncing houses: ${e.message}")
                                }
                            }
                        }
                    }
                listeners.add(houseReg)

                // 3. Sinkronisasi Catatan Jimpitan (Jimpitan Records)
                val recordsReg = db.collection("jimpitan_records")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(tag, "Records listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (isClearingCloud) return@addSnapshotListener
                        if (snapshot != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    if (snapshot.isEmpty) {
                                        dao.clearAllJimpitanRecords()
                                        Log.d(tag, "Jimpitan records in cloud is empty, cleared local records")
                                    } else {
                                        val records = snapshot.documents.mapNotNull { doc ->
                                            val d = doc.data ?: return@mapNotNull null
                                            val statusStr = d["status"] as? String ?: JimpitanStatus.TERAMBIL_TUNAI.name
                                            val status = try { JimpitanStatus.valueOf(statusStr) } catch (_: Exception) { JimpitanStatus.TERAMBIL_TUNAI }
                                            JimpitanRecord(
                                                id = (d["id"] as? Number)?.toLong() ?: doc.id.hashCode().toLong(),
                                                houseId = d["houseId"] as? String ?: "",
                                                amount = (d["amount"] as? Number)?.toLong() ?: 0L,
                                                timestamp = (d["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                dateString = d["dateString"] as? String ?: "",
                                                status = status,
                                                officerName = d["officerName"] as? String ?: "Petugas Ronda",
                                                notes = d["notes"] as? String ?: "",
                                                billingPeriodDays = (d["billingPeriodDays"] as? Number)?.toInt() ?: 1,
                                                periodType = d["periodType"] as? String ?: "HARIAN",
                                                monthKey = d["monthKey"] as? String ?: ""
                                            )
                                        }
                                        if (records.isNotEmpty()) {
                                            dao.clearAllJimpitanRecords()
                                            dao.insertJimpitanRecords(records)
                                            Log.d(tag, "Synced ${records.size} records from Firebase")
                                        } else {
                                            dao.clearAllJimpitanRecords()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(tag, "Error syncing records: ${e.message}")
                                }
                            }
                        }
                    }
                listeners.add(recordsReg)

                // 4. Sinkronisasi Pemasukan Kas Lainnya (Cash Incomes)
                val incomesReg = db.collection("cash_incomes")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (isClearingCloud) return@addSnapshotListener
                        if (snapshot != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    if (snapshot.isEmpty) {
                                        dao.clearAllCashIncomes()
                                    } else {
                                        val incomes = snapshot.documents.mapNotNull { doc ->
                                            val d = doc.data ?: return@mapNotNull null
                                            CashIncome(
                                                id = (d["id"] as? Number)?.toLong() ?: doc.id.hashCode().toLong(),
                                                title = d["title"] as? String ?: "",
                                                category = d["category"] as? String ?: "",
                                                amount = (d["amount"] as? Number)?.toLong() ?: 0L,
                                                timestamp = (d["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                dateString = d["dateString"] as? String ?: "",
                                                receivedFrom = d["receivedFrom"] as? String ?: "",
                                                receiptNote = d["receiptNote"] as? String ?: ""
                                            )
                                        }
                                        if (incomes.isNotEmpty()) {
                                            dao.clearAllCashIncomes()
                                            dao.insertCashIncomes(incomes)
                                        } else {
                                            dao.clearAllCashIncomes()
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                listeners.add(incomesReg)

                // 5. Sinkronisasi Pengeluaran Kas RT (Cash Expenses)
                val expensesReg = db.collection("cash_expenses")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (isClearingCloud) return@addSnapshotListener
                        if (snapshot != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    if (snapshot.isEmpty) {
                                        dao.clearAllCashExpenses()
                                    } else {
                                        val expenses = snapshot.documents.mapNotNull { doc ->
                                            val d = doc.data ?: return@mapNotNull null
                                            CashExpense(
                                                id = (d["id"] as? Number)?.toLong() ?: doc.id.hashCode().toLong(),
                                                title = d["title"] as? String ?: "",
                                                category = d["category"] as? String ?: "",
                                                amount = (d["amount"] as? Number)?.toLong() ?: 0L,
                                                timestamp = (d["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                dateString = d["dateString"] as? String ?: "",
                                                recordedBy = d["recordedBy"] as? String ?: "",
                                                receiptNote = d["receiptNote"] as? String ?: ""
                                            )
                                        }
                                        if (expenses.isNotEmpty()) {
                                            dao.clearAllCashExpenses()
                                            dao.insertExpenses(expenses)
                                        } else {
                                            dao.clearAllCashExpenses()
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                listeners.add(expensesReg)

                // 6. Sinkronisasi Pengumuman Warga
                val annReg = db.collection("announcements")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (isClearingCloud) return@addSnapshotListener
                        if (snapshot != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    if (snapshot.isEmpty) {
                                        dao.clearAllAnnouncements()
                                    } else {
                                        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                                        val anns = snapshot.documents.mapNotNull { doc ->
                                            val d = doc.data ?: return@mapNotNull null
                                            val ts = (d["timestamp"] as? Number)?.toLong() ?: 0L
                                            if (ts < cutoff) return@mapNotNull null
                                            Announcement(
                                                id = (d["id"] as? Number)?.toLong() ?: doc.id.hashCode().toLong(),
                                                title = d["title"] as? String ?: "",
                                                content = d["content"] as? String ?: "",
                                                dateString = d["dateString"] as? String ?: "",
                                                priority = d["priority"] as? String ?: "BIASA",
                                                timestamp = ts
                                            )
                                        }
                                        if (anns.isNotEmpty()) {
                                            dao.clearAllAnnouncements()
                                            dao.insertAnnouncements(anns)
                                        } else {
                                            dao.clearAllAnnouncements()
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                listeners.add(annReg)

                // 7. Sinkronisasi Bantuan Warga Sakit
                val sickAidReg = db.collection("sick_resident_aids")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (isClearingCloud) return@addSnapshotListener
                        if (snapshot != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    if (snapshot.isEmpty) {
                                        dao.clearAllSickResidentAids()
                                    } else {
                                        val aids = snapshot.documents.mapNotNull { doc ->
                                            val d = doc.data ?: return@mapNotNull null
                                            SickResidentAid(
                                                id = (d["id"] as? Number)?.toLong() ?: doc.id.hashCode().toLong(),
                                                houseId = d["houseId"] as? String ?: "",
                                                applicantName = d["applicantName"] as? String ?: "",
                                                patientName = d["patientName"] as? String ?: "",
                                                diagnosisOrCondition = d["diagnosisOrCondition"] as? String ?: "",
                                                amount = (d["amount"] as? Number)?.toLong() ?: 0L,
                                                hospitalOrNeeds = d["hospitalOrNeeds"] as? String ?: "",
                                                timestamp = (d["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                                status = d["status"] as? String ?: "MENUNGGU"
                                            )
                                        }
                                        if (aids.isNotEmpty()) {
                                            dao.clearAllSickResidentAids()
                                            dao.insertSickResidentAids(aids)
                                        } else {
                                            dao.clearAllSickResidentAids()
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                listeners.add(sickAidReg)

                // Initial Seed: Kirim konfigurasi & KK lokal jika di Firebase masih kosong
                checkAndSeedCloudIfEmpty(db)

            } catch (e: Exception) {
                Log.w(tag, "Start sync exception: ${e.message}")
            }
        }
    }

    private suspend fun checkAndSeedCloudIfEmpty(db: FirebaseFirestore) {
        try {
            val configSnapshot = db.collection("rt_configs").document("MAIN_CONFIG").get().result
            if (configSnapshot == null || !configSnapshot.exists()) {
                val localConfig = dao.getConfigOnce()
                if (localConfig != null) {
                    uploadConfig(localConfig)
                }
                val localHouses = dao.getAllHousesOnce()
                for (house in localHouses) {
                    uploadHouse(house)
                }
                Log.i(tag, "Initial cloud seed complete: RT Config and ${localHouses.size} houses synced to Firestore.")
            }
        } catch (e: Exception) {
            Log.d(tag, "Cloud seed check (offline or rules restricted): ${e.message}")
        }
    }

    // --- Outbound Cloud Upload Methods (Dipanggil otomatis saat input data) ---

    fun uploadConfig(config: RtConfig) {
        val db = firestore ?: return
        val map = hashMapOf(
            "dailyJimpitanRate" to config.dailyJimpitanRate,
            "rtName" to config.rtName,
            "neighborhoodName" to config.neighborhoodName,
            "treasurerUsername" to config.treasurerUsername,
            "treasurerPassword" to config.treasurerPassword,
            "treasurerName" to config.treasurerName,
            "officerUsername" to config.officerUsername,
            "officerPassword" to config.officerPassword,
            "standardSickAidAmount" to config.standardSickAidAmount
        )
        db.collection("rt_configs").document("MAIN_CONFIG")
            .set(map, SetOptions.merge())
            .addOnFailureListener { Log.w(tag, "Failed upload config to Firestore: ${it.message}") }
    }

    fun uploadHouse(house: House) {
        val db = firestore ?: return
        val map = hashMapOf(
            "id" to house.id,
            "block" to house.block,
            "houseNumber" to house.houseNumber,
            "headOfFamily" to house.headOfFamily,
            "spouseOrSecondary" to house.spouseOrSecondary,
            "phoneNumber" to house.phoneNumber,
            "pin" to house.pin,
            "familyMembersCount" to house.familyMembersCount,
            "monthlyTarget" to house.monthlyTarget,
            "qrPayload" to house.qrPayload
        )
        db.collection("houses").document(house.id)
            .set(map, SetOptions.merge())
            .addOnSuccessListener { Log.i(tag, "Berhasil sync warga ${house.id} (${house.headOfFamily}) ke Firestore") }
            .addOnFailureListener { Log.w(tag, "Failed upload house ${house.id}: ${it.message}") }
    }

    fun deleteHouseFromCloud(houseId: String) {
        val db = firestore ?: return
        db.collection("houses").document(houseId).delete()
            .addOnSuccessListener { Log.i(tag, "Berhasil hapus warga $houseId dari Firestore") }
            .addOnFailureListener { Log.w(tag, "Failed delete house $houseId from cloud: ${it.message}") }
    }

    fun uploadRecord(record: JimpitanRecord) {
        val db = firestore ?: return
        val docId = if (record.id > 0) "rec_${record.id}" else "rec_${record.houseId}_${record.timestamp}"
        val map = hashMapOf(
            "id" to record.id,
            "houseId" to record.houseId,
            "amount" to record.amount,
            "timestamp" to record.timestamp,
            "dateString" to record.dateString,
            "status" to record.status.name,
            "officerName" to record.officerName,
            "notes" to record.notes,
            "billingPeriodDays" to record.billingPeriodDays,
            "periodType" to record.periodType,
            "monthKey" to record.monthKey
        )
        db.collection("jimpitan_records").document(docId)
            .set(map, SetOptions.merge())
            .addOnFailureListener { Log.w(tag, "Failed upload jimpitan record: ${it.message}") }
    }

    fun uploadIncome(income: CashIncome) {
        val db = firestore ?: return
        val docId = if (income.id > 0) "inc_${income.id}" else "inc_${income.timestamp}"
        val map = hashMapOf(
            "id" to income.id,
            "title" to income.title,
            "category" to income.category,
            "amount" to income.amount,
            "timestamp" to income.timestamp,
            "dateString" to income.dateString,
            "receivedFrom" to income.receivedFrom,
            "receiptNote" to income.receiptNote
        )
        db.collection("cash_incomes").document(docId)
            .set(map, SetOptions.merge())
            .addOnFailureListener { Log.w(tag, "Failed upload income: ${it.message}") }
    }

    fun uploadExpense(expense: CashExpense) {
        val db = firestore ?: return
        val docId = if (expense.id > 0) "exp_${expense.id}" else "exp_${expense.timestamp}"
        val map = hashMapOf(
            "id" to expense.id,
            "title" to expense.title,
            "category" to expense.category,
            "amount" to expense.amount,
            "timestamp" to expense.timestamp,
            "dateString" to expense.dateString,
            "recordedBy" to expense.recordedBy,
            "receiptNote" to expense.receiptNote
        )
        db.collection("cash_expenses").document(docId)
            .set(map, SetOptions.merge())
            .addOnFailureListener { Log.w(tag, "Failed upload expense: ${it.message}") }
    }

    fun uploadAnnouncement(ann: Announcement) {
        val db = firestore ?: return
        val docId = if (ann.id > 0) "ann_${ann.id}" else "ann_${ann.timestamp}"
        val map = hashMapOf(
            "id" to ann.id,
            "title" to ann.title,
            "content" to ann.content,
            "dateString" to ann.dateString,
            "priority" to ann.priority,
            "timestamp" to ann.timestamp
        )
        db.collection("announcements").document(docId)
            .set(map, SetOptions.merge())
            .addOnFailureListener { Log.w(tag, "Failed upload announcement: ${it.message}") }
    }

    fun uploadSickAid(aid: SickResidentAid) {
        val db = firestore ?: return
        val docId = if (aid.id > 0) "aid_${aid.id}" else "aid_${aid.timestamp}"
        val map = hashMapOf(
            "id" to aid.id,
            "houseId" to aid.houseId,
            "applicantName" to aid.applicantName,
            "patientName" to aid.patientName,
            "diagnosisOrCondition" to aid.diagnosisOrCondition,
            "amount" to aid.amount,
            "hospitalOrNeeds" to aid.hospitalOrNeeds,
            "status" to aid.status,
            "timestamp" to aid.timestamp
        )
        db.collection("sick_resident_aids").document(docId)
            .set(map, SetOptions.merge())
            .addOnFailureListener { Log.w(tag, "Failed upload sick aid: ${it.message}") }
    }

    fun deleteIncomeFromCloud(id: Long) {
        val db = firestore ?: return
        db.collection("cash_incomes").document("inc_$id").delete()
            .addOnFailureListener { Log.w(tag, "Failed delete income $id from cloud: ${it.message}") }
    }

    fun deleteExpenseFromCloud(id: Long) {
        val db = firestore ?: return
        db.collection("cash_expenses").document("exp_$id").delete()
            .addOnFailureListener { Log.w(tag, "Failed delete expense $id from cloud: ${it.message}") }
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T? =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                cont.resume(result)
            }.addOnFailureListener {
                cont.resume(null)
            }
        }

    suspend fun clearAllCloudData(dao: JimpitanDao) {
        isClearingCloud = true
        try {
            // 1. Bersihkan database lokal Room seketika (0ms, langsung Rp 0 di UI)
            dao.clearAllJimpitanRecords()
            dao.clearAllCashIncomes()
            dao.clearAllCashExpenses()
            dao.clearAllNotifications()
            dao.clearAllAnnouncements()
            dao.clearAllSickResidentAids()
            Log.i(tag, "Local Room database cleared to 0")

            // 2. Bersihkan Cloud Firestore secara atomic batch per koleksi
            val db = firestore
            if (db != null) {
                val collections = listOf("jimpitan_records", "cash_incomes", "cash_expenses", "announcements", "sick_resident_aids")
                for (col in collections) {
                    try {
                        val snapshot = awaitTask(db.collection(col).get())
                        if (snapshot != null && !snapshot.isEmpty) {
                            val batch = db.batch()
                            for (doc in snapshot.documents) {
                                batch.delete(doc.reference)
                            }
                            awaitTask(batch.commit())
                            Log.i(tag, "Firestore collection $col cleared (${snapshot.size()} docs)")
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to clear Firestore collection $col: ${e.message}")
                    }
                }
            }

            // 3. Pastikan kembali database lokal tetap bersih mulai dari 0
            dao.clearAllJimpitanRecords()
            dao.clearAllCashIncomes()
            dao.clearAllCashExpenses()
            dao.clearAllNotifications()
            dao.clearAllAnnouncements()
            dao.clearAllSickResidentAids()
        } finally {
            kotlinx.coroutines.delay(600L) // Jeda stabilisasi listener
            isClearingCloud = false
        }
    }

    fun clearCollection(collectionName: String) {
        val db = firestore ?: return
        db.collection(collectionName).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) return@addOnSuccessListener
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit()
        }.addOnFailureListener { Log.w(tag, "Failed to clear collection $collectionName: ${it.message}") }
    }

    fun clearAllDataFromCloud() {
        val db = firestore ?: return
        val collections = listOf("jimpitan_records", "cash_incomes", "cash_expenses", "announcements", "sick_resident_aids")
        for (col in collections) {
            clearCollection(col)
        }
    }

    fun stopSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
