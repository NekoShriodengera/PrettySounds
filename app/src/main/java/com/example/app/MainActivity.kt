package com.example.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    private lateinit var themeToggleButton1: ImageButton
    private lateinit var themeToggleButton2: ImageButton
    private lateinit var mainLayout: ConstraintLayout

    // id всех 8 кнопок
    private val circleButtonIds = listOf(
        R.id.closeButton1,
        R.id.searchButton2,
        R.id.likeButton3,
        R.id.folderButton4,
        R.id.cloudappButton5,
        R.id.themeButton6,
        R.id.galleryButton7,
        R.id.notesButton8
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.mainLayout)
        themeToggleButton1 = findViewById(R.id.themeButton)
        themeToggleButton2 = findViewById(R.id.themeButton6)

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        updateBackgroundAndButton() // обновление фон

        // переключение темы
        themeToggleButton1.setOnClickListener {
            toggleTheme()
        }
        themeToggleButton2.setOnClickListener {
            toggleTheme()
        }

        // Переходы на 8 страниц
        val buttonActivityPairs = listOf(
            R.id.settingsButton to SettingPage::class.java,
            R.id.searchButton2 to SearchPage::class.java,
            R.id.closeButton1 to MainActivity::class.java,
            R.id.folderButton4 to MainActivity::class.java,
            R.id.galleryButton7 to GalleryPage::class.java,
            R.id.notesButton8 to NotesPage::class.java
        )

        for ((id, activityClass) in buttonActivityPairs) {
            findViewById<ImageButton>(id).setOnClickListener {
                val intent = Intent(this, activityClass)
                startActivity(intent)
            }
        }

        updateCircleImages()
    }

    private fun updateBackgroundAndButton() {
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        mainLayout.setBackgroundResource(
            if (isNight) R.drawable.fondark else R.drawable.fonday
        )
        themeToggleButton1.setImageResource(
            if (isNight) R.drawable.night else R.drawable.cloud
        )
        themeToggleButton2.setImageResource(
            if (isNight) R.drawable.night else R.drawable.cloud
        )
    }

    private fun toggleTheme() {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(
            if (nightMode == AppCompatDelegate.MODE_NIGHT_YES)
                AppCompatDelegate.MODE_NIGHT_NO
            else
                AppCompatDelegate.MODE_NIGHT_YES
        )
        recreate() // Пересоздаем активность для применения изменений
    }

    private fun updateCircleImages() {
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        val res1 = if (isNight) R.drawable.close_night else R.drawable.close
        val res2 = if (isNight) R.drawable.search_night else R.drawable.search
        val res3 = if (isNight) R.drawable.like_night else R.drawable.like
        val res4 = if (isNight) R.drawable.folder_night else R.drawable.folder
        val res5 = if (isNight) R.drawable.cloudapp_night else R.drawable.cloudapp
        val res6 = if (isNight) R.drawable.day_night else R.drawable.night
        val res7 = if (isNight) R.drawable.gallery_night else R.drawable.gallery
        val res8 = if (isNight) R.drawable.notes_night else R.drawable.notes
        val res9 = if (isNight) R.drawable.settings_night else R.drawable.settings
        val res10 = if (isNight) R.drawable.arrow_night else R.drawable.arrow

        findViewById<ImageButton>(R.id.closeButton1).setImageResource(res1)
        findViewById<ImageButton>(R.id.searchButton2).setImageResource(res2)
        findViewById<ImageButton>(R.id.likeButton3).setImageResource(res3)
        findViewById<ImageButton>(R.id.folderButton4).setImageResource(res4)
        findViewById<ImageButton>(R.id.cloudappButton5).setImageResource(res5)
        findViewById<ImageButton>(R.id.themeButton6).setImageResource(res6)
        findViewById<ImageButton>(R.id.galleryButton7).setImageResource(res7)
        findViewById<ImageButton>(R.id.notesButton8).setImageResource(res8)
        findViewById<ImageButton>(R.id.settingsButton).setImageResource(res9)
        findViewById<ImageButton>(R.id.arrow).setImageResource(res10)
    }
}
