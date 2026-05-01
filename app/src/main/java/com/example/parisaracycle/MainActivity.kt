package com.example.parisaracycle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.parisaracycle.ui.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // ✅ FIXED
        if (savedInstanceState == null) {
            loadFragment(MapFragment())
        }

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_map -> {
                    loadFragment(MapFragment())
                    true
                }
                R.id.nav_report -> {
                    loadFragment(DangerPinFragment())
                    true
                }
                R.id.nav_pitstop -> {
                    loadFragment(PitStopFragment())
                    true
                }
                R.id.nav_eco -> {
                    loadFragment(EcoStatsFragment())
                    true
                }
                R.id.nav_buddy -> {
                    loadFragment(BuddyFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}