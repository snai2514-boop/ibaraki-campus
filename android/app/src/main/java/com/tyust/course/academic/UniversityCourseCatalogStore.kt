package com.tyust.course.academic

import android.content.Context

object UniversityCourseCatalogStore {
    @Volatile private var cached: UniversityCourseCatalog? = null
    fun load(context: Context): UniversityCourseCatalog = cached ?: synchronized(this) {
        cached ?: context.assets.open("course-categories.tsv").bufferedReader(Charsets.UTF_8).use {
            UniversityCourseCatalog.parse(it.readText())
        }.also { cached = it }
    }
}
