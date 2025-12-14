package com.milkaholic.authenty.data

data class UserProfile(
    val id: String = generateProfileId(),
    val name: String,
    val iconResName: String = "default_profile", // For future icon customization
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val settings: ProfileSettings = ProfileSettings()
) {
    companion object {
        fun generateProfileId(): String = "profile_${System.currentTimeMillis()}_${(1000..9999).random()}"
        
        fun createDefaultProfile(): UserProfile = UserProfile(
            id = "default_profile",
            name = "Personal",
            isDefault = true
        )
    }
}

data class ProfileSettings(
    val autoLockEnabled: Boolean = true,
    val autoLockTimeoutMs: Long = 1 * 60 * 1000L, // 1 minute default
    val biometricEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val defaultCategory: String = "Personal"
)

data class AccountWithProfile(
    val account: AccountModel,
    val profileId: String,
    val category: String = "Personal",
    val addedAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)

enum class AccountCategory(val displayName: String, val colorHex: String) {
    PERSONAL("Personal", "#2196F3"),     // Blue
    WORK("Work", "#FF9800"),            // Orange  
    SOCIAL("Social Media", "#9C27B0"),  // Purple
    GAMING("Gaming", "#4CAF50"),        // Green
    FINANCE("Finance", "#F44336"),      // Red
    SHOPPING("Shopping", "#FF5722"),    // Deep Orange
    EDUCATION("Education", "#3F51B5"),  // Indigo
    HEALTH("Health", "#E91E63"),        // Pink
    CUSTOM("Custom", "#607D8B");        // Blue Grey
    
    companion object {
        fun fromString(category: String): AccountCategory {
            return values().find { it.displayName.equals(category, ignoreCase = true) }
                ?: PERSONAL
        }
        
        fun getAllCategories(): List<AccountCategory> = values().toList()
    }
}