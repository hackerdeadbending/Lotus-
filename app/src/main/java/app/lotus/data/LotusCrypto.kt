package app.lotus.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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

/**
 * Per-account, per-install ECDH key storage and AES-GCM message encryption.
 * The private key is encrypted by an Android Keystore AES key and never uploaded.
 */
object LotusCrypto {
    private const val PREFIX = "LOTUS1:"
    private val keyStore: KeyStore
        get() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreatePublicKey(context: Context, userId: String): String {
        require(userId.isNotBlank()) { "Не удалось определить аккаунт Lotus" }
        val prefs = preferences(context, userId)
        prefs.getString("ecdh_public_key", null)?.let { existing ->
            if (prefs.getString("ecdh_private_key_encrypted", null) != null) {
                loadPrivateKey(context, userId)
                return existing
            }
        }

        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val pair = generator.generateKeyPair()
        val encryptedPrivate = encryptPrivateKey(userId, pair.private.encoded)
        val publicEncoded = Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
        prefs.edit()
            .putString("ecdh_public_key", publicEncoded)
            .putString("ecdh_private_key_encrypted", encryptedPrivate)
            .apply()
        return publicEncoded
    }

    fun encrypt(
        context: Context,
        userId: String,
        conversationId: String,
        peerPublicKey: String?,
        plaintext: String
    ): String {
        require(plaintext.isNotBlank()) { "Сообщение пустое" }
        require(plaintext.length <= 4000) { "Сообщение слишком длинное" }
        val peer = peerPublicKey?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("У собеседника ещё нет ключа шифрования. Попросите его открыть Lotus с обновлённой версией.")
        val key = deriveConversationKey(context, userId, conversationId, peer)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(
        context: Context,
        userId: String,
        conversationId: String,
        peerPublicKey: String?,
        value: String
    ): String {
        if (!value.startsWith(PREFIX)) return value // legacy plaintext messages from pre-E2EE builds
        val peer = peerPublicKey?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Нет открытого ключа собеседника")
        val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
        require(payload.size > 12 + 16) { "Повреждённое зашифрованное сообщение" }
        val nonce = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            deriveConversationKey(context, userId, conversationId, peer),
            GCMParameterSpec(128, nonce)
        )
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveConversationKey(
        context: Context,
        userId: String,
        conversationId: String,
        peerPublicKey: String
    ): SecretKeySpec {
        require(conversationId.isNotBlank()) { "Нет идентификатора чата" }
        val privateKey = loadPrivateKey(context, userId)
        val publicBytes = Base64.decode(peerPublicKey, Base64.NO_WRAP)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicBytes))
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val sharedSecret = agreement.generateSecret()

        val extract = Mac.getInstance("HmacSHA256")
        extract.init(SecretKeySpec("Lotus-E2EE-v1".toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val pseudoRandomKey = extract.doFinal(sharedSecret)

        val expand = Mac.getInstance("HmacSHA256")
        expand.init(SecretKeySpec(pseudoRandomKey, "HmacSHA256"))
        val info = ("lotus-conversation:" + conversationId).toByteArray(Charsets.UTF_8) + byteArrayOf(1)
        val keyBytes = expand.doFinal(info).copyOf(32)
        sharedSecret.fill(0)
        pseudoRandomKey.fill(0)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun loadPrivateKey(context: Context, userId: String): java.security.PrivateKey {
        val encrypted = preferences(context, userId).getString("ecdh_private_key_encrypted", null)
            ?: throw IllegalStateException("Ключ шифрования не создан")
        val payload = Base64.decode(encrypted, Base64.NO_WRAP)
        require(payload.size > 12) { "Повреждён локальный ключ шифрования" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            masterKey(userId),
            GCMParameterSpec(128, payload.copyOfRange(0, 12))
        )
        val privateBytes = cipher.doFinal(payload.copyOfRange(12, payload.size))
        return try {
            KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(privateBytes))
        } finally {
            privateBytes.fill(0)
        }
    }

    private fun encryptPrivateKey(userId: String, privateBytes: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey(userId))
        val ciphertext = cipher.doFinal(privateBytes)
        privateBytes.fill(0)
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun masterKey(userId: String): java.security.Key {
        val alias = "lotus_e2ee_" + userId.replace("-", "").take(32)
        val store = keyStore
        store.getKey(alias, null)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun preferences(context: Context, userId: String) =
        context.getSharedPreferences("lotus_crypto_${userId.replace("-", "")}", Context.MODE_PRIVATE)
}
