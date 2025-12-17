# MIT License

Copyright (c) 2025 Authenty Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

## Third-Party Licenses

This project uses the following third-party libraries and frameworks:

### Android Jetpack Libraries
- **AndroidX Core, Lifecycle, Activity, Compose**
  - License: Apache License 2.0
  - Copyright: The Android Open Source Project
  - [License Details](https://source.android.com/setup/start/licenses)

### Jetpack Security (EncryptedSharedPreferences)
- **androidx.security:security-crypto**
  - License: Apache License 2.0
  - Copyright: The Android Open Source Project
  - [License Details](https://developer.android.com/jetpack/androidx/releases/security)

### Material Design Components
- **Material Design 3 / Material Components for Android**
  - License: Apache License 2.0
  - Copyright: Google Inc.
  - [License Details](https://github.com/material-components/material-components-android)

### Kotlin Standard Library
- **org.jetbrains.kotlin:kotlin-stdlib**
  - License: Apache License 2.0
  - Copyright: JetBrains s.r.o. and Kotlin Programming Language contributors
  - [License Details](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt)

### Google Gson
- **com.google.code.gson:gson**
  - License: Apache License 2.0
  - Copyright: Google Inc.
  - [License Details](https://github.com/google/gson/blob/master/LICENSE)

### ZXing (QR Code Library)
- **com.google.zxing:core** (if used)
  - License: Apache License 2.0
  - Copyright: ZXing authors
  - [License Details](https://github.com/zxing/zxing/blob/master/LICENSE)

### CameraX
- **androidx.camera:camera-***
  - License: Apache License 2.0
  - Copyright: The Android Open Source Project
  - [License Details](https://developer.android.com/jetpack/androidx/releases/camera)

---

## Cryptographic Standards & Specifications

This software implements the following cryptographic standards and specifications:

### IETF RFCs
- **RFC 4226** - HOTP: An HMAC-Based One-Time Password Algorithm
  - Copyright: Internet Engineering Task Force (IETF)
  - [Specification](https://datatracker.ietf.org/doc/html/rfc4226)

- **RFC 6238** - TOTP: Time-Based One-Time Password Algorithm
  - Copyright: Internet Engineering Task Force (IETF)
  - [Specification](https://datatracker.ietf.org/doc/html/rfc6238)

### NIST Standards
- **NIST SP 800-63B** - Digital Identity Guidelines: Authentication and Lifecycle Management
  - Public domain (U.S. Government work)
  - [Publication](https://pages.nist.gov/800-63-3/sp800-63b.html)

- **NIST SP 800-132** - Recommendation for Password-Based Key Derivation
  - Public domain (U.S. Government work)
  - [Publication](https://csrc.nist.gov/publications/detail/sp/800-132/final)

- **NIST SP 800-38D** - Recommendation for Block Cipher Modes: Galois/Counter Mode (GCM)
  - Public domain (U.S. Government work)
  - [Publication](https://csrc.nist.gov/publications/detail/sp/800-38d/final)

- **NIST FIPS 180-4** - Secure Hash Standard (SHS)
  - Public domain (U.S. Government work)
  - [Publication](https://csrc.nist.gov/publications/detail/fips/180/4/final)

- **NIST FIPS 198-1** - The Keyed-Hash Message Authentication Code (HMAC)
  - Public domain (U.S. Government work)
  - [Publication](https://csrc.nist.gov/publications/detail/fips/198/1/final)

---

## Security Disclaimer

This software implements cryptographic algorithms and security mechanisms based on industry standards and best practices. However:

1. **No Warranty**: The cryptographic implementations are provided "as-is" without warranty of any kind, as stated in the MIT License above.

2. **Security Audits**: This software has not undergone formal third-party security audits. Users requiring certified security should commission independent audits.

3. **Compliance**: While this software implements NIST-recommended algorithms, it has not been validated under NIST's Cryptographic Algorithm Validation Program (CAVP) or Cryptographic Module Validation Program (CMVP).

4. **Export Control**: This software contains encryption technology. Users are responsible for compliance with applicable export control laws and regulations in their jurisdiction.

5. **Regulatory Compliance**: Users in regulated industries (finance, healthcare, government) should verify that this software meets their specific regulatory requirements before deployment.

---

## Attribution

If you use this software or derivative works in your project, we appreciate (but do not require) attribution:

```
Authenty - Secure Two-Factor Authentication for Android
https://github.com/mehboobulqadri/authenty
Licensed under MIT License
```

---

**Last Updated**: December 2025
