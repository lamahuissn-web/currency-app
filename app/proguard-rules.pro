# No reflection-driven class lookups in this app: no WorkManager workers, no manifest-declared
# receivers constructed by name. R8 may rename freely.
-dontwarn okhttp3.**
-dontwarn okio.**
