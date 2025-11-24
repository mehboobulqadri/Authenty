package com.milkaholic.authenty.data

data class AccountModel(
    val id: Long = System.currentTimeMillis(), // Unique ID
    val name: String,       // e.g. "user@gmail.com"
    val issuer: String,     // e.g. "Google"
    val secret: String      // The Base32 Key
)