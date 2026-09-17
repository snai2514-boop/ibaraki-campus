package com.tyust.course

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tyust.course.ui.screen.StudyProgressScreen
import com.tyust.course.ui.theme.CourseSelectorTheme

class StudyProgressActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CourseSelectorTheme { com.tyust.course.ui.screen.SchoolSyncFailureDialog(); StudyProgressScreen(onBack = { finish() }) } }
    }
}
