package com.tyust.course.academic

import org.junit.Assert.*
import org.junit.Test

class EnrollmentCapTest {
    @Test fun annualBoundaryAndExcess() {
        assertFalse(EnrollmentCap.assess("26".toBigDecimal(), "20".toBigDecimal(), null).exceeded)
        assertTrue(EnrollmentCap.assess("26".toBigDecimal(), "20.5".toBigDecimal(), null).exceeded)
    }
    @Test fun periodCanBlockEvenWithinAnnualLimit() {
        val r = EnrollmentCap.assess("26".toBigDecimal(), "10".toBigDecimal(), "8".toBigDecimal())
        assertTrue(r.exceeded)
        assertEquals("-2".toBigDecimal(), r.periodRemaining)
    }
    @Test fun absentPeriodIsUnknownAndNegativeInputsRejected() {
        assertNull(EnrollmentCap.assess("26".toBigDecimal(), "0".toBigDecimal(), null).periodRemaining)
        assertTrue(runCatching { EnrollmentCap.assess("-1".toBigDecimal(), "0".toBigDecimal(), null) }.isFailure)
    }
}
