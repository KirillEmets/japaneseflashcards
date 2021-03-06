package com.kirillemets.flashcards

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kirillemets.flashcards.database.CardDatabase
import com.kirillemets.flashcards.database.DatabaseRepository
import com.kirillemets.flashcards.review.ReviewFragmentViewModel
import com.kirillemets.flashcards.review.ReviewFragmentViewModelFactory
import net.danlew.android.joda.JodaTimeAndroid

lateinit var bottomNavigation: BottomNavigationView

class MainActivity : AppCompatActivity() {

    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var viewModel:ReviewFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        JodaTimeAndroid.init(this)
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = preferenceManager.getString("theme", "Auto")
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                "Auto" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                "Light" -> AppCompatDelegate.MODE_NIGHT_NO
                "Black" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )

        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        val navController = this.findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration.Builder(
            R.id.reviewStarterFragment,
            R.id.addWordFragment,
            R.id.myDictionaryFragment,
            R.id.settingsFragment
        ).build()

        setupActionBarWithNavController(navController, appBarConfiguration)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            if(bottomNavigation.selectedItemId == item.itemId)
                return@setOnNavigationItemSelectedListener false

            this.currentFocus?.let { view ->
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }

            when(item.itemId) {
                R.id.page_review -> {
                    navController.navigate(R.id.action_global_reviewStarterFragment)
                    true
                }
                R.id.page_search -> {
                    navController.navigate(R.id.action_global_addWordFragment)
                    true
                }
                R.id.page_my_words -> {
                    navController.navigate(R.id.action_global_myDictionaryFragment)
                    true
                }
                R.id.page_settings -> {
                    navController.navigate(R.id.action_global_settingsFragment)
                    true
                }
                else -> false
            }
        }

        val database = CardDatabase.getInstance(this).flashCardsDao()
        viewModel = ViewModelProvider(
            this,
            ReviewFragmentViewModelFactory(DatabaseRepository(database))
        ).get(ReviewFragmentViewModel::class.java)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        onUpNavigation()
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        onUpNavigation()
        super.onBackPressed()
    }

    private fun onUpNavigation() {
        val navController = findNavController(R.id.nav_host_fragment)
        if(navController.currentDestination?.id == R.id.reviewFragment) {
            if(viewModel.reviewGoing) {
                viewModel.restart()
            }
        }
    }
}

