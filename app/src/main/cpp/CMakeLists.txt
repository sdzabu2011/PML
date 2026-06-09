cmake_minimum_required(VERSION 3.22.1)
project("nyx_esp")

# Include directories for memory mapping, math and ImGui
include_directories(ue4)
include_directories(memory)
include_directories(imgui)

# Add all ImGui source files if you decide to implement native UI later
# However, this current version binds memory reading to Java Overlay
file(GLOB_RECURSE CPP_SOURCES "*.cpp" "*.c")

add_library(
        nyx_esp
        SHARED
        ${CPP_SOURCES})

find_library(
        log-lib
        log)

# Add Android GLES and EGL if drawing from Native
find_library(EGL-lib EGL)
find_library(GLESv2-lib GLESv2)
find_library(android-lib android)

target_link_libraries(
        nyx_esp
        ${log-lib}
        ${EGL-lib}
        ${GLESv2-lib}
        ${android-lib})
