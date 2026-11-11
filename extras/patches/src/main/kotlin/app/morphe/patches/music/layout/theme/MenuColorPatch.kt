package app.morphe.patches.music.layout.theme

import app.morphe.patcher.patch.listDecodableFiles
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXml

/** The color of the dark theme the Theme patch selects at runtime, declared by every dark variant. */
private const val THEME_COLOR_DARK = "morphe_theme_color_dark"

/** The color of menus and sheets in the dark theme. */
private const val MENU_COLOR_DARK = "morphe_menu_color_dark"

/** Menus of a pure black theme, which would not stand out from the background in black. */
private const val PURE_BLACK_MENU_COLOR = "#FF0F0F0F"

private val BLACK_VALUES = setOf("@android:color/black", "#000000", "#ff000000")

/** The style of the dark theme and its item of the raised background of menus and sheets. */
private const val DARK_STYLE = "yt.sys.color.baseline.dark"
private const val RAISED_BACKGROUND_ITEM = "yt_sys_color_baseline_raised_background"

/**
 * The app color of menus and sheets, which the Theme patch gives the theme color. Unlike YouTube,
 * yt_black1 is not one of them: YouTube Music uses it for the background of the app.
 */
private const val RAISED_BACKGROUND_COLOR = "yt_sys_color_baseline_mobile_dark_default_raised_background"

/**
 * Menus and sheets in the theme color, with #0F0F0F for a pure black theme.
 *
 * The dark theme takes the raised background of menus and sheets from a style item that points to
 * the grey #212121 of the app (yt_ref_color_constants_default_grey20), which no theme color
 * replaces. The item gets a color of its own here that follows the theme color, as in YT Morphe.
 * With pure black it is #0F0F0F instead, and with the app default theme color the app's own color.
 */
internal val menuColorPatch = patch {
    dependsOn(themePatch)

    execute {
        // Only with the theme colors of the app settings: a color set while patching replaces everything itself.
        val hasThemeColor = resourceXml("res/values/colors.xml").use { colors ->
            colors.root.children.any { it.attribute("name") == THEME_COLOR_DARK }
        }
        if (!hasThemeColor) return@execute

        resourceXml("res/values/colors.xml").use { colors ->
            colors.root.children.forEach { color ->
                if (color.attribute("name") == RAISED_BACKGROUND_COLOR && color.text == "@color/$THEME_COLOR_DARK") {
                    color.text = "@color/$MENU_COLOR_DARK"
                }
            }
            colors.root.appendChild("color") {
                setAttribute("name", MENU_COLOR_DARK)
                text = "@color/$THEME_COLOR_DARK"
            }
        }

        // The variants of the dark theme colors: a pure black one gets #0F0F0F, the app default
        // one (which declares the app colors again) the menu color of the app.
        listDecodableFiles("res/values-mcc*/colors.xml").forEach { path ->
            resourceXml(path).use { colors ->
                val declared = colors.root.children.associate { it.attribute("name") to it.text }
                val themeColor = declared[THEME_COLOR_DARK] ?: return@use
                val menuColor = when {
                    themeColor.lowercase() in BLACK_VALUES -> PURE_BLACK_MENU_COLOR
                    else -> declared[RAISED_BACKGROUND_COLOR]
                        ?.takeIf { it != "@color/$THEME_COLOR_DARK" && it != "@color/$MENU_COLOR_DARK" }
                        ?: return@use
                }
                colors.root.appendChild("color") {
                    setAttribute("name", MENU_COLOR_DARK)
                    text = menuColor
                }
            }
        }

        listDecodableFiles("res/values*/styles.xml").forEach { path ->
            resourceXml(path).use { styles ->
                styles.root.children("style")
                    .filter { it.attribute("name") == DARK_STYLE }
                    .forEach { style ->
                        style.children("item")
                            .filter { it.attribute("name") == RAISED_BACKGROUND_ITEM }
                            .forEach { it.text = "@color/$MENU_COLOR_DARK" }
                    }
            }
        }
    }
}
