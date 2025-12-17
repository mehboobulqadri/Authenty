# Authenty

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.md)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-purple.svg)](https://kotlinlang.org)
[![Security](https://img.shields.io/badge/Security-NIST%20Compliant-orange.svg)](#standards-compliance)

**Enterprise-Grade Two-Factor Authentication for Android**

## Overview

Authenty is a security-hardened, open-source Android authenticator application implementing industry-standard Time-based One-Time Password (TOTP) and HMAC-based One-Time Password (HOTP) protocols. Designed with defense-in-depth principles and compliance with NIST cryptographic standards, Authenty provides institutional-grade protection for authentication credentials while maintaining an intuitive, accessible user experience.

### Design Philosophy

- **Security-First Architecture**: Every component designed with threat modeling and secure coding practices
- **Standards Compliance**: Full adherence to RFC specifications and NIST cryptographic recommendations
- **Defense in Depth**: Multi-layered security controls protecting against diverse threat vectors
- **Privacy by Design**: Zero telemetry, no network permissions, complete local data sovereignty
- **Transparency**: Open-source codebase enabling independent security audits and verification

## Key Features

### Authentication & Security
- **TOTP/HOTP Support**: Full implementation of RFC 4226 (HOTP) and RFC 6238 (TOTP) standards
- **Multi-Algorithm Support**: SHA-1, SHA-256, and SHA-512 hash algorithms
- **Flexible Code Generation**: Support for 6-8 digit codes with customizable time periods
- **Multi-PIN System**: Primary, backup, and duress PIN configurations
- **Biometric Authentication**: Fingerprint and face recognition support
- **Auto-Lock Protection**: Configurable timeout-based automatic locking
- **Progressive Lockout**: Escalating delays after failed authentication attempts

### Data Protection
- **Hardware-Backed Encryption**: AES-256-GCM encryption using Android Keystore
- **Encrypted Storage**: All sensitive data protected with EncryptedSharedPreferences
- **PBKDF2 Key Derivation**: 600,000 iterations with unique salts for maximum resistance
- **Secure Backup/Export**: Password-protected AES-256-GCM encrypted backups
- **Memory Protection**: FLAG_SECURE prevents screenshots and screen recording
- **Clipboard Auto-Clear**: Automatic clipboard sanitization after 30 seconds

### User Experience
- **Multiple Profiles**: Organize accounts by context (Work, Personal, etc.)
- **Category Management**: Group accounts by service type or purpose
- **Search & Filter**: Quick access to accounts with real-time search
- **Drag & Reorder**: Customize account ordering within profiles
- **Dark/Light Themes**: Full Material Design 3 theme support
- **Backup Codes**: Store and manage emergency backup codes
- **QR Code Scanning**: Easy account setup via QR code import

### Advanced Security Features
- **Duress Mode**: Special PIN that hides all accounts under coercion
- **Root Detection**: Identifies compromised devices and warns users
- **Security Event Logging**: Comprehensive audit trail of security events
- **Failed Attempt Tracking**: Monitors and responds to suspicious activity
- **Security Reports**: Detailed analytics on authentication patterns and threats

## Technical Architecture

### Security Implementation

#### Encryption Stack

All cryptographic implementations follow **NIST-approved algorithms** and industry best practices:

- **Storage Encryption**: 
  - AES-256-GCM for values (NIST SP 800-38D)
  - AES-256-SIV for keys (RFC 5297, nonce-misuse resistant)
  - Authenticated Encryption with Associated Data (AEAD)
  
- **Master Key Management**: 
  - Hardware-backed key stored in Android Keystore
  - Trusted Execution Environment (TEE) or StrongBox isolation
  - Keys never extractable from secure hardware
  
- **Password-Based Key Derivation**: 
  - PBKDF2-HMAC-SHA256 with 600,000 iterations (NIST SP 800-132)
  - Cryptographically random 256-bit salts (NIST SP 800-90A)
  - Exceeds NIST SP 800-63B minimum recommendations (10,000 iterations)
  
- **Backup Encryption**: 
  - AES-256-GCM authenticated encryption
  - PBKDF2-derived encryption keys from user passwords
  - Per-backup random initialization vectors (IVs)

#### Cryptographic Algorithm Details

**One-Time Password (OTP) Generation** (RFC 6238 / RFC 4226):
- **Time-based Counter Calculation**: `⌊(Unix_timestamp / 1000) / period⌋`
- **HMAC Computation**: HMAC-SHA-1/256/512 per NIST FIPS 198-1
  - SHA-1 (legacy compatibility, NIST FIPS 180-4)
  - SHA-256 (default, NIST FIPS 180-4)
  - SHA-512 (enhanced security, NIST FIPS 180-4)
- **Dynamic Truncation**: RFC 4226 offset-based extraction
- **Code Generation**: Modulo `10^digits` for 6-8 digit codes

**Authenticated Encryption (AEAD)**:
- **Galois/Counter Mode (GCM)**: NIST SP 800-38D compliant
  - 128-bit authentication tags (prevents tampering)
  - 96-bit initialization vectors (recommended by NIST)
  - Constant-time tag comparison (timing attack resistant)
- **Synthetic IV (SIV) Mode**: RFC 5297 for key encryption
  - Nonce-misuse resistant AEAD
  - Deterministic authenticated encryption for preference keys

**Random Number Generation**:
- Android `SecureRandom` (backed by `/dev/urandom`)
- NIST SP 800-90A compliant DRBG implementation
- Used for: salts, IVs, nonces, secret generation

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM with Repository Pattern
- **Dependency Injection**: Manual DI with singleton managers
- **Concurrency**: Kotlin Coroutines + Flow
- **Storage**: EncryptedSharedPreferences (Jetpack Security)
- **Camera**: CameraX for QR code scanning
- **Biometrics**: AndroidX Biometric library

### Project Structure

```
com.milkaholic.authenty/
├── data/                    # Data layer
│   ├── AccountModel.kt      # Account data models
│   ├── PinManager.kt        # PIN authentication
│   ├── SecureRepository.kt  # Encrypted storage
│   └── SecurityEventRepository.kt
├── domain/                  # Business logic
│   ├── EnhancedCryptoManager.kt  # TOTP/HOTP generation
│   ├── SecurityManager.kt        # Security orchestration
│   ├── ProfileManager.kt         # Profile management
│   ├── BackupManager.kt          # Backup/restore
│   └── AutoLockManager.kt        # Auto-lock logic
├── presentation/            # UI layer
│   ├── screens/            # Compose screens
│   ├── components/         # Reusable UI components
│   └── MainViewModel.kt    # UI state management
└── ui/theme/               # Material Design theme
```

## Installation

### Requirements
- Android 8.0 (API 26) or higher
- Biometric hardware (optional, for fingerprint/face unlock)

### Building from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/authenty.git
   cd authenty
   ```

2. **Open in Android Studio**
   - Android Studio Hedgehog (2023.1.1) or newer
   - Kotlin plugin 1.9.0+

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

## Usage

### First-Time Setup

1. **Create a PIN**: Set up a 4-12 digit primary PIN on first launch
2. **Optional**: Configure backup PIN and duress PIN
3. **Enable Biometrics**: Set up fingerprint or face unlock (optional)

### Adding Accounts

**Via QR Code**:
1. Tap the '+' button on the home screen
2. Select "Scan QR Code"
3. Point camera at the QR code provided by the service

**Manual Entry**:
1. Tap the '+' button
2. Select "Manual Entry"
3. Enter account name, issuer, and secret key
4. Configure algorithm (SHA-1/256/512), digits (6-8), and period (default 30s)

### Managing Profiles

1. Open navigation drawer
2. Select "Profile Management"
3. Create new profiles for different contexts (Work, Personal, etc.)
4. Switch between profiles to organize accounts

### Backup & Restore

**Export Backup**:
1. Navigate to Settings → Backup & Export
2. Set a strong password for encryption
3. Choose export location
4. Backup file is encrypted with AES-256-GCM

**Import Backup**:
1. Navigate to Settings → Backup & Import
2. Select backup file
3. Enter decryption password
4. All accounts and profiles are restored

### Duress Mode

**Setup**:
1. Settings → Security Settings
2. Enable "Duress PIN"
3. Set a separate PIN for coercion scenarios

**Activation**:
- Enter duress PIN when unlocking the app
- All accounts are hidden automatically
- App appears empty to prevent forced disclosure
- Enter duress PIN again to deactivate

## Security Considerations

### Threat Model

Authenty is designed to protect against:

- **Unauthorized Access**: Multi-factor authentication with PINs and biometrics
- **Data Theft**: Full encryption of all sensitive data at rest
- **Memory Dumps**: FLAG_SECURE prevents OS-level screen capture
- **Brute Force Attacks**: PBKDF2 with 600k iterations slows offline attacks
- **Rainbow Tables**: Unique random salts for each PIN hash
- **Coercion Attacks**: Duress mode provides plausible deniability
- **Root Detection**: Warns users of compromised devices

### Known Limitations

- **Physical Access**: Device encryption relies on Android's security
- **Root Access**: Rooted devices can potentially bypass some protections
- **Memory Residency**: Sensitive data in RAM until garbage collection
- **Clipboard Security**: 30-second window before auto-clear

### Best Practices

1. **Use Strong PINs**: Minimum 6 digits, avoid sequential patterns
2. **Enable Auto-Lock**: Set aggressive timeout (1-2 minutes)
3. **Regular Backups**: Export encrypted backups to secure storage
4. **Verify QR Codes**: Ensure QR codes are from legitimate sources
5. **Monitor Security Events**: Review security logs for suspicious activity
6. **Keep Updated**: Install updates promptly for security patches

## Standards Compliance

### NIST Special Publications (SP)

Authenty's cryptographic implementation aligns with the following NIST recommendations:

| NIST Publication | Title | Implementation |
|-----------------|-------|----------------|
| **SP 800-63B** | Digital Identity Guidelines: Authentication and Lifecycle Management | PIN requirements (4-12 digits), memorized secret management, multi-factor authentication |
| **SP 800-132** | Recommendation for Password-Based Key Derivation | PBKDF2-HMAC-SHA256, 600,000 iterations (60× NIST minimum), unique salts |
| **SP 800-38D** | Recommendation for Block Cipher Modes: Galois/Counter Mode (GCM) | AES-256-GCM for authenticated encryption, 96-bit IVs, 128-bit tags |
| **SP 800-90A** | Recommendation for Random Number Generation | SecureRandom for cryptographic randomness (salts, IVs, keys) |
| **FIPS 180-4** | Secure Hash Standard (SHS) | SHA-1, SHA-256, SHA-512 hash functions for HMAC operations |
| **FIPS 198-1** | The Keyed-Hash Message Authentication Code (HMAC) | HMAC-SHA1/256/512 for TOTP/HOTP generation |

### IETF RFCs

| RFC | Title | Compliance |
|-----|-------|-----------|
| **RFC 4226** | HOTP: An HMAC-Based One-Time Password Algorithm | Full implementation with counter management |
| **RFC 6238** | TOTP: Time-Based One-Time Password Algorithm | Full implementation with configurable periods (15-60s) |
| **RFC 5297** | Synthetic Initialization Vector (SIV) Authenticated Encryption | AES-SIV for preference key encryption |

### Security Audit Summary

| Security Layer | Mechanism | Standard Compliance |
|---------------|-----------|-------------------|
| **Authentication** | Multi-factor (PIN + Biometric) | NIST SP 800-63B Level 2 |
| **Data Encryption** | AES-256-GCM/SIV | NIST SP 800-38D, FIPS 197 |
| **Key Derivation** | PBKDF2-HMAC-SHA256 | NIST SP 800-132 |
| **Key Storage** | Android Keystore (TEE/StrongBox) | Hardware-backed, FIPS 140-2 Level 1+ |
| **OTP Generation** | TOTP/HOTP | RFC 6238 / RFC 4226 |
| **Hash Functions** | SHA-1/256/512 | NIST FIPS 180-4 |
| **HMAC** | HMAC-SHA1/256/512 | NIST FIPS 198-1 |
| **Random Generation** | SecureRandom | NIST SP 800-90A |
| **Memory Protection** | FLAG_SECURE | Android platform security |

### Compliance Notes

- **FIPS 140-2/3**: While using NIST-approved algorithms, this implementation has not undergone formal FIPS validation
- **NIST SP 800-63B**: Exceeds memorized secret requirements (600k iterations vs. 10k minimum)
- **CAVP Validation**: Algorithms not validated under NIST's Cryptographic Algorithm Validation Program
- **Export Control**: Contains encryption technology subject to export regulations (check local laws)

## Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/your-feature`
3. **Follow code style**: Kotlin coding conventions
4. **Write tests**: Ensure security-critical code has test coverage
5. **Commit changes**: Use clear, descriptive commit messages
6. **Submit pull request**: Describe changes and security implications

### Code Review Focus

- Security implications of changes
- Proper use of cryptographic APIs
- Memory management for sensitive data
- UI/UX consistency with Material Design
- Performance impact on OTP generation

## License

This project is licensed under the MIT License. See [LICENSE.md](LICENSE.md) for complete terms, third-party attributions, and cryptographic standards references.

## Acknowledgments

### Standards Organizations
- **NIST** - National Institute of Standards and Technology for cryptographic standards
- **IETF** - Internet Engineering Task Force for RFC specifications
- **FIDO Alliance** - For authentication standards and best practices

### Open Source Community
- Android Open Source Project (AOSP) and Jetpack Security library
- Material Design team for design system and components
- Kotlin language team for modern, safe programming tools
- Security researchers contributing to public cryptographic knowledge

## Responsible Disclosure

Security is our top priority. If you discover a security vulnerability:

1. **DO NOT** open a public issue
2. Email security details to: **mehboobulqadri@gmail.com**
3. Include: vulnerability description, reproduction steps, potential impact
4. Allow 90 days for patch development before public disclosure
5. We will acknowledge receipt within 48 hours

### Security Hall of Fame

We recognize security researchers who responsibly disclose vulnerabilities:
- *[Awaiting first contributor]*

## Contact

- **Security Issues**: [mehboobulqadri@gmail.com] (PGP key available on request)
- **General Inquiries**: [mehboobulqadri@gmail.com]
- **Bug Reports**: [GitHub Issues](https://github.com/mehboobulqadri/authenty/issues)

## Citation

If using Authenty in academic research, please cite:

```bibtex
@software{authenty2024,
  title = {Authenty: Enterprise-Grade Two-Factor Authentication for Android},
  author = {Authenty Contributors},
  year = {2025},
  url = {https://github.com/mehboobulqadri/authenty},
  note = {NIST-compliant TOTP/HOTP implementation with hardware-backed encryption}
}
```

---

<div align="center">

**Built with security, designed for privacy, crafted for users.**

*Implementing NIST-approved cryptography for authentication you can trust*

[Report Bug](https://github.com/mehboobulqadri/authenty/issues) · [Request Feature](https://github.com/mehboobulqadri/authenty/issues) 

</div>
