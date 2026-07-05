package com.app.muzzutech.ui.update

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.app.muzzutech.MobileRepairApp
import com.app.muzzutech.R
import com.app.muzzutech.databinding.FragmentWhatsNewBinding
import com.app.muzzutech.utils.update.UpdateRepository

class WhatsNewFragment : Fragment() {

    private var _binding: FragmentWhatsNewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhatsNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val versionName = args.getString("version_name")
            ?: UpdateRepository(requireContext()).getCurrentVersionName()
        val notes = args.getString("release_notes") ?: ""
        val versionCode = args.getInt("version_code", 0)

        binding.tvWhatsNewVersion.text = "What's New in v$versionName"
        binding.tvVersionLabel.text = "v$versionName"

        val formatted = notes
            .replace(Regex("^#{1,3}\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\*{1,2}(.+?)\\*{1,2}"), "$1")
            .split("\n")
            .map { it.trim().removePrefix("-").removePrefix("\u2022").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n") { "\u2022 $it" }

        binding.tvNotes.text = formatted

        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        binding.iconContainer.startAnimation(fadeIn)
        binding.versionCard.startAnimation(slideUp)
        binding.btnLetsGo.startAnimation(slideUp)

        binding.btnLetsGo.setOnClickListener {
            UpdateRepository(requireContext()).markWhatsNewShown()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
