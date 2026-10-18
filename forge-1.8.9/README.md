# Forge 1.8.9 port

This restores the author's published 1.2.0 Forge settings and warning behavior, with the config/preview fixes from the Fabric update. It uses H or the Forge Mods menu's Config button. The original Forge layout retains half-heart buttons and cycling volume/interval controls.

Build with Java 21 and run with Java 8:

```sh
JAVA_HOME="$JAVA21_HOME" ./gradlew build -Porg.gradle.java.installations.paths="$JAVA8_HOME"
```

From the repository root, run the packaged client audit:

```sh
JAVA21_HOME=/path/to/jdk21 JAVA8_HOME=/path/to/jdk8 python3 scripts/audit_forge.py
```

The script also recognizes the local runtimes under `build/toolchains/`. On Apple Silicon the legacy LWJGL 2 natives require an x86_64 Java 8 runtime and Rosetta; the verified local run used that combination. The script launches Forge directly, without the official launcher, in `build/run/packaged-1.8.9-forge/`. Its test mod stays separate from the distributable jar. It uses Mojang's version-matched logging libraries and official logging configuration, checked against their published SHA-1 values.

The audit checks the Mods-menu Config entry, H shortcut, controls and persistence, migration and malformed-file backup, Cancel/Escape/Reset, three window sizes, all preview modes, preview sound without replay on resize, actual live warning and sound cadence, absorption, Creative/Spectator filtering, unchanged health, and clean shutdown. Only passing candidates are copied to the release folder. No publishing occurs.

`WarningConfig`, `WarningState`, and `ConfigStore` mirror the shared behavior with Java 8-compatible syntax/APIs. Keep them aligned when making future behavior changes.
