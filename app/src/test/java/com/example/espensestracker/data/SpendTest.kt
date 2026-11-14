package com.example.espensestracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SpendTest {
    @Test fun spendRules() {
        assertEquals(100.0, Transaction(amount = 100.0).spend(), 0.0)
        assertEquals(-40.0, Transaction(amount = 40.0, type = TYPE_REFUND).spend(), 0.0)
        assertEquals(0.0, Transaction(amount = 500.0, category = TRANSFER_CATEGORY).spend(), 0.0)
        assertEquals(0.0, Transaction(amount = 500.0, type = TYPE_CREDIT).spend(), 0.0)
    }
}
