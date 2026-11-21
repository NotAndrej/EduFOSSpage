# EduFOSSpage

A small, privacy-respecting **Android** client for [EduPage](https://edupage.org) — the
school information system used by many schools. It shows your timetable, grades and
messages, stores your credentials encrypted on the device, and talks to nothing but your
school's server.

> An optional **Wear OS** build ships the same app to the wrist. It is a secondary target
> and does not affect the phone APK; see [Wear OS build](#wear-os-build-optional).

## Features

- **Timetable** — day and week views, with cancellations and substitutions marked.
- **Grades** — every subject, with class averages.
- **Messages** — a read-only list and detail view.
- **Notifications** — background polling for new messages, grades and schedule changes,
  with deep links back into the app.
- **App lock** — an optional PIN and/or biometric prompt.
- **Themes** — system, light, dark and OLED black.
- **Local cache** for instant launch, plus a built-in self-update check.
- No accounts beyond your school's, no ads, no analytics, no WebView, no Google Play
  Services.

The UI ships in English, Slovak, Czech, Hungarian and Polish.

## Building

The app is a single `:app` module:

```bash
./gradlew :app:assembleDebug     # phone, debug-signed
./gradlew :app:assembleRelease   # phone, minified (see signing below)
```

Artifacts land in `app/build/outputs/apk/<variant>/`.

### Release signing

Signing material is never committed. Provide it through environment variables or
Gradle properties; without it, minified builds still assemble and stay unsigned
(which is what reproducible/F-Droid builds expect):

```bash
export EDUFOSS_STORE_FILE=/path/to/release.jks
export EDUFOSS_STORE_PASSWORD=...
export EDUFOSS_KEY_ALIAS=...
export EDUFOSS_KEY_PASSWORD=...
```

or the equivalent `edufoss.storeFile`, `edufoss.storePassword`, `edufoss.keyAlias`,
`edufoss.keyPassword` Gradle properties.

## Privacy

- Credentials are stored with `androidx.security:security-crypto`
  (`EncryptedStore` / `CredentialStore`); nothing is written in clear text.
- An optional PIN is stored only as a salted PBKDF2-HMAC-SHA256 hash
  (`PinStore`); the digits themselves are never written to disk.
- No analytics, no ads, no WebView, no Google Play Services.
- Network access is limited to the configured EduPage school host.

## Wear OS build (optional)

The phone app is the main product. The watch build is derived from it and reuses the
phone app's `org.edufosspage.data` layer and its UI-agnostic view models verbatim — only
the presentation layer differs.

```bash
./gradlew :app:assembleWear          # watch, debuggable
./gradlew :app:assembleWearRelease   # watch, minified + resource-shrunk
```

- Application ID `org.edufosspage.wear`, `minSdk 30`, `armeabi-v7a`, standalone.
- The watch UI lives in `app/src/wear/` and is only compiled into the `wear*` variants, so
  the phone APK is unaffected. Wear Compose is scoped with `wearImplementation` /
  `wearReleaseImplementation`.
- The watch manifest declares `android.hardware.type.watch`, makes the app standalone and
  swaps the phone launcher for the watch one.
- Round-screen layout uses `ScalingLazyColumn`, `PositionIndicator` and `TimeText`; the
  palette is derived from the phone app's dark blue scheme.
- The watch mirrors the phone's settings — theme, privacy (PIN, screenshot blocking,
  cache), notifications and about — and honours the shared theme preference. It also runs
  the same background notification poll and self-update check, with notification taps
  deep-linking into the matching screen.
- Its PIN lock shares the phone app's `PinStore` (PBKDF2-HMAC-SHA256, salted per PIN) with
  a fixed one-minute unlock window after the app is backgrounded, so a glance at the watch
  does not force a re-entry but walking away does. There is no biometric prompt on the
  watch.

## Known limitations

- The watch build is a subset of the phone app: composing new messages and the dashboard
  are phone-only.
- Credentials on Wear are stored in the app's own encrypted store; there is no
  phone-to-watch credential sync.

## License

GPL-3.0-or-later — see [LICENSE](LICENSE).

The upstream EduFOSSpage sources carried no license file. Because this is a
student-data application, the project makes the conservative, F-Droid-friendly
choice of GPL-3.0-or-later. The dependency set (AndroidX, Compose, Material,
`FreeDroidWarn`) is Apache-2.0 and therefore compatible. The copyright holder
may relicense at will.
