cmake_minimum_required(VERSION 3.22.1)

project("pos_sdk_ggwave")

# Add GGWave library
add_subdirectory(ggwave)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Create our JNI wrapper library
add_library(
    pos_sdk_ggwave
    SHARED
    ggwave_jni.cpp
)

# Include directories
target_include_directories(pos_sdk_ggwave PRIVATE
    ggwave/include
)

# Link libraries
target_link_libraries(
    pos_sdk_ggwave
    ggwave
    ${log-lib}
    ${android-lib}
)

# Set C++ standard
set_target_properties(pos_sdk_ggwave PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)