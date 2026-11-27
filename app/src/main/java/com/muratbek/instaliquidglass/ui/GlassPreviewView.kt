package com.muratbek.instaliquidglass.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.muratbek.instaliquidglass.model.GlassConfig
import com.muratbek.instaliquidglass.xposed.graphics.LiquidGlassCapsuleDrawable
import com.muratbek.instaliquidglass.xposed.graphics.LiquidGlassPillView

class GlassPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var config = GlassConfig()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillContainer = FrameLayout(context)
    private val tabRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    private val pillView = LiquidGlassPillView(context)
    private var capsuleDrawable: LiquidGlassCapsuleDrawable? = null
    private var selectedIndex = 0
    private val tabLabels = listOf("⌂", "⌕", "＋", "▷", "◉")

    init {
        setWillNotDraw(false)
        clipChildren = true

        val density = resources.displayMetrics.density
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, (60 * density).toInt()).apply {
            gravity = Gravity.BOTTOM
            leftMargin = (16 * density).toInt()
            rightMargin = (16 * density).toInt()
            bottomMargin = (14 * density).toInt()
        }
        addView(pillContainer, lp)
        pillContainer.addView(pillView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        pillContainer.addView(tabRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val drawable = LiquidGlassCapsuleDrawable(pillContainer, config)
        capsuleDrawable = drawable
        pillContainer.background = drawable

        buildTabs()
    }

    private fun buildTabs() {
        tabRow.removeAllViews()
        tabLabels.forEachIndexed { idx, icon ->
            val tv = TextView(context).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(if (idx == selectedIndex) Color.WHITE else 0xAAFFFFFF.toInt())
                isSelected = idx == selectedIndex
                setOnClickListener {
                    selectTab(idx)
                }
            }
            val itemLp = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            tabRow.addView(tv, itemLp)
        }
        post { selectTab(selectedIndex, animate = false) }
    }

    private fun selectTab(index: Int, animate: Boolean = true) {
        selectedIndex = index
        for (i in 0 until tabRow.childCount) {
            val child = tabRow.getChildAt(i) as? TextView ?: continue
            child.isSelected = i == index
            child.setTextColor(if (i == index) Color.WHITE else 0x99FFFFFF.toInt())
            child.scaleX = config.iconScale
            child.scaleY = config.iconScale
        }
        val target = tabRow.getChildAt(index) ?: return
        if (target.width > 0) {
            val cx = target.left + target.width * 0.5f
            val w = target.width * 0.84f
            if (animate) pillView.snapToTab(cx, w) else pillView.setDragPosition(cx, w, false)
        }
    }

    fun applyConfig(newConfig: GlassConfig) {
        this.config = newConfig
        capsuleDrawable?.updateConfig(newConfig)
        pillView.updateConfig(newConfig)

        val density = resources.displayMetrics.density
        val lp = pillContainer.layoutParams as LayoutParams
        lp.height = (newConfig.barHeightDp * density).toInt()
        lp.leftMargin = (newConfig.marginHorizontalDp * density).toInt()
        lp.rightMargin = (newConfig.marginHorizontalDp * density).toInt()
        lp.bottomMargin = (newConfig.marginBottomDp * density).toInt()
        pillContainer.layoutParams = lp
        pillContainer.visibility = if (newConfig.enabled) View.VISIBLE else View.INVISIBLE

        val createTab = tabRow.getChildAt(2)
        createTab?.visibility = if (newConfig.hideCreateTab) View.GONE else View.VISIBLE

        post { selectTab(selectedIndex, animate = false) }
        invalidate()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val loc = IntArray(2)
        pillContainer.getLocationInWindow(loc)
        val localX = ev.x - pillContainer.left
        val localY = ev.y - pillContainer.top
        if (localX in 0f..pillContainer.width.toFloat() && localY in 0f..pillContainer.height.toFloat()) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    capsuleDrawable?.setTouchSpotlight(localX, localY, 1.0f)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    capsuleDrawable?.setTouchSpotlight(localX, localY, 0f)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density

        // Vibrant Instagram-like feed gradient backdrop so blur & refraction are clearly visible
        bgPaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(0xFF1E1B4B.toInt(), 0xFF831843.toInt(), 0xFF0F766E.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(0f, 0f, w, h), 22f * density, 22f * density, bgPaint)

        // Simulated feed photo cards scrolling underneath the floating bar
        cardPaint.color = 0x55F43F5E
        canvas.drawRoundRect(RectF(18f * density, 16f * density, w * 0.52f, h - 10f * density), 14f * density, 14f * density, cardPaint)

        cardPaint.color = 0x6638BDF8
        canvas.drawRoundRect(RectF(w * 0.56f, 24f * density, w - 18f * density, h - 8f * density), 14f * density, 14f * density, cardPaint)

        super.onDraw(canvas)
    }
}
