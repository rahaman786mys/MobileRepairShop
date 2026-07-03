package com.app.muzzutech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
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
    @Volatile private var pendingNavDestId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UpdateManager.checkForUpdates(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        val prefs = getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
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
            binding.toolbar.title = destination.label ?: "Repair Shop"
            when (destination.id) {
                R.id.loginFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.toolbar.visibility = View.GONE
                }
                R.id.dashboardFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                }
            }
        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navDest = intent.getStringExtra(EXTRA_NAV_DEST)
        if (navDest != null) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
            val destId = navDest.toDestId()
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
                            if (fragment === navHostFragment) {
                                fm.unregisterFragmentLifecycleCallbacks(this)
                                view.post { commitPendingNav(navHostFragment.navController) }
                            }
                        }
                    }, false
                )
            }
        }
    }
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
    TestLauncherActivity.DEST_FAULTS -> R.id.commonFaultsFragment
    TestLauncherActivity.DEST_INVENTORY -> R.id.inventoryFragment
    TestLauncherActivity.DEST_PROFILE -> R.id.profileFragment
    else -> null
}

