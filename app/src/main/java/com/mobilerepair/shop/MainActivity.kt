package com.mobilerepair.shop

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobilerepair.shop.databinding.ActivityMainBinding
import com.mobilerepair.shop.utils.UpdateManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for updates from GitHub
        UpdateManager.checkForUpdates(this)

        // Set up navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Check login state
        val prefs = getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        
        if (isLoggedIn && navController.currentDestination?.id == R.id.loginFragment) {
            navController.navigate(R.id.dashboardFragment)
        }

        // Set up bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // Update toolbar title and visibility of bottom nav based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = destination.label ?: "Mobile Repair Shop"
            
            if (destination.id == R.id.loginFragment) {
                binding.bottomNavigation.visibility = View.GONE
                binding.toolbar.visibility = View.GONE
            } else {
                binding.bottomNavigation.visibility = View.VISIBLE
                binding.toolbar.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }
}
