package com.muratbek.instaliquidglass.model

import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONObject

data class GlassConfig(
    val enabled: Boolean = true,
    val themeMode: Int = THEME_AUTO,          // 0 = Auto, 1 = Dark Glass, 2 = Light Crystal
    val blurRadiusDp: Float = 28f,          // 0..60 dp
    val tintAlpha: Float = 0.24f,           // 0.05..0.85
    val darkTintColor: Int = 0xFF10151E.toInt(),
    val lightTintColor: Int = 0xFFF4F8FC.toInt(),
    val saturationBoost: Float = 1.32f,     // 0.8..2.0
    val lensRefraction: Float = 0.65f,      // 0.0..1.0 (AGSL chromatic & edge bend)
    val specularBorderAlpha: Float = 0.55f, // 0.0..1.0
    val borderWidthDp: Float = 1.25f,       // 0.0..3.0 dp
    val cornerRadiusDp: Float = 30f,        // 12..42 dp
    val marginHorizontalDp: Float = 16f,    // 4..36 dp
    val marginBottomDp: Float = 12f,        // 2..36 dp
    val barHeightDp: Float = 60f,           // 48..76 dp
    val pillEnabled: Boolean = true,
    val pillAlpha: Float = 0.26f,           // 0.05..0.70
    val pillBounce: Float = 0.68f,          // 0.30..0.95 (spring damping inverse)
    val touchRubberBand: Boolean = true,
    val dragToSwitchTab: Boolean = true,
    val hideLabels: Boolean = true,
    val hideCreateTab: Boolean = false,
    val iconScale: Float = 1.0f,            // 0.75..1.30
    val reelsAdaptiveGlass: Boolean = true,
    val compactOnScroll: Boolean = false
) {
    companion object {
        const val THEME_AUTO = 0
        const val THEME_DARK = 1
        const val THEME_LIGHT = 2

        fun fromPrefs(prefs: SharedPreferences?): GlassConfig {
            if (prefs == null) return GlassConfig()
            return try {
                GlassConfig(
                    enabled = prefs.getBoolean("enabled", true),
                    themeMode = prefs.getInt("themeMode", THEME_AUTO),
                    blurRadiusDp = prefs.getFloat("blurRadiusDp", 28f),
                    tintAlpha = prefs.getFloat("tintAlpha", 0.24f),
                    darkTintColor = prefs.getInt("darkTintColor", 0xFF10151E.toInt()),
                    lightTintColor = prefs.getInt("lightTintColor", 0xFFF4F8FC.toInt()),
                    saturationBoost = prefs.getFloat("saturationBoost", 1.32f),
                    lensRefraction = prefs.getFloat("lensRefraction", 0.65f),
                    specularBorderAlpha = prefs.getFloat("specularBorderAlpha", 0.55f),
                    borderWidthDp = prefs.getFloat("borderWidthDp", 1.25f),
                    cornerRadiusDp = prefs.getFloat("cornerRadiusDp", 30f),
                    marginHorizontalDp = prefs.getFloat("marginHorizontalDp", 16f),
                    marginBottomDp = prefs.getFloat("marginBottomDp", 12f),
                    barHeightDp = prefs.getFloat("barHeightDp", 60f),
                    pillEnabled = prefs.getBoolean("pillEnabled", true),
                    pillAlpha = prefs.getFloat("pillAlpha", 0.26f),
                    pillBounce = prefs.getFloat("pillBounce", 0.68f),
                    touchRubberBand = prefs.getBoolean("touchRubberBand", true),
                    dragToSwitchTab = prefs.getBoolean("dragToSwitchTab", true),
                    hideLabels = prefs.getBoolean("hideLabels", true),
                    hideCreateTab = prefs.getBoolean("hideCreateTab", false),
                    iconScale = prefs.getFloat("iconScale", 1.0f),
                    reelsAdaptiveGlass = prefs.getBoolean("reelsAdaptiveGlass", true),
                    compactOnScroll = prefs.getBoolean("compactOnScroll", false)
                )
            } catch (_: Throwable) {
                GlassConfig()
            }
        }

        fun presetIos26Crystal() = GlassConfig(
            themeMode = THEME_AUTO,
            blurRadiusDp = 30f,
            tintAlpha = 0.22f,
            saturationBoost = 1.38f,
            lensRefraction = 0.72f,
            specularBorderAlpha = 0.62f,
            borderWidthDp = 1.3f,
            cornerRadiusDp = 31f,
            marginHorizontalDp = 16f,
            marginBottomDp = 12f,
            barHeightDp = 60f,
            pillEnabled = true,
            pillAlpha = 0.28f,
            pillBounce = 0.65f
        )

        fun presetMidnightObsidian() = GlassConfig(
            themeMode = THEME_DARK,
            blurRadiusDp = 36f,
            tintAlpha = 0.48f,
            darkTintColor = 0xFF090C12.toInt(),
            saturationBoost = 1.45f,
            lensRefraction = 0.80f,
            specularBorderAlpha = 0.45f,
            borderWidthDp = 1.1f,
            cornerRadiusDp = 28f,
            marginHorizontalDp = 14f,
            marginBottomDp = 10f,
            barHeightDp = 58f,
            pillEnabled = true,
            pillAlpha = 0.22f,
            pillBounce = 0.72f
        )

        fun presetFrostedIce() = GlassConfig(
            themeMode = THEME_LIGHT,
            blurRadiusDp = 42f,
            tintAlpha = 0.36f,
            lightTintColor = 0xFFEAF6FF.toInt(),
            saturationBoost = 1.25f,
            lensRefraction = 0.55f,
            specularBorderAlpha = 0.75f,
            borderWidthDp = 1.5f,
            cornerRadiusDp = 32f,
            marginHorizontalDp = 18f,
            marginBottomDp = 14f,
            barHeightDp = 62f,
            pillEnabled = true,
            pillAlpha = 0.34f,
            pillBounce = 0.60f
        )

        fun presetNeonAurora() = GlassConfig(
            themeMode = THEME_DARK,
            blurRadiusDp = 26f,
            tintAlpha = 0.30f,
            darkTintColor = 0xFF13112B.toInt(),
            saturationBoost = 1.65f,
            lensRefraction = 0.95f,
            specularBorderAlpha = 0.80f,
            borderWidthDp = 1.5f,
            cornerRadiusDp = 30f,
            marginHorizontalDp = 16f,
            marginBottomDp = 12f,
            barHeightDp = 60f,
            pillEnabled = true,
            pillAlpha = 0.32f,
            pillBounce = 0.58f
        )

        fun fromJson(raw: String): GlassConfig {
            val j = JSONObject(raw)
            val d = GlassConfig()
            return GlassConfig(
                enabled = j.optBoolean("enabled", d.enabled),
                themeMode = j.optInt("themeMode", d.themeMode),
                blurRadiusDp = j.optDouble("blurRadiusDp", d.blurRadiusDp.toDouble()).toFloat(),
                tintAlpha = j.optDouble("tintAlpha", d.tintAlpha.toDouble()).toFloat(),
                darkTintColor = j.optInt("darkTintColor", d.darkTintColor),
                lightTintColor = j.optInt("lightTintColor", d.lightTintColor),
                saturationBoost = j.optDouble("saturationBoost", d.saturationBoost.toDouble()).toFloat(),
                lensRefraction = j.optDouble("lensRefraction", d.lensRefraction.toDouble()).toFloat(),
                specularBorderAlpha = j.optDouble("specularBorderAlpha", d.specularBorderAlpha.toDouble()).toFloat(),
                borderWidthDp = j.optDouble("borderWidthDp", d.borderWidthDp.toDouble()).toFloat(),
                cornerRadiusDp = j.optDouble("cornerRadiusDp", d.cornerRadiusDp.toDouble()).toFloat(),
                marginHorizontalDp = j.optDouble("marginHorizontalDp", d.marginHorizontalDp.toDouble()).toFloat(),
                marginBottomDp = j.optDouble("marginBottomDp", d.marginBottomDp.toDouble()).toFloat(),
                barHeightDp = j.optDouble("barHeightDp", d.barHeightDp.toDouble()).toFloat(),
                pillEnabled = j.optBoolean("pillEnabled", d.pillEnabled),
                pillAlpha = j.optDouble("pillAlpha", d.pillAlpha.toDouble()).toFloat(),
                pillBounce = j.optDouble("pillBounce", d.pillBounce.toDouble()).toFloat(),
                touchRubberBand = j.optBoolean("touchRubberBand", d.touchRubberBand),
                dragToSwitchTab = j.optBoolean("dragToSwitchTab", d.dragToSwitchTab),
                hideLabels = j.optBoolean("hideLabels", d.hideLabels),
                hideCreateTab = j.optBoolean("hideCreateTab", d.hideCreateTab),
                iconScale = j.optDouble("iconScale", d.iconScale.toDouble()).toFloat(),
                reelsAdaptiveGlass = j.optBoolean("reelsAdaptiveGlass", d.reelsAdaptiveGlass),
                compactOnScroll = j.optBoolean("compactOnScroll", d.compactOnScroll)
            )
        }
    }

    fun writeTo(editor: SharedPreferences.Editor) {
        editor.putBoolean("enabled", enabled)
            .putInt("themeMode", themeMode)
            .putFloat("blurRadiusDp", blurRadiusDp)
            .putFloat("tintAlpha", tintAlpha)
            .putInt("darkTintColor", darkTintColor)
            .putInt("lightTintColor", lightTintColor)
            .putFloat("saturationBoost", saturationBoost)
            .putFloat("lensRefraction", lensRefraction)
            .putFloat("specularBorderAlpha", specularBorderAlpha)
            .putFloat("borderWidthDp", borderWidthDp)
            .putFloat("cornerRadiusDp", cornerRadiusDp)
            .putFloat("marginHorizontalDp", marginHorizontalDp)
            .putFloat("marginBottomDp", marginBottomDp)
            .putFloat("barHeightDp", barHeightDp)
            .putBoolean("pillEnabled", pillEnabled)
            .putFloat("pillAlpha", pillAlpha)
            .putFloat("pillBounce", pillBounce)
            .putBoolean("touchRubberBand", touchRubberBand)
            .putBoolean("dragToSwitchTab", dragToSwitchTab)
            .putBoolean("hideLabels", hideLabels)
            .putBoolean("hideCreateTab", hideCreateTab)
            .putFloat("iconScale", iconScale)
            .putBoolean("reelsAdaptiveGlass", reelsAdaptiveGlass)
            .putBoolean("compactOnScroll", compactOnScroll)
            .apply()
    }

    fun toJson(): String = JSONObject().apply {
        put("enabled", enabled)
        put("themeMode", themeMode)
        put("blurRadiusDp", blurRadiusDp.toDouble())
        put("tintAlpha", tintAlpha.toDouble())
        put("darkTintColor", darkTintColor)
        put("lightTintColor", lightTintColor)
        put("saturationBoost", saturationBoost.toDouble())
        put("lensRefraction", lensRefraction.toDouble())
        put("specularBorderAlpha", specularBorderAlpha.toDouble())
        put("borderWidthDp", borderWidthDp.toDouble())
        put("cornerRadiusDp", cornerRadiusDp.toDouble())
        put("marginHorizontalDp", marginHorizontalDp.toDouble())
        put("marginBottomDp", marginBottomDp.toDouble())
        put("barHeightDp", barHeightDp.toDouble())
        put("pillEnabled", pillEnabled)
        put("pillAlpha", pillAlpha.toDouble())
        put("pillBounce", pillBounce.toDouble())
        put("touchRubberBand", touchRubberBand)
        put("dragToSwitchTab", dragToSwitchTab)
        put("hideLabels", hideLabels)
        put("hideCreateTab", hideCreateTab)
        put("iconScale", iconScale.toDouble())
        put("reelsAdaptiveGlass", reelsAdaptiveGlass)
        put("compactOnScroll", compactOnScroll)
    }.toString(2)

    fun resolveTintColor(isSystemDark: Boolean): Int {
        val useDark = when (themeMode) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            else -> isSystemDark
        }
        val base = if (useDark) darkTintColor else lightTintColor
        val alphaInt = (tintAlpha.coerceIn(0.02f, 0.92f) * 255f).toInt()
        return Color.argb(alphaInt, Color.red(base), Color.green(base), Color.blue(base))
    }
}
