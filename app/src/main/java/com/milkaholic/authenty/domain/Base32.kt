package com.milkaholic.authenty.domain

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /**
     * Legacy decode method for backward compatibility
     */
    fun decode(secret: String): ByteArray {
        val result = decodeWithResult(secret)
        return when (result) {
            is AuthentyResult.Success -> result.data
            is AuthentyResult.Error -> throw IllegalArgumentException(result.error.message)
        }
    }
    
    /**
     * Enhanced decode method that returns detailed error information
     */
    fun decodeWithResult(secret: String): AuthentyResult<ByteArray> {
        try {
            // Validate input first
            ValidationUtils.validateBase32Secret(secret)?.let { error ->
                return AuthentyResult.Error(error)
            }
            
            // Clean the input: remove spaces, dashes, convert to uppercase
            val cleanSecret = secret.trim().replace(" ", "").uppercase()
                .replace("-", "")

            // Remove standard padding '=' characters
            val formatted = cleanSecret.trimEnd('=')

            val output = ArrayList<Byte>()
            var buffer = 0
            var bitsLeft = 0

            for (char in formatted) {
                val value = ALPHABET.indexOf(char)
                if (value == -1) {
                    return AuthentyResult.Error(AuthentyError.InvalidBase32Format(secret))
                }

                buffer = (buffer shl 5) or value
                bitsLeft += 5

                if (bitsLeft >= 8) {
                    output.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                    bitsLeft -= 8
                }
            }
            
            return AuthentyResult.Success(output.toByteArray())
        } catch (e: Exception) {
            return AuthentyResult.Error(AuthentyError.UnknownError(e.message ?: "Unknown Base32 decoding error"))
        }
    }
    
    /**
     * Validate if a string is a valid Base32 secret
     */
    fun isValidBase32(secret: String): Boolean {
        return ValidationUtils.validateBase32Secret(secret) == null
    }
}