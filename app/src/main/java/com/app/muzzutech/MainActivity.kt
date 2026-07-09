package com.app.muzzutech

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.app.muzzutech.databinding.ActivityMainBinding
import com.app.muzzutech.utils.UpdateManager
import com.app.muzzutech.utils.crpto.SecurePrefs
import com.app.muzzutech.utils.update.PlayUpdateHelper
import com.app.muzzutech.utils.update.UpdateRepository
import com.app.muzzutech.ui.update.WhatsNewFragment
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAV_DEST = "extra_nav_dest"
        const val RELEASES_NOTES_V1512 = """Welcome to v1.5.12!

• What's New popup now appears after every update
• Smooth flow Entry → Spare Parts → Handover
• Quotation + Common Faults removed for simplicity
• Charge + Advance captured at entry
• Time-based greeting on dashboard

Tap Let's Go to continue."""
    }

    private lateinit var binding: ActivityMainBinding
    @Volatile private var pendingNavDestId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        UpdateManager.checkForUpdates(this)
        checkForWhatIsNew()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.entryFragment,
                R.id.entriesFragment,
                R.id.reportsFragment,
                R.id.moreFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        val prefs = SecurePrefs.authPrefs(this)
        prefs.edit().putBoolean("is_logged_in", true).apply()

        val navDest = intent.getStringExtra(EXTRA_NAV_DEST)
        if (navDest != null) {
            val destId = navDest.toDestId()
            Log.d("TestLauncher", "Main.onCreate: navDest=$navDest destId=$destId")
            if (destId != null) {
                pendingNavDestId = destId
                supportFragmentManager.registerFragmentLifecycleCallbacks(
                    object : FragmentManager.FragmentLifecycleCallbacks() {
                        override fun onFragmentViewCreated(
                            fm: FragmentManager,
                            fragment: Fragment,
                            view: View,
                            savedInstanceState: Bundle?
                        ) {
                            super.onFragmentViewCreated(fm, fragment, view, savedInstanceState)
                            Log.d("TestLauncher", "onFragmentViewCreated: fragment=$fragment navHost=$navHostFragment match=${fragment === navHostFragment}")
                            if (fragment === navHostFragment) {
                                fm.unregisterFragmentLifecycleCallbacks(this)
                                Log.d("TestLauncher", "Matched NavHostFragment, posting commitPendingNav")
                                view.post {
                                    Log.d("TestLauncher", "post callback running, destId=$pendingNavDestId")
                                    commitPendingNav(navController)
                                }
                            }
                        }
                    }, false
                )
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.toolbar.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun checkForWhatIsNew() {
        val repo = UpdateRepository(this)
        if (!repo.shouldShowWhatsNew()) return
        val versionName = repo.getCurrentVersionName()
        val versionCode = repo.getCurrentVersionCode()
        repo.markWhatsNewShown()

        val bundle = android.os.Bundle().apply {
            putString("version_name", versionName)
            putInt("version_code", versionCode)
            putString("release_notes", RELEASES_NOTES_V1512)
        }

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, WhatsNewFragment::class.java, bundle)
            .addToBackStack("whats_new")
            .commitAllowingStateLoss()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("show_update_dialog", false)) {
            UpdateManager.handleNotificationIntent(this, intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        if (intent.getBooleanExtra("show_update_dialog", false)) {
            UpdateManager.handleNotificationIntent(this, intent)
            intent.removeExtra("show_update_dialog")
        }
        // Resume Play In-App Updates if interrupted (e.g., user went to Play Store to update)
        try {
            val appUpdateManager = AppUpdateManagerFactory.create(this)
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        info, com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE,
                        this, PlayUpdateHelper.REQUEST_CODE_IMMEDIATE_UPDATE
                    )
                }
            }
        } catch (_: Exception) { }
    }

    private fun commitPendingNav(navController: androidx.navigation.NavController) {
        val destId = pendingNavDestId ?: return
        pendingNavDestId = null
        val currentId = navController.currentDestination?.id
        Log.d("TestLauncher", "commitPendingNav: current=$currentId start=${navController.graph.startDestinationId} dest=$destId")
        if (currentId != null && currentId != navController.graph.startDestinationId) {
            navController.popBackStack(navController.graph.startDestinationId, false)
        }
        navController.navigate(destId)
        Log.d("TestLauncher", "navigate($destId) done, now at ${navController.currentDestination?.id}")
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }

    private fun String.toDestId(): Int? = when (this) {
        TestLauncherActivity.DEST_ENTRY -> R.id.entryFragment
        TestLauncherActivity.DEST_SALE -> R.id.saleFragment
        TestLauncherActivity.DEST_DUES -> R.id.duesFragment
        TestLauncherActivity.DEST_REPORTS -> R.id.reportsFragment
        TestLauncherActivity.DEST_MORE -> R.id.moreFragment
        TestLauncherActivity.DEST_PAYROLL -> R.id.payrollFragment
        TestLauncherActivity.DEST_EXPENSES -> R.id.expensesFragment
        TestLauncherActivity.DEST_SUPPLIERS -> R.id.supplierListFragment
        TestLauncherActivity.DEST_CUSTOMERS -> R.id.customerListFragment
        TestLauncherActivity.DEST_INVENTORY -> R.id.inventoryFragment
        TestLauncherActivity.DEST_PROFILE -> R.id.profileFragment
        else -> null
    }
}
