package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.CashExpense
import com.example.data.model.CashIncome
import com.example.data.model.House
import com.example.data.model.JimpitanRecord
import com.example.data.model.RtConfig
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    data class HouseMonthSummary(
        val house: House,
        val targetAmount: Long,
        val paidAmount: Long,
        val shortageAmount: Long,
        val isLunas: Boolean
    )

    fun generateMonthlyPdfReport(
        context: Context,
        config: RtConfig,
        monthInfo: MonthInfo,
        houses: List<House>,
        records: List<JimpitanRecord>,
        incomes: List<CashIncome>,
        expenses: List<CashExpense>,
        totalJimpitan: Long,
        totalOtherIncome: Long,
        totalExpenses: Long,
        balance: Long
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(13, 110, 79) // Emerald primary
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
                typeface = Typeface.DEFAULT
            }
            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rightAlignPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
                textAlign = Paint.Align.RIGHT
            }

            var y = 25f

            // Top Header Bar
            paint.color = Color.rgb(13, 110, 79)
            canvas.drawRect(0f, 0f, 595f, 8f, paint)

            y += 18f
            canvas.drawText("PENGURUS ${config.rtName.uppercase()}", 297.5f, y, titlePaint)
            y += 15f
            canvas.drawText("LAPORAN KAS & REKAPITULASI JIMPITAN WARGA", 297.5f, y, subTitlePaint)
            y += 13f
            normalPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Periode: ${monthInfo.monthName} ${monthInfo.year} (${monthInfo.daysInMonth} Hari - Tarif: ${Formatters.formatRupiah(config.dailyJimpitanRate)}/hari)", 297.5f, y, normalPaint)
            normalPaint.textAlign = Paint.Align.LEFT

            y += 10f
            // Separator lines
            paint.color = Color.rgb(13, 110, 79)
            paint.strokeWidth = 1.5f
            canvas.drawLine(30f, y, 565f, y, paint)
            y += 3f
            paint.strokeWidth = 0.5f
            canvas.drawLine(30f, y, 565f, y, paint)

            y += 18f

            // Financial Summary Box
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(30f, y, 565f, y + 55f, 6f, 6f, paint)
            paint.color = Color.rgb(203, 213, 225)
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(30f, y, 565f, y + 55f, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Row 1 inside box
            boldPaint.textSize = 9.5f
            canvas.drawText("RINGKASAN KEUANGAN KAS RT:", 42f, y + 16f, boldPaint)

            normalPaint.textSize = 8.5f
            canvas.drawText("Total Pemasukan Jimpitan : ${Formatters.formatRupiah(totalJimpitan)}", 42f, y + 32f, normalPaint)
            canvas.drawText("Total Pemasukan Kas Lain : ${Formatters.formatRupiah(totalOtherIncome)}", 42f, y + 46f, normalPaint)

            canvas.drawText("Total Pengeluaran Kas RT : ${Formatters.formatRupiah(totalExpenses)}", 240f, y + 32f, normalPaint)
            canvas.drawText("Alokasi Warga Sakit/Sosial : ${Formatters.formatRupiah(expenses.filter { it.category.contains("Sakit", true) }.sumOf { it.amount })}", 240f, y + 46f, normalPaint)

            boldPaint.color = Color.rgb(13, 110, 79)
            boldPaint.textSize = 10.5f
            canvas.drawText("SISA SALDO KAS RT:", 430f, y + 26f, boldPaint)
            canvas.drawText(Formatters.formatRupiah(balance), 430f, y + 44f, boldPaint)
            boldPaint.color = Color.rgb(15, 23, 42)

            y += 72f

            // Section 1: Jimpitan Table
            boldPaint.textSize = 10f
            canvas.drawText("A. Rekapitulasi Iuran Jimpitan Warga (${monthInfo.monthName} ${monthInfo.year})", 30f, y, boldPaint)
            y += 10f

            // Table Header
            paint.color = Color.rgb(13, 110, 79)
            canvas.drawRect(30f, y, 565f, y + 16f, paint)

            val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("NO", 36f, y + 11f, headerTextPaint)
            canvas.drawText("RUMAH / BLOK", 58f, y + 11f, headerTextPaint)
            canvas.drawText("KEPALA KELUARGA", 145f, y + 11f, headerTextPaint)
            canvas.drawText("KEWAJIBAN", 280f, y + 11f, headerTextPaint)
            canvas.drawText("TERBAYAR", 355f, y + 11f, headerTextPaint)
            canvas.drawText("KEKURANGAN", 430f, y + 11f, headerTextPaint)
            canvas.drawText("STATUS", 510f, y + 11f, headerTextPaint)

            y += 16f

            // House rows
            val targetPerHouse = monthInfo.daysInMonth * config.dailyJimpitanRate
            val houseRecordsMap = records.groupBy { it.houseId }

            houses.forEachIndexed { index, house ->
                val houseRecords = houseRecordsMap[house.id] ?: emptyList()
                val totalPaid = houseRecords.sumOf { it.amount }
                val shortage = (targetPerHouse - totalPaid).coerceAtLeast(0L)
                val isLunas = shortage == 0L

                // Alternate row color
                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(30f, y, 565f, y + 14f, paint)
                }

                normalPaint.textSize = 8f
                canvas.drawText("${index + 1}", 36f, y + 10f, normalPaint)
                canvas.drawText("${house.block} ${house.houseNumber}", 58f, y + 10f, normalPaint)
                canvas.drawText(house.headOfFamily.take(22), 145f, y + 10f, normalPaint)
                canvas.drawText(Formatters.formatRupiah(targetPerHouse), 280f, y + 10f, normalPaint)
                canvas.drawText(Formatters.formatRupiah(totalPaid), 355f, y + 10f, normalPaint)

                if (isLunas) {
                    canvas.drawText("Rp 0", 430f, y + 10f, normalPaint)
                    boldPaint.color = Color.rgb(22, 163, 74) // Green
                    boldPaint.textSize = 8f
                    canvas.drawText("LUNAS", 510f, y + 10f, boldPaint)
                } else {
                    boldPaint.color = Color.rgb(220, 38, 38) // Red
                    boldPaint.textSize = 8f
                    canvas.drawText(Formatters.formatRupiah(shortage), 430f, y + 10f, boldPaint)
                    canvas.drawText("KURANG", 510f, y + 10f, boldPaint)
                }
                boldPaint.color = Color.rgb(15, 23, 42)

                y += 14f
            }

            y += 12f

            // Section 2: Cash Expenses & Incomes Brief Table (Side-by-side or stacked)
            boldPaint.textSize = 9.5f
            canvas.drawText("B. Rincian Pemasukan Kas Lainnya & Pengeluaran Kas Periode Ini", 30f, y, boldPaint)
            y += 10f

            paint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(30f, y, 565f, y + 14f, paint)
            boldPaint.textSize = 7.5f
            canvas.drawText("TGL", 36f, y + 10f, boldPaint)
            canvas.drawText("KETERANGAN / KEPERLUAN", 90f, y + 10f, boldPaint)
            canvas.drawText("KATEGORI", 310f, y + 10f, boldPaint)
            canvas.drawText("ARUS", 430f, y + 10f, boldPaint)
            canvas.drawText("NOMINAL", 500f, y + 10f, boldPaint)
            y += 14f

            val combinedList = mutableListOf<Triple<String, String, String>>() // Date, Title & Cat, AmountWithSign
            incomes.take(3).forEach {
                combinedList.add(Triple(it.dateString, "${it.title} (${it.category})", "+${Formatters.formatRupiah(it.amount)}"))
            }
            expenses.take(4).forEach {
                combinedList.add(Triple(it.dateString, "${it.title} (${it.category})", "-${Formatters.formatRupiah(it.amount)}"))
            }

            combinedList.forEachIndexed { idx, item ->
                if (idx % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(30f, y, 565f, y + 12f, paint)
                }
                normalPaint.textSize = 7.5f
                canvas.drawText(item.first, 36f, y + 9f, normalPaint)
                canvas.drawText(item.second.take(45), 90f, y + 9f, normalPaint)
                if (item.third.startsWith("+")) {
                    boldPaint.color = Color.rgb(22, 163, 74)
                    canvas.drawText("MASUK", 430f, y + 9f, boldPaint)
                } else {
                    boldPaint.color = Color.rgb(220, 38, 38)
                    canvas.drawText("KELUAR", 430f, y + 9f, boldPaint)
                }
                canvas.drawText(item.third, 500f, y + 9f, boldPaint)
                boldPaint.color = Color.rgb(15, 23, 42)
                y += 12f
            }

            y += 24f

            // Signatures Section
            val signY = y.coerceAtLeast(730f)
            normalPaint.textSize = 8.5f
            boldPaint.textSize = 8.5f

            canvas.drawText("Mengetahui,", 70f, signY, normalPaint)
            canvas.drawText("Ketua ${config.rtName}", 70f, signY + 12f, boldPaint)
            canvas.drawText("( Bpk. Bambang Pamungkas )", 70f, signY + 58f, boldPaint)

            canvas.drawText("Dibuat & Diverifikasi,", 390f, signY, normalPaint)
            canvas.drawText("Bendahara ${config.rtName}", 390f, signY + 12f, boldPaint)
            canvas.drawText("( ${config.treasurerName} )", 390f, signY + 58f, boldPaint)

            // Footer
            paint.color = Color.rgb(148, 163, 184)
            normalPaint.textSize = 7.5f
            normalPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText("Dicetak resmi melalui Aplikasi JimpitanKita pada ${Formatters.formatDate(System.currentTimeMillis())}. Transparan, Amanah, Guyub Rukun.", 30f, 820f, normalPaint)

            pdfDocument.finishPage(page)

            // Write to file
            val reportsDir = File(context.cacheDir, "laporan_pdf")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val fileName = "Laporan_Jimpitan_${config.rtName.replace(" ", "_")}_${monthInfo.monthName}_${monthInfo.year}.pdf"
            val file = File(reportsDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sharePdfReport(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan & Jimpitan ${pdfFile.name}")
                putExtra(Intent.EXTRA_TEXT, "Berikut kami lampirkan Laporan Resmi Keuangan Kas & Rekapitulasi Jimpitan Warga (${pdfFile.name}).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF via WhatsApp / Aplikasi Lain"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
