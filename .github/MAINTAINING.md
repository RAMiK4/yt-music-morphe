# Maintaining

## Release

1. **Source APK** — the original YouTube Music APK (arm64, full APK) is attached to the draft release `source`. Replace it when a new version is supported. Do not delete or publish this draft.
2. **Actions → Release YT Music Morphe → Run workflow**
   - `patches`: `stable` (latest stable Morphe release) or `dev` (latest dev release)
   - `publish`: unchecked builds a test APK (clone variant only) into the run's *Artifacts*
   - `update_preview` (test builds): the APK offers the latest release, to see the update dialog

Every published build becomes the latest release. The workflow builds the official Morphe patches with the additions in `extras/`, verifies package name and signature, publishes the release (the two APKs, then `YT_Music_Settings_v<patches version>.txt` with the settings of `settings/defaults.json`) and updates `updates/*.json` for the in-app update check.

The changelog is generated from the Morphe changelog (YouTube Music only), in the same way as in `youtube-morphe`. Fixes of the mod go under `## <patches version>` in `notes/mod-fixes.md`; important features of skipped versions under `## <patches version>` in `notes/extra-features.md`; the features of the mod are in `notes/mod-features.md`.

## Configuration

| File | Purpose |
|---|---|
| `settings/defaults.json` | Recommended settings (Morphe settings export). Each setting is applied once: on first install, or on the update that adds it. |
| `branding/` | App icon (`mipmap-*`), header logo and notification icon (`drawable-*`) |
| `extras/` | Additional patches: theme defaults, default settings, update check, restart on update, experimental notice. Strings of `extras/strings/<locale>/` are appended to the YouTube Music ones |

Variant options (package, theme defaults) are set in `build_variant` calls of `.github/workflows/release.yml`.

## Secrets (Settings → Secrets and variables → Actions)

| Name | Type | Value |
|---|---|---|
| `KEYSTORE_B64` | secret | Keystore, base64 |
| `KEYSTORE_PASSWORD` | secret | Keystore password (empty: Morphe default) |
| `KEY_ALIAS` | secret | Key alias (empty: `Morphe`) |
| `KEY_PASSWORD` | secret | Key password (empty: `Morphe`) |
| `CERT_SHA256` | variable | SHA-256 fingerprint of the signing certificate |
