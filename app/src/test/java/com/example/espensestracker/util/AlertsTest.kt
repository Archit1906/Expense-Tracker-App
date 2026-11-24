package com.example.espensestracker.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlertsTest {
    private val usual = listOf(500.0, 600.0, 550.0)

    @Test fun flagsPaymentFarAboveMerchantAverage() = assertNotNull(UnusualSpend.reason(3000.0, usual, emptyList()))
    @Test fun ignoresNormalPayment() = assertNull(UnusualSpend.reason(600.0, usual, emptyList()))
    @Test fun ignoresSmallAmounts() = assertNull(UnusualSpend.reason(400.0, listOf(50.0, 60.0, 55.0), emptyList()))
    @Test fun usesOverallHistoryForNewMerchants() = assertNotNull(UnusualSpend.reason(5000.0, emptyList(), List(10) { 300.0 }))
    @Test fun needsHistory() = assertNull(UnusualSpend.reason(5000.0, emptyList(), emptyList()))
}
