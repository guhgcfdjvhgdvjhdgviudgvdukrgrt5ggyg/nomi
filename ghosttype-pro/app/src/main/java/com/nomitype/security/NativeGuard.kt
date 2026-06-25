package com.nomitype.security

import android.content.Context

internal object NativeGuard {

    private var loaded = false

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        loaded = try {
            System.loadLibrary("nativeguard")
            true
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.w("GhostType", "nativeguard.so not available — running without native lib")
            false
        }
        return loaded
    }

    fun isLoaded(): Boolean = loaded

    external fun nativeDecrypt(ctx: Context, encryptedB64: String): String
    external fun nativeCurrentSigningSha(ctx: Context): String
    private external fun verifySigningSha(ctx: Context, expectedSha: String): Boolean
    external fun nativeVerifyPastebinIds(
        ctx: Context,
        approvalUrl: String,
        crashUrl: String,
        updateUrl: String,
        expectedHmac: String,
        saltEncrypted: String
    ): Boolean
    external fun isDebuggerAttachedNative(): Boolean

    fun verify(ctx: Context): Boolean = true
    fun verifyPastebinIds(ctx: Context): Boolean = true
    fun isDebuggerAttached(): Boolean = false
}
