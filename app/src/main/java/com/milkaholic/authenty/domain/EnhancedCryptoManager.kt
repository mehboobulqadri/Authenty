package com.milkaholic.authenty.domain

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * Enhanced cryptographic manager supporting multiple TOTP/HOTP algorithms
 * Supports SHA1, SHA256, SHA512 with custom time intervals and digit lengths
 */
object EnhancedCryptoManager {
    
    enum class Algorithm(val value: String) {
        SHA1("HmacSHA1"),
        SHA256("HmacSHA256"),
        SHA512("HmacSHA512");
        
        companion object {
            fun fromString(algorithm: String): Algorithm {
                return when (algorithm.uppercase()) {
                    "SHA1", "SHA-1" -> SHA1
                    "SHA256", "SHA-256" -> SHA256
                    "SHA512", "SHA-512" -> SHA512
                    else -> SHA1 // Default fallback
                }
            }
        }
    }
    
    enum class TokenType {
        TOTP, // Time-based One-Time Password
        HOTP  // HMAC-based One-Time Password (counter-based)
    }
    
    data class OtpConfig(
        val algorithm: Algorithm = Algorithm.SHA1,
        val digits: Int = 6, // 6 or 8 digits
        val period: Int = 30, // Time period in seconds (15, 30, 60, etc.)
        val tokenType: TokenType = TokenType.TOTP,
        val counter: Long = 0L // Only used for HOTP
    ) {
        companion object {
            fun createDefault(): OtpConfig = OtpConfig()
            
            fun createTotpConfig(
                algorithm: Algorithm = Algorithm.SHA1,
                digits: Int = 6,
                period: Int = 30
            ): OtpConfig = OtpConfig(
                algorithm = algorithm,
                digits = digits,
                period = period,
                tokenType = TokenType.TOTP
            )
            
            fun createHotpConfig(
                algorithm: Algorithm = Algorithm.SHA1,
                digits: Int = 6,
                initialCounter: Long = 0L
            ): OtpConfig = OtpConfig(
                algorithm = algorithm,
                digits = digits,
                period = 30, // Not used for HOTP but kept for consistency
                tokenType = TokenType.HOTP,
                counter = initialCounter
            )
        }
        
        fun isValid(): Boolean {
            return digits in 6..8 && 
                   period in listOf(15, 30, 60, 120) &&
                   (tokenType != TokenType.HOTP || counter >= 0)
        }
    }
    
    /**
     * Enhanced Account Model with extended crypto support
     */
    data class EnhancedAccountModel(
        val id: Long = System.currentTimeMillis(),
        val name: String,
        val issuer: String,
        val secret: String,
        val config: OtpConfig = OtpConfig.createDefault(),
        val createdAt: Long = System.currentTimeMillis(),
        val lastUsed: Long = System.currentTimeMillis()
    ) {
        fun toBasicAccountModel(): com.milkaholic.authenty.data.AccountModel {
            return com.milkaholic.authenty.data.AccountModel(
                id = id,
                name = name,
                issuer = issuer,
                secret = secret
            )
        }
        
        companion object {
            fun fromBasicAccount(account: com.milkaholic.authenty.data.AccountModel): EnhancedAccountModel {
                return EnhancedAccountModel(
                    id = account.id,
                    name = account.name,
                    issuer = account.issuer,
                    secret = account.secret,
                    config = OtpConfig.createTotpConfig(
                        algorithm = Algorithm.fromString(account.safeAlgorithm),
                        digits = account.safeDigits,
                        period = account.safePeriod
                    )
                )
            }
        }
    }
    
    /**
     * Generate TOTP code with enhanced configuration
     */
    fun generateTotpCode(
        secret: String,
        config: OtpConfig = OtpConfig.createDefault(),
        timeStamp: Long = System.currentTimeMillis()
    ): String {
        require(config.tokenType == TokenType.TOTP) { "Config must be for TOTP" }
        require(config.isValid()) { "Invalid OTP configuration" }
        
        val timeSlot = timeStamp / 1000 / config.period
        return generateOtpCode(secret, timeSlot, config)
    }
    
    /**
     * Generate HOTP code with counter
     */
    fun generateHotpCode(
        secret: String,
        counter: Long,
        config: OtpConfig = OtpConfig.createHotpConfig()
    ): String {
        require(config.tokenType == TokenType.HOTP) { "Config must be for HOTP" }
        require(config.isValid()) { "Invalid OTP configuration" }
        
        return generateOtpCode(secret, counter, config)
    }
    
    /**
     * Core OTP generation algorithm
     */
    private fun generateOtpCode(secret: String, counter: Long, config: OtpConfig): String {
        try {
            // Decode the Base32 secret
            val decodedSecret = decodeBase32(secret)
            
            // Convert counter to byte array
            val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
            
            // Generate HMAC
            val mac = Mac.getInstance(config.algorithm.value)
            val keySpec = SecretKeySpec(decodedSecret, config.algorithm.value)
            mac.init(keySpec)
            val hash = mac.doFinal(counterBytes)
            
            // Dynamic truncation
            val offset = (hash.last().toInt() and 0x0F)
            val truncatedHash = ByteBuffer.wrap(hash, offset, 4).int and 0x7FFFFFFF
            
            // Generate the final code
            val modulus = 10.0.pow(config.digits.toDouble()).toInt()
            val code = truncatedHash % modulus
            
            return code.toString().padStart(config.digits, '0')
            
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to generate OTP: ${e.message}", e)
        }
    }
    
    /**
     * Get remaining time for current TOTP period
     */
    fun getRemainingTime(config: OtpConfig, currentTime: Long = System.currentTimeMillis()): Int {
        require(config.tokenType == TokenType.TOTP) { "Only applicable for TOTP" }
        
        val currentTimeSlot = (currentTime / 1000) % config.period
        return config.period - currentTimeSlot.toInt()
    }
    
    /**
     * Check if TOTP code is about to expire (within 5 seconds)
     */
    fun isCodeExpiringSoon(config: OtpConfig, currentTime: Long = System.currentTimeMillis()): Boolean {
        require(config.tokenType == TokenType.TOTP) { "Only applicable for TOTP" }
        return getRemainingTime(config, currentTime) <= 5
    }
    
    /**
     * Generate next TOTP code (for preview)
     */
    fun generateNextTotpCode(
        secret: String,
        config: OtpConfig = OtpConfig.createDefault(),
        currentTime: Long = System.currentTimeMillis()
    ): String {
        require(config.tokenType == TokenType.TOTP) { "Config must be for TOTP" }
        
        val nextPeriodTime = currentTime + (config.period * 1000)
        return generateTotpCode(secret, config, nextPeriodTime)
    }
    
    /**
     * Parse OTP Auth URI to extract enhanced configuration
     */
    fun parseOtpAuthUri(uri: String): EnhancedAccountModel? {
        try {
            if (!uri.startsWith("otpauth://")) return null
            
            val parts = uri.substringAfter("otpauth://").split("?")
            if (parts.size < 2) return null
            
            val typePath = parts[0].split("/")
            if (typePath.size < 2) return null
            
            val tokenType = when (typePath[0].lowercase()) {
                "totp" -> TokenType.TOTP
                "hotp" -> TokenType.HOTP
                else -> return null
            }
            
            val label = java.net.URLDecoder.decode(typePath[1], "UTF-8")
            val params = parseQueryParams(parts[1])
            
            val secret = params["secret"] ?: return null
            val issuer = params["issuer"] ?: label.substringBefore(":")
            val name = if (label.contains(":")) label.substringAfter(":") else label
            
            val algorithm = Algorithm.fromString(params["algorithm"] ?: "SHA1")
            val digits = params["digits"]?.toIntOrNull() ?: 6
            val period = params["period"]?.toIntOrNull() ?: 30
            val counter = params["counter"]?.toLongOrNull() ?: 0L
            
            val config = if (tokenType == TokenType.TOTP) {
                OtpConfig.createTotpConfig(algorithm, digits, period)
            } else {
                OtpConfig.createHotpConfig(algorithm, digits, counter)
            }
            
            return EnhancedAccountModel(
                name = name,
                issuer = issuer,
                secret = secret.uppercase(),
                config = config
            )
            
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Generate OTP Auth URI from enhanced account
     */
    fun generateOtpAuthUri(account: EnhancedAccountModel): String {
        val typeString = when (account.config.tokenType) {
            TokenType.TOTP -> "totp"
            TokenType.HOTP -> "hotp"
        }
        
        val label = "${account.issuer}:${account.name}"
        val encodedLabel = java.net.URLEncoder.encode(label, "UTF-8")
        
        val params = mutableMapOf<String, String>().apply {
            put("secret", account.secret)
            put("issuer", account.issuer)
            if (account.config.algorithm != Algorithm.SHA1) {
                put("algorithm", account.config.algorithm.name)
            }
            if (account.config.digits != 6) {
                put("digits", account.config.digits.toString())
            }
            if (account.config.tokenType == TokenType.TOTP && account.config.period != 30) {
                put("period", account.config.period.toString())
            }
            if (account.config.tokenType == TokenType.HOTP) {
                put("counter", account.config.counter.toString())
            }
        }
        
        val queryString = params.map { "${it.key}=${it.value}" }.joinToString("&")
        return "otpauth://$typeString/$encodedLabel?$queryString"
    }
    
    /**
     * Validate Base32 secret and normalize it
     */
    fun validateAndNormalizeSecret(secret: String): String {
        val cleaned = secret.trim()
            .replace(" ", "")
            .replace("-", "")
            .uppercase()
        
        // Validate Base32 characters
        if (!cleaned.matches(Regex("[A-Z2-7=]*"))) {
            throw IllegalArgumentException("Invalid Base32 secret")
        }
        
        // Validate length
        if (cleaned.length < 16) {
            throw IllegalArgumentException("Secret too short (minimum 16 characters)")
        }
        
        if (cleaned.length > 128) {
            throw IllegalArgumentException("Secret too long (maximum 128 characters)")
        }
        
        return cleaned
    }
    
    /**
     * Generate random Base32 secret for testing
     */
    fun generateRandomSecret(length: Int = 32): String {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val random = SecureRandom()
        return (1..length)
            .map { base32Chars[random.nextInt(base32Chars.length)] }
            .joinToString("")
    }
    
    // Private helper methods
    private fun decodeBase32(base32: String): ByteArray {
        val cleanInput = base32.replace("=", "").uppercase()
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        
        var bits = 0L
        var value = 0
        var index = 0
        val output = mutableListOf<Byte>()
        
        for (char in cleanInput) {
            val charIndex = alphabet.indexOf(char)
            if (charIndex < 0) continue
            
            value = (value shl 5) or charIndex
            bits += 5
            
            if (bits >= 8) {
                output.add((value shr (bits.toInt() - 8)).toByte())
                value = value and ((1 shl (bits.toInt() - 8)) - 1)
                bits -= 8
            }
        }
        
        return output.toByteArray()
    }
    
    private fun parseQueryParams(query: String): Map<String, String> {
        return query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            val key = java.net.URLDecoder.decode(parts[0], "UTF-8")
            val value = if (parts.size > 1) {
                java.net.URLDecoder.decode(parts[1], "UTF-8")
            } else ""
            key to value
        }
    }
}