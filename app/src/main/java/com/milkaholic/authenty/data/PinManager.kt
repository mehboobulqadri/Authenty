package com.milkaholic.authenty.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.milkaholic.authenty.domain.AuthentyError
import com.milkaholic.authenty.domain.AuthentyResult
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

class PinManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val pinPrefs = EncryptedSharedPreferences.create(
        context,
        "pin_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_BACKUP_PIN_HASH = "backup_pin_hash"
        private const val KEY_BACKUP_PIN_SALT = "backup_pin_salt"
        private const val KEY_BACKUP_PIN_ENABLED = "backup_pin_enabled"
        private const val KEY_DURESS_PIN_HASH = "duress_pin_hash"
        private const val KEY_DURESS_PIN_SALT = "duress_pin_salt"
        private const val KEY_DURESS_PIN_ENABLED = "duress_pin_enabled"
        private const val PBKDF2_ITERATIONS = 600000
        private const val SALT_LENGTH = 32
    }

    fun isPinSet(): Boolean {
        return pinPrefs.getBoolean(KEY_PIN_ENABLED, false) && 
               !pinPrefs.getString(KEY_PIN_HASH, "").isNullOrEmpty()
    }

    fun isBackupPinSet(): Boolean {
        return pinPrefs.getBoolean(KEY_BACKUP_PIN_ENABLED, false) && 
               !pinPrefs.getString(KEY_BACKUP_PIN_HASH, "").isNullOrEmpty()
    }

    fun isDuressPinSet(): Boolean {
        return pinPrefs.getBoolean(KEY_DURESS_PIN_ENABLED, false) && 
               !pinPrefs.getString(KEY_DURESS_PIN_HASH, "").isNullOrEmpty()
    }

    fun setPrimaryPin(pin: String): AuthentyResult<Unit> {
        return try {
            if (pin.length < 4) {
                return AuthentyResult.Error(AuthentyError.ValidationError("PIN must be at least 4 digits"))
            }

            if (pin.length > 12) {
                return AuthentyResult.Error(AuthentyError.ValidationError("PIN cannot be longer than 12 digits"))
            }

            if (!pin.all { it.isDigit() }) {
                return AuthentyResult.Error(AuthentyError.ValidationError("PIN must contain only numbers"))
            }

            val salt = generateSalt()
            val hash = hashPin(pin, salt)

            val editor = pinPrefs.edit()
            editor.putString(KEY_PIN_HASH, hash)
            editor.putString(KEY_PIN_SALT, salt)
            editor.putBoolean(KEY_PIN_ENABLED, true)
            editor.apply()

            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun setBackupPin(pin: String): AuthentyResult<Unit> {
        return try {
            if (pin.length < 4) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Backup PIN must be at least 4 digits"))
            }

            if (pin.length > 12) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Backup PIN cannot be longer than 12 digits"))
            }

            if (!pin.all { it.isDigit() }) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Backup PIN must contain only numbers"))
            }

            if (isPinSet()) {
                val primaryHash = pinPrefs.getString(KEY_PIN_HASH, "")
                val primarySalt = pinPrefs.getString(KEY_PIN_SALT, "")
                if (!primaryHash.isNullOrEmpty() && !primarySalt.isNullOrEmpty()) {
                    val testHash = hashPin(pin, primarySalt)
                    if (testHash == primaryHash) {
                        return AuthentyResult.Error(AuthentyError.ValidationError("Backup PIN must be different from primary PIN"))
                    }
                }
            }

            val salt = generateSalt()
            val hash = hashPin(pin, salt)

            val editor = pinPrefs.edit()
            editor.putString(KEY_BACKUP_PIN_HASH, hash)
            editor.putString(KEY_BACKUP_PIN_SALT, salt)
            editor.putBoolean(KEY_BACKUP_PIN_ENABLED, true)
            editor.apply()

            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun setDuressPin(pin: String): AuthentyResult<Unit> {
        return try {
            if (pin.length < 4) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Duress PIN must be at least 4 digits"))
            }

            if (pin.length > 12) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Duress PIN cannot be longer than 12 digits"))
            }

            if (!pin.all { it.isDigit() }) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Duress PIN must contain only numbers"))
            }

            if (isPinSet()) {
                val primaryHash = pinPrefs.getString(KEY_PIN_HASH, "")
                val primarySalt = pinPrefs.getString(KEY_PIN_SALT, "")
                if (!primaryHash.isNullOrEmpty() && !primarySalt.isNullOrEmpty()) {
                    val testHash = hashPin(pin, primarySalt)
                    if (testHash == primaryHash) {
                        return AuthentyResult.Error(AuthentyError.ValidationError("Duress PIN must be different from primary PIN"))
                    }
                }
            }
            
            if (isBackupPinSet()) {
                val backupHash = pinPrefs.getString(KEY_BACKUP_PIN_HASH, "")
                val backupSalt = pinPrefs.getString(KEY_BACKUP_PIN_SALT, "")
                if (!backupHash.isNullOrEmpty() && !backupSalt.isNullOrEmpty()) {
                    val testHash = hashPin(pin, backupSalt)
                    if (testHash == backupHash) {
                        return AuthentyResult.Error(AuthentyError.ValidationError("Duress PIN must be different from backup PIN"))
                    }
                }
            }

            val salt = generateSalt()
            val hash = hashPin(pin, salt)

            val editor = pinPrefs.edit()
            editor.putString(KEY_DURESS_PIN_HASH, hash)
            editor.putString(KEY_DURESS_PIN_SALT, salt)
            editor.putBoolean(KEY_DURESS_PIN_ENABLED, true)
            editor.apply()

            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun verifyPrimaryPin(pin: String): AuthentyResult<Boolean> {
        return try {
            if (!isPinSet()) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Primary PIN is not set"))
            }

            val storedHash = pinPrefs.getString(KEY_PIN_HASH, "") ?: ""
            val storedSalt = pinPrefs.getString(KEY_PIN_SALT, "") ?: ""

            if (storedHash.isEmpty() || storedSalt.isEmpty()) {
                return AuthentyResult.Error(AuthentyError.StorageCorrupted)
            }

            val inputHash = hashPin(pin, storedSalt)
            val isValid = storedHash == inputHash

            AuthentyResult.Success(isValid)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.UnknownError("PIN verification failed"))
        }
    }

    fun verifyBackupPin(pin: String): AuthentyResult<Boolean> {
        return try {
            if (!isBackupPinSet()) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Backup PIN is not set"))
            }

            val storedHash = pinPrefs.getString(KEY_BACKUP_PIN_HASH, "") ?: ""
            val storedSalt = pinPrefs.getString(KEY_BACKUP_PIN_SALT, "") ?: ""

            if (storedHash.isEmpty() || storedSalt.isEmpty()) {
                return AuthentyResult.Error(AuthentyError.StorageCorrupted)
            }

            val inputHash = hashPin(pin, storedSalt)
            val isValid = storedHash == inputHash

            AuthentyResult.Success(isValid)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.UnknownError("Backup PIN verification failed"))
        }
    }

    fun verifyDuressPin(pin: String): AuthentyResult<Boolean> {
        return try {
            if (!isDuressPinSet()) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Duress PIN is not set"))
            }

            val storedHash = pinPrefs.getString(KEY_DURESS_PIN_HASH, "") ?: ""
            val storedSalt = pinPrefs.getString(KEY_DURESS_PIN_SALT, "") ?: ""

            if (storedHash.isEmpty() || storedSalt.isEmpty()) {
                return AuthentyResult.Error(AuthentyError.StorageCorrupted)
            }

            val inputHash = hashPin(pin, storedSalt)
            val isValid = storedHash == inputHash

            AuthentyResult.Success(isValid)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.UnknownError("Duress PIN verification failed"))
        }
    }

    fun verifyAnyPin(pin: String): AuthentyResult<PinVerificationResult> {
        return try {
            if (isPinSet()) {
                val primaryResult = verifyPrimaryPin(pin)
                if (primaryResult is AuthentyResult.Success && primaryResult.data) {
                    return AuthentyResult.Success(PinVerificationResult.PRIMARY_PIN_VALID)
                }
            }

            if (isBackupPinSet()) {
                val backupResult = verifyBackupPin(pin)
                if (backupResult is AuthentyResult.Success && backupResult.data) {
                    return AuthentyResult.Success(PinVerificationResult.BACKUP_PIN_VALID)
                }
            }

            if (isDuressPinSet()) {
                val duressResult = verifyDuressPin(pin)
                if (duressResult is AuthentyResult.Success && duressResult.data) {
                    return AuthentyResult.Success(PinVerificationResult.DURESS_PIN_VALID)
                }
            }

            AuthentyResult.Success(PinVerificationResult.INVALID)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.UnknownError("PIN verification failed"))
        }
    }

    fun changePrimaryPin(oldPin: String, newPin: String): AuthentyResult<Unit> {
        return try {
            val verifyResult = verifyPrimaryPin(oldPin)
            if (verifyResult is AuthentyResult.Error) {
                return verifyResult
            }

            if (verifyResult is AuthentyResult.Success && !verifyResult.data) {
                return AuthentyResult.Error(AuthentyError.ValidationError("Current PIN is incorrect"))
            }

            setPrimaryPin(newPin)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.UnknownError("Failed to change PIN"))
        }
    }

    fun removePin(): AuthentyResult<Unit> {
        return try {
            val editor = pinPrefs.edit()
            editor.remove(KEY_PIN_HASH)
            editor.remove(KEY_PIN_SALT)
            editor.putBoolean(KEY_PIN_ENABLED, false)
            editor.apply()

            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun removeBackupPin(): AuthentyResult<Unit> {
        return try {
            val editor = pinPrefs.edit()
            editor.remove(KEY_BACKUP_PIN_HASH)
            editor.remove(KEY_BACKUP_PIN_SALT)
            editor.putBoolean(KEY_BACKUP_PIN_ENABLED, false)
            editor.apply()

            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun removeDuressPin(): AuthentyResult<Unit> {
        return try {
            val editor = pinPrefs.edit()
            editor.remove(KEY_DURESS_PIN_HASH)
            editor.remove(KEY_DURESS_PIN_SALT)
            editor.putBoolean(KEY_DURESS_PIN_ENABLED, false)
            editor.apply()

            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    fun clearAllPins(): AuthentyResult<Unit> {
        return try {
            val editor = pinPrefs.edit()
            editor.clear()
            editor.apply()

            AuthentyResult.Success(Unit)
        } catch (e: Exception) {
            AuthentyResult.Error(AuthentyError.StorageError)
        }
    }

    private fun generateSalt(): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        return try {
            val saltBytes = salt.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()

            val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded

            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            val fallbackHash = MessageDigest.getInstance("SHA-256")
            fallbackHash.update((pin + salt).toByteArray())
            fallbackHash.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

enum class PinVerificationResult {
    PRIMARY_PIN_VALID,
    BACKUP_PIN_VALID,
    DURESS_PIN_VALID,
    INVALID
}