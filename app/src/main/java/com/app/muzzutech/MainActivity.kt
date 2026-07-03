package com.app.muzzutech

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.app.muzzutech.databinding.ActivityMainBinding
import com.app.muzzutech.utils.UpdateManager

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAV_DEST = "extra_nav_dest"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for updates from GitHub
        UpdateManager.checkForUpdates(this)

        // Set up navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        // Set up bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Login removed — go straight to dashboard. Keep auth_prefs write for compatibility.
        val prefs = getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", true).apply()

        // Handle test-launcher navigation (debug scenarios)
        val navDest = intent.getStringExtra(EXTRA_NAV_DEST)
        if (navDest != null) {
            intent.removeExtra(EXTRA_NAV_DEST)
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id != navController.graph.startDestinationId) {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                }
            }
            val destId = when (navDest) {
                TestLauncherActivity.DEST_ENTRY -> R.id.entryFragment
                TestLauncherActivity.DEST_SALE -> R.id.saleFragment
                TestLauncherActivity.DEST_DUES -> R.id.duesFragment
                TestLauncherActivity.DEST_REPORTS -> R.id.reportsFragment
                TestLauncherActivity.DEST_MORE -> R.id.moreFragment
                TestLauncherActivity.DEST_PAYROLL -> R.id.payrollFragment
                TestLauncherActivity.DEST_EXPENSES -> R.id.expensesFragment
                TestLauncherActivity.DEST_SUPPLIERS -> R.id.supplierListFragment
                TestLauncherActivity.DEST_CUSTOMERS -> R.id.customerListFragment
                TestLauncherActivity.DEST_FAULTS -> R.id.commonFaultsFragment
                TestLauncherActivity.DEST_INVENTORY -> R.id.inventoryFragment
                TestLauncherActivity.DEST_PROFILE -> R.id.profileFragment
                else -> null
            }
            if (destId != null) {
                navController.navigate(destId)
            }
        }

        // Update toolbar title and visibility of bottom nav based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = destination.label ?: "Repair Shop"

            when (destination.id) {
                R.id.loginFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.toolbar.visibility = View.GONE
                }
                R.id.dashboardFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.GONE // Hide toolbar for dashboard
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }
}
