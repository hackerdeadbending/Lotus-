package app.lotus.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Per-install end-to-end message encryption.
 * The private ECDH key is encrypted with an Android Keystore AES key and never uploaded.
 */
object LotusCrypto {
    private const val PREFS = "lotus_crypto"
    private const val PUBLIC_KEY = "ecdh_public_key"
    private const val PRIVATE_KEY_ENCRYPTED = "ecdh_private_key_encrypted"
    private const val MASTER_ALIAS = "lotus_e2ee_storage_v1"
    private const val PREFIX = "LOTUS1:"
    private val keyStore: KeyStore
        get() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreatePublicKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(PUBLIC_KEY, null)?.let { existing ->
            if (prefs.getString(PRIVATE_KEY_ENCRYPTED, null) != null) {
                loadPrivateKey(context)
                return existing
            }
        }

        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val pair = generator.generateKeyPair()
        val encryptedPrivate = encryptPrivateKey(context, pair.private.encoded)
        val publicEncoded = Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
        prefs.edit()
            .putString(PUBLIC_KEY, publicEncoded)
            .putString(PRIVATE_KEY_ENCRYPTED, encryptedPrivate)
            .apply()
        return publicEncoded
    }

    fun encrypt(context: Context, conversationId: String, peerPublicKey: String?, plaintext: String): String {
        require(plaintext.isNotBlank()) { "Сообщение пустое" }
        require(plaintext.length <= 4000) { "Сообщение слишком длинное" }
        val peer = peerPublicKey?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("У собеседника ещё нет ключа шифрования. Попросите его открыть Lotus с обновлённой версией.")
        val key = deriveConversationKey(context, conversationId, peer)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(context: Context, conversationId: String, peerPublicKey: String?, value: String): String {
        if (!value.startsWith(PREFIX)) return value // legacy plaintext messages from pre-E2EE builds
        val peer = peerPublicKey?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Нет открытого ключа собеседника")
        val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
        require(payload.size > 12 + 16) { "Повреждённое зашифрованное сообщение" }
        val nonce = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveConversationKey(context, conversationId, peer), GCMParameterSpec(128, nonce))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveConversationKey(context: Context, conversationId: String, peerPublicKey: String): SecretKeySpec {
        require(conversationId.isNotBlank()) { "Нет идентификатора чата" }
        val privateKey = loadPrivateKey(context)
        val publicBytes = Base64.decode(peerPublicKey, Base64.NO_WRAP)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicBytes))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val sharedSecret = agreement.generateSecret()

        val salt = "Lotus-E2EE-v1".toByteArray(Charsets.UTF_8)
        val extract = Mac.getInstance("HmacSHA256")
        extract.init(SecretKeySpec(salt, "HmacSHA256"))
        val pseudoRandomKey = extract.doFinal(sharedSecret)

        val expand = Mac.getInstance("HmacSHA256")
        expand.init(SecretKeySpec(pseudoRandomKey, "HmacSHA256"))
        val info = ("lotus-conversation:" + conversationId).toByteArray(Charsets.UTF_8) + byteArrayOf(1)
        val keyBytes = expand.doFinal(info).copyOf(32)
        sharedSecret.fill(0)
        pseudoRandomKey.fill(0)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun loadPrivateKey(context: Context): java.security.PrivateKey {
        val encrypted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PRIVATE_KEY_ENCRYPTED, null)
            ?: throw IllegalStateException("Ключ шифрования не создан")
        val payload = Base64.decode(encrypted, Base64.NO_WRAP)
        require(payload.size > 12) { "Повреждён локальный ключ шифрования" }
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        val privateBytes = cipher.doFinal(ciphertext)
        return try {
            KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(privateBytes))
        } finally {
            privateBytes.fill(0)
        }
    }

    private fun encryptPrivateKey(context: Context, privateBytes: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val ciphertext = cipher.doFinal(privateBytes)
        privateBytes.fill(0)
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun masterKey(): java.security.Key {
        val store = keyStore
        store.getKey(MASTER_ALIAS, null)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                MASTER_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
