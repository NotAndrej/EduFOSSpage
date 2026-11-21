# EduFOSSpage

A small, privacy-respecting client for [EduPage](https://edupage.org) — the school
information system used by many schools. It shows your timetable, grades and
messages, and stores your credentials encrypted on the device.

The project builds two things from a single `:app` module:

| Target | Build task | Application ID | Notes |
| --- | --- | --- | --- |
| Phone | `:app:assembleDebug` / `:app:assembleRelease` | `org.edufosspage` | Material 3, `minSdk 23` |
| Wear OS | `:app:assembleWear` / `:app:assembleWearRelease` | `org.edufosspage.wear` | Wear Compose, `minSdk 30`, standalone |

The watch build reuses the phone app's `org.edufosspage.data` layer and its
UI-agnostic view models verbatim. Only the presentation layer differs.

## Building

```bash
# Phone
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease

# Wear OS watch
./gradlew :app:assembleWear          # debuggable, signed with the debug key
./gradlew :app:assembleWearRelease   # minified + resource-shrunk (unsigned by default)
```

Artifacts land in `app/build/outputs/apk/<variant>/`.

### Release signing

Signing material is never committed. Provide it through environment variables or
Gradle properties; without it, minified builds still assemble and stay unsigned
(which is what reproducible/F-Droid builds expect):

```bash
export EDUFOSS_STORE_FILE=/path/to/release.jks
export EDUFOSS_STORE_PASSWORD=…
export EDUFOSS_KEY_ALIAS=…
export EDUFOSS_KEY_PASSWORD=…
```

or the equivalent `edufoss.storeFile`, `edufoss.storePassword`, `edufoss.keyAlias`,
`edufoss.keyPassword` Gradle properties.

## Wear OS notes

- The watch UI lives in `app/src/wear/` and is only compiled into the `wear*`
  variants, so the phone APK is unaffected.
- The watch manifest (`app/src/wear/AndroidManifest.xml`) declares
  `android.hardware.type.watch`, makes the app standalone, drops the phone
  launcher and installs the watch launcher instead.
- Wear Compose is scoped with `wearImplementation` / `wearReleaseImplementation`.
- The watch build is limited to `armeabi-v7a`, matching the target device.
- Round-screen layout uses `ScalingLazyColumn`, `PositionIndicator` and
  `TimeText`; the palette is derived from the phone app's dark blue scheme.
- A 6-digit PIN lock guards the watch UI, set from the Account screen. It shares
  the phone app's `PinStore` (PBKDF2-HMAC-SHA256, salted per PIN) and has a fixed
  one-minute unlock window after the app is backgrounded, so a glance at the
  watch does not force a re-entry but walking away does. There is no biometric
  prompt on the watch.

## Privacy

- Credentials are stored with `androidx.security:security-crypto`
  (`EncryptedStore` / `CredentialStore`); nothing is written in clear text.
- An optional PIN is stored only as a salted PBKDF2-HMAC-SHA256 hash
  (`PinStore`); the digits themselves are never written to disk.
- No analytics, no ads, no WebView, no Google Play Services.
- Network access is limited to the configured EduPage school host.

## License

GPL-3.0-or-later — see [LICENSE](LICENSE).

The upstream EduFOSSpage sources carried no license file. Because this is a
student-data application, the project makes the conservative, F-Droid-friendly
choice of GPL-3.0-or-later. The dependency set (AndroidX, Compose, Material,
`FreeDroidWarn`) is Apache-2.0 and therefore compatible. The copyright holder
may relicense at will.

## Known limitations

- Only the watch essentials are implemented: sign-in, timetable, grades and a
  read-only message list/detail view. Composed new messages, the dashboard,
  settings, themes, background notifications and update checks exist only in the
  phone build. (The PIN lock exists on both; the watch sets its own.)
- The watch UI is not yet localised; strings are hard-coded English.
- Credentials on Wear are stored in the app's own encrypted store; there is no
  phone-to-watch credential sync.
