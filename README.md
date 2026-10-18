# Low Health Warning

A client-side Fabric and Forge mod that shows a configurable warning when health is at or below your chosen threshold. Open settings through Mod Menu (Fabric), the Mods menu Config button (Forge), or press **H** in game (rebindable in Controls).

## Settings and preview

- Enable or disable warnings; choose Overlay, Title, or Both.
- Set the threshold from **0.5 to 20 hearts**.
- Customize the message with `§` or `&` color/style codes, up to 120 characters.
- Optionally count absorption hearts and allow warnings in Creative. Spectators never receive warnings.
- Enable a warning sound, adjust its volume, and choose a 1–10 second repeat interval.
- Preview the current draft from the main menu or in a world, including its sound. Preview does not alter health or saved settings.

**Done** saves and applies the draft. **Cancel** or Escape discards changes. **Reset** restores defaults in the draft; it is saved only with Done. Preview's Back button and Escape return to the same draft. Long messages scale to fit the screen.

Default settings match the published 1.2.0 release: enabled, 4 hearts, Overlay, red “Low Health!”, sound off, 50% volume, 3-second interval, absorption excluded, and Creative warnings off.

Settings remain in `config/lowhealth_config.json`. Existing settings are retained, missing fields receive defaults, and invalid values are normalized. Writes use an atomic replacement where supported. Malformed JSON is backed up next to the original and defaults are used; a save failure remains visible in the config screen.

## Release candidates

Version **1.2.1-rc.1** uses six artifacts for all 24 published Minecraft/loader targets:

| Loader | Artifact group | Runtime-tested Minecraft versions | Game runtime Java |
| --- | --- | --- | --- |
| Fabric | 1.20–1.20.6 | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 | 17 through 1.20.4; 21 for 1.20.5/6 |
| Fabric | 1.21–1.21.5 | 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, 1.21.5 | 21 |
| Fabric | 1.21.6–1.21.8 | 1.21.6, 1.21.7, 1.21.8 | 21 |
| Fabric | 1.21.9–1.21.11 | 1.21.9, 1.21.10, 1.21.11 | 21 |
| Fabric | 26.1–26.2 | 26.1, 26.1.1, 26.1.2, 26.2 | 25 |
| Forge | 1.8.9 | 1.8.9 | 8 |

Install **one** matching jar. Fabric requires Loader 0.19.5 or newer, Fabric API, and Fabric Language Kotlin; Mod Menu provides the menu entry. Forge requires Forge 11.15.1.2318 for 1.8.9, with no Fabric dependencies. Exact tested dependencies are pinned in `scripts/release-targets.json` and the generated manifests. No NeoForge build is included.

Forge preserves its published button-based layout: half-heart adjustment buttons and cycling volume/interval buttons. It has the same settings and preview behavior, plus a separate sound-test button. Fabric uses sliders for threshold and volume. Minecraft 1.20/1.20.1 receives an explicit dim background so previews remain readable over the world.

## Build and verification

Use JDK 25 to run the included Gradle wrapper. Install JDK 17 and 21 as well for the older Fabric runtime audits; `JAVA17_HOME` and `JAVA21_HOME` can point to them. Forge uses a separate Gradle 8.8 project, Java 21 for the build process, and Java 8 for compilation/runtime (`JAVA8_HOME`). See `forge-1.8.9/README.md`.

```sh
# Default development target: Minecraft 26.1.2
./gradlew build

# Build each group once, then run its identical jar on every listed version
python3 scripts/build_release.py

# Or verify one compatibility group
python3 scripts/build_release.py --group 1.21-1.21.5

# Build and test Forge separately, then collect the six selected artifacts
python3 scripts/audit_forge.py
python3 scripts/package_release.py
```

Only groups that pass every runtime audit are copied into `build/releases/1.2.1-rc.1/`. Each jar has a JSON manifest with its SHA-256, tested game versions, Java version, and pinned dependencies. The complete six-jar bundle is `build/releases/low-health-warning-1.2.1-rc.1-complete-bundle.zip`. Intermediate narrower candidates can remain in the working release folder; the packaging script selects only the final matrix groups. Installable jars exclude the test fixture. Do not install source jars or the separate client-audit jar.

The automated audit launches actual Minecraft clients through Gradle, with isolated profiles and newly created test worlds under `build/run/packaged-<version>/`. For versions before Fabric client game tests existed, a separate fixture drives real Minecraft UI events and waits for rendered frames before screenshots. It checks config controls, slider endpoints, persistence and migration, Cancel/Reset behavior, three window sizes, all preview modes, unchanged health, H shortcut, live warning thresholds, absorption and game-mode filtering, and dispatched sound events with volume and repeat timing. Logs and rendered screenshots are collected under the release folder's `reports/` directory. Sound-event assertions do not certify human audible playback. These tests do not certify multiplayer or compatibility with arbitrary mod packs.

GitHub Actions runs the same group matrix and uploads build artifacts and logs; it does not publish releases. The workflow must run on GitHub before its hosted execution can be considered verified.

## Source restoration

The checkout and GitHub history lacked the settings and preview implementation already shipped in the author's public 1.2.0 jars. Those behaviors and the JSON schema were reconstructed from those jars, then integrated with the fixes and version adapters here. This is restored source, not a recovered original development checkout.

The original four-jar verified bundle is preserved separately in `backups/verified-1.2.1-rc.1/` and retains its original checksum. The local pre-restoration source is preserved at `backups/pre-restoration-2026-10-15.zip` (ignored by Git). Published artifacts were used only as a reference; nothing has been uploaded or published by the release script.

[Public project](https://modrinth.com/mod/low-health-warning) · [Source repository](https://github.com/Sl0oth/Low_Health_Warning_Mc_Mod)
