package app.morphe.patches.music.layout.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.music.shared.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val SETTINGS_CLASS = "Lapp/morphe/extension/shared/settings/SharedYouTubeSettings;"
private const val THEME_CLASS = "Lapp/morphe/extension/shared/theme/ThemeColorPatch"
private const val DARK_ENUM = "$THEME_CLASS\$ThemeColorDark;"

// Defaults of the extension. Nothing is changed for an option left at these values.
private const val DEFAULT_DARK = "PURE_BLACK"
private const val DEFAULT_DARK_CUSTOM = "#0F0F0F"

private val darkValues = mapOf(
    "App default" to "APP_DEFAULT",
    "Pure black" to "PURE_BLACK",
    "Material You (Pure black)" to "MATERIAL_YOU_PURE_BLACK",
    "Material You (Neutral)" to "MATERIAL_YOU_NEUTRAL",
    "Material You (Primary)" to "MATERIAL_YOU_PRIMARY",
    "Material You (Secondary)" to "MATERIAL_YOU_SECONDARY",
    "Material You (Tertiary)" to "MATERIAL_YOU_TERTIARY",
    "Catppuccin (Mocha)" to "CATPPUCCIN_MOCHA",
    "Classic YouTube" to "CLASSIC_YOUTUBE",
    "Dark pink" to "DARK_PINK",
    "Dark blue" to "DARK_BLUE",
    "Dark green" to "DARK_GREEN",
    "Dark yellow" to "DARK_YELLOW",
    "Dark orange" to "DARK_ORANGE",
    "Dark red" to "DARK_RED",
    "Custom" to "CUSTOM",
)

private val hexColor = Regex("^#[0-9A-Fa-f]{6}$")

@Suppress("unused")
val themeDefaultsPatch = bytecodePatch(
    name = "Theme defaults",
    description = "Changes the theme color a new install starts with. " +
        "The color can still be changed in the app settings. " +
        "Requires the theme color of the Theme patch to be changed in the app.",
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(themePatch, menuColorPatch)

    val darkDefault by stringOption(
        key = "darkDefault",
        default = DEFAULT_DARK,
        values = darkValues,
        title = "Default dark theme color",
        description = "The dark theme color selected in the app settings after install.",
        required = true,
    ) { it != null && it in darkValues.values }

    val darkCustomDefault by stringOption(
        key = "darkCustomDefault",
        default = DEFAULT_DARK_CUSTOM,
        title = "Default custom dark color",
        description = "The value of the custom dark theme color, as #RRGGBB.",
        required = true,
    ) { it != null && hexColor.matches(it) }

    execute {
        val dark = darkDefault!!
        val darkCustom = darkCustomDefault!!.uppercase()

        if (dark == DEFAULT_DARK && darkCustom == DEFAULT_DARK_CUSTOM) return@execute

        val clinit = mutableClassDefBy(SETTINGS_CLASS).methods.firstOrNull { it.name == "<clinit>" }
            ?: throw PatchException("Static initializer of $SETTINGS_CLASS not found")

        if (dark != DEFAULT_DARK) {
            clinit.replaceEnumDefault("morphe_theme_color_dark", DARK_ENUM, dark, DEFAULT_DARK)
        }
        if (darkCustom != DEFAULT_DARK_CUSTOM) {
            clinit.replaceStringDefault("morphe_theme_color_dark_custom", darkCustom, DEFAULT_DARK_CUSTOM)
        }
    }
}

private fun MutableMethod.indexOfKey(key: String): Int =
    instructions.indexOfFirst {
        it.opcode == Opcode.CONST_STRING &&
            ((it as ReferenceInstruction).reference as StringReference).string == key
    }.takeIf { it >= 0 } ?: throw PatchException("Setting key not found: $key")

/**
 * Changes the default value argument of the setting constructor that follows [key].
 *
 * The compiler can share a register holding a constant between several settings, so the value
 * is set right before the constructor call and the original value restored right after it.
 */
private fun MutableMethod.replaceDefaultArgument(
    key: String,
    settingClassSuffix: String,
    expectedTypes: Set<String>,
    load: (register: Int, original: Boolean) -> String,
) {
    val start = indexOfKey(key)
    val index = (start + 1 until instructions.size).firstOrNull { i ->
        val instruction = getInstruction(i)
        (instruction.opcode == Opcode.INVOKE_DIRECT || instruction.opcode == Opcode.INVOKE_DIRECT_RANGE) &&
            ((instruction as ReferenceInstruction).reference as MethodReference).let {
                it.name == "<init>" && it.definingClass.endsWith(settingClassSuffix)
            }
    } ?: throw PatchException("Constructor of $key not found")

    val call = getInstruction(index)
    val constructor = (call as ReferenceInstruction).reference as MethodReference
    val parameterType = constructor.parameterTypes.getOrNull(1)?.toString()
    if (parameterType == null || parameterType !in expectedTypes) {
        throw PatchException("Unexpected default value type of $key: $constructor")
    }

    // Arguments: this, key, default value, ...
    val register = when (call) {
        is FiveRegisterInstruction -> call.registerE
        is RegisterRangeInstruction -> call.startRegister + 2
        else -> throw PatchException("Unexpected constructor call of $key")
    }

    addInstruction(index + 1, load(register, true))
    addInstruction(index, load(register, false))
}

private fun MutableMethod.replaceEnumDefault(key: String, enumType: String, value: String, original: String) =
    replaceDefaultArgument(key, "/EnumSetting;", setOf("Ljava/lang/Enum;")) { register, restore ->
        "sget-object v$register, $enumType->${if (restore) original else value}:$enumType"
    }

private fun MutableMethod.replaceStringDefault(key: String, value: String, original: String) =
    replaceDefaultArgument(key, "/StringSetting;", setOf("Ljava/lang/String;")) { register, restore ->
        "const-string v$register, \"${if (restore) original else value}\""
    }
