package com.ghosttype.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Runtime decryption for the obfuscated constants generated at build
 * time by the Gradle task `generateObfConstants` (see app/build.gradle.kts).
 *
 * ALL actual decryption is delegated to NativeGuard (native C++ code).
 * If the native library is removed or fails to load, every decode()
 * returns "" → app cannot function.
 *
 * The encryption key is derived from `packageName + SHA-256(signing
 * cert)`, so a thief who decompiles the APK and re-signs with their
 * own keystore produces a DIFFERENT key — every decode() returns
 * garbage.
 */
internal object Obf {

    /** Decrypts an obfuscated constant via native C++ code.
     *  Returns "" on any failure so callers can defensively check
     *  `isBlank()` and fall back to the lock screen. */
    fun decode(ctx: Context, encrypted: String): String {
        if (!ObfConstants.IS_OBFUSCATED) return encrypted
        // Native XOR decryption — if the .so is missing this returns ""
        if (!NativeGuard.ensureLoaded()) return ""
        return try {
            NativeGuard.nativeDecrypt(ctx, encrypted)
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Verifies that the pastebin IDs embedded in the app's URLs match
     * the HMAC computed at build time. Runs in native code.
     */
    fun verifyPastebinIds(ctx: Context): Boolean {
        if (!ObfConstants.IS_OBFUSCATED) return true
        return NativeGuard.verifyPastebinIds(ctx)
    }

    /** SHA-256 of the APK's first signing cert, lowercase hex, no
     *  separators. Uses native code to compute it. */
    fun currentSigningSha(ctx: Context): String {
        if (NativeGuard.ensureLoaded()) {
            return try {
                NativeGuard.nativeCurrentSigningSha(ctx)
            } catch (_: Throwable) {
                ""
            }
        }
        // Fallback — but if native isn't loaded, we're already compromised.
        return try {
            val pm = ctx.packageManager
            @Suppress("DEPRECATION")
            val pi = if (Build.VERSION.SDK_INT >= 28) {
                pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
            }
            val sigs = if (Build.VERSION.SDK_INT >= 28) {
                pi.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION") pi.signatures
            }
            if (sigs.isNullOrEmpty()) return ""
            MessageDigest.getInstance("SHA-256")
                .digest(sigs[0].toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        } catch (_: Throwable) {
            ""
        }
    }
}
