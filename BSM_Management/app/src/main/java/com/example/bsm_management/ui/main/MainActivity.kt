package com.example.bsm_management.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bsm_management.R
import com.example.bsm_management.ui.hostel.AddHostelActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import database.DatabaseHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 👉 Router: nếu chưa có nhà trọ => ép vào AddHostel rồi dừng MainActivity
        val db = database.DatabaseHelper(this)
        if (!db.hasAnyRoom()) {
            startActivity(
                Intent(this, com.example.bsm_management.ui.intro.EmptyStateActivity::class.java)
                    .putExtra("firstSetup", true)
            )
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, 0)
            insets
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHost) as NavHostFragment
        val navController = navHost.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener(null)
        bottomNav.setOnItemReselectedListener(null)
        bottomNav.setupWithNavController(navController)
        bottomNav.selectedItemId = R.id.homeFragment
    }
}
