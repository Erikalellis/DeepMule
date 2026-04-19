package com.example.deepmule.emu

import android.view.Surface

/**
 * Ponte JNI para runtime de emulacao.
 * Nesta fase inicial, os metodos nativos ja compilam e retornam status basico.
 */
object LibretroDroid {
    private val nativeLoaded: Boolean = runCatching {
        System.loadLibrary("libretrodroid_jni")
        true
    }.getOrElse {
        false
    }

    fun isNativeReady(): Boolean = nativeLoaded

    fun loadCore(corePath: String): Boolean =
        nativeLoaded && nativeLoadCore(corePath)

    fun loadGame(gamePath: String): Boolean =
        nativeLoaded && nativeLoadGame(gamePath)

    fun attachSurface(surface: Surface): Boolean =
        nativeLoaded && nativeAttachSurface(surface)

    fun detachSurface() {
        if (nativeLoaded) {
            nativeDetachSurface()
        }
    }

    fun step() {
        if (nativeLoaded) {
            nativeStep()
        }
    }

    fun saveState(slot: Int, outputPath: String): Boolean =
        nativeLoaded && nativeSaveState(slot, outputPath)

    fun loadState(slot: Int, inputPath: String): Boolean =
        nativeLoaded && nativeLoadState(slot, inputPath)

    private external fun nativeLoadCore(corePath: String): Boolean
    private external fun nativeLoadGame(gamePath: String): Boolean
    private external fun nativeAttachSurface(surface: Surface): Boolean
    private external fun nativeDetachSurface()
    private external fun nativeStep()
    private external fun nativeSaveState(slot: Int, outputPath: String): Boolean
    private external fun nativeLoadState(slot: Int, inputPath: String): Boolean
}
