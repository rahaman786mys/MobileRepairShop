package com.app.muzzutech.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.app.muzzutech.data.model.RepairEntry
import com.app.muzzutech.data.model.SparePartPurchase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object InvoiceGenerator {

    fun generateInvoice(
        context: Context,
        entry: RepairEntry,
        parts: List<SparePartPurchase>
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 24f
        titlePaint.color = Color.parseColor("#6366F1")
        canvas.drawText("MuZZu Tech", 40f, 60f, titlePaint)

        paint.textSize = 12f
        paint.color = Color.DKGRAY
        canvas.drawText("Mobile Repair Services", 40f, 80f, paint)
        canvas.drawText("Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", 370f, 60f, paint)

        paint.strokeWidth = 2f
        canvas.drawLine(40f, 100f, 555f, 100f, paint)

        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER DETAILS", 40f, 130f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Name: ${entry.customerName.ifEmpty { "-" }}", 40f, 155f, paint)
        canvas.drawText("Mobile: ${entry.customerMobile}", 40f, 175f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DEVICE INFORMATION", 40f, 210f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Brand: ${entry.deviceBrand.ifEmpty { "-" }}", 40f, 235f, paint)
        canvas.drawText("Model: ${entry.deviceModel.ifEmpty { "-" }}", 40f, 255f, paint)
        canvas.drawText("Issue: ${entry.faultDetected.ifEmpty { "-" }}", 40f, 275f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DESCRIPTION", 40f, 330f, paint)
        canvas.drawText("AMOUNT", 480f, 330f, paint)
        canvas.drawLine(40f, 340f, 555f, 340f, paint)

        var yPos = 365f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText("Repair Service & Labor", 40f, yPos, paint)
        canvas.drawText("₹${String.format("%.0f", entry.chargeAmount)}", 480f, yPos, paint)
        yPos += 25f

        parts.forEach { part ->
            canvas.drawText("Part: ${part.partName}", 40f, yPos, paint)
            canvas.drawText("₹${String.format("%.0f", part.purchasePrice * part.quantity)}", 480f, yPos, paint)
            yPos += 25f
        }

        canvas.drawLine(40f, yPos, 555f, yPos, paint)
        yPos += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL PAID", 350f, yPos, paint)
        canvas.drawText("₹${String.format("%.0f", entry.finalAmount)}", 480f, yPos, paint)

        yPos += 50f
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("This is a computer generated invoice. No signature required.", 40f, yPos, paint)

        pdfDocument.finishPage(page)

        val invoicesDir = File(context.filesDir, "invoices")
        if (!invoicesDir.exists()) invoicesDir.mkdirs()
        val file = File(invoicesDir, "Invoice_${entry.id}_${System.currentTimeMillis()}.pdf")

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
