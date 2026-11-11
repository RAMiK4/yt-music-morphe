package app.morphe.extension.music.patches.theme;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import app.morphe.extension.shared.theme.ThemeColorPatch;
import app.morphe.extension.shared.theme.ThemeUtils;

/**
 * The dark background of the menus and sheets of Morphe (quick download menu, playlist tags,
 * SoundProof), which use the grey #212121 of the app. Like the menus of the app (Menu colors patch),
 * they get the theme color, #0F0F0F with pure black, so they do not stand out from the background.
 */
@SuppressWarnings("unused")
public final class MenuColor {

    private static final int PURE_BLACK_MENU_COLOR = 0xFF0F0F0F;

    private MenuColor() {
    }

    /**
     * @param appColor The color of the app, kept with the app default theme color.
     */
    @ColorInt
    public static int getDark(@ColorInt int appColor) {
        if (!ThemeColorPatch.isPatchIncluded() || ThemeColorPatch.isAppDefaultColor()) {
            return appColor;
        }
        final int themeColor = ThemeUtils.getThemeDarkColor();
        return themeColor == Color.BLACK ? PURE_BLACK_MENU_COLOR : themeColor;
    }
}
