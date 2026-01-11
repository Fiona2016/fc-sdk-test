package com.example.fc_sdk_test

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import com.example.fc_sdk_test.databinding.ActivityMainBinding
import cloud.flashcat.android.log.Logger
import cloud.flashcat.android.rum.GlobalRumMonitor
import cloud.flashcat.android.rum.RumActionType

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var logger: Logger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "=== MainActivity onCreate ===")
        
        // Create Logger after SDK initialization (in Application.onCreate)
        logger = Logger.Builder()
            .setNetworkInfoEnabled(true)
            .setLogcatLogsEnabled(true)
            .setName("MainActivity")
            .build()
        
        // Test FlashCat SDK logging
        logger.i("MainActivity onCreate - FlashCat SDK initialized")
        Log.d(TAG, "Logger 已创建并记录日志")
        
        try {
            val rumMonitor = GlobalRumMonitor.get()
            Log.d(TAG, "GlobalRumMonitor.get() 成功: $rumMonitor")
            
            rumMonitor.startView(
                key = this,
                name = "MainActivity",
                attributes = mapOf("test_attribute" to "test_value")
            )
            Log.d(TAG, "✓ RUM startView() 调用成功 - View: MainActivity")
        } catch (e: Exception) {
            Log.e(TAG, "✗ RUM startView() 失败", e)
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab?.setOnClickListener { view ->
            // Log FAB click event
            Log.d(TAG, "FAB 按钮被点击")
            logger.i("FAB clicked")
            
            try {
                GlobalRumMonitor.get().addAction(
                    type = RumActionType.TAP,
                    name = "fab_click",
                    attributes = emptyMap()
                )
                Log.d(TAG, "✓ RUM addAction() 调用成功 - Action: fab_click")
            } catch (e: Exception) {
                Log.e(TAG, "✗ RUM addAction() 失败", e)
            }
            
            Snackbar.make(view, "FlashCat SDK is working!", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController

        binding.navView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_webview, R.id.nav_settings
                ),
                binding.drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            it.setupWithNavController(navController)
        }

        binding.appBarMain.contentMain.bottomNavView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_webview
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            it.setupWithNavController(navController)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            menuInflater.inflate(R.menu.overflow, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_settings)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    override fun onDestroy() {
        try {
            Log.d(TAG, "MainActivity onDestroy - 停止 RUM View")
            GlobalRumMonitor.get().stopView(this)
            Log.d(TAG, "✓ RUM stopView() 调用成功")
        } catch (e: Exception) {
            Log.e(TAG, "✗ RUM stopView() 失败", e)
        }
        super.onDestroy()
    }
}