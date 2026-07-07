package com.example.dindoripranityadnyiki.core.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/**
 * Security utilities for client-side encryption using Android Keystore.
 *
 * This implementation provides AES-GCM encryption for sensitive data.
 * Keys are stored securely in the Android Keystore system.
 *
 * Security Architecture:
 * - Client-side: AES-256-GCM encryption for sensitive fields (optional, for extra protection)
 * - Transport: HTTPS with TLS 1.2+
 * - Server-side: Firestore security rules enforce access control
 * - Data at rest: Firestore encryption handled by Firebase
 *
 * Usage:
 * - Use encrypt() for sensitive data before storing locally or transmitting
 * - Use decrypt() to retrieve sensitive data
 * - Keys are non-exportable and tied to device hardware (Keymaster)
 */
object SecurityUtils {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "DindoriPranit_EncryptionKey"
    private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val GCM_TAG_LENGTH = 128

    /**
     * Encrypts a plaintext string using AES-GCM.
     *
     * @param plaintext The data to encrypt
     * @return Base64-encoded string containing IV + ciphertext
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return plaintext

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getOrCreateSecretKey()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

            // Combine IV and ciphertext, then Base64 encode
            val combined = iv + ciphertext
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed: ${e.message}", e)
        }
    }

    /**
     * Decrypts a Base64-encoded string containing IV + ciphertext.
     *
     * @param encrypted Base64-encoded string from encrypt()
     * @return Decrypted plaintext
     */
    fun decrypt(encrypted: String): String {
        if (encrypted.isBlank()) return encrypted

        try {
            val combined = Base64.decode(encrypted, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = getOrCreateSecretKey()

            // Extract IV (first 12 bytes for GCM)
            val iv = combined.sliceArray(0..11)
            val ciphertext = combined.sliceArray(12 until combined.size)

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plaintext = cipher.doFinal(ciphertext)

            return String(plaintext, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed: ${e.message}", e)
        }
    }

    /**
     * Gets or creates a secret key in the Android Keystore.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        // Create new key
        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, ANDROID_KEYSTORE)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Checks if the encryption key exists in the keystore.
     */
    fun isKeyAvailable(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
}
