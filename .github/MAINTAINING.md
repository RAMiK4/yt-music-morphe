# Maintaining

## Release

1. **Source APK** — the original YouTube Music APK (arm64, full APK) is attached to the draft release `source`. Replace it when a new version is supported. Do not delete or publish this draft.
2. **Actions → Release YT Music Morphe → Run workflow**
   - `patches`: `stable` (latest stable Morphe release) or `dev` (latest dev release)
   - `publish`: unchecked builds a test APK (clone variant only) into the run's *Artifacts*
   - `update_preview` (test builds): the APK offers the latest release, to see the update dialog

Every published build becomes the latest release. The workflow builds the official Morphe patches with the additions in `extras/`, verifies package name and signature, uploads the two APKs to R2, publishes the release (`YT_Music_Settings_v<patches version>.txt` with the settings of `settings/defaults.json`, and links to the APKs on R2) and updates `updates/*.json` for the in-app update check.

The changelog is generated from the Morphe changelog (YouTube Music only), in the same way as in `youtube-morphe`. Fixes of the mod go under `## <patches version>` in `notes/mod-fixes.md`; important features of skipped versions under `## <patches version>` in `notes/extra-features.md`; the features of the mod are in `notes/mod-features.md`.

## 4PDA post

`notes/4pda-post.txt` is the post for the 4PDA topic, updated for each release (YT Music and patches versions, What's new, links), in the same layout as the post of `youtube-morphe`.

- An empty line after each `[spoiler=...]` title
- No "test version" note next to dev patches versions, users could misread it
- What's new is a plain list, without a line for the YT Music and patches versions. A stable version lists its full changelog, the best YT Music features first; a dev version lists the new features of the last 2 dev versions
- Link titles in `[color="#808080"]`, followed by the file size
- Below the `YT Music Morphe Settings` title, a 🔗 link to `YT_Music_Settings_v<patches version>.txt` of the release (`releases/download/<tag>/...`) with its size

## Configuration

| File | Purpose |
|---|---|
| `settings/defaults.json` | Recommended settings (Morphe settings export). Each setting is applied once: on first install, or on the update that adds it. |
| `branding/` | App icon (`mipmap-*`), header logo and notification icon (`drawable-*`) |
| `extras/` | Additional patches: theme defaults, default settings, update check, restart on update, experimental notice. Strings of `extras/strings/<locale>/` are appended to the YouTube Music ones |

Variant options (package, theme defaults) are set in `build_variant` calls of `.github/workflows/release.yml`.

## Downloads (Cloudflare R2)

Published builds are uploaded to the R2 bucket `R2_BUCKET`, below `music/` (the variable `DL_BASE_URL` is required to publish; the APKs are not attached to the GitHub release): the APKs to `music/apk/`, the update info to `music/updates/<variant>.json`. The apps check `DL_BASE_URL/music/updates/<variant>.json` and download the APK from `DL_BASE_URL/music/apk/`, so they no longer depend on GitHub. The APKs of the last 3 builds are kept. The bucket and its Worker are shared with YT Morphe (`cloudflare/worker.js` in RAMiK4/youtube-morphe), which uses the root of the bucket. `updates/*.json` in this repository stays up to date for builds that still check GitHub.

`r2_test` (test builds, with `update_preview`): the APK and its update info are uploaded as test files (`music/apk/test_*`, `music/updates/*-test.json`) and the APK checks those; the next published build removes them.

The update address is built into each APK: `DL_BASE_URL` must not change, or the builds that use it stop getting updates.

## Secrets (Settings → Secrets and variables → Actions)

| Name | Type | Value |
|---|---|---|
| `KEYSTORE_B64` | secret | Keystore, base64 |
| `KEYSTORE_PASSWORD` | secret | Keystore password (empty: Morphe default) |
| `KEY_ALIAS` | secret | Key alias (empty: `Morphe`) |
| `KEY_PASSWORD` | secret | Key password (empty: `Morphe`) |
| `CERT_SHA256` | variable | SHA-256 fingerprint of the signing certificate |
| `R2_ACCOUNT_ID` | secret | Cloudflare account ID |
| `R2_ACCESS_KEY_ID` | secret | R2 API token: access key ID (Object Read & Write, bucket only) |
| `R2_SECRET_ACCESS_KEY` | secret | R2 API token: secret access key |
| `R2_BUCKET` | variable | R2 bucket name |
| `DL_BASE_URL` | variable | Address of the Worker, without a trailing slash |
