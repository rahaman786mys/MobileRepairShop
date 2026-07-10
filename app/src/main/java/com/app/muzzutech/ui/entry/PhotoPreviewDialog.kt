package com.app.muzzutech.ui.entry

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.app.muzzutech.R
import com.bumptech.glide.Glide

class PhotoPreviewDialog : DialogFragment() {

    companion object {
        private const val ARG_PATH = "photo_path"
        private const val ARG_SLOT = "slot"

        fun newInstance(path: String, slot: Int): PhotoPreviewDialog {
            return PhotoPreviewDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, path)
                    putInt(ARG_SLOT, slot)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_photo_preview)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_photo_preview, container, false)

        val path = requireArguments().getString(ARG_PATH) ?: ""
        val slot = requireArguments().getInt(ARG_SLOT)
        val file = java.io.File(path)

        Glide.with(this)
            .load(file)
            .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))
            .centerInside()
            .into(root.findViewById(R.id.ivPreview))

        root.findViewById<Button>(R.id.btnRetake).setOnClickListener {
            val fragment = parentFragment as? EntryFragment
            fragment?.openCameraForSlot(slot)
            dismiss()
        }

        root.findViewById<Button>(R.id.btnMarkIssue).setOnClickListener {
            val fragment = parentFragment as? EntryFragment
            fragment?.let { 
                val bundle = Bundle().apply { putString("imagePath", path) }
                androidx.navigation.fragment.NavHostFragment.findNavController(it).navigate(R.id.imageEditorFragment, bundle)
            }
            dismiss()
        }

        root.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            val fragment = parentFragment as? EntryFragment
            fragment?.let { entry ->
                if (slot == 1) {
                    entry.viewModel.setPhoto1(null)
                } else {
                    entry.viewModel.setPhoto2(null)
                }
            }
            dismiss()
        }

        root.findViewById<ImageView>(R.id.btnClosePreview).setOnClickListener {
            dismiss()
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}
