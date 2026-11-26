package com.mochyy.mochymusic.importer

import com.mochyy.mochymusic.importer.transfer.ImportByteBudget
import com.mochyy.mochymusic.importer.transfer.readBoundedManifest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportByteBudgetTest {
    @Test fun validManifestAndBudget() {
        val bytes = "test".encodeToByteArray()
        assertEquals("test", readBoundedManifest(bytes.inputStream()).decodeToString())
        ImportByteBudget(128L * 1024 * 1024, 1024).consume(1024)
    }

    @Test(expected = IllegalStateException::class)
    fun extractionCannotConsumeReservedSpace() {
        ImportByteBudget(64L * 1024 * 1024 + 10).apply { consume(10); consume(1) }
    }

    @Test(expected = IllegalStateException::class)
    fun oversizedManifestRejected() {
        readBoundedManifest(ByteArray(1024 * 1024 + 1).inputStream())
    }
}
