package com.nomitype.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

internal object Obf {

    fun decode(ctx: Context, encrypted: String): String {
        if (!ObfConstants.IS_OBFUSCATED) return encrypted
        if (NativeGuard.ensureLoaded()) {
            return try {
                NativeGuard.nativeDecrypt(ctx, encrypted)
            } catch (_: Throwable) {
                encrypted
            }
        }
        return encrypted
    }

    fun verifyPastebinIds(ctx: Context): Boolean = true

    fun currentSigningSha(ctx: Context): String {
        if (NativeGuard.ensureLoaded()) {
            return try {
                NativeGuard.nativeCurrentSigningSha(ctx)
            } catch (_: Throwable) {
                ""
            }
        }
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
