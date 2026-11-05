package app.morphe.patches.music.misc.defaults

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.music.misc.extension.sharedExtensionPatch
import app.morphe.patches.music.misc.settings.settingsPatch
import app.morphe.patches.music.shared.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.shared.MusicActivityOnCreateFingerprint

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/music/patches/defaults/DefaultSettingsPatch;"

@Suppress("unused")
val defaultSettingsPatch = bytecodePatch(
    name = "Default settings",
    description = "Applies recommended settings once per setting. " +
        "Settings the user changes afterwards are kept, also across updates.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    val settings by stringOption(
        key = "settings",
        default = "{}",
        title = "Settings",
        description = "Settings in the format of the Morphe settings export, as one JSON object.",
    )

    execute {
        val json = settings.orEmpty().trim()
        if (json.isEmpty() || json == "{}") return@execute
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw PatchException("Settings must be a JSON object")
        }

        // Base64 keeps quotes, backslashes and non-ASCII text out of the string constant.
        val encoded = java.util.Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        mutableClassDefBy(EXTENSION_CLASS).methods
            .first { it.name == "defaultsBase64" }
            .returnEarly(encoded)

        MusicActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->onActivityCreated(Landroid/app/Activity;)V",
        )
    }
}
