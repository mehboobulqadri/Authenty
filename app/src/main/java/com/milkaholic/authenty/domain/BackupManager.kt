package com.milkaholic.authenty.domain

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.milkaholic.authenty.data.AccountWithProfile
import com.milkaholic.authenty.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupManager(
    private val context: Context,
    private val profileManager: ProfileManager
) {

    private val gson = com.google.gson.GsonBuilder().enableComplexMapKeySerialization().create()

    data class BackupData(
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis(),
        val profiles: List<UserProfile>,
        val accounts: Map<String, List<AccountWithProfile>>
    )

    suspend fun exportBackup(password: String, uri: Uri): AuthentyResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (password.isBlank()) {
                    return@withContext AuthentyResult.Error(AuthentyError.ValidationError("Password cannot be empty"))
                }

                // Gather data
                val profiles = profileManager.getAllProfiles()
                if (profiles.isEmpty()) {
                    return@withContext AuthentyResult.Error(AuthentyError.ValidationError("No profiles to export"))
                }

                val accountsMap = profiles.associate { profile ->
                    profile.id to profileManager.getAccountsForProfile(profile.id)
                }

                val backupData = BackupData(
                    profiles = profiles,
                    accounts = accountsMap
                )

                val json = gson.toJson(backupData)
                if (json.isBlank()) {
                    return@withContext AuthentyResult.Error(AuthentyError.UnknownError("Failed to serialize backup data"))
                }

                val encryptedData = encrypt(json, password)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(encryptedData)
                    outputStream.flush()
                } ?: return@withContext AuthentyResult.Error(AuthentyError.UnknownError("Could not open file for writing"))

                AuthentyResult.Success(Unit)
            } catch (e: SecurityException) {
                e.printStackTrace()
                AuthentyResult.Error(AuthentyError.UnknownError("Security error: ${e.message}"))
            } catch (e: java.io.IOException) {
                e.printStackTrace()
                AuthentyResult.Error(AuthentyError.UnknownError("File write error: ${e.message}"))
            } catch (e: Exception) {
                e.printStackTrace()
                AuthentyResult.Error(AuthentyError.UnknownError("Export failed: ${e.message}"))
            }
        }
    }

    suspend fun importBackup(password: String, uri: Uri): AuthentyResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (password.isBlank()) {
                    return@withContext AuthentyResult.Error(AuthentyError.ValidationError("Password cannot be empty"))
                }

                val encryptedData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: return@withContext AuthentyResult.Error(AuthentyError.UnknownError("Could not read file"))

                if (encryptedData.isEmpty()) {
                    return@withContext AuthentyResult.Error(AuthentyError.UnknownError("Backup file is empty"))
                }

                val json = try {
                    decrypt(encryptedData, password)
                } catch (e: javax.crypto.BadPaddingException) {
                    return@withContext AuthentyResult.Error(AuthentyError.ValidationError("Incorrect password or corrupted file"))
                } catch (e: javax.crypto.AEADBadTagException) {
                    return@withContext AuthentyResult.Error(AuthentyError.ValidationError("Incorrect password or corrupted file"))
                }

                val backupData = try {
                    gson.fromJson(json, BackupData::class.java)
                } catch (e: com.google.gson.JsonSyntaxException) {
                    return@withContext AuthentyResult.Error(AuthentyError.UnknownError("Invalid backup file format"))
                }

                if (backupData == null || backupData.profiles.isEmpty()) {
                    return@withContext AuthentyResult.Error(AuthentyError.UnknownError("Backup contains no data"))
                }

                // Restore data
                restoreData(backupData)

                AuthentyResult.Success(Unit)
            } catch (e: SecurityException) {
                e.printStackTrace()
                AuthentyResult.Error(AuthentyError.UnknownError("Security error: ${e.message}"))
            } catch (e: java.io.IOException) {
                e.printStackTrace()
                AuthentyResult.Error(AuthentyError.UnknownError("File read error: ${e.message}"))
            } catch (e: Exception) {
                e.printStackTrace()
                AuthentyResult.Error(AuthentyError.UnknownError("Import failed: ${e.message}"))
            }
        }
    }
    
    private fun restoreData(backupData: BackupData) {
        // This is a bit hacky because we are accessing ProfileManager from outside.
        // Ideally ProfileManager should handle the restoration logic.
        // But since I can't easily change ProfileManager's private methods without a big refactor,
        // I'll use the public API to recreate the structure.
        
        backupData.profiles.forEach { profile ->
            // Try to find existing profile by name
            val existingProfile = profileManager.getAllProfiles().find { it.name == profile.name }
            
            val targetProfileId = if (existingProfile != null) {
                existingProfile.id
            } else {
                // Create new profile
                val result = profileManager.createProfile(profile.name, profile.settings)
                if (result is AuthentyResult.Success) {
                    result.data.id
                } else {
                    null // Skip if failed
                }
            }
            
            if (targetProfileId != null) {
                // Restore accounts for this profile
                val accounts = backupData.accounts[profile.id] ?: emptyList()
                accounts.forEach { accountWithProfile ->
                    // Add account to target profile
                    // Check for duplicates is handled in addAccountToProfile
                    profileManager.addAccountToProfile(targetProfileId, accountWithProfile.account, accountWithProfile.category)
                }
            }
        }
    }

    // Encryption Helpers
    private fun encrypt(data: String, password: String): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        // Try PBKDF2WithHmacSHA256, fallback to PBKDF2WithHmacSHA1 if not available
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (e: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        
        val keySpec = PBEKeySpec(password.toCharArray(), salt, 600000, 256)
        val key = factory.generateSecret(keySpec)
        val secretKey = SecretKeySpec(key.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        // Format: Salt (16) + IV (12) + Encrypted Data
        return salt + iv + encrypted
    }

    private fun decrypt(data: ByteArray, password: String): String {
        if (data.size < 28) throw IllegalArgumentException("Invalid data")

        val salt = data.copyOfRange(0, 16)
        val iv = data.copyOfRange(16, 28)
        val encrypted = data.copyOfRange(28, data.size)

        // Try PBKDF2WithHmacSHA256, fallback to PBKDF2WithHmacSHA1 if not available
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (e: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }

        val keySpec = PBEKeySpec(password.toCharArray(), salt, 600000, 256)
        val key = factory.generateSecret(keySpec)
        val secretKey = SecretKeySpec(key.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
