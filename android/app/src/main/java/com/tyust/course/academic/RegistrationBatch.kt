package com.tyust.course.academic

/** Sending advances the queue; only a final fresh timetable establishes registration. */
data class RegistrationBatch private constructor(
    val courses: List<RegistrationCourse>,
    val attemptedIds: Set<String> = emptySet(),
    val sentIds: Set<String> = emptySet(),
    val confirmedIds: Set<String> = emptySet(),
    val verified: Boolean = false,
    val stopped: Boolean = false,
    val feedback: Map<String,String> = emptyMap()
) {
    val remaining get() = courses.filter { it.id !in attemptedIds }
    val current get() = if(stopped || attemptedIds != sentIds) null else remaining.firstOrNull()

    fun beginCurrent(id: String): RegistrationBatch {
        require(current?.id == id) { "登记队列与当前课程不一致" }
        return copy(attemptedIds=attemptedIds + id)
    }

    fun dispatched(id: String): RegistrationBatch {
        require(!stopped && id in attemptedIds && id !in sentIds)
        return copy(sentIds=sentIds + id)
    }

    fun recordFeedback(id: String, message: String): RegistrationBatch {
        require(id in attemptedIds)
        if(message.isBlank()) return this
        return copy(feedback=feedback + (id to message.take(1200)))
    }

    fun verify(registeredCodes: Set<String>): RegistrationBatch = copy(
        confirmedIds=courses.filter {it.id in attemptedIds && it.code in registeredCodes}.map {it.id}.toSet(),
        verified=true, stopped=true)

    fun stop() = copy(stopped=true)

    fun report(message: String, remainingCredits: String = ""): String = buildString {
        append(message)
        if(verified) {
            append("\n查询已完成：${confirmedIds.size} 门已登记，${attemptedIds.size-confirmedIds.size} 门未登记")
            append("\n已确认 ${confirmedIds.size} / ${courses.size} 门")
        } else append("\n已发送 ${sentIds.size} / ${courses.size} 门；尚待查询学校结果")
        if(remainingCredits.toBigDecimalOrNull()?.signum()?.let {it>=0}==true)
            append("\n学校当前还可登记 $remainingCredits 学分")
        fun group(label: String, rows: List<RegistrationCourse>) {
            if(rows.isNotEmpty()) append("\n$label：" + rows.joinToString("、") {it.name})
        }
        group("已登记",courses.filter {it.id in confirmedIds})
        group(if(verified) "未登记（学校课表未显示）" else "等待统一查询",courses.filter {it.id in attemptedIds && it.id !in confirmedIds})
        group("未发送",remaining)
        courses.filter {it.id in attemptedIds && it.id !in confirmedIds}.forEach {course ->
            feedback[course.id]?.let {append("\n${course.name} · 学校反馈：$it")}
        }
        if(verified && attemptedIds.any {it !in confirmedIds}) {
            append("\n本次处理已结束，未登记课程没有加入学校课表。")
            append("请检查学校反馈及可登记学分；系统不会自动重复发送。")
        }
    }

    companion object {
        fun approve(snapshot: RegistrationSnapshot, selected: Set<String>): RegistrationBatch {
            require(selected.isNotEmpty())
            val chosen=snapshot.rows.filter {it.id in selected}
            require(chosen.size==selected.size && chosen.map {it.id}.distinct().size==chosen.size)
            require(chosen.all {it.available && it.slotKey.isNotBlank() && it.signature.isNotBlank()})
            return RegistrationBatch(chosen.toList())
        }

        /** Interrupted batches are query-only, never resumed for submission. */
        fun recover(courses: List<RegistrationCourse>, attempted: Set<String>, sent: Set<String>, feedback: Map<String,String> = emptyMap()): RegistrationBatch {
            require(courses.isNotEmpty() && courses.map {it.id}.distinct().size==courses.size)
            require(courses.map {it.id}.containsAll(attempted) && attempted.containsAll(sent))
            require(attempted.containsAll(feedback.keys))
            return RegistrationBatch(courses,attempted,sent,stopped=true,feedback=feedback)
        }
    }
}
