package com.milkaholic.authenty.data

data class AccountModel(
    val id: Long = System.currentTimeMillis(), // Unique ID
    val name: String,       // e.g. "user@gmail.com"
    val issuer: String,     // e.g. "Google"
    val secret: String,      // The Base32 Key
    val algorithm: String? = "SHA1",
    val digits: Int? = 6,
    val period: Int? = 30,
    val backupCodes: List<String>? = null
) {
    // Safe accessors for GSON compatibility
    val safeAlgorithm: String get() = algorithm ?: "SHA1"
    val safeDigits: Int get() = if (digits == null || digits == 0) 6 else digits
    val safePeriod: Int get() = if (period == null || period == 0) 30 else period
    val safeBackupCodes: List<String> get() = backupCodes ?: emptyList()
}