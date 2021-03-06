/*
 * Copyright (c) 2020 Cossack Labs Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cossacklabs.themis.test

import java.nio.charset.StandardCharsets
import kotlin.experimental.inv

import com.cossacklabs.themis.*
import com.cossacklabs.themis.test.Assert.assertThrows

import org.junit.Assert.*
import org.junit.Test

class SecureCellSealTestKotlin {
    @Test
    fun initWithGenerated() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        assertNotNull(cell)
    }

    @Test
    fun initWithFixed() {
        val keyBase64 = "UkVDMgAAAC13PCVZAKOczZXUpvkhsC+xvwWnv3CLmlG0Wzy8ZBMnT+2yx/dg"
        val keyBytes = Base64.getDecoder().decode(keyBase64)
        val cell = SecureCell.SealWithKey(keyBytes)
        assertNotNull(cell)
    }

    @Test
    fun initWithEmpty() {
        assertThrows(NullArgumentException::class.java) {
            SecureCell.SealWithKey(null as SymmetricKey?)
        }
        assertThrows(NullArgumentException::class.java) {
            SecureCell.SealWithKey(null as ByteArray?)
        }
        assertThrows(InvalidArgumentException::class.java) {
            SecureCell.SealWithKey(byteArrayOf())
        }
    }

    @Test
    @Throws(SecureCellException::class)
    fun roundtrip() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)
        val context: ByteArray = "For great justice".toByteArray(StandardCharsets.UTF_8)

        val encrypted = cell.encrypt(message, context)
        assertNotNull(encrypted)

        val decrypted = cell.decrypt(encrypted, context)
        assertNotNull(decrypted)
        assertArrayEquals(message, decrypted)
    }

    @Test
    fun dataLengthExtension() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)

        val encrypted = cell.encrypt(message)
        assertTrue(encrypted.size > message.size)
    }

    @Test
    fun contextInclusion() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)
        val shortContext: ByteArray = ".".toByteArray(StandardCharsets.UTF_8)
        val longContext: ByteArray = "You have no chance to survive make your time. Ha ha ha ha ...".toByteArray(StandardCharsets.UTF_8)

        val encryptedShort = cell.encrypt(message, shortContext)
        val encryptedLong = cell.encrypt(message, longContext)

        // Context is not (directly) included into encrypted message.
        assertEquals(encryptedShort.size, encryptedLong.size)
    }

    @Test
    @Throws(SecureCellException::class)
    fun withoutContext() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)

        // Absent, empty, or nil context are all the same.
        val encrypted1 = cell.encrypt(message)
        val encrypted2 = cell.encrypt(message, null)
        val encrypted3 = cell.encrypt(message, byteArrayOf())

        assertArrayEquals(message, cell.decrypt(encrypted1))
        assertArrayEquals(message, cell.decrypt(encrypted2))
        assertArrayEquals(message, cell.decrypt(encrypted3))
        assertArrayEquals(message, cell.decrypt(encrypted1, null))
        assertArrayEquals(message, cell.decrypt(encrypted2, null))
        assertArrayEquals(message, cell.decrypt(encrypted3, null))
        assertArrayEquals(message, cell.decrypt(encrypted1, byteArrayOf()))
        assertArrayEquals(message, cell.decrypt(encrypted2, byteArrayOf()))
        assertArrayEquals(message, cell.decrypt(encrypted3, byteArrayOf()))
    }

    @Test
    @Throws(SecureCellException::class)
    fun contextSignificance() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)
        val correctContext: ByteArray = "We are CATS".toByteArray(StandardCharsets.UTF_8)
        val incorrectContext: ByteArray = "Captain !!".toByteArray(StandardCharsets.UTF_8)

        val encrypted = cell.encrypt(message, correctContext)

        // You cannot use a different context to decrypt data.
        assertThrows(SecureCellException::class.java) {
            cell.decrypt(encrypted, incorrectContext)
        }

        // Only the original context will work.
        val decrypted = cell.decrypt(encrypted, correctContext)
        assertArrayEquals(message, decrypted)
    }

    @Test
    fun detectCorruptedData() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)

        val encrypted = cell.encrypt(message)

        // Invert every odd byte, this will surely break the message.
        val corrupted = encrypted.copyOf(encrypted.size)
        for (i in corrupted.indices) {
            if (i % 2 == 1) {
                corrupted[i] = corrupted[i].inv()
            }
        }

        assertThrows(SecureCellException::class.java) {
            cell.decrypt(corrupted)
        }
    }

    @Test
    fun detectTruncatedData() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)

        val encrypted = cell.encrypt(message)

        val truncated = encrypted.copyOf(encrypted.size - 1)

        assertThrows(SecureCellException::class.java) {
            cell.decrypt(truncated)
        }
    }

    @Test
    fun detectExtendedData() {
        val cell = SecureCell.SealWithKey(SymmetricKey())
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)

        val encrypted = cell.encrypt(message)

        val extended = encrypted.copyOf(encrypted.size + 1)

        assertThrows(SecureCellException::class.java) {
            cell.decrypt(extended)
        }
    }

    @Test
    @Throws(SecureCellException::class)
    fun emptyMessage() {
        val cell = SecureCell.SealWithKey(SymmetricKey())

        assertThrows(NullArgumentException::class.java) { cell.encrypt(null) }
        assertThrows(NullArgumentException::class.java) { cell.decrypt(null) }

        assertThrows(InvalidArgumentException::class.java) { cell.encrypt(byteArrayOf()) }
        assertThrows(InvalidArgumentException::class.java) { cell.decrypt(byteArrayOf()) }
    }

    @Test
    @Suppress("DEPRECATION")
    @Throws(SecureCellException::class)
    fun oldAPI() {
        val key = SymmetricKey()
        val newCell = SecureCell.SealWithKey(key)
        val oldCell = SecureCell(key.toByteArray(), SecureCell.MODE_SEAL)
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)
        val context: ByteArray = "We are CATS".toByteArray(StandardCharsets.UTF_8)

        var encrypted: ByteArray
        var decrypted: ByteArray?
        val result = oldCell.protect(context, message)
        encrypted = result.protectedData
        assertNotNull(encrypted)
        decrypted = newCell.decrypt(encrypted, context)
        assertArrayEquals(message, decrypted)

        encrypted = newCell.encrypt(message, context)
        assertNotNull(encrypted)
        decrypted = oldCell.unprotect(context, SecureCellData(encrypted, null))
        assertArrayEquals(message, decrypted)
    }

    @Test
    @Suppress("DEPRECATION")
    @Throws(SecureCellException::class)
    fun oldAPIWithoutContext() {
        val key = SymmetricKey()
        val newCell = SecureCell.SealWithKey(key)
        val oldCell = SecureCell(key.toByteArray(), SecureCell.MODE_SEAL)
        val message: ByteArray = "All your base are belong to us!".toByteArray(StandardCharsets.UTF_8)

        var encrypted: ByteArray
        var decrypted: ByteArray?
        val result = oldCell.protect(null as ByteArray?, message)
        encrypted = result.protectedData
        assertNotNull(encrypted)
        decrypted = newCell.decrypt(encrypted)
        assertArrayEquals(message, decrypted)

        encrypted = newCell.encrypt(message)
        assertNotNull(encrypted)
        decrypted = oldCell.unprotect(null as ByteArray?, SecureCellData(encrypted, null))
        assertArrayEquals(message, decrypted)
    }
}
