# SatFinder Build Instructions

## Voraussetzungen

1. Android Studio mit NDK-Unterstützung
2. CMake (Version 3.10.2 oder höher)
3. Android NDK (Version 26.1.10909125 oder kompatibel)

## Build-Setup

### 1. Repository klonen
```bash
git clone <repository-url>
cd SatFinder
```

### 2. Android Studio Setup
- Öffnen Sie das Projekt in Android Studio
- Stellen Sie sicher, dass die folgenden SDK-Komponenten installiert sind:
  - Android SDK Platform 35
  - Android NDK (Version 26.1.10909125)
  - CMake

### 3. Native Dependencies
Das Projekt enthält die SGP4-Bibliothek als Teil des Repositorys im `sgp4/` Verzeichnis. 
Keine zusätzlichen Downloads erforderlich.

### 4. Build
- Öffnen Sie das Projekt in Android Studio
- Gradle sollte automatisch synchronisieren
- Build > Make Project

## Fehlerbehebung

### CMake-Fehler beim Build
Wenn CMake die SGP4-Quellen nicht finden kann:
1. Überprüfen Sie, dass das `sgp4/libsgp4/` Verzeichnis existiert
2. Führen Sie "Clean Project" aus und bauen Sie erneut

### NDK-Version-Konflikte
Falls NDK-Versionskonflikte auftreten, aktualisieren Sie die NDK-Version in `app/build.gradle.kts`:
```kotlin
ndkVersion = "26.1.10909125"
```

## Projektstruktur
```
SatFinder/
├── app/
│   ├── src/main/cpp/          # Native C++ Code
│   │   ├── CMakeLists.txt     # CMake Build-Konfiguration
│   │   └── sgp4_jni.cpp       # JNI-Interface
│   └── build.gradle.kts       # Android Build-Konfiguration
├── sgp4/                      # SGP4-Bibliothek (enthalten)
│   └── libsgp4/              # SGP4-Quellcode
└── BUILD_INSTRUCTIONS.md     # Diese Datei
```
