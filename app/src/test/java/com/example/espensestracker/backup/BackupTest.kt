package com.example.espensestracker.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.GeneralSecurityException

class BackupTest {
    private val plain = "{\"hello\":\"world\"}".toByteArray()

    @Test fun encryptDecryptRoundTrip() {
        val enc = Backup.encrypt(plain, "secret")
        assertTrue(Backup.isEncrypted(enc))
        assertFalse(Backup.isEncrypted(plain))
        assertArrayEquals(plain, Backup.decrypt(enc, "secret"))
    }

    @Test fun wrongPasswordFails() {
        val enc = Backup.encrypt(plain, "secret")
        try {
            Backup.decrypt(enc, "wrong")
            fail("expected failure")
        } catch (e: GeneralSecurityException) { /* expected */ }
    }
}
