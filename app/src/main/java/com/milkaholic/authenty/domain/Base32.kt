package com.milkaholic.authenty.domain

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(secret: String): ByteArray {
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
            if (value == -1) throw IllegalArgumentException("Invalid Base32 character: $char")

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                output.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }
}