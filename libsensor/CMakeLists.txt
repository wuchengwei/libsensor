# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

project("sensor")

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
        sensor
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        src/main/cpp/sensor.cpp )

find_library(log-lib log)

add_library(swr-interface SHARED IMPORTED)
set_target_properties(
        swr-interface
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/vendor/swr/lib/${ANDROID_ABI}/libSWR_OpenInterface_${ANDROID_ABI}.so )
include_directories(vendor/swr/include)

add_library(libc++_shared SHARED IMPORTED)
set_target_properties(
        libc++_shared
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/vendor/swr/lib/${ANDROID_ABI}/libc++_shared.so )
include_directories(vendor/swr/include)

target_link_libraries(sensor swr-interface libc++_shared ${log-lib})