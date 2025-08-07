cmake_minimum_required(VERSION 3.10.2)

project(sgp4native)

# SGP4-Quellen aus dem Projekt-Repository
set(SGP4_SRC
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/CoordGeodetic.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/CoordTopocentric.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/DateTime.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/DecayedException.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/Eci.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/Globals.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/Observer.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/OrbitalElements.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/SatelliteException.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/SGP4.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/SolarPosition.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/TimeSpan.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/Tle.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/TleException.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/Util.cc
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4/Vector.cc
)

# JNI-Quellen
set(SGP4_JNI_SRC sgp4_jni.cpp)

# Erzeuge die native Bibliothek
add_library(sgp4native SHARED ${SGP4_JNI_SRC} ${SGP4_SRC})

# Include-Pfade für JNI und SGP4
include_directories(
    ${CMAKE_SOURCE_DIR}/../../../../sgp4/libsgp4
    ${CMAKE_SOURCE_DIR}
)
