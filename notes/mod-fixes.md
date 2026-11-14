<!-- Fixes of the mod. No longer listed in the release notes (they end with "Bug fixes and other improvements"); kept as a record. -->

## 2.3.0-dev.1

* **Theme:** Menus and sheets (quick download, playlist tags, SoundProof...) stay grey `#212121` instead of the theme color. They now use the theme color, `#0F0F0F` in the Original and Amoled variants
* **Updates:** The update dialog reminds to install the downloaded APK from the Downloads folder

## 2.1.0-dev.11

* Non-experimental targets do not patch
* **Spoof video streams:** Do not block get_watch, initplayback, or Onesie requests
* **Downloads:** Downloading with flyout menu shows premium popup
* **Downloads:** Downloads screen freezes with a large catalogue
* **Downloads:** Offline playback crashes with a large queue
* **Spoof video streams:** Video opened from the search results can load forever when another video is fetched at the same time
* **Downloads:** Offline playback audio focus handling
* **Hide flyout menu components:** Fix patching 9.44.52
* **Settings:** App crashes when opening settings with 9.44.52
* **Third-party lyrics:** Various minor bug fixes
