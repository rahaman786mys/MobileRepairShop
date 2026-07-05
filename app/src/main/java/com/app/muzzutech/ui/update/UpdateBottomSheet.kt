package com.app.muzzutech.ui.update

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.app.muzzutech.R
import com.app.muzzutech.utils.UpdateManager
import com.app.muzzutech.utils.update.VersionInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class UpdateBottomSheet : androidx.fragment.app.DialogFragment() {

    private var downloadUrl: String = ""
    private var targetVersionCode: Int = 0
    private var downloadFile: java.io.File? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var tvFailed: TextView
    private lateinit var btnUpdateNow: Button
    private lateinit var btnLater: Button
    private lateinit var btnRetry: Button

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        downloadFile?.let { file ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !requireContext().packageManager.canRequestPackageInstalls()
            ) {
                return@registerForActivityResult
            }
            UpdateManager.installApk(requireContext(), file)
        }
    }

    companion object {
        private const val ARG_VERSION_NAME = "version_name"
        private const val ARG_CURRENT_VERSION = "current_version"
        private const val ARG_RELEASE_NOTES = "release_notes"
        private const val ARG_SIZE_BYTES = "size_bytes"
        private const val ARG_DOWNLOAD_URL = "download_url"
        private const val ARG_VERSION_CODE = "version_code"

        fun newInstance(
            versionName: String,
            currentVersionName: String,
            releaseNotes: String,
            sizeBytes: Long?,
            downloadUrl: String,
            versionCode: Int
        ): UpdateBottomSheet {
            val args = Bundle().apply {
                putString(ARG_VERSION_NAME, versionName)
                putString(ARG_CURRENT_VERSION, currentVersionName)
                putString(ARG_RELEASE_NOTES, releaseNotes)
                putLong(ARG_SIZE_BYTES, sizeBytes ?: 0)
                putString(ARG_DOWNLOAD_URL, downloadUrl)
                putInt(ARG_VERSION_CODE, versionCode)
            }
            return UpdateBottomSheet().apply { arguments = args }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val args = requireArguments()
        val versionName = args.getString(ARG_VERSION_NAME, "?")
        val currentVersion = args.getString(ARG_CURRENT_VERSION, "?")
        val notes = args.getString(ARG_RELEASE_NOTES, "")
        val sizeBytes = args.getLong(ARG_SIZE_BYTES, 0)
        downloadUrl = args.getString(ARG_DOWNLOAD_URL, "") ?: ""
        targetVersionCode = args.getInt(ARG_VERSION_CODE)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val versionText = TextView(ctx).apply {
            text = "v$currentVersion → v$versionName"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
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
        }
        root.addView(sizeText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 16 })

        val notesTitle = TextView(ctx).apply {
            text = "What's New"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
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
        }
        root.addView(notesText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 24 })

        progressBar = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
        }
        root.addView(progressBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            12
        ).apply { bottomMargin = 8 })

        tvProgress = TextView(ctx).apply {
            visibility = View.GONE
            textSize = 12f
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

        btnLater = Button(ctx).apply { text = "Later" }
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

        return MaterialAlertDialogBuilder(ctx, R.style.ThemeOverlay_MuZZu_BottomSheet)
            .setView(root)
            .create()
            .also { it.setCanceledOnTouchOutside(true) }
    }

    private fun formatReleaseNotes(raw: String): String {
        val cleaned = raw
            .replace(Regex("""^#{1,3}\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\*{1,2}(.+?)\*{1,2}"""), "$1")
            .trim()
        if (cleaned.isEmpty()) return getString(R.string.update_no_notes)
        return cleaned.split("\n").joinToString("\n") { line ->
            line.trim().removePrefix("-").removePrefix("•").trim().let {
                if (it.isEmpty()) "" else "• $it"
            }
        }.trim()
    }

    private fun startDownload() {
        val ctx = requireContext()
        UpdateManager.downloadAndInstall(
            context = ctx,
            url = downloadUrl,
            onProgress = { pct, mb ->
                progressBar.progress = pct
                tvProgress.text = "$pct%\n$mb"
            },
            onComplete = {
                progressBar.visibility = View.GONE
                tvProgress.visibility = View.GONE
                tvFailed.visibility = View.GONE
                Toast.makeText(ctx, R.string.download_complete, Toast.LENGTH_SHORT).show()
                downloadFile?.let { UpdateManager.installApk(ctx, it) }
            },
            onFailed = { err ->
                progressBar.visibility = View.GONE
                tvProgress.visibility = View.GONE
                btnRetry.visibility = View.VISIBLE
                btnUpdateNow.visibility = View.VISIBLE
                btnLater.isEnabled = true
                tvFailed.text = getString(R.string.download_failed_short, err)
                tvFailed.visibility = View.VISIBLE
            }
        )
    }
}
