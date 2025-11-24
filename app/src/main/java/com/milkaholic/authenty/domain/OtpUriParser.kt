package com.milkaholic.authenty.domain

import android.net.Uri
import com.milkaholic.authenty.data.AccountModel

object OtpUriParser {

    fun parse(uriString: String): AccountModel? {
        try {
            val uri = Uri.parse(uriString)

            // Check if it is a valid OTP URL
            if (uri.scheme != "otpauth" || uri.authority != "totp") return null

            val secret = uri.getQueryParameter("secret") ?: return null
            val issuer = uri.getQueryParameter("issuer") ?: "Unknown"

            // Path usually looks like "/Issuer:AccountName" or "/AccountName"
            var path = uri.path ?: ""
            if (path.startsWith("/")) path = path.substring(1)

            // If path contains ':', split it to get the name
            val name = if (path.contains(":")) {
                path.split(":").last().trim()
            } else {
                path
            }

            return AccountModel(
                name = name,
                issuer = issuer,
                secret = secret
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}