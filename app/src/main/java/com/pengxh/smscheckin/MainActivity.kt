package com.pengxh.smscheckin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pengxh.smscheckin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MainFragment())
                .commit()
        }

        binding.toolbar.inflateMenu(R.menu.toolbar_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    showSettingsFragment()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateToolbarForBackStack()
        }

        updateToolbarForBackStack()
    }

    private fun updateToolbarForBackStack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            binding.toolbar.title = getString(R.string.edit_keyword)
            binding.toolbar.menu.clear()
            binding.toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_arrow_back, theme)
        } else {
            binding.toolbar.title = getString(R.string.app_name)
            binding.toolbar.menu.clear()
            binding.toolbar.inflateMenu(R.menu.toolbar_menu)
            binding.toolbar.navigationIcon = null
        }
    }

    private fun showSettingsFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SettingsFragment())
            .addToBackStack("settings")
            .commit()
    }
}
