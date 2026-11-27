package com.muratbek.instaliquidglass.xposed

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class LiquidGlassModule : XposedModule() {

    private var controller: TabBarHookController? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log("InstaLiquidGlass loaded in process: ${param.processName} (API ${apiVersion})")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.instagram.android") return
        if (!param.isFirstPackage) return

        log("Initializing Liquid Glass hooks for com.instagram.android")

        val remotePrefs = try {
            getRemotePreferences("liquid_glass_prefs")
        } catch (t: Throwable) {
            log("RemotePreferences fallback: ${t.message}")
            null
        }

        val hookController = TabBarHookController(remotePrefs) { msg -> log(msg) }
        controller = hookController

        // Hook 1: Activity.onResume() -> Reliable entry after InstagramMainActivity inflates its tab bar
        try {
            val onResumeMethod = Activity::class.java.getDeclaredMethod("onResume")
            hook(onResumeMethod).intercept { chain ->
                val result = chain.proceed()
                val activity = chain.thisObject as? Activity
                if (activity != null) {
                    hookController.onActivityResumed(activity)
                }
                result
            }
        } catch (t: Throwable) {
            log("Failed hooking Activity.onResume: ${t.message}")
        }

        // Hook 2: ViewGroup.addView(View, int, ViewGroup.LayoutParams) -> Catches dynamic tab_bar inflation
        try {
            val addViewMethod = ViewGroup::class.java.getDeclaredMethod(
                "addView",
                View::class.java,
                Int::class.javaPrimitiveType,
                ViewGroup.LayoutParams::class.java
            )
            hook(addViewMethod).intercept { chain ->
                val result = chain.proceed()
                val added = chain.args.getOrNull(0) as? View
                if (added != null && added.id != View.NO_ID) {
                    val entryName = try {
                        added.resources.getResourceEntryName(added.id)
                    } catch (_: Throwable) {
                        null
                    }
                    if (entryName == "tab_bar" || entryName == "layout_container_bottom_sections") {
                        val ctx = added.context
                        if (ctx is Activity) {
                            added.post { hookController.scanAndAttach(ctx, ctx.window.decorView) }
                        }
                    }
                }
                result
            }
        } catch (t: Throwable) {
            log("Failed hooking ViewGroup.addView: ${t.message}")
        }
    }
}
