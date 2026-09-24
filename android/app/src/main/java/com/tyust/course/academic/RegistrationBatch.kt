package com.tyust.course.academic

/** Sending advances the queue; only a final fresh timetable establishes registration. */
data class RegistrationBatch private constructor(
    val courses: List<RegistrationCourse>,
    val attemptedIds: Set<String> = emptySet(),
    val sentIds: Set<String> = emptySet(),
    val confirmedIds: Set<String> = emptySet(),
    val verified: Boolean = false,
    val stopped: Boolean = false
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

    fun verify(registeredCodes: Set<String>): RegistrationBatch = copy(
        confirmedIds=courses.filter {it.id in attemptedIds && it.code in registeredCodes}.map {it.id}.toSet(),
        verified=true, stopped=true)

    fun stop() = copy(stopped=true)

    fun report(message: String): String = buildString {
        append(message)
        append("\n已发送 ${sentIds.size} / ${courses.size} 门")
        if(verified) append("\n已确认 ${confirmedIds.size} / ${courses.size} 门")
        fun group(label: String, rows: List<RegistrationCourse>) {
            if(rows.isNotEmpty()) append("\n$label：" + rows.joinToString("、") {it.name})
        }
        group("已登记",courses.filter {it.id in confirmedIds})
        group(if(verified) "学校课表未显示登记" else "等待统一查询",courses.filter {it.id in attemptedIds && it.id !in confirmedIds})
        group("未发送",remaining)
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
        fun recover(courses: List<RegistrationCourse>, attempted: Set<String>, sent: Set<String>): RegistrationBatch {
            require(courses.isNotEmpty() && courses.map {it.id}.distinct().size==courses.size)
            require(courses.map {it.id}.containsAll(attempted) && attempted.containsAll(sent))
            return RegistrationBatch(courses,attempted,sent,stopped=true)
        }
    }
}
