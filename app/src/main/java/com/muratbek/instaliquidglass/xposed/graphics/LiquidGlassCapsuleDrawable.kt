package com.muratbek.instaliquidglass.xposed.graphics

import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import com.muratbek.instaliquidglass.model.GlassConfig

class LiquidGlassCapsuleDrawable(
    private val targetBar: View,
    private var config: GlassConfig
) : Drawable() {

    private var backdropNode: RenderNode? = null
    private var blurNode: RenderNode? = null
    private var runtimeShader: RuntimeShader? = null
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    private var isCapturing = false
    private var touchX = -1f
    private var touchY = -1f
    private var touchIntensity = 0f
    private var isReelsSurfaceActive = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backdropNode = RenderNode("IGLiquidBackdrop")
            blurNode = RenderNode("IGLiquidBlur")
            runtimeShader = LiquidGlassShaders.createLiquidGlassShader()
        }
    }

    fun updateConfig(newConfig: GlassConfig) {
        this.config = newConfig
        invalidateSelf()
    }

    fun setTouchSpotlight(x: Float, y: Float, intensity: Float) {
        this.touchX = x
        this.touchY = y
        this.touchIntensity = intensity.coerceIn(0f, 1f)
        invalidateSelf()
    }

    fun setReelsSurfaceActive(active: Boolean) {
        if (this.isReelsSurfaceActive != active) {
            this.isReelsSurfaceActive = active
            invalidateSelf()
        }
    }

    private fun isSystemDark(): Boolean {
        val uiMode = targetBar.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun draw(canvas: Canvas) {
        if (isCapturing) return
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 2f || h <= 2f) return

        val density = targetBar.resources.displayMetrics.density
        val radiusPx = (config.cornerRadiusDp * density).coerceAtMost(h * 0.5f)
        val dark = when (config.themeMode) {
            GlassConfig.THEME_DARK -> true
            GlassConfig.THEME_LIGHT -> false
            else -> isSystemDark()
        }

        rectF.set(0f, 0f, w, h)
        shadowPaint.color = Color.TRANSPARENT
        shadowPaint.setShadowLayer(
            14f * density,
            0f,
            5f * density,
            if (dark) 0x70000000 else 0x26000000
        )
        canvas.drawRoundRect(rectF, radiusPx, radiusPx, shadowPaint)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            canvas.isHardwareAccelerated &&
            backdropNode != null &&
            blurNode != null &&
            runtimeShader != null
        ) {
            if (drawHardwareLiquidGlass(canvas, w.toInt(), h.toInt(), radiusPx, density, dark)) {
                return
            }
        }

        drawFallbackGlass(canvas, w, h, radiusPx, density, dark)
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun drawHardwareLiquidGlass(
        canvas: Canvas,
        width: Int,
        height: Int,
        radiusPx: Float,
        density: Float,
        dark: Boolean
    ): Boolean {
        return try {
            val root = targetBar.rootView ?: return false
            val bNode = backdropNode ?: return false
            val fNode = blurNode ?: return false
            val shader = runtimeShader ?: return false

            val locBar = IntArray(2)
            val locRoot = IntArray(2)
            targetBar.getLocationInWindow(locBar)
            root.getLocationInWindow(locRoot)
            val offsetX = (locBar[0] - locRoot[0]).toFloat()
            val offsetY = (locBar[1] - locRoot[1]).toFloat()

            bNode.setPosition(0, 0, width, height)
            val recCanvas = bNode.beginRecording(width, height)
            isCapturing = true
            try {
                recCanvas.drawColor(if (dark) 0xFF0B0E14.toInt() else 0xFFEBF0F5.toInt())
                recCanvas.translate(-offsetX, -offsetY)
                root.draw(recCanvas)
            } finally {
                isCapturing = false
                bNode.endRecording()
            }

            val effectiveBlur = (config.blurRadiusDp * density).coerceIn(0.5f, 140f)
            val blurEffect = RenderEffect.createBlurEffect(
                effectiveBlur,
                effectiveBlur,
                Shader.TileMode.CLAMP
            )
            val satMatrix = ColorMatrix().apply {
                setSaturation(config.saturationBoost.coerceIn(0.5f, 2.2f))
            }
            val satEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(satMatrix))
            fNode.setPosition(0, 0, width, height)
            fNode.setRenderEffect(RenderEffect.createChainEffect(satEffect, blurEffect))
            val blurCanvas = fNode.beginRecording(width, height)
            blurCanvas.drawRenderNode(bNode)
            fNode.endRecording()

            var tint = config.resolveTintColor(dark)
            if (isReelsSurfaceActive && config.reelsAdaptiveGlass) {
                val boostedAlpha = ((Color.alpha(tint) / 255f) + 0.24f).coerceIn(0.25f, 0.85f)
                tint = Color.argb((boostedAlpha * 255).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint))
            }

            val agslEffect = RenderEffect.createRuntimeShaderEffect(shader, "backdrop")
            shader.setFloatUniform("resolution", width.toFloat(), height.toFloat())
            shader.setFloatUniform("cornerRadius", radiusPx)
            shader.setFloatUniform("lensStrength", config.lensRefraction.coerceIn(0f, 1.2f))
            shader.setFloatUniform("borderWidth", config.borderWidthDp * density)
            shader.setFloatUniform("borderAlpha", config.specularBorderAlpha.coerceIn(0f, 1f))
            shader.setFloatUniform(
                "tintColor",
                Color.red(tint) / 255f,
                Color.green(tint) / 255f,
                Color.blue(tint) / 255f,
                Color.alpha(tint) / 255f
            )
            shader.setFloatUniform(
                "touchPos",
                if (touchX >= 0f) touchX else width * 0.5f,
                if (touchY >= 0f) touchY else height * 0.5f
            )
            shader.setFloatUniform("touchIntensity", touchIntensity)
            shader.setFloatUniform("isDark", if (dark) 1.0f else 0.0f)

            val outNode = RenderNode("IGLiquidOut")
            outNode.setPosition(0, 0, width, height)
            outNode.setRenderEffect(agslEffect)
            val outCanvas = outNode.beginRecording(width, height)
            outCanvas.drawRenderNode(fNode)
            outNode.endRecording()

            canvas.drawRenderNode(outNode)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun drawFallbackGlass(
        canvas: Canvas,
        w: Float,
        h: Float,
        radiusPx: Float,
        density: Float,
        dark: Boolean
    ) {
        var tint = config.resolveTintColor(dark)
        val boostedAlpha = ((Color.alpha(tint) / 255f) + 0.38f).coerceIn(0.45f, 0.92f)
        tint = Color.argb((boostedAlpha * 255).toInt(), Color.red(tint), Color.green(tint), Color.blue(tint))

        val topGlow = if (dark) 0x26FFFFFF else 0x66FFFFFF
        fallbackFillPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(blendColors(tint, topGlow), tint),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rectF, radiusPx, radiusPx, fallbackFillPaint)

        val strokePx = config.borderWidthDp * density
        if (strokePx > 0.2f && config.specularBorderAlpha > 0.02f) {
            fallbackBorderPaint.strokeWidth = strokePx
            val rimAlpha = (config.specularBorderAlpha * 255).toInt().coerceIn(0, 255)
            fallbackBorderPaint.color = if (dark) {
                Color.argb(rimAlpha, 220, 238, 255)
            } else {
                Color.argb(rimAlpha, 255, 255, 255)
            }
            val inset = strokePx * 0.5f
            rectF.set(inset, inset, w - inset, h - inset)
            canvas.drawRoundRect(rectF, radiusPx, radiusPx, fallbackBorderPaint)
        }
    }

    private fun blendColors(base: Int, overlay: Int): Int {
        val a = Color.alpha(overlay) / 255f
        val r = (Color.red(base) * (1 - a) + Color.red(overlay) * a).toInt()
        val g = (Color.green(base) * (1 - a) + Color.green(overlay) * a).toInt()
        val b = (Color.blue(base) * (1 - a) + Color.blue(overlay) * a).toInt()
        return Color.argb(Color.alpha(base), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    override fun setAlpha(alpha: Int) {
        shaderPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        shaderPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
