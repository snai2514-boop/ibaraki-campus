package com.tyust.course.academic

import java.math.BigDecimal

data class EnrollmentCapResult(val annualTotal: BigDecimal, val annualRemaining: BigDecimal, val periodRemaining: BigDecimal?) {
    val exceeded get() = annualRemaining.signum() < 0 || (periodRemaining?.signum() ?: 0) < 0
}

/** 2026 engineering handbook, printed page 5. No automatic GPA-based exemption. */
object EnrollmentCap {
    val annualLimit = BigDecimal("46")
    fun assess(registered: BigDecimal, planned: BigDecimal, periodAvailable: BigDecimal?): EnrollmentCapResult {
        require(registered.signum() >= 0 && planned.signum() >= 0 && (periodAvailable == null || periodAvailable.signum() >= 0))
        val total = registered + planned
        return EnrollmentCapResult(total, annualLimit - total, periodAvailable?.minus(planned))
    }
}
