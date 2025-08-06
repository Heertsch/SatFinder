cmake_minimum_required(VERSION 3.10.2)

project(sgp4native)

# SGP4-Quellen direkt einbinden
set(SGP4_SRC
    ../../../../sgp4/libsgp4/CoordGeodetic.cc
    ../../../../sgp4/libsgp4/CoordTopocentric.cc
    ../../../../sgp4/libsgp4/DateTime.cc
    ../../../../sgp4/libsgp4/DecayedException.cc
    ../../../../sgp4/libsgp4/Eci.cc
    ../../../../sgp4/libsgp4/Globals.cc
    ../../../../sgp4/libsgp4/Observer.cc
    ../../../../sgp4/libsgp4/OrbitalElements.cc
    ../../../../sgp4/libsgp4/SatelliteException.cc
    ../../../../sgp4/libsgp4/SGP4.cc
    ../../../../sgp4/libsgp4/SolarPosition.cc
    ../../../../sgp4/libsgp4/TimeSpan.cc
    ../../../../sgp4/libsgp4/Tle.cc
    ../../../../sgp4/libsgp4/TleException.cc
    ../../../../sgp4/libsgp4/Util.cc
    ../../../../sgp4/libsgp4/Vector.cc
)

# JNI-Quellen
set(SGP4_JNI_SRC sgp4_jni.cpp)

# Erzeuge die native Bibliothek
add_library(sgp4native SHARED ${SGP4_JNI_SRC} ${SGP4_SRC})
#set_target_properties(sgp4native PROPERTIES LINK_FLAGS "-Wl,--max-page-size=16384")
# Include-Pfade für JNI und SGP4
include_directories(
    ../../../../sgp4/libsgp4
    ../../../../sgp4/libsgp4/include
    ${CMAKE_SOURCE_DIR}
)
