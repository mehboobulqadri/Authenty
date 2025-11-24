package com.milkaholic.authenty.domain

import java.nio.ByteBuffer
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TokenGenerator {

    // Generates the 6-digit code
    fun generateTOTP(secret: String, time: Long = Instant.now().epochSecond): String {
        try {
            val keyBytes = Base32.decode(secret)

            // Time step is 30 seconds
            val interval = time / 30

            val data = ByteBuffer.allocate(8).putLong(interval).array()

            val signKey = SecretKeySpec(keyBytes, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signKey)
            val hash = mac.doFinal(data)

            val offset = hash[hash.size - 1].toInt() and 0xF
            val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)

            val otp = binary % 1_000_000

            return otp.toString().padStart(6, '0')
        } catch (e: Exception) {
            e.printStackTrace()
            return "000000"
        }
    }

    // Calculates how much time is left (0.0 to 1.0) for the progress bar
    fun getProgress(): Float {
        val time = Instant.now().epochSecond
        val secondsPast = time % 30
        return 1f - (secondsPast / 30f)
    }
}