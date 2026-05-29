#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

#include <dlfcn.h>
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <fstream>
#include <mutex>
#include <string>
#include <vector>

namespace {
constexpr const char* kTag = "DeepMuleJNI";
ANativeWindow* gWindow = nullptr;
std::recursive_mutex gMutex;
bool gInputState[16] = {false};
unsigned gPixelFormat = 2; // RETRO_PIXEL_FORMAT_RGB565
std::string gSystemDir;
std::string gSaveDir;
std::vector<uint8_t> gRomData; // Persist ROM data during game session

// ─── Audio ring buffer ────────────────────────────────────────────────────────
static std::mutex gAudioMutex;
static constexpr size_t kAudioRingCapacity = 16384;
static int16_t gAudioRing[kAudioRingCapacity];
static size_t gAudioWr = 0;
static size_t gAudioRd = 0;

using retro_environment_t = bool (*)(unsigned, void*);
using retro_video_refresh_t = void (*)(const void*, unsigned, unsigned, size_t);
using retro_audio_sample_t = void (*)(int16_t, int16_t);
using retro_audio_sample_batch_t = size_t (*)(const int16_t*, size_t);
using retro_input_poll_t = void (*)();
using retro_input_state_t = int16_t (*)(unsigned, unsigned, unsigned, unsigned);

struct retro_game_info {
    const char* path;
    const void* data;
    size_t size;
    const char* meta;
};

struct retro_system_info {
    const char* library_name;
    const char* library_version;
    const char* valid_extensions;
    bool need_fullpath;
    bool block_extract;
};

struct retro_variable {
    const char *key;
    const char *value;
};

// RetroArch environment command ids
constexpr unsigned RETRO_ENVIRONMENT_SET_PIXEL_FORMAT = 10;
constexpr unsigned RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9;
constexpr unsigned RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY = 31;
constexpr unsigned RETRO_ENVIRONMENT_GET_CAN_DUPE = 7;
constexpr unsigned RETRO_ENVIRONMENT_GET_LOG_INTERFACE = 27;
constexpr unsigned RETRO_ENVIRONMENT_GET_VARIABLE = 15;
constexpr unsigned RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS = 11;
constexpr unsigned RETRO_ENVIRONMENT_SET_CONTROLLER_INFO = 12;
constexpr unsigned RETRO_ENVIRONMENT_SET_HW_RENDER = 14;
constexpr unsigned RETRO_ENVIRONMENT_GET_LANGUAGE = 21;
constexpr unsigned RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE = 35;

constexpr unsigned RETRO_DEVICE_JOYPAD = 1;

enum retro_log_level {
    RETRO_LOG_DEBUG = 0,
    RETRO_LOG_INFO,
    RETRO_LOG_WARN,
    RETRO_LOG_ERROR,
    RETRO_LOG_DUMMY = 0x7fffffff
};

typedef void (*retro_log_printf_t)(enum retro_log_level level, const char *fmt, ...);

struct retro_log_callback {
    retro_log_printf_t log;
};

void coreLogCallback(enum retro_log_level level, const char* fmt, ...) {
    char buffer[4096];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);

    int androidLevel = ANDROID_LOG_INFO;
    switch (level) {
        case RETRO_LOG_DEBUG: androidLevel = ANDROID_LOG_DEBUG; break;
        case RETRO_LOG_INFO:  androidLevel = ANDROID_LOG_INFO;  break;
        case RETRO_LOG_WARN:  androidLevel = ANDROID_LOG_WARN;  break;
        case RETRO_LOG_ERROR: androidLevel = ANDROID_LOG_ERROR; break;
        default: break;
    }
    __android_log_print(androidLevel, "DeepMuleCore", "%s", buffer);
}

constexpr unsigned RETRO_PIXEL_FORMAT_0RGB1555 = 0;
constexpr unsigned RETRO_PIXEL_FORMAT_XRGB8888 = 1;
constexpr unsigned RETRO_PIXEL_FORMAT_RGB565 = 2;

constexpr int WINDOW_FMT_RGB_565 = 4;

struct LibretroApi {
    void* coreHandle = nullptr;
    bool gameLoaded = false;

    void (*retro_set_environment)(retro_environment_t) = nullptr;
    void (*retro_set_video_refresh)(retro_video_refresh_t) = nullptr;
    void (*retro_set_audio_sample)(retro_audio_sample_t) = nullptr;
    void (*retro_set_audio_sample_batch)(retro_audio_sample_batch_t) = nullptr;
    void (*retro_set_input_poll)(retro_input_poll_t) = nullptr;
    void (*retro_set_input_state)(retro_input_state_t) = nullptr;

    void (*retro_init)() = nullptr;
    void (*retro_deinit)() = nullptr;
    unsigned (*retro_api_version)() = nullptr;
    void (*retro_get_system_info)(retro_system_info*) = nullptr;
    bool (*retro_load_game)(const retro_game_info*) = nullptr;
    void (*retro_unload_game)() = nullptr;
    void (*retro_run)() = nullptr;
    void (*retro_set_controller_port_device)(unsigned, unsigned) = nullptr;
    size_t (*retro_serialize_size)() = nullptr;
    bool (*retro_serialize)(void*, size_t) = nullptr;
    bool (*retro_unserialize)(const void*, size_t) = nullptr;
};

LibretroApi gApi;

void logInfo(const std::string& message) {
    __android_log_print(ANDROID_LOG_INFO, kTag, "%s", message.c_str());
}

void logError(const std::string& message) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "%s", message.c_str());
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

bool environmentCallback(unsigned cmd, void* data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            if (data != nullptr) {
                const auto* requested = reinterpret_cast<unsigned*>(data);
                gPixelFormat = *requested;
            }
            return true;
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            if (data != nullptr) {
                *reinterpret_cast<const char**>(data) = gSystemDir.c_str();
                return true;
            }
            return false;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            if (data != nullptr) {
                *reinterpret_cast<const char**>(data) = gSaveDir.c_str();
                return true;
            }
            return false;
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            if (data != nullptr) {
                *reinterpret_cast<bool*>(data) = true;
                return true;
            }
            return false;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            if (data != nullptr) {
                auto* cb = reinterpret_cast<retro_log_callback*>(data);
                cb->log = coreLogCallback;
                return true;
            }
            return false;
        case RETRO_ENVIRONMENT_GET_VARIABLE:
            if (data != nullptr) {
                auto* var = reinterpret_cast<retro_variable*>(data);
                if (var->key) {
                    // logInfo("Core solicitou variavel: " + std::string(var->key));
                }
                var->value = nullptr;
                return true;
            }
            return false;
        case RETRO_ENVIRONMENT_GET_LANGUAGE:
            if (data != nullptr) {
                *reinterpret_cast<unsigned*>(data) = 0; // RETRO_LANGUAGE_ENGLISH
                return true;
            }
            return false;
        case RETRO_ENVIRONMENT_GET_AUDIO_VIDEO_ENABLE:
            if (data != nullptr) {
                *reinterpret_cast<int*>(data) = 3; // Enable both
                return true;
            }
            return false;
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
            return true; // Aceita mas ignora por enquanto
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
            logError("Core solicitou HW Render (OpenGL/Vulkan), mas suporte atual e apenas Software.");
            return false;
        default:
            return false;
    }
}

static inline uint16_t xrgb8888ToRgb565(uint32_t px) {
    const uint16_t r = static_cast<uint16_t>((px >> 19) & 0x1F);
    const uint16_t g = static_cast<uint16_t>((px >> 10) & 0x3F);
    const uint16_t b = static_cast<uint16_t>((px >> 3) & 0x1F);
    return static_cast<uint16_t>((r << 11) | (g << 5) | b);
}

static inline uint16_t rgb1555ToRgb565(uint16_t px) {
    const uint16_t r = static_cast<uint16_t>((px >> 10) & 0x1F);
    const uint16_t g = static_cast<uint16_t>((px >> 5) & 0x1F);
    const uint16_t b = static_cast<uint16_t>(px & 0x1F);
    const uint16_t g6 = static_cast<uint16_t>((g << 1) | (g >> 4));
    return static_cast<uint16_t>((r << 11) | (g6 << 5) | b);
}

void videoRefreshCallback(const void* data, unsigned width, unsigned height, size_t pitch) {
    if (gWindow == nullptr || data == nullptr || width == 0 || height == 0) {
        return;
    }

    ANativeWindow_setBuffersGeometry(gWindow, static_cast<int>(width), static_cast<int>(height), WINDOW_FMT_RGB_565);

    ANativeWindow_Buffer buffer{};
    if (ANativeWindow_lock(gWindow, &buffer, nullptr) != 0) {
        return;
    }

    uint8_t* dstBase = static_cast<uint8_t*>(buffer.bits);
    const auto dstStridePx = static_cast<size_t>(buffer.stride);
    const auto copyWidth = std::min(width, static_cast<unsigned>(buffer.width));
    const auto copyHeight = std::min(height, static_cast<unsigned>(buffer.height));

    if (gPixelFormat == RETRO_PIXEL_FORMAT_RGB565) {
        for (size_t y = 0; y < copyHeight; ++y) {
            const uint8_t* srcRow = static_cast<const uint8_t*>(data) + (y * pitch);
            uint16_t* dstRow = reinterpret_cast<uint16_t*>(dstBase) + (y * dstStridePx);
            std::memcpy(dstRow, srcRow, copyWidth * sizeof(uint16_t));
        }
    } else if (gPixelFormat == RETRO_PIXEL_FORMAT_XRGB8888) {
        for (size_t y = 0; y < copyHeight; ++y) {
            const uint32_t* srcRow = reinterpret_cast<const uint32_t*>(static_cast<const uint8_t*>(data) + (y * pitch));
            uint16_t* dstRow = reinterpret_cast<uint16_t*>(dstBase) + (y * dstStridePx);
            for (size_t x = 0; x < copyWidth; ++x) {
                dstRow[x] = xrgb8888ToRgb565(srcRow[x]);
            }
        }
    } else if (gPixelFormat == RETRO_PIXEL_FORMAT_0RGB1555) {
        for (size_t y = 0; y < copyHeight; ++y) {
            const uint16_t* srcRow = reinterpret_cast<const uint16_t*>(static_cast<const uint8_t*>(data) + (y * pitch));
            uint16_t* dstRow = reinterpret_cast<uint16_t*>(dstBase) + (y * dstStridePx);
            for (size_t x = 0; x < copyWidth; ++x) {
                dstRow[x] = rgb1555ToRgb565(srcRow[x]);
            }
        }
    }

    ANativeWindow_unlockAndPost(gWindow);
}

void audioSampleCallback(int16_t left, int16_t right) {
    std::lock_guard<std::mutex> lock(gAudioMutex);
    const size_t used = gAudioWr - gAudioRd;
    if (used + 2 <= kAudioRingCapacity) {
        gAudioRing[gAudioWr % kAudioRingCapacity] = left;
        ++gAudioWr;
        gAudioRing[gAudioWr % kAudioRingCapacity] = right;
        ++gAudioWr;
    }
}

size_t audioSampleBatchCallback(const int16_t* data, size_t frames) {
    std::lock_guard<std::mutex> lock(gAudioMutex);
    const size_t samples = frames * 2; // stereo interleaved
    const size_t used = gAudioWr - gAudioRd;
    const size_t space = kAudioRingCapacity - used;
    const size_t toWrite = std::min(samples, space);
    for (size_t i = 0; i < toWrite; ++i) {
        gAudioRing[gAudioWr % kAudioRingCapacity] = data[i];
        ++gAudioWr;
    }
    return frames;
}

void inputPollCallback() {
    // No-op, we update state asynchronously via nativeSetInputState
}

int16_t inputStateCallback(unsigned port, unsigned device, unsigned /*index*/, unsigned id) {
    if (port != 0 || device != RETRO_DEVICE_JOYPAD || id >= 16) {
        return 0;
    }
    return gInputState[id] ? 1 : 0;
}

template <typename T>
bool resolveSymbol(void* handle, const char* name, T* out) {
    void* symbol = dlsym(handle, name);
    if (symbol == nullptr) {
        logError(std::string("Simbolo ausente no core: ") + name);
        return false;
    }
    *out = reinterpret_cast<T>(symbol);
    return true;
}

void unloadCoreLocked() {
    if (gApi.gameLoaded && gApi.retro_unload_game != nullptr) {
        gApi.retro_unload_game();
    }
    gApi.gameLoaded = false;
    gRomData.clear();

    if (gApi.retro_deinit != nullptr) {
        gApi.retro_deinit();
    }

    if (gApi.coreHandle != nullptr) {
        dlclose(gApi.coreHandle);
    }

    gApi = LibretroApi{};

    // Reset audio ring buffer
    std::lock_guard<std::mutex> audioLock(gAudioMutex);
    gAudioWr = 0;
    gAudioRd = 0;
}

bool bindCoreApiLocked(void* handle) {
    resolveSymbol(handle, "retro_set_controller_port_device", &gApi.retro_set_controller_port_device); // Opcional

    return
        resolveSymbol(handle, "retro_set_environment", &gApi.retro_set_environment) &&
        resolveSymbol(handle, "retro_set_video_refresh", &gApi.retro_set_video_refresh) &&
        resolveSymbol(handle, "retro_set_audio_sample", &gApi.retro_set_audio_sample) &&
        resolveSymbol(handle, "retro_set_audio_sample_batch", &gApi.retro_set_audio_sample_batch) &&
        resolveSymbol(handle, "retro_set_input_poll", &gApi.retro_set_input_poll) &&
        resolveSymbol(handle, "retro_set_input_state", &gApi.retro_set_input_state) &&
        resolveSymbol(handle, "retro_init", &gApi.retro_init) &&
        resolveSymbol(handle, "retro_deinit", &gApi.retro_deinit) &&
        resolveSymbol(handle, "retro_api_version", &gApi.retro_api_version) &&
        resolveSymbol(handle, "retro_get_system_info", &gApi.retro_get_system_info) &&
        resolveSymbol(handle, "retro_load_game", &gApi.retro_load_game) &&
        resolveSymbol(handle, "retro_unload_game", &gApi.retro_unload_game) &&
        resolveSymbol(handle, "retro_run", &gApi.retro_run) &&
        resolveSymbol(handle, "retro_serialize_size", &gApi.retro_serialize_size) &&
        resolveSymbol(handle, "retro_serialize", &gApi.retro_serialize) &&
        resolveSymbol(handle, "retro_unserialize", &gApi.retro_unserialize);
}
}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeSetDirectories(
        JNIEnv* env,
        jobject /* thiz */,
        jstring systemPath,
        jstring savePath
) {
    std::lock_guard<std::recursive_mutex> lock(gMutex);
    gSystemDir = jstringToString(env, systemPath);
    gSaveDir = jstringToString(env, savePath);
    logInfo("Diretorios configurados: system=" + gSystemDir + ", save=" + gSaveDir);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeLoadCore(
        JNIEnv* env,
        jobject /* thiz */,
        jstring corePath
) {
    std::lock_guard<std::recursive_mutex> lock(gMutex);
    std::string path = jstringToString(env, corePath);
    if (path.empty()) {
        return JNI_FALSE;
    }

    unloadCoreLocked();

    void* handle = dlopen(path.c_str(), RTLD_NOW);
    if (handle == nullptr) {
        logError(std::string("Falha ao carregar core: ") + dlerror());
        return JNI_FALSE;
    }

    gApi.coreHandle = handle;
    if (!bindCoreApiLocked(handle)) {
        unloadCoreLocked();
        return JNI_FALSE;
    }

    gApi.retro_set_environment(environmentCallback);
    gApi.retro_set_video_refresh(videoRefreshCallback);
    gApi.retro_set_audio_sample(audioSampleCallback);
    gApi.retro_set_audio_sample_batch(audioSampleBatchCallback);
    gApi.retro_set_input_poll(inputPollCallback);
    gApi.retro_set_input_state(inputStateCallback);

    if (gApi.retro_set_controller_port_device) {
        gApi.retro_set_controller_port_device(0, RETRO_DEVICE_JOYPAD);
    }

    std::fill(std::begin(gInputState), std::end(gInputState), false);
    gPixelFormat = RETRO_PIXEL_FORMAT_RGB565;
    gApi.retro_init();

    retro_system_info info{};
    gApi.retro_get_system_info(&info);
    logInfo(std::string("Core carregado: ") + (info.library_name ? info.library_name : "desconhecido"));
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeLoadGame(
        JNIEnv* env,
        jobject /* thiz */,
        jstring gamePath
) {
    std::lock_guard<std::recursive_mutex> lock(gMutex);
    std::string path = jstringToString(env, gamePath);
    if (path.empty() || gApi.coreHandle == nullptr || gApi.retro_load_game == nullptr) {
        return JNI_FALSE;
    }

    if (gApi.gameLoaded && gApi.retro_unload_game != nullptr) {
        gApi.retro_unload_game();
        gApi.gameLoaded = false;
        gRomData.clear();
    }

    retro_system_info sysInfo{};
    gApi.retro_get_system_info(&sysInfo);

    retro_game_info gameInfo{};
    gameInfo.path = path.c_str();
    gameInfo.data = nullptr;
    gameInfo.size = 0;
    gameInfo.meta = nullptr;

    if (!sysInfo.need_fullpath) {
        std::ifstream file(path, std::ios::binary | std::ios::ate);
        if (file.is_open()) {
            size_t size = file.tellg();
            file.seekg(0, std::ios::beg);
            gRomData.resize(size);
            if (file.read(reinterpret_cast<char*>(gRomData.data()), size)) {
                gameInfo.data = gRomData.data();
                gameInfo.size = gRomData.size();
                logInfo("ROM carregada na memoria (" + std::to_string(size) + " bytes)");
            }
        }
    }

    const bool ok = gApi.retro_load_game(&gameInfo);
    gApi.gameLoaded = ok;
    if (!ok) {
        logError("retro_load_game retornou false");
        gRomData.clear();
        return JNI_FALSE;
    }

    logInfo("Jogo carregado: " + path);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeAttachSurface(
        JNIEnv* env,
        jobject /* thiz */,
        jobject surface
) {
    std::lock_guard<std::recursive_mutex> lock(gMutex);
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
    std::lock_guard<std::recursive_mutex> lock(gMutex);
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
    std::lock_guard<std::recursive_mutex> lock(gMutex);
    if (gApi.gameLoaded && gApi.retro_run != nullptr) {
        gApi.retro_run();
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeSaveState(
        JNIEnv* env,
        jobject /* thiz */,
        jint slot,
        jstring outputPath
) {
    std::lock_guard<std::recursive_mutex> lock(gMutex);
    std::string path = jstringToString(env, outputPath);
    if (path.empty()) {
        return JNI_FALSE;
    }

    if (gApi.gameLoaded && gApi.retro_serialize_size != nullptr && gApi.retro_serialize != nullptr) {
        const size_t size = gApi.retro_serialize_size();
        if (size > 0) {
            std::vector<uint8_t> state(size);
            if (!gApi.retro_serialize(state.data(), size)) {
                return JNI_FALSE;
            }
            std::ofstream out(path, std::ios::binary | std::ios::trunc);
            if (!out.is_open()) {
                return JNI_FALSE;
            }
            out.write(reinterpret_cast<const char*>(state.data()), static_cast<std::streamsize>(state.size()));
            return out.good() ? JNI_TRUE : JNI_FALSE;
        }
    }

    // Fallback de compatibilidade para casos sem serialize no core.
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
    std::lock_guard<std::recursive_mutex> lock(gMutex);
    std::string path = jstringToString(env, inputPath);
    if (path.empty()) {
        return JNI_FALSE;
    }

    std::ifstream in(path, std::ios::binary);
    if (!in.is_open()) {
        return JNI_FALSE;
    }

    std::vector<uint8_t> bytes((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
    if (bytes.empty()) {
        return JNI_FALSE;
    }

    if (gApi.gameLoaded && gApi.retro_unserialize != nullptr) {
        const bool ok = gApi.retro_unserialize(bytes.data(), bytes.size());
        return ok ? JNI_TRUE : JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeSetInputState(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jint buttonId,
        jboolean pressed
) {
    std::lock_guard<std::recursive_mutex> lock(gMutex);
    if (buttonId < 0 || buttonId >= 16) {
        return;
    }
    gInputState[buttonId] = (pressed == JNI_TRUE);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_deepmule_emu_LibretroDroid_nativeDrainAudio(
        JNIEnv* env,
        jobject /* thiz */,
        jshortArray outBuffer,
        jint maxSamples
) {
    std::lock_guard<std::mutex> lock(gAudioMutex);
    const size_t available = gAudioWr - gAudioRd;
    const size_t toRead = std::min(available, static_cast<size_t>(maxSamples));
    if (toRead == 0) return 0;

    jshort* ptr = env->GetShortArrayElements(outBuffer, nullptr);
    if (!ptr) return 0;

    for (size_t i = 0; i < toRead; ++i) {
        ptr[i] = gAudioRing[gAudioRd % kAudioRingCapacity];
        ++gAudioRd;
    }

    env->ReleaseShortArrayElements(outBuffer, ptr, 0);
    return static_cast<jint>(toRead);
}
