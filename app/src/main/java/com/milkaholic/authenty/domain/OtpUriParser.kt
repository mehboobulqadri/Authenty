package com.milkaholic.authenty.domain

import android.net.Uri
import com.milkaholic.authenty.data.AccountModel

object OtpUriParser {

    /**
     * Legacy parse method for backward compatibility
     */
    fun parse(uriString: String): AccountModel? {
        val result = parseWithResult(uriString)
        return result.getOrNull()
    }
    
    /**
     * Enhanced parse method that returns detailed error information
     */
    fun parseWithResult(uriString: String): AuthentyResult<AccountModel> {
        try {
            // Basic URI validation
            ValidationUtils.validateOtpUri(uriString)?.let { error ->
                return AuthentyResult.Error(error)
            }
            
            val uri = Uri.parse(uriString)

            // Check if it is a valid OTP URL
            if (uri.scheme != "otpauth") {
                return AuthentyResult.Error(AuthentyError.QrInvalidFormat)
            }
            
            if (uri.authority != "totp") {
                return AuthentyResult.Error(AuthentyError.QrInvalidFormat)
            }

            // Extract and validate secret
            val secret = uri.getQueryParameter("secret")
                ?: return AuthentyResult.Error(AuthentyError.QrMissingData("secret"))
            
            // Validate the Base32 secret
            ValidationUtils.validateBase32Secret(secret)?.let { error ->
                return AuthentyResult.Error(error)
            }
            
            // Extract issuer (with fallback)
            val issuer = uri.getQueryParameter("issuer") ?: "Unknown"
            
            // Validate issuer
            ValidationUtils.validateIssuer(issuer)?.let { error ->
                return AuthentyResult.Error(error)
            }

            // Extract account name from path
            // Path usually looks like "/Issuer:AccountName" or "/AccountName"
            var path = uri.path ?: ""
            if (path.startsWith("/")) path = path.substring(1)

            val name = if (path.contains(":")) {
                path.split(":").last().trim()
            } else {
                path.ifEmpty { "Unknown Account" }
            }
            
            // Validate account name
            ValidationUtils.validateAccountName(name)?.let { error ->
                return AuthentyResult.Error(error)
            }

            // Extract additional parameters
            val algorithm = uri.getQueryParameter("algorithm") ?: "SHA1"
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30

            return AuthentyResult.Success(
                AccountModel(
                    name = name,
                    issuer = issuer,
                    secret = secret,
                    algorithm = algorithm.uppercase(),
                    digits = if (digits in 6..8) digits else 6,
                    period = if (period in 15..300) period else 30
                )
            )
        } catch (e: Exception) {
            return AuthentyResult.Error(AuthentyError.UnknownError(e.message ?: "Unknown QR parsing error"))
        }
    }
    
    /**
     * Extract additional OTP parameters (algorithm, digits, period)
     */
    data class OtpParams(
        val algorithm: String = "SHA1",
        val digits: Int = 6,
        val period: Int = 30
    )
    
    fun parseOtpParams(uriString: String): OtpParams {
        try {
            val uri = Uri.parse(uriString)
            val algorithm = uri.getQueryParameter("algorithm") ?: "SHA1"
            val digits = uri.getQueryParameter("digits")?.toIntOrNull() ?: 6
            val period = uri.getQueryParameter("period")?.toIntOrNull() ?: 30
            
            return OtpParams(
                algorithm = algorithm.uppercase(),
                digits = if (digits in 6..8) digits else 6,
                period = if (period in 15..300) period else 30
            )
        } catch (e: Exception) {
            return OtpParams() // Return defaults on error
        }
    }
}