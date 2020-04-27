package com.skeep.pscal

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.skeep.pscal.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout : DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        drawerLayout = binding.drawer

        setSupportActionBar(binding.menuToolbar)
        val navcontroller = findNavController(R.id.navHostFragment)
        NavigationUI.setupActionBarWithNavController(this,navcontroller,drawerLayout)
        NavigationUI.setupWithNavController(binding.navigation,navcontroller)
        supportActionBar?.setTitle(R.string.app_name)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.navHostFragment)
        return NavigationUI.navigateUp(navController,drawerLayout)
    }
}
