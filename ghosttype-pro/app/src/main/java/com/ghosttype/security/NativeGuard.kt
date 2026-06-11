package com.ghosttype.security

import android.content.Context
import android.util.Base64

/**
 * JNI/NDK native security core. ALL sensitive operations (XOR
 * decryption, signing SHA, pastebin ID verification) run in C++.
 *
 * If someone removes nativeguard.so from the APK, everything breaks:
 *   - Obf.decode() returns garbage → pastebin URLs are wrong → gates
 *     can't fetch → brick
 *   - Signing SHA can't be verified → SecurityGuard/Hardener fail → brick
 *   - Branding text, license, space label all become garbage
 *
 * The app literally CANNOT function without this native library.
 */
internal object NativeGuard {

    private var loaded = false

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        loaded = try {
            System.loadLibrary("nativeguard")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
        return loaded
    }

    fun isLoaded(): Boolean = loaded

    // ── Native methods (core decryption — app DEPENDS on these) ──

    /** XOR-decrypt using keystore-derived key. Returns "" on failure. */
    external fun nativeDecrypt(ctx: Context, encryptedB64: String): String

    /** Returns the APK signing SHA256 (native computation). */
    external fun nativeCurrentSigningSha(ctx: Context): String

    /** Native signing SHA verification. */
    private external fun verifySigningSha(ctx: Context, expectedSha: String): Boolean

    /** Native pastebin ID HMAC verification. */
    external fun nativeVerifyPastebinIds(
        ctx: Context,
        approvalUrl: String,
        crashUrl: String,
        updateUrl: String,
        expectedHmac: String,
        saltEncrypted: String
    ): Boolean

    /** Debugger check in native code. */
    external fun isDebuggerAttachedNative(): Boolean

    // ── Public wrappers ─────────────────────────────────────────

    fun verify(ctx: Context): Boolean {
        if (!loaded) return false
        return try {
            val expected = ObfConstants.EXPECTED_SIGNING_SHA256
            if (expected.isEmpty() || expected == "0".repeat(64)) return true
            verifySigningSha(ctx, expected)
        } catch (e: Exception) {
            false
        }
    }

    fun verifyPastebinIds(ctx: Context): Boolean {
        if (!loaded) return false
        return try {
            // Decrypt URLs and get constants — all via native XOR
            val approvalUrl = Obf.decode(ctx, ObfConstants.APPROVAL_URL)
            val crashUrl    = Obf.decode(ctx, ObfConstants.CRASH_URL)
            val updateUrl   = Obf.decode(ctx, ObfConstants.UPDATE_URL)
            if (approvalUrl.isBlank() || crashUrl.isBlank() || updateUrl.isBlank()) return false
            nativeVerifyPastebinIds(
                ctx, approvalUrl, crashUrl, updateUrl,
                ObfConstants.PASTEBIN_IDS_HMAC,
                ObfConstants.PASTEBIN_SALT_ENCRYPTED
            )
        } catch (e: Exception) {
            false
        }
    }

    fun isDebuggerAttached(): Boolean {
        if (!loaded) return false
        return try {
            isDebuggerAttachedNative()
        } catch (e: Exception) {
            false
        }
    }
}
