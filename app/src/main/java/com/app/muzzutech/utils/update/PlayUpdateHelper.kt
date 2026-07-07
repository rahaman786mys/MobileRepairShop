package com.app.muzzutech.utils.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

object PlayUpdateHelper {

    private const val TAG = "PlayUpdateHelper"
    const val REQUEST_CODE_IMMEDIATE_UPDATE = 7702

    fun tryImmediateUpdate(activity: Activity, onFallback: () -> Unit) {
        try {
            val manager = AppUpdateManagerFactory.create(activity)
            manager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    val started = manager.startUpdateFlowForResult(
                        info, AppUpdateType.IMMEDIATE,
                        activity, REQUEST_CODE_IMMEDIATE_UPDATE
                    )
                    if (!started) onFallback()
                } else {
                    onFallback()
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Play In-App Updates not available: ${e.message}")
                onFallback()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Play In-App Updates not available: ${e.message}")
            onFallback()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode == REQUEST_CODE_IMMEDIATE_UPDATE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "Update flow cancelled by user")
            }
            return true
        }
        return false
    }
}
