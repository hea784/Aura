package com.heasafe.aura.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 数据库静态加密密钥管理：随机 passphrase 由 Android Keystore 的 AES-GCM 密钥包裹后落盘，
 * passphrase 本身不以明文存储；Keystore 密钥不可导出，提升本地数据静态安全。
 */
object DbKeyStore {
    private const val ALIAS = "aura-db-key"
    private const val FILE = "db.pass"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    fun passphrase(context: Context): ByteArray {
        val f = File(context.filesDir, FILE)
        val k = key()
        if (f.exists()) {
            return try {
                val blob = f.readBytes()
                val iv = blob.copyOfRange(0, 12)
                val ct = blob.copyOfRange(12, blob.size)
                Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(128, iv))
                }.doFinal(ct)
            } catch (_: Exception) {
                regenerate(f, k)
            }
        }
        return regenerate(f, k)
    }

    private fun regenerate(f: File, k: SecretKey): ByteArray {
        val pass = SecureRandom().generateSeed(32)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, k) }
        val ct = c.doFinal(pass)
        f.writeBytes(c.iv + ct)
        return pass
    }
}
