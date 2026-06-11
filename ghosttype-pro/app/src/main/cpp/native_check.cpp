#include <jni.h>
#include <string>
#include <cstring>
#include <vector>
#include <android/log.h>

#define LOG_TAG "NativeGuard"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ---------- helpers ----------

static std::string sha256_hex(JNIEnv *env, const jbyteArray &input) {
    jclass mdCls = env->FindClass("java/security/MessageDigest");
    jmethodID getInstance = env->GetStaticMethodID(mdCls, "getInstance",
        "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jobject md = env->CallStaticObjectMethod(mdCls, getInstance,
        env->NewStringUTF("SHA-256"));
    jmethodID digest = env->GetMethodID(mdCls, "digest", "([B)[B");
    jbyteArray hashBytes = (jbyteArray)env->CallObjectMethod(md, digest, input);
    jsize len = env->GetArrayLength(hashBytes);
    jbyte *bytes = env->GetByteArrayElements(hashBytes, nullptr);
    std::string hex;
    char buf[3];
    for (jsize i = 0; i < len; i++) {
        unsigned char c = (unsigned char)bytes[i];
        snprintf(buf, sizeof(buf), "%02x", c);
        hex += buf;
    }
    env->ReleaseByteArrayElements(hashBytes, bytes, JNI_ABORT);
    env->DeleteLocalRef(hashBytes);
    env->DeleteLocalRef(md);
    env->DeleteLocalRef(mdCls);
    return hex;
}

static std::string getSigningShaNative(JNIEnv *env, jobject ctx) {
    jclass ctxCls = env->GetObjectClass(ctx);
    jmethodID getPm = env->GetMethodID(ctxCls, "getPackageManager",
        "()Landroid/content/pm/PackageManager;");
    jobject pm = env->CallObjectMethod(ctx, getPm);
    jmethodID getPkgName = env->GetMethodID(ctxCls, "getPackageName",
        "()Ljava/lang/String;");
    jstring pkgStr = (jstring)env->CallObjectMethod(ctx, getPkgName);
    const char *pkgChars = env->GetStringUTFChars(pkgStr, nullptr);
    jclass pmCls = env->FindClass("android/content/pm/PackageManager");
    jmethodID getPkgInfo = env->GetMethodID(pmCls, "getPackageInfo",
        "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    jint GET_SIGNATURES = 0x00000040;
    jobject pkgInfo = env->CallObjectMethod(pm, getPkgInfo, pkgStr, GET_SIGNATURES);
    env->ReleaseStringUTFChars(pkgStr, pkgChars);
    if (pkgInfo == nullptr) { LOGE("getSigningShaNative: getPackageInfo returned null"); return ""; }
    jclass pkgInfoCls = env->FindClass("android/content/pm/PackageInfo");
    jfieldID sigsField = env->GetFieldID(pkgInfoCls, "signatures", "[Landroid/content/pm/Signature;");
    jobjectArray sigs = (jobjectArray)env->GetObjectField(pkgInfo, sigsField);
    if (sigs == nullptr || env->GetArrayLength(sigs) == 0) { return ""; }
    jobject firstSig = env->GetObjectArrayElement(sigs, 0);
    jclass sigCls = env->FindClass("android/content/pm/Signature");
    jmethodID toByteArray = env->GetMethodID(sigCls, "toByteArray", "()[B");
    jbyteArray certBytes = (jbyteArray)env->CallObjectMethod(firstSig, toByteArray);
    std::string sha = sha256_hex(env, certBytes);
    env->DeleteLocalRef(certBytes);
    env->DeleteLocalRef(firstSig);
    env->DeleteLocalRef(sigs);
    env->DeleteLocalRef(pkgInfo);
    env->DeleteLocalRef(pkgInfoCls);
    env->DeleteLocalRef(pmCls);
    env->DeleteLocalRef(pm);
    env->DeleteLocalRef(ctxCls);
    return sha;
}

static jbyteArray deriveKeyNative(JNIEnv *env, jobject ctx) {
    std::string sha = getSigningShaNative(env, ctx);
    // Lowercase + remove colons (same as Kotlin currentSigningSha output)
    for (auto &c : sha) c = tolower(c);
    // Remove possible colons
    sha.erase(std::remove(sha.begin(), sha.end(), ':'), sha.end());

    if (sha.empty()) return nullptr;

    // build seed = "ghosttype_obf_v1::" + pkgName + "::" + sha
    jclass ctxCls = env->GetObjectClass(ctx);
    jmethodID getPkgName = env->GetMethodID(ctxCls, "getPackageName", "()Ljava/lang/String;");
    jstring pkgStr = (jstring)env->CallObjectMethod(ctx, getPkgName);
    const char *pkg = env->GetStringUTFChars(pkgStr, nullptr);
    std::string seed = "ghosttype_obf_v1::";
    seed += pkg;
    seed += "::";
    seed += sha;
    env->ReleaseStringUTFChars(pkgStr, pkg);
    env->DeleteLocalRef(pkgStr);
    env->DeleteLocalRef(ctxCls);

    jbyteArray seedArr = env->NewByteArray(seed.size());
    env->SetByteArrayRegion(seedArr, 0, seed.size(), (const jbyte*)seed.c_str());

    // SHA-256 of seed = key
    jclass mdCls = env->FindClass("java/security/MessageDigest");
    jmethodID getInstance = env->GetStaticMethodID(mdCls, "getInstance",
        "(Ljava/lang/String;)Ljava/security/MessageDigest;");
    jobject md = env->CallStaticObjectMethod(mdCls, getInstance, env->NewStringUTF("SHA-256"));
    jmethodID digest = env->GetMethodID(mdCls, "digest", "([B)[B");
    jbyteArray keyBytes = (jbyteArray)env->CallObjectMethod(md, digest, seedArr);

    env->DeleteLocalRef(seedArr);
    env->DeleteLocalRef(md);
    env->DeleteLocalRef(mdCls);

    return keyBytes;
}

// ---------- XOR decryption (core dependency) ----------

// Kotlin calls this instead of doing XOR in Java/Kotlin bytecode.
// If someone removes nativeguard.so, EVERY decode() returns garbage.
extern "C" JNIEXPORT jstring JNICALL
Java_com_ghosttype_security_NativeGuard_nativeDecrypt(
    JNIEnv *env, jclass /*clazz*/, jobject ctx, jstring encryptedB64) {

    if (ctx == nullptr || encryptedB64 == nullptr) return env->NewStringUTF("");

    const char *encStr = env->GetStringUTFChars(encryptedB64, nullptr);
    if (encStr == nullptr) return env->NewStringUTF("");

    // Base64 decode
    jclass b64Cls = env->FindClass("android/util/Base64");
    jmethodID b64Decode = env->GetStaticMethodID(b64Cls, "decode",
        "(Ljava/lang/String;I)[B");
    jbyteArray encBytes = (jbyteArray)env->CallStaticObjectMethod(b64Cls, b64Decode,
        encryptedB64, 0);
    env->DeleteLocalRef(b64Cls);

    if (encBytes == nullptr) {
        env->ReleaseStringUTFChars(encryptedB64, encStr);
        return env->NewStringUTF("");
    }

    // Get key
    jbyteArray keyBytes = deriveKeyNative(env, ctx);
    if (keyBytes == nullptr) {
        env->ReleaseStringUTFChars(encryptedB64, encStr);
        env->DeleteLocalRef(encBytes);
        return env->NewStringUTF("");
    }

    jsize encLen = env->GetArrayLength(encBytes);
    jsize keyLen = env->GetArrayLength(keyBytes);
    jbyte *encData = env->GetByteArrayElements(encBytes, nullptr);
    jbyte *keyData = env->GetByteArrayElements(keyBytes, nullptr);

    // XOR in native
    std::vector<jbyte> out(encLen);
    for (jsize i = 0; i < encLen; i++) {
        out[i] = encData[i] ^ keyData[i % keyLen];
    }

    env->ReleaseByteArrayElements(encBytes, encData, JNI_ABORT);
    env->ReleaseByteArrayElements(keyBytes, keyData, JNI_ABORT);
    env->DeleteLocalRef(keyBytes);

    jstring result = env->NewStringUTF(std::string(out.begin(), out.end()).c_str());

    env->DeleteLocalRef(encBytes);
    env->ReleaseStringUTFChars(encryptedB64, encStr);

    return result;
}

// ---------- signing SHA (native) ----------

extern "C" JNIEXPORT jstring JNICALL
Java_com_ghosttype_security_NativeGuard_nativeCurrentSigningSha(
    JNIEnv *env, jclass /*clazz*/, jobject ctx) {
    if (ctx == nullptr) return env->NewStringUTF("");
    std::string sha = getSigningShaNative(env, ctx);
    for (auto &c : sha) c = tolower(c);
    return env->NewStringUTF(sha.c_str());
}

// ---------- verify signing SHA ----------

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ghosttype_security_NativeGuard_verifySigningSha(
    JNIEnv *env, jclass /*clazz*/, jobject ctx, jstring expectedSha) {
    if (ctx == nullptr || expectedSha == nullptr) return JNI_FALSE;
    const char *expected = env->GetStringUTFChars(expectedSha, nullptr);
    std::string actual = getSigningShaNative(env, ctx);
    if (actual.empty()) { env->ReleaseStringUTFChars(expectedSha, expected); return JNI_FALSE; }
    bool match = (strcasecmp(expected, actual.c_str()) == 0);
    env->ReleaseStringUTFChars(expectedSha, expected);
    return match ? JNI_TRUE : JNI_FALSE;
}

// ---------- pastebin ID verification (native HMAC) ----------

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ghosttype_security_NativeGuard_nativeVerifyPastebinIds(
    JNIEnv *env, jclass /*clazz*/, jobject ctx,
    jstring approvalUrl, jstring crashUrl, jstring updateUrl,
    jstring expectedHmac, jstring saltEncrypted) {

    if (ctx == nullptr || approvalUrl == nullptr || crashUrl == nullptr ||
        updateUrl == nullptr || expectedHmac == nullptr || saltEncrypted == nullptr) {
        return JNI_FALSE;
    }

    // Decrypt salt
    jclass b64Cls = env->FindClass("android/util/Base64");
    jmethodID b64Decode = env->GetStaticMethodID(b64Cls, "decode", "(Ljava/lang/String;I)[B");
    jbyteArray saltEncBytes = (jbyteArray)env->CallStaticObjectMethod(b64Cls, b64Decode, saltEncrypted, 0);
    env->DeleteLocalRef(b64Cls);
    if (saltEncBytes == nullptr) return JNI_FALSE;

    jbyteArray keyBytes = deriveKeyNative(env, ctx);
    if (keyBytes == nullptr) { env->DeleteLocalRef(saltEncBytes); return JNI_FALSE; }

    jsize saltLen = env->GetArrayLength(saltEncBytes);
    jsize kLen = env->GetArrayLength(keyBytes);
    jbyte *saltData = env->GetByteArrayElements(saltEncBytes, nullptr);
    jbyte *kData = env->GetByteArrayElements(keyBytes, nullptr);

    std::vector<jbyte> saltOut(saltLen);
    for (jsize i = 0; i < saltLen; i++) {
        saltOut[i] = saltData[i] ^ kData[i % kLen];
    }
    env->ReleaseByteArrayElements(saltEncBytes, saltData, JNI_ABORT);
    env->ReleaseByteArrayElements(keyBytes, kData, JNI_ABORT);
    env->DeleteLocalRef(keyBytes);

    std::string salt(saltOut.begin(), saltOut.end());
    env->DeleteLocalRef(saltEncBytes);
    if (salt.empty()) return JNI_FALSE;

    // Extract IDs from URLs
    auto lastSegment = [](const std::string &url) -> std::string {
        auto pos = url.rfind('/');
        return (pos == std::string::npos) ? url : url.substr(pos + 1);
    };

    const char *aUrl = env->GetStringUTFChars(approvalUrl, nullptr);
    const char *cUrl = env->GetStringUTFChars(crashUrl, nullptr);
    const char *uUrl = env->GetStringUTFChars(updateUrl, nullptr);

    std::string ids = lastSegment(aUrl) + "|" + lastSegment(cUrl) + "|" + lastSegment(uUrl);

    env->ReleaseStringUTFChars(approvalUrl, aUrl);
    env->ReleaseStringUTFChars(crashUrl, cUrl);
    env->ReleaseStringUTFChars(updateUrl, uUrl);

    // Compute HMAC-SHA256(salt, ids) via JNI
    jclass macCls = env->FindClass("javax/crypto/Mac");
    jmethodID macGetInstance = env->GetStaticMethodID(macCls, "getInstance",
        "(Ljava/lang/String;)Ljavax/crypto/Mac;");
    jobject mac = env->CallStaticObjectMethod(macCls, macGetInstance, env->NewStringUTF("HmacSHA256"));

    jclass specCls = env->FindClass("javax/crypto/spec/SecretKeySpec");
    jmethodID specCtor = env->GetMethodID(specCls, "<init>", "([BLjava/lang/String;)V");

    jbyteArray saltArr = env->NewByteArray(salt.size());
    env->SetByteArrayRegion(saltArr, 0, salt.size(), (const jbyte*)salt.c_str());
    jobject keySpec = env->NewObject(specCls, specCtor, saltArr, env->NewStringUTF("HmacSHA256"));
    env->DeleteLocalRef(saltArr);

    jmethodID macInit = env->GetMethodID(macCls, "init", "(Ljava/security/Key;)V");
    env->CallVoidMethod(mac, macInit, keySpec);
    env->DeleteLocalRef(keySpec);

    jbyteArray idsArr = env->NewByteArray(ids.size());
    env->SetByteArrayRegion(idsArr, 0, ids.size(), (const jbyte*)ids.c_str());
    jmethodID macDoFinal = env->GetMethodID(macCls, "doFinal", "([B)[B");
    jbyteArray hmacBytes = (jbyteArray)env->CallObjectMethod(mac, macDoFinal, idsArr);

    env->DeleteLocalRef(idsArr);
    env->DeleteLocalRef(mac);
    env->DeleteLocalRef(macCls);
    env->DeleteLocalRef(specCls);

    if (hmacBytes == nullptr) return JNI_FALSE;

    std::string hmacHex = sha256_hex(env, hmacBytes);
    env->DeleteLocalRef(hmacBytes);

    const char *expected = env->GetStringUTFChars(expectedHmac, nullptr);
    bool match = (strcasecmp(expected, hmacHex.c_str()) == 0);
    env->ReleaseStringUTFChars(expectedHmac, expected);

    return match ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_ghosttype_security_NativeGuard_isDebuggerAttachedNative(JNIEnv *env, jclass /*clazz*/) {
    jclass debugCls = env->FindClass("android/os/Debug");
    jmethodID isConnected = env->GetStaticMethodID(debugCls, "isDebuggerConnected", "()Z");
    jboolean connected = env->CallStaticBooleanMethod(debugCls, isConnected);
    env->DeleteLocalRef(debugCls);
    return connected;
}
