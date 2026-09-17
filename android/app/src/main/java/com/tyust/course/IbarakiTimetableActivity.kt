package com.tyust.course

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tyust.course.ui.screen.IbarakiTimetableScreen
import com.tyust.course.ui.theme.CourseSelectorTheme

class IbarakiTimetableActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CourseSelectorTheme { com.tyust.course.ui.screen.SchoolSyncFailureDialog(); IbarakiTimetableScreen { finish() } } }
    }
}
