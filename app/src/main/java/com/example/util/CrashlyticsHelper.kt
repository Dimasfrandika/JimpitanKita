package com.example.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Helper terpusat untuk Firebase Crashlytics:
 * Menangani pencatatan galat, log diagnostik, dan identifikasi sesi pengguna.
 */
object CrashlyticsHelper {
    private const val TAG = "CrashlyticsHelper"

    /**
     * Mencatat pesan log ke Firebase Crashlytics.
     * Log ini akan disertakan secara otomatis saat terjadi crash atau pelaporan galat.
     */
    fun log(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log(message)
            Log.d(TAG, message)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mencatat log Crashlytics: ${e.message}")
        }
    }

    /**
     * Merekam exception non-fatal ke Firebase Crashlytics dashboard.
     */
    fun recordException(throwable: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
            Log.e(TAG, "Exception tercatat di Crashlytics: ${throwable.message}", throwable)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal merekam exception Crashlytics: ${e.message}")
        }
    }

    /**
     * Menyimpan custom key-value untuk mempermudah analisis konteks crash di Firebase Console.
     */
    fun setCustomKey(key: String, value: String) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (_: Exception) {}
    }

    /**
     * Menetapkan ID pengguna/KK pada laporan crash.
     */
    fun setUserId(userId: String) {
        try {
            FirebaseCrashlytics.getInstance().setUserId(userId)
        } catch (_: Exception) {}
    }

    /**
     * Memeriksa apakah Firebase Crashlytics aktif dan siap menerima data.
     */
    fun isAvailable(): Boolean {
        return try {
            FirebaseCrashlytics.getInstance()
            true
        } catch (_: Exception) {
            false
        }
    }
}
