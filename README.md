# Authenty

**A secure, privacy-focused two-factor authentication (2FA) application for Android**

## Overview

Authenty is an open-source Android authenticator application that provides robust two-factor authentication using Time-based One-Time Password (TOTP) and HMAC-based One-Time Password (HOTP) algorithms. Built with security-first principles, Authenty offers enterprise-grade protection for your authentication tokens while maintaining a clean, intuitive user interface.

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
- **Storage Encryption**: AES-256-GCM for values, AES-256-SIV for keys
- **Master Key**: Hardware-backed key stored in Android Keystore (TEE/StrongBox)
- **PIN Hashing**: PBKDF2-HMAC-SHA256 with 600,000 iterations
- **Backup Encryption**: AES-256-GCM with PBKDF2-derived keys

#### Cryptographic Details
- **TOTP Generation**: 
  - Time-based counter: `⌊(timestamp / 1000) / period⌋`
  - HMAC computation: `HMAC-SHA(secret, counter)`
  - Dynamic truncation per RFC 4226
  - Modulo operation for final code

- **Authentication Tag Verification**:
  - GCM mode provides authenticated encryption
  - 128-bit authentication tags prevent tampering
  - Constant-time comparison prevents timing attacks

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

## Security Audit

Authenty implements multiple layers of defense:

| Layer | Mechanism | Standard/Algorithm |
|-------|-----------|-------------------|
| **Authentication** | PIN + Biometric | PBKDF2-HMAC-SHA256 (600k iterations) |
| **Data Encryption** | At-rest storage | AES-256-GCM / AES-256-SIV |
| **Key Storage** | Hardware-backed | Android Keystore (TEE/StrongBox) |
| **OTP Generation** | TOTP/HOTP | RFC 6238 / RFC 4226 |
| **Hash Algorithms** | HMAC | SHA-1, SHA-256, SHA-512 |
| **Backup Encryption** | Password-based | AES-256-GCM + PBKDF2 |
| **UI Protection** | Screen capture blocking | FLAG_SECURE |

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

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- RFC 4226 (HOTP) and RFC 6238 (TOTP) specifications
- Android Jetpack Security library
- Material Design 3 design system
- Open-source cryptography community

## Disclaimer

Authenty is provided as-is for educational and personal use. While it implements industry-standard cryptographic algorithms and security practices, users should perform their own security assessment before using it for critical authentication. The developers are not responsible for any security breaches or data loss resulting from the use of this application.

## Contact

For security issues, please email: [your-security-email@domain.com]

For general inquiries: [your-email@domain.com]

---

**Built with security, designed for privacy, crafted for users.**
