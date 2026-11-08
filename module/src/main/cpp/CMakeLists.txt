cmake_minimum_required(VERSION 3.22.1)

foreach(required MODULE_NAME MODULE_ID MODULE_LOG_TAG)
    if(NOT DEFINED ${required} OR "${${required}}" STREQUAL "")
        message(FATAL_ERROR "${required} must be provided by the Gradle build from module.gradle.kts")
    endif()
endforeach()
if(NOT MODULE_ID MATCHES "^[A-Za-z0-9_.-]+$" OR MODULE_ID STREQUAL "." OR MODULE_ID STREQUAL "..")
    message(FATAL_ERROR "MODULE_ID must be a single safe path component")
endif()
if(NOT MODULE_LOG_TAG MATCHES "^[A-Za-z0-9_.-]+$")
    message(FATAL_ERROR "MODULE_LOG_TAG may only contain letters, digits, '_', '.', and '-'")
endif()

project(${MODULE_NAME})

set(CXX_FLAGS "${CXX_FLAGS} -fno-exceptions -fno-rtti -fvisibility=hidden -fvisibility-inlines-hidden")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CXX_FLAGS}")

add_library(${MODULE_NAME} SHARED
        zygisk_module.cpp
        external/yyjson.c
)
target_include_directories(${MODULE_NAME} PRIVATE external)
target_link_libraries(${MODULE_NAME} log)
target_compile_definitions(${MODULE_NAME} PRIVATE
        MODULE_ID="${MODULE_ID}"
        MODULE_LOG_TAG="${MODULE_LOG_TAG}"
)
