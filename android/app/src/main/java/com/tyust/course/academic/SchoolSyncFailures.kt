package com.tyust.course.academic

import kotlinx.coroutines.flow.MutableStateFlow

enum class SchoolSyncFailureKind(val label: String, val feedback: Boolean = false) {
    NETWORK("网络连接失败"), TIMEOUT("读取超时"), AUTH("学校登录已过期"),
    ACTION_REQUIRED("学校要求确认资料"),
    DATA("数据读取或校验失败", true), STORAGE("本机保存失败"),
    PROGRAM("程序异常", true), SECURITY("学校网站证书异常"),
    RESOURCE("系统释放了网页进程"), UNKNOWN("未能确认失败原因", true)
}
data class SchoolSyncFailure(val part: String, val reason: String, val kind: SchoolSyncFailureKind)

object SchoolSyncFailures {
    val pending = MutableStateFlow<List<SchoolSyncFailure>>(emptyList())
    fun part(step: Int, syllabus: Boolean = false): String = when (step) {
        -1 -> "学生资料"
        0 -> "成绩与学分"
        in 1..4 -> "Q${step} 课表"
        5 -> "教室信息"
        6 -> if (syllabus) "课程大纲" else "学校公告"
        7 -> "履修状况"
        else -> "学校同步"
    }
    fun classify(reason: String): SchoolSyncFailureKind = when {
        reason.contains("超时") -> SchoolSyncFailureKind.TIMEOUT
        reason.contains("登录已过期") -> SchoolSyncFailureKind.AUTH
        reason.contains("保存失败") -> SchoolSyncFailureKind.STORAGE
        listOf("数据校验失败", "结构变化", "学分无法识别", "未确定成绩", "分数超出", "合否不一致", "明细学分", "未适配").any(reason::contains) -> SchoolSyncFailureKind.DATA
        else -> SchoolSyncFailureKind.UNKNOWN
    }
    @Synchronized fun report(part: String, reason: String, kind: SchoolSyncFailureKind = classify(reason)) {
        val failure = SchoolSyncFailure(part, reason, kind)
        if (failure !in pending.value) pending.value = pending.value + failure
    }
    @Synchronized fun dismiss(failure: SchoolSyncFailure) {
        pending.value = pending.value.filterNot { it == failure }
    }
}
