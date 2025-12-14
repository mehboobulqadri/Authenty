package com.milkaholic.authenty.domain

import java.time.Instant

object TokenGenerator {

    // Generates the TOTP code with enhanced support
    fun generateTOTP(
        secret: String, 
        algorithm: String = "SHA1", 
        digits: Int = 6, 
        period: Int = 30,
        time: Long = System.currentTimeMillis()
    ): String {
        try {
            val config = EnhancedCryptoManager.OtpConfig.createTotpConfig(
                algorithm = EnhancedCryptoManager.Algorithm.fromString(algorithm),
                digits = digits,
                period = period
            )
            return EnhancedCryptoManager.generateTotpCode(secret, config, time)
        } catch (e: Exception) {
            e.printStackTrace()
            return "0".repeat(digits)
        }
    }

    // Calculates how much time is left (0.0 to 1.0) for the progress bar
    fun getProgress(period: Int = 30): Float {
        val time = System.currentTimeMillis() / 1000
        val secondsPast = time % period
        return 1f - (secondsPast / period.toFloat())
    }
}
