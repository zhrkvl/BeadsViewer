# Plugin Signing Configuration

This document describes the plugin signing setup for BeadsViewer.

## Overview

BeadsViewer is configured to sign the plugin distribution using RSA-4096 keys and X.509 certificates. The `signPlugin` task runs automatically before `publishPlugin`.

## Key Information

**Important Findings:**
- ❌ **SSH Ed25519 keys are NOT compatible** - IntelliJ plugin signing requires RSA keys in PEM format
- ❌ **YubiKey is NOT supported** for plugin signing (only supported for Git commit signing via GPG)
- ✅ **RSA-4096 minimum** is required for security

## Key Location

Signing keys are stored in: `~/.intellij-plugin-signing/`

Files:
- `private.pem` - RSA-4096 private key (password-protected)
- `chain.crt` - X.509 certificate chain (self-signed for development)

## Configuration

The signing is configured in `build.gradle.kts`:

```kotlin
intellijPlatform {
    signing {
        // Uses environment variables with fallback to local files
        certificateChain.set(...)
        privateKey.set(...)
        password.set(...)
    }
}
```

### Environment Variables (Recommended for CI/CD)

For production/CI builds, set these environment variables:

```bash
export CERTIFICATE_CHAIN=$(cat ~/.intellij-plugin-signing/chain.crt)
export PRIVATE_KEY=$(cat ~/.intellij-plugin-signing/private.pem)
export PRIVATE_KEY_PASSWORD="your-secure-password"
```

### Local Development

For local development, the configuration automatically falls back to reading from:
- `~/.intellij-plugin-signing/chain.crt`
- `~/.intellij-plugin-signing/private.pem`
- Default password: `changeme` (⚠️ **CHANGE THIS!**)

## Security Recommendations

1. **Change the default password immediately:**
   ```bash
   # Re-encrypt the private key with a new password
   openssl rsa -aes256 -in ~/.intellij-plugin-signing/private.pem -out ~/.intellij-plugin-signing/private_new.pem
   mv ~/.intellij-plugin-signing/private_new.pem ~/.intellij-plugin-signing/private.pem
   ```

2. **Update the password in your environment:**
   ```bash
   export PRIVATE_KEY_PASSWORD="your-new-secure-password"
   ```

3. **For CI/CD**, use secrets management:
   - GitHub Actions: Use repository secrets
   - GitLab CI: Use CI/CD variables
   - Jenkins: Use credentials plugin

4. **Never commit signing keys** to version control (already in `.gitignore`)

## Regenerating Keys

If you need to regenerate the signing keys:

```bash
# 1. Generate new RSA-4096 private key
openssl genpkey -aes-256-cbc -algorithm RSA \
  -out ~/.intellij-plugin-signing/private.pem \
  -pkeyopt rsa_keygen_bits:4096

# 2. Generate new self-signed certificate (valid for 1 year)
openssl req -key ~/.intellij-plugin-signing/private.pem \
  -new -x509 -days 365 \
  -out ~/.intellij-plugin-signing/chain.crt

# 3. Set proper permissions
chmod 600 ~/.intellij-plugin-signing/private.pem
chmod 644 ~/.intellij-plugin-signing/chain.crt
```

## Building Signed Plugin

The plugin is automatically signed during the build process:

```bash
# Build and sign the plugin
./gradlew buildPlugin

# The signed plugin will be in: build/distributions/
```

## Publishing

When publishing to JetBrains Marketplace:

```bash
# Signs and publishes the plugin
./gradlew publishPlugin
```

The `signPlugin` task runs automatically before `publishPlugin`.

## Production Certificates

For production releases, consider:
1. Obtaining a certificate from a trusted Certificate Authority (CA)
2. Using a longer validity period (e.g., 2-3 years)
3. Storing production keys in a secure key management system (AWS KMS, HashiCorp Vault, etc.)

## Troubleshooting

### "Failed to sign plugin" error

Check:
1. Private key exists: `ls -la ~/.intellij-plugin-signing/private.pem`
2. Certificate exists: `ls -la ~/.intellij-plugin-signing/chain.crt`
3. Password is correct (try the `PRIVATE_KEY_PASSWORD` environment variable)
4. Keys are in the correct format (PEM with RSA headers)

### Verifying key format

```bash
# Check private key
openssl rsa -in ~/.intellij-plugin-signing/private.pem -check -noout

# Check certificate
openssl x509 -in ~/.intellij-plugin-signing/chain.crt -text -noout
```

## References

- [Plugin Signing | IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
