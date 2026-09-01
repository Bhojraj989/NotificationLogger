package com.nlite.notiflogger

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts/decrypts the notification log using AES. The log file on disk is
 * never stored as plain text — if opened with any other app (file manager,
 * text editor, etc.) it just shows unreadable bytes. Only this app, with the
 * correct passcode, can turn it back into readable text.
 */
object CryptoUtils {

    // The passcode required to unlock the in-app log viewer/editor.
    private const val PASSCODE = "3232"

    private const val IV_LENGTH = 16

    fun verifyPasscode(input: String): Boolean = input == PASSCODE

    private fun secretKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256").digest(PASSCODE.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    /** Encrypts [plainText] and overwrites [file] with the encrypted bytes. */
    fun encryptToFile(file: File, plainText: String) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        file.writeBytes(iv + encrypted)
    }

    /**
     * Reads and decrypts [file]. Returns an empty string if the file doesn't
     * exist yet, or if it's not valid encrypted data (e.g. someone tried to
     * edit it outside the app and broke the format).
     */
    fun decryptFile(file: File): String {
        if (!file.exists() || file.length() <= IV_LENGTH) return ""
        return try {
            val data = file.readBytes()
            val iv = data.copyOfRange(0, IV_LENGTH)
            val cipherText = data.copyOfRange(IV_LENGTH, data.size)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), IvParameterSpec(iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
