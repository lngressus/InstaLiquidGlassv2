package com.muratbek.instaliquidglass.xposed

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.muratbek.instaliquidglass.model.GlassConfig
import com.muratbek.instaliquidglass.xposed.graphics.LiquidGlassCapsuleDrawable
import com.muratbek.instaliquidglass.xposed.graphics.LiquidGlassPillView
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.abs

class TabBarHookController(
    private val remotePrefs: SharedPreferences?,
    private val logger: (String) -> Unit
) {
    @Volatile
    private var config: GlassConfig = GlassConfig.fromPrefs(remotePrefs)

    private val attachedBars = WeakHashMap<ViewGroup, BarController>()

    init {
        try {
            remotePrefs?.registerOnSharedPreferenceChangeListener { prefs, _ ->
                config = GlassConfig.fromPrefs(prefs)
                attachedBars.values.toList().forEach { controller ->
                    controller.postApplyConfig(config)
                }
            }
        } catch (t: Throwable) {
            logger("Preference listener warning: ${t.message}")
        }
    }

    fun onActivityResumed(activity: Activity) {
        val name = activity.javaClass.name
        if (!name.contains("MainActivity", ignoreCase = true) &&
            !name.contains("Instagram", ignoreCase = true)
        ) {
            return
        }
        val decor = activity.window?.decorView ?: return
        decor.post { scanAndAttach(activity, decor) }
        decor.postDelayed({ scanAndAttach(activity, decor) }, 350L)
        decor.postDelayed({ scanAndAttach(activity, decor) }, 1100L)
    }

    fun scanAndAttach(activity: Activity, root: View) {
        try {
            if (remotePrefs != null) {
                config = GlassConfig.fromPrefs(remotePrefs)
            }
            val pkg = activity.packageName
            val res = activity.resources
            val tabBarId = res.getIdentifier("tab_bar", "id", pkg)
            val tabBar: ViewGroup? = if (tabBarId != 0) {
                root.findViewById(tabBarId)
            } else {
                findTabBarHeuristic(root)
            }

            if (tabBar == null) return
            val existing = attachedBars[tabBar]
            if (existing != null) {
                existing.postApplyConfig(config)
                return
            }

            val controller = BarController(activity, tabBar, config)
            attachedBars[tabBar] = controller
            controller.install()
            logger("Attached Liquid Glass to Instagram tab_bar (${tabBar.childCount} tabs)")
        } catch (t: Throwable) {
            logger("scanAndAttach error: ${t.message}")
        }
    }

    private fun findTabBarHeuristic(view: View): ViewGroup? {
        if (view is ViewGroup && view.childCount in 4..6) {
            val idName = try {
                if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else ""
            } catch (_: Throwable) { "" }
            if (idName == "tab_bar") return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findTabBarHeuristic(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private inner class BarController(
        activity: Activity,
        private val tabBar: ViewGroup,
        private var currentConfig: GlassConfig
    ) {
        private val activityRef = WeakReference(activity)
        private val capsuleDrawable = LiquidGlassCapsuleDrawable(tabBar, currentConfig)
        private var pillView: LiquidGlassPillView? = null
        private var wrapperLayout: FrameLayout? = null
        private var guardingLayout = false
        private var lastActiveTabIndex = 0
        private var compactProgress = 0f

        private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
            if (currentConfig.enabled) {
                capsuleDrawable.invalidateSelf()
            }
            true
        }

        fun postApplyConfig(newConfig: GlassConfig) {
            tabBar.post {
                currentConfig = newConfig
                capsuleDrawable.updateConfig(newConfig)
                pillView?.updateConfig(newConfig)
                applyStyling()
            }
        }

        fun install() {
            val activity = activityRef.get() ?: return

            try {
                val window = activity.window
                window.navigationBarColor = Color.TRANSPARENT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            } catch (_: Throwable) {
            }

            stripParentOpaqueBackgrounds()
            injectPillLayer()

            tabBar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                if (!guardingLayout && currentConfig.enabled) {
                    enforceFloatingLayout()
                }
                syncActiveTabPill(animate = true)
            }

            tabBar.viewTreeObserver.addOnPreDrawListener(preDrawListener)
            installTouchAndDragGestures()
            installScrollCompactWatcher(activity.window.decorView)

            applyStyling()
        }

        private fun stripParentOpaqueBackgrounds() {
            var p: ViewParent? = tabBar.parent
            var depth = 0
            while (p is ViewGroup && depth < 4) {
                if (p.background is ColorDrawable) {
                    p.background = null
                }
                p.clipChildren = false
                p.clipToPadding = false
                p = p.parent
                depth++
            }
        }

        private fun injectPillLayer() {
            val parent = tabBar.parent as? ViewGroup ?: return
            if (wrapperLayout != null) return

            val index = parent.indexOfChild(tabBar)
            val lp = tabBar.layoutParams

            val wrapper = FrameLayout(tabBar.context).apply {
                clipChildren = false
                clipToPadding = false
            }
            val pill = LiquidGlassPillView(tabBar.context)

            parent.removeViewAt(index)
            wrapper.addView(
                pill,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            wrapper.addView(
                tabBar,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            parent.addView(wrapper, index, lp)

            wrapperLayout = wrapper
            pillView = pill
        }

        private fun applyStyling() {
            val wrapper = wrapperLayout ?: tabBar
            if (!currentConfig.enabled) {
                wrapper.background = null
                tabBar.background = null
                pillView?.visibility = View.GONE
                return
            }

            wrapper.background = capsuleDrawable
            tabBar.background = null
            tabBar.clipChildren = false
            tabBar.clipToPadding = false
            pillView?.updateConfig(currentConfig)

            enforceFloatingLayout()
            customizeTabChildren()
            syncActiveTabPill(animate = false)
        }

        private fun enforceFloatingLayout() {
            val wrapper = wrapperLayout ?: return
            val lp = wrapper.layoutParams as? ViewGroup.MarginLayoutParams ?: return
            val density = tabBar.resources.displayMetrics.density

            val scaleFactor = if (currentConfig.compactOnScroll) (1f - compactProgress * 0.14f) else 1f
            val targetH = (currentConfig.barHeightDp * density * scaleFactor).toInt()
            val targetMarginH = (currentConfig.marginHorizontalDp * density + compactProgress * 22f * density).toInt()
            val targetMarginB = (currentConfig.marginBottomDp * density).toInt()

            if (lp.height != targetH ||
                lp.leftMargin != targetMarginH ||
                lp.rightMargin != targetMarginH ||
                lp.bottomMargin != targetMarginB
            ) {
                guardingLayout = true
                try {
                    lp.height = targetH
                    lp.leftMargin = targetMarginH
                    lp.rightMargin = targetMarginH
                    lp.bottomMargin = targetMarginB
                    wrapper.layoutParams = lp
                } finally {
                    guardingLayout = false
                }
            }
        }

        private fun customizeTabChildren() {
            val pkg = tabBar.context.packageName
            val res = tabBar.resources
            val createId = res.getIdentifier("creation_tab", "id", pkg)
            val clipsId = res.getIdentifier("clips_tab", "id", pkg)

            for (i in 0 until tabBar.childCount) {
                val child = tabBar.getChildAt(i) ?: continue
                if (createId != 0 && child.id == createId) {
                    child.visibility = if (currentConfig.hideCreateTab) View.GONE else View.VISIBLE
                }
                if (child.visibility == View.VISIBLE) {
                    applyIconAndLabelTweaks(child)
                }
                if (clipsId != 0 && child.id == clipsId && child.isSelected) {
                    capsuleDrawable.setReelsSurfaceActive(true)
                } else if (child.isSelected) {
                    capsuleDrawable.setReelsSurfaceActive(false)
                }
            }
        }

        private fun applyIconAndLabelTweaks(tabItem: View) {
            if (tabItem is TextView) {
                tabItem.visibility = if (currentConfig.hideLabels) View.GONE else View.VISIBLE
                return
            }
            if (tabItem is ImageView) {
                tabItem.scaleX = currentConfig.iconScale
                tabItem.scaleY = currentConfig.iconScale
                return
            }
            if (tabItem is ViewGroup) {
                for (i in 0 until tabItem.childCount) {
                    applyIconAndLabelTweaks(tabItem.getChildAt(i))
                }
            }
        }

        private fun syncActiveTabPill(animate: Boolean) {
            val pill = pillView ?: return
            if (!currentConfig.enabled || !currentConfig.pillEnabled) return

            var selectedTab: View? = null
            var selectedIdx = -1
            for (i in 0 until tabBar.childCount) {
                val child = tabBar.getChildAt(i) ?: continue
                if (child.visibility != View.VISIBLE) continue
                if (child.isSelected || isAnyDescendantSelected(child)) {
                    selectedTab = child
                    selectedIdx = i
                    break
                }
            }

            if (selectedTab != null && selectedTab.width > 0) {
                lastActiveTabIndex = selectedIdx
                val centerX = selectedTab.left + selectedTab.width * 0.5f
                val pillW = (selectedTab.width * 0.84f).coerceAtLeast(48f)
                if (animate) {
                    pill.snapToTab(centerX, pillW)
                } else {
                    pill.setDragPosition(centerX, pillW, dragging = false)
                }
            }
        }

        private fun isAnyDescendantSelected(view: View): Boolean {
            if (view.isSelected) return true
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    if (isAnyDescendantSelected(view.getChildAt(i))) return true
                }
            }
            return false
        }

        private fun installTouchAndDragGestures() {
            val wrapper = wrapperLayout ?: return
            var downX = 0f
            var dragging = false

            tabBar.setOnTouchListener { _, event ->
                if (!currentConfig.enabled) return@setOnTouchListener false

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        dragging = false
                        capsuleDrawable.setTouchSpotlight(event.x, event.y, 1.0f)
                        if (currentConfig.touchRubberBand) {
                            wrapper.animate().scaleX(1.035f).scaleY(1.045f).setDuration(140).start()
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        capsuleDrawable.setTouchSpotlight(event.x, event.y, 1.0f)
                        val dx = event.x - downX
                        if (currentConfig.touchRubberBand) {
                            wrapper.translationX = (dx * 0.06f).coerceIn(-18f, 18f)
                        }
                        if (currentConfig.dragToSwitchTab && abs(dx) > 22f) {
                            dragging = true
                            val targetTab = findTabAtX(event.x)
                            if (targetTab != null) {
                                pillView?.setDragPosition(
                                    targetTab.left + targetTab.width * 0.5f,
                                    targetTab.width * 0.84f,
                                    dragging = true
                                )
                            }
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        capsuleDrawable.setTouchSpotlight(event.x, event.y, 0f)
                        wrapper.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationX(0f)
                            .setDuration(220)
                            .start()

                        if (dragging && currentConfig.dragToSwitchTab && event.actionMasked == MotionEvent.ACTION_UP) {
                            val targetTab = findTabAtX(event.x)
                            targetTab?.performClick()
                            dragging = false
                            syncActiveTabPill(animate = true)
                            return@setOnTouchListener true
                        }
                        dragging = false
                        tabBar.postDelayed({ syncActiveTabPill(animate = true) }, 40L)
                    }
                }
                false
            }
        }

        private fun findTabAtX(x: Float): View? {
            for (i in 0 until tabBar.childCount) {
                val child = tabBar.getChildAt(i) ?: continue
                if (child.visibility == View.VISIBLE && x >= child.left && x <= child.right) {
                    return child
                }
            }
            return null
        }

        private fun installScrollCompactWatcher(decorView: View) {
            decorView.viewTreeObserver.addOnScrollChangedListener {
                if (!currentConfig.enabled || !currentConfig.compactOnScroll) {
                    if (compactProgress != 0f) {
                        compactProgress = 0f
                        enforceFloatingLayout()
                    }
                    return@addOnScrollChangedListener
                }
                capsuleDrawable.invalidateSelf()
            }
        }
    }
}
