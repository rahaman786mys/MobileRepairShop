package com.mobilerepair.shop.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.mobilerepair.shop.data.model.RepairEntry
import com.mobilerepair.shop.data.model.SparePartPurchase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates professional PDF invoices for customers
 */
object InvoiceGenerator {

    fun generateInvoice(
        context: Context,
        entry: RepairEntry,
        parts: List<SparePartPurchase>
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        // Page info: A4 size approx (595 x 842)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Header - Branding
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.parseColor("#2563EB")
        canvas.drawText("Repair Shop", 40f, 60f, titlePaint)

        paint.textSize = 12f
        paint.color = Color.DKGRAY
        canvas.drawText("Professional Mobile Repair Services", 40f, 80f, paint)
        canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", 450f, 60f, paint)

        // Divider
        paint.strokeWidth = 2f
        canvas.drawLine(40f, 100f, 555f, 100f, paint)

        // Customer Details
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER DETAILS", 40f, 130f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Name: ${entry.customerName}", 40f, 155f, paint)
        canvas.drawText("Mobile: ${entry.customerMobile}", 40f, 175f, paint)
        canvas.drawText("Address: ${entry.customerCity}", 40f, 195f, paint)

        // Device Info
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DEVICE INFORMATION", 40f, 230f, paint)
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Brand: ${entry.deviceBrand}", 40f, 255f, paint)
        canvas.drawText("Model: ${entry.deviceModel}", 40f, 275f, paint)
        canvas.drawText("Issue: ${entry.faultDetected}", 40f, 295f, paint)

        // Table Header for Charges
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DESCRIPTION", 40f, 350f, paint)
        canvas.drawText("AMOUNT", 480f, 350f, paint)
        canvas.drawLine(40f, 360f, 555f, 360f, paint)

        // Charges List
        var yPos = 385f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        // Labor/Repair Charge
        canvas.drawText("Repair Service & Labor", 40f, yPos, paint)
        canvas.drawText("₹${entry.chargeAmount}", 480f, yPos, paint)
        yPos += 25f

        // Parts
        parts.forEach { part ->
            canvas.drawText("Part: ${part.partName}", 40f, yPos, paint)
            // Note: In some cases you might hide cost of parts, but here we show details
            yPos += 25f
        }

        // Summary
        canvas.drawLine(40f, yPos, 555f, yPos, paint)
        yPos += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL PAID", 350f, yPos, paint)
        canvas.drawText("₹${entry.finalAmount}", 480f, yPos, paint)

        yPos += 50f
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("This is a computer generated invoice. No signature required.", 40f, yPos, paint)

        pdfDocument.finishPage(page)

        // Save file
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "Invoice_${entry.id}_${System.currentTimeMillis()}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
