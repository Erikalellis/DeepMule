cmake_minimum_required(VERSION 3.22.1)
project(libretrodroid_jni)

add_library(
    libretrodroid_jni
    SHARED
    libretrodroid_jni.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(dl-lib dl)

target_link_libraries(
    libretrodroid_jni
    ${log-lib}
    ${android-lib}
    ${dl-lib}
)

