#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

#include <fstream>
#include <string>

namespace {
constexpr const char* kTag = "DeepMuleJNI";
ANativeWindow* gWindow = nullptr;

void logInfo(const std::string& message) {
    __android_log_print(ANDROID_LOG_INFO, kTag, "%s", message.c_str());
}

std::string jstringToString(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return "";
    }

    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result = chars == nullptr ? "" : chars;
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return result;
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeLoadCore(
        JNIEnv* env,
        jobject /* thiz */,
        jstring corePath
) {
    std::string path = jstringToString(env, corePath);
    if (path.empty()) {
        return JNI_FALSE;
    }
    logInfo("Core solicitado: " + path);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeLoadGame(
        JNIEnv* env,
        jobject /* thiz */,
        jstring gamePath
) {
    std::string path = jstringToString(env, gamePath);
    if (path.empty()) {
        return JNI_FALSE;
    }
    logInfo("Jogo solicitado: " + path);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeAttachSurface(
        JNIEnv* env,
        jobject /* thiz */,
        jobject surface
) {
    if (surface == nullptr) {
        return JNI_FALSE;
    }

    if (gWindow != nullptr) {
        ANativeWindow_release(gWindow);
        gWindow = nullptr;
    }

    gWindow = ANativeWindow_fromSurface(env, surface);
    return gWindow != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeDetachSurface(
        JNIEnv* /* env */,
        jobject /* thiz */
) {
    if (gWindow != nullptr) {
        ANativeWindow_release(gWindow);
        gWindow = nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeStep(
        JNIEnv* /* env */,
        jobject /* thiz */
) {
    // Placeholder para o loop de frame real (fase seguinte).
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeSaveState(
        JNIEnv* env,
        jobject /* thiz */,
        jint slot,
        jstring outputPath
) {
    std::string path = jstringToString(env, outputPath);
    if (path.empty()) {
        return JNI_FALSE;
    }

    std::ofstream file(path, std::ios::binary | std::ios::trunc);
    if (!file.is_open()) {
        return JNI_FALSE;
    }

    file << "deepmule_state_slot=" << slot;
    file.close();
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeLoadState(
        JNIEnv* env,
        jobject /* thiz */,
        jint /* slot */,
        jstring inputPath
) {
    std::string path = jstringToString(env, inputPath);
    if (path.empty()) {
        return JNI_FALSE;
    }

    std::ifstream file(path, std::ios::binary);
    return file.good() ? JNI_TRUE : JNI_FALSE;
}

