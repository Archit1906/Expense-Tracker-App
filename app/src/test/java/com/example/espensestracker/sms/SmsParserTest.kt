package com.example.espensestracker.sms

import com.example.espensestracker.data.TYPE_CREDIT
import com.example.espensestracker.data.TYPE_DEBIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {
    private fun p(sender: String, body: String) = SmsParser.parse(sender, body, 1_700_000_000_000L)

    @Test fun knownMerchantUpiDebit() {
        val r = p("VM-HDFCBK", "Sent Rs.1,240.00 from HDFC Bank A/C **4921 to VPA swiggy@icici on 19-10-26. Ref 4293810293. Avail Bal: INR 1,42,390.40") as ParseResult.Ok
        assertEquals(1240.0, r.tx.amount, 0.001)
        assertEquals(TYPE_DEBIT, r.tx.type)
        assertEquals("Swiggy", r.tx.merchant)
        assertEquals("Food & Dining", r.tx.category)
        assertEquals("4921", r.tx.account)
        assertEquals("4293810293", r.tx.refNo)
    }

    @Test fun unmappedVpaGoesToReview() {
        val r = p("VM-HDFCBK", "Sent Rs.2,499.00 from HDFC Bank A/C **4921 to VPA merchant9823@okaxis on 19-10-26. Ref 4293810293. If not you, SMS BLOCK to 56767.")
        assertTrue(r is ParseResult.Review && r.sms.reason.contains("VPA") && r.sms.amount == 2499.0)
    }

    @Test fun posCodeGoesToReview() {
        val r = p("BZ-ICICIB", "Dear Customer, your Acct XX8820 is debited with INR 850.00 on 18-Oct-26 at POS 883920 BLR. Avail Bal: INR 1,42,390.40 - ICICI Bank")
        assertTrue(r is ParseResult.Review && r.sms.reason.contains("POS") && r.sms.account == "8820")
    }

    @Test fun foreignMerchantGoesToReview() {
        val r = p("VK-SBICRD", "Txn of INR 14,200.00 spent on your SBI Credit Card ending 3312 at INTNL-ONLINE-LUX on 17/10/26. OTP was used.")
        assertTrue(r is ParseResult.Review && r.sms.reason.contains("Foreign") && r.sms.amount == 14200.0)
    }

    @Test fun creditIsParsed() {
        val r = p("AX-AXISBK", "Rs 5,000.00 credited to your A/c XX1234 on 19-Oct-26 by UPI from RAHUL KUMAR. Ref 123456789012.") as ParseResult.Ok
        assertEquals(TYPE_CREDIT, r.tx.type)
        assertEquals(5000.0, r.tx.amount, 0.001)
    }

    @Test fun otpIsIgnored() =
        assertEquals(ParseResult.Ignore, p("VM-HDFCBK", "123456 is your OTP for txn of Rs 500.00 at AMAZON. Do not share it with anyone."))

    @Test fun promoIsIgnored() =
        assertEquals(ParseResult.Ignore, p("VM-JIOPAY", "Get 20% cashback offer on Rs 500 recharge. Apply now at jio.com"))

    @Test fun personalNumberIsIgnored() =
        assertEquals(ParseResult.Ignore, p("+919876543210", "Sent Rs.100.00 from HDFC Bank A/C **4921 to VPA swiggy@icici"))

    @Test fun merchantKeysAreStable() {
        val ok = p("VM-HDFCBK", "Sent Rs.1,240.00 from HDFC Bank A/C **4921 to VPA swiggy@icici on 19-10-26. Ref 4293810293.") as ParseResult.Ok
        assertEquals("swiggy", ok.tx.merchantKey)
        val vpa = p("VM-HDFCBK", "Sent Rs.2,499.00 from HDFC Bank A/C **4921 to VPA merchant9823@okaxis on 19-10-26. Ref 4293810294.") as ParseResult.Review
        assertEquals("merchant9823", vpa.sms.merchantKey)
        val pos = p("BZ-ICICIB", "Dear Customer, your Acct XX8820 is debited with INR 850.00 on 18-Oct-26 at POS 883920 BLR. Avail Bal: INR 1,42,390.40") as ParseResult.Review
        assertEquals("pos883920", pos.sms.merchantKey)
    }
}
