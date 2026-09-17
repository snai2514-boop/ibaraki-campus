package com.tyust.course.ui.screen

import androidx.compose.runtime.Composable

/** Unified school calendar entry; historical manual files are retained on disk. */
@Composable
fun IbarakiTimetableScreen(onBack: () -> Unit) {
    SchoolTimetableScreen(onBack)
}
