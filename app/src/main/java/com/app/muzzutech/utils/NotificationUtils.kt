package com.app.muzzutech.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.app.muzzutech.data.model.RepairEntry
import java.net.URLEncoder

/**
 * Utility to send WhatsApp notifications to customers
 */
object NotificationUtils {

    /**
     * Send repair start notification with device details and quoted amount
     */
    fun sendRepairStartedWhatsApp(context: Context, entry: RepairEntry) {
        val message = """
            🛠️ *Repair Started - Repair Shop*
            
            Hello ${entry.customerName},
            Your device *${entry.deviceBrand} ${entry.deviceModel}* repair has started.
            
            📍 *Issue:* ${entry.faultDetected}
            💰 *Estimated Charge:* ₹${entry.chargeAmount}
            
            Please wait while we work on your device. We will update you here as soon as it's completed!
            
            🙏 *Thank you for choosing us!*
        """.trimIndent()
        
        sendWhatsApp(context, entry.customerMobile, message)
    }

    /**
     * Send repair completed notification
     */
    fun sendRepairCompletedWhatsApp(context: Context, entry: RepairEntry) {
        val message = """
            ✅ *Repair Completed - Repair Shop*
            
            Hello ${entry.customerName},
            Great news! Your *${entry.deviceBrand} ${entry.deviceModel}* is ready for collection.
            
            💰 *Final Amount:* ₹${if (entry.finalAmount > 0) entry.finalAmount else entry.chargeAmount}
            
            Please visit our shop to collect your device.
        """.trimIndent()
        
        sendWhatsApp(context, entry.customerMobile, message)
    }

    /**
     * Send handover summary with digital receipt link/info
     */
    fun sendHandoverSummaryWhatsApp(context: Context, entry: RepairEntry, issuesFixed: String) {
        val message = """
            🤝 *Device Collected - Repair Shop*
            
            Hello ${entry.customerName},
            Thank you for collecting your *${entry.deviceBrand} ${entry.deviceModel}*.
            
            📑 *Issues Fixed:* $issuesFixed
            💵 *Amount Received:* ₹${entry.finalAmount} (${entry.paymentMode})
            
            We've attached your digital invoice below.
            
            🌟 *Please rate our service!*
        """.trimIndent()
        
        sendWhatsApp(context, entry.customerMobile, message)
    }

    private fun sendWhatsApp(context: Context, mobile: String, message: String) {
        val cleanMobile = mobile.replace("+", "").replace(" ", "")
        val formattedMobile = if (cleanMobile.length == 10) "91$cleanMobile" else cleanMobile
        
        try {
            val packageManager = context.packageManager
            val i = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$formattedMobile&text=" + URLEncoder.encode(message, "UTF-8")
            i.setPackage("com.whatsapp")
            i.data = Uri.parse(url)
            if (i.resolveActivity(packageManager) != null) {
                context.startActivity(i)
            } else {
                // Fallback to browser if WhatsApp not installed
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
