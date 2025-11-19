package com.movieroulette.app.utils

import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object E2EEncryption {
    
    private const val RSA_ALGORITHM = "RSA"
    private const val AES_ALGORITHM = "AES"
    private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val RSA_KEY_SIZE = 2048
    private const val AES_KEY_SIZE = 256
    
    /**
     * Genera un par de claves RSA (pública y privada)
     */
    fun generateRSAKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        keyPairGenerator.initialize(RSA_KEY_SIZE, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }
    
    /**
     * Convierte una clave pública a String Base64
     */
    fun publicKeyToString(publicKey: PublicKey): String {
        return Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
    }
    
    /**
     * Convierte un String Base64 a clave pública
     */
    fun stringToPublicKey(keyString: String): PublicKey {
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
        val keyFactory = java.security.KeyFactory.getInstance(RSA_ALGORITHM)
        return keyFactory.generatePublic(keySpec)
    }
    
    /**
     * Convierte una clave privada a String Base64
     */
    fun privateKeyToString(privateKey: PrivateKey): String {
        return Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
    }
    
    /**
     * Convierte un String Base64 a clave privada
     */
    fun stringToPrivateKey(keyString: String): PrivateKey {
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = java.security.KeyFactory.getInstance(RSA_ALGORITHM)
        return keyFactory.generatePrivate(keySpec)
    }
    
    /**
     * Cifra un mensaje usando AES con una clave simétrica temporal
     * y luego cifra la clave AES con RSA
     */
    fun encryptMessage(message: String, recipientPublicKey: PublicKey): EncryptedMessage {
        // Generar clave AES temporal
        val aesKey = generateAESKey()
        
        // Generar IV aleatorio
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        
        // Cifrar mensaje con AES
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(iv))
        val encryptedBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
        
        // Cifrar clave AES con RSA
        val rsaCipher = Cipher.getInstance(RSA_ALGORITHM)
        rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
        val encryptedKey = rsaCipher.doFinal(aesKey.encoded)
        
        return EncryptedMessage(
            encryptedContent = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            encryptedKey = Base64.encodeToString(encryptedKey, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }
    
    /**
     * Descifra un mensaje usando la clave privada RSA
     */
    fun decryptMessage(encryptedMessage: EncryptedMessage, privateKey: PrivateKey): String {
        // Descifrar clave AES con RSA
        val rsaCipher = Cipher.getInstance(RSA_ALGORITHM)
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
        val encryptedKeyBytes = Base64.decode(encryptedMessage.encryptedKey, Base64.NO_WRAP)
        val aesKeyBytes = rsaCipher.doFinal(encryptedKeyBytes)
        val aesKey = SecretKeySpec(aesKeyBytes, AES_ALGORITHM)
        
        // Descifrar mensaje con AES
        val iv = Base64.decode(encryptedMessage.iv, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
        val encryptedBytes = Base64.decode(encryptedMessage.encryptedContent, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        
        return String(decryptedBytes, Charsets.UTF_8)
    }
    
    /**
     * Genera una clave AES temporal
     */
    private fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGenerator.init(AES_KEY_SIZE, SecureRandom())
        return keyGenerator.generateKey()
    }
}

/**
 * Representa un mensaje cifrado con todos sus componentes
 */
data class EncryptedMessage(
    val encryptedContent: String,  // Contenido cifrado con AES (Base64)
    val encryptedKey: String,      // Clave AES cifrada con RSA (Base64)
    val iv: String                 // Vector de inicialización (Base64)
)
