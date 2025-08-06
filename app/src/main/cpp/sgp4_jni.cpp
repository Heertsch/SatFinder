#include <jni.h>
#include <string>
#include "SGP4.h"
#include "Tle.h"
#include "Eci.h"
#include "CoordGeodetic.h"
#include "Globals.h"

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_support_hb9hci_satfinder_Sgp4Native_propagate(JNIEnv *env, jclass,
                                                  jstring tleLine1,
                                                  jstring tleLine2,
                                                  jdouble minutesSinceEpoch) {
    // TLE-Zeilen aus Java-Strings holen
    const char *tle1 = env->GetStringUTFChars(tleLine1, nullptr);
    const char *tle2 = env->GetStringUTFChars(tleLine2, nullptr);

    printf("[JNI] propagate called!\n");
    printf("[JNI] TLE1: %s\n", tle1);
    printf("[JNI] TLE2: %s\n", tle2);
    printf("[JNI] minutesSinceEpoch: %f\n", (double)minutesSinceEpoch);
    fflush(stdout);

    // TLE und SGP4 initialisieren
    libsgp4::Tle tle("SGP4SAT", tle1, tle2);
    libsgp4::SGP4 sgp4(tle);

    // Propagation durchführen
    libsgp4::Eci eci = sgp4.FindPosition(minutesSinceEpoch);
    libsgp4::CoordGeodetic geo = eci.ToGeodetic();

    printf("[JNI] Ergebnis: lat=%f, lon=%f, alt=%f\n", geo.latitude * 180.0 / M_PI, geo.longitude * 180.0 / M_PI, geo.altitude * 1000.0);
    fflush(stdout);

    // Ergebnis-Array: [lat, lon, alt]
    jdoubleArray result = env->NewDoubleArray(3);
    double vals[3];
    vals[0] = geo.latitude * 180.0 / M_PI; // Grad
    vals[1] = geo.longitude * 180.0 / M_PI; // Grad
    vals[2] = geo.altitude * 1000.0; // Meter
    env->SetDoubleArrayRegion(result, 0, 3, vals);

    // Speicher freigeben
    env->ReleaseStringUTFChars(tleLine1, tle1);
    env->ReleaseStringUTFChars(tleLine2, tle2);

    return result;
}
