package com.muratbek.instaliquidglass.xposed.graphics

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.view.Choreographer
import android.view.View
import com.muratbek.instaliquidglass.model.GlassConfig
import kotlin.math.abs

class LiquidGlassPillView(context: Context) : View(context), Choreographer.FrameCallback {

    private var config = GlassConfig()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val pillRect = RectF()

    // Spring physics state
    private var currentX = -1f
    private var targetX = -1f
    private var velocityX = 0f

    private var currentWidth = 0f
    private var targetWidth = 0f

    private var isDragging = false
    private var lastFrameNanos = 0L
    private var animating = false

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun updateConfig(newConfig: GlassConfig) {
        this.config = newConfig
        visibility = if (newConfig.enabled && newConfig.pillEnabled) VISIBLE else GONE
        invalidate()
    }

    fun snapToTab(centerX: Float, width: Float) {
        targetX = centerX
        targetWidth = width
        if (currentX < 0f) {
            currentX = centerX
            currentWidth = width
            velocityX = 0f
            invalidate()
        } else {
            startSpringLoop()
        }
    }

    fun setDragPosition(centerX: Float, width: Float, dragging: Boolean) {
        isDragging = dragging
        targetX = centerX
        targetWidth = width
        if (dragging) {
            velocityX = (centerX - currentX) * 18f
            currentX = centerX
            currentWidth = width
            invalidate()
        } else {
            startSpringLoop()
        }
    }

    private fun startSpringLoop() {
        if (!animating) {
            animating = true
            lastFrameNanos = 0L
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!animating) return
        val dt = if (lastFrameNanos == 0L) {
            0.016f
        } else {
            ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
        }
        lastFrameNanos = frameTimeNanos

        // Critically-to-underdamped spring controlled by pillBounce
        val stiffness = 360f
        val dampingRatio = (1.05f - config.pillBounce * 0.65f).coerceIn(0.38f, 0.92f)
        val damping = 2f * dampingRatio * kotlin.math.sqrt(stiffness)

        val displacement = currentX - targetX
        val accel = -stiffness * displacement - damping * velocityX
        velocityX += accel * dt
        currentX += velocityX * dt
        currentWidth += (targetWidth - currentWidth) * (12f * dt).coerceIn(0f, 1f)

        invalidate()

        if (abs(displacement) < 0.35f && abs(velocityX) < 2.0f) {
            currentX = targetX
            currentWidth = targetWidth
            velocityX = 0f
            animating = false
            invalidate()
        } else {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onDetachedFromWindow() {
        animating = false
        Choreographer.getInstance().removeFrameCallback(this)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        if (!config.pillEnabled || currentX < 0f || currentWidth <= 4f) return
        val h = height.toFloat()
        if (h <= 4f) return

        val density = resources.displayMetrics.density
        val dark = when (config.themeMode) {
            GlassConfig.THEME_DARK -> true
            GlassConfig.THEME_LIGHT -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        // Liquid velocity stretch: elongates horizontally while moving fast
        val stretch = (abs(velocityX) * 0.018f).coerceAtMost(currentWidth * 0.35f) +
            if (isDragging) 6f * density else 0f
        val drawW = currentWidth + stretch
        val vInset = 5f * density
        val drawH = (h - vInset * 2f).coerceAtLeast(24f * density)
        val radius = drawH * 0.5f

        val left = (currentX - drawW * 0.5f).coerceIn(4f * density, (width - drawW - 4f * density).coerceAtLeast(4f * density))
        pillRect.set(left, vInset, left + drawW, vInset + drawH)

        val alphaInt = (config.pillAlpha.coerceIn(0.05f, 0.75f) * 255f).toInt()
        val topColor = if (dark) Color.argb(alphaInt, 255, 255, 255) else Color.argb(alphaInt, 255, 255, 255)
        val botColor = if (dark) Color.argb((alphaInt * 0.45f).toInt(), 180, 215, 255) else Color.argb((alphaInt * 0.55f).toInt(), 210, 232, 255)

        fillPaint.shader = LinearGradient(
            0f, pillRect.top, 0f, pillRect.bottom,
            topColor, botColor, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(pillRect, radius, radius, fillPaint)

        // Subtle glass rim on the floating pill
        rimPaint.strokeWidth = 1.0f * density
        rimPaint.color = if (dark) 0x55FFFFFF else 0x88FFFFFF.toInt()
        canvas.drawRoundRect(pillRect, radius, radius, rimPaint)
    }
}
