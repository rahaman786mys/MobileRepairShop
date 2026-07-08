package com.app.muzzutech.ui.update

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.app.muzzutech.BuildConfig
import com.app.muzzutech.R
import com.app.muzzutech.utils.UpdateManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class UpdateBottomSheet : androidx.fragment.app.DialogFragment() {

    companion object {
        private const val TAG = "UpdateBottomSheet"
        private const val ARG_VERSION_NAME = "version_name"
        private const val ARG_CURRENT_VERSION = "current_version"
        private const val ARG_RELEASE_NOTES = "release_notes"
        private const val ARG_SIZE_BYTES = "size_bytes"
        private const val ARG_DOWNLOAD_URL = "download_url"
        private const val ARG_VERSION_CODE = "version_code"
        private const val ARG_FORCE_UPDATE = "force_update"

        fun newInstance(
            versionName: String,
            currentVersionName: String,
            releaseNotes: String,
            sizeBytes: Long?,
            downloadUrl: String,
            versionCode: Int,
            forceUpdate: Boolean = false
        ): UpdateBottomSheet {
            val args = Bundle().apply {
                putString(ARG_VERSION_NAME, versionName)
                putString(ARG_CURRENT_VERSION, currentVersionName)
                putString(ARG_RELEASE_NOTES, releaseNotes)
                putLong(ARG_SIZE_BYTES, sizeBytes ?: 0)
                putString(ARG_DOWNLOAD_URL, downloadUrl)
                putInt(ARG_VERSION_CODE, versionCode)
                putBoolean(ARG_FORCE_UPDATE, forceUpdate)
            }
            return UpdateBottomSheet().apply { arguments = args }
        }
    }

    private var downloadUrl: String = ""
    private var targetVersionCode: Int = 0
    private var downloadFile: java.io.File? = null
    private var forceUpdate = false

    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var tvFailed: TextView
    private lateinit var btnUpdateNow: Button
    private lateinit var btnLater: Button
    private lateinit var btnRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate savedInstanceState=$savedInstanceState")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog building dialog")
        val ctx = requireContext()
        val args = requireArguments()
        val versionName = args.getString(ARG_VERSION_NAME, "?")
        val currentVersion = args.getString(ARG_CURRENT_VERSION, "?")
        val notes = args.getString(ARG_RELEASE_NOTES, "")
        val sizeBytes = args.getLong(ARG_SIZE_BYTES, 0)
        forceUpdate = args.getBoolean(ARG_FORCE_UPDATE, false)
        downloadUrl = args.getString(ARG_DOWNLOAD_URL, "") ?: ""
        targetVersionCode = args.getInt(ARG_VERSION_CODE)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xFF0B1120.toInt())
        }

        val versionText = TextView(ctx).apply {
            text = "v$currentVersion \u2192 v$versionName"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(0xFFF1F5F9.toInt())
        }
        root.addView(versionText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 24 })

        val sizeText = TextView(ctx).apply {
            text = if (sizeBytes > 0) String.format(
                Locale.getDefault(), "%.1f MB", sizeBytes / (1024.0 * 1024.0)
            ) else ""
            visibility = if (sizeBytes > 0) View.VISIBLE else View.GONE
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(0xFFCBD5E1.toInt())
        }
        root.addView(sizeText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 })

        val notesTitle = TextView(ctx).apply {
            text = "What's New"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(0xFFF1F5F9.toInt())
        }
        root.addView(notesTitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 8 })

        val formattedNotes = formatReleaseNotes(notes)
        val notesText = TextView(ctx).apply {
            text = formattedNotes
            textSize = 13f
            setLineSpacing(3f, 1f)
            setBackgroundColor(0x1AFFFFFF.toInt())
            setPadding(28, 28, 28, 28)
            setTextColor(0xFFE2E8F0.toInt())
        }
        root.addView(notesText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 24 })

        progressBar = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            id = View.generateViewId()
            max = 100
            progress = 0
            visibility = View.GONE
            progressDrawable?.setTint(0xFF3B82F6.toInt())
        }
        root.addView(progressBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            24 // Increased height
        ).apply { bottomMargin = 16 })

        tvProgress = TextView(ctx).apply {
            id = View.generateViewId()
            text = "Starting download..."
            visibility = View.GONE
            textSize = 12f
            setTextColor(0xFFCBD5E1.toInt())
        }
        root.addView(tvProgress, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 8 })

        tvFailed = TextView(ctx).apply {
            text = "Download failed"
            setTextColor(0xFFEF4444.toInt())
            visibility = View.GONE
        }
        root.addView(tvFailed, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 8 })

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }

        btnLater = Button(ctx).apply {
            text = "Later"
            visibility = if (forceUpdate) View.GONE else View.VISIBLE
        }
        btnUpdateNow = Button(ctx).apply { text = "Update Now" }
        btnRetry = Button(ctx).apply {
            text = "Retry"
            visibility = View.GONE
        }

        val lpWrap = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnRow.addView(btnLater, lpWrap)
        btnRow.addView(btnUpdateNow, lpWrap.apply { leftMargin = 16 })
        btnRow.addView(btnRetry, lpWrap.apply { leftMargin = 16 })

        root.addView(btnRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8 })

        btnLater.setOnClickListener { dismiss() }

        btnUpdateNow.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            tvProgress.visibility = View.VISIBLE
            btnUpdateNow.visibility = View.GONE
            btnLater.isEnabled = false
            btnRetry.visibility = View.GONE
            startDownload()
        }

        btnRetry.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            tvProgress.visibility = View.VISIBLE
            btnRetry.visibility = View.GONE
            tvFailed.visibility = View.GONE
            btnUpdateNow.visibility = View.GONE
            btnLater.isEnabled = false
            startDownload()
        }

        val dialog = MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_MuZZu_BottomSheet)
            .setView(root)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.color.muzzu_surface)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0.5f)
        Log.d(TAG, "onCreateDialog dialog created forceUpdate=$forceUpdate window=${dialog.window}")
        dialog.setCanceledOnTouchOutside(!forceUpdate)
        dialog.setCancelable(!forceUpdate)
        Log.d(TAG, "onCreateDialog returning dialog")
        return dialog
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart dialog=${dialog?.window}")
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume dialog=${dialog?.window}")
    }

    override fun onDismiss(dialogInterface: android.content.DialogInterface) {
        super.onDismiss(dialogInterface)
        Log.d(TAG, "onDismiss")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    private fun formatReleaseNotes(raw: String): String {
        val cleaned = raw
            .replace(Regex("""^#{1,3}\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\*{1,2}(.+?)\*{1,2}"""), "$1")
            .trim()
        if (cleaned.isEmpty()) return getString(R.string.update_no_notes)
        return cleaned.split("\n").joinToString("\n") { line ->
            line.trim().removePrefix("-").removePrefix("\u2022").trim().let {
                if (it.isEmpty()) "" else "\u2022 $it"
            }
        }.trim()
    }

    private fun startDownload() {
        val ctx = context ?: return
        UpdateManager.downloadAndInstall(
            context = ctx,
            url = downloadUrl,
            onProgress = { pct, mb ->
                if (isAdded) {
                    progressBar.progress = pct
                    tvProgress.text = "$pct%\n$mb"
                }
            },
            onComplete = { file ->
                if (isAdded) {
                    downloadFile = file
                    progressBar.visibility = View.GONE
                    tvProgress.visibility = View.GONE
                    tvFailed.visibility = View.GONE
                    Toast.makeText(ctx, R.string.download_complete, Toast.LENGTH_SHORT).show()
                    UpdateManager.installApk(ctx, file)
                }
            },
            onFailed = { err ->
                if (isAdded) {
                    progressBar.visibility = View.GONE
                    tvProgress.visibility = View.GONE
                    btnRetry.visibility = View.VISIBLE
                    btnUpdateNow.visibility = View.VISIBLE
                    btnLater.isEnabled = true
                    tvFailed.text = getString(R.string.download_failed_short, err)
                    tvFailed.visibility = View.VISIBLE
                }
            }
        )
    }
}
