package com.muratbek.instaliquidglass.xposed

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class LiquidGlassModule : XposedModule() {

    private var controller: TabBarHookController? = null

    private fun logInfo(msg: String) {
        log(Log.INFO, "InstaLiquidGlass", msg)
    }

    private fun logErr(msg: String, tr: Throwable? = null) {
        if (tr != null) {
            log(Log.ERROR, "InstaLiquidGlass", msg, tr)
        } else {
            log(Log.ERROR, "InstaLiquidGlass", msg)
        }
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        logInfo("Loaded in process: ${param.processName} (API $apiVersion)")
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.instagram.android") return
        if (!param.isFirstPackage) return

        logInfo("Initializing Liquid Glass hooks for com.instagram.android")

        val remotePrefs = try {
            getRemotePreferences("liquid_glass_prefs")
        } catch (t: Throwable) {
            logErr("RemotePreferences fallback: ${t.message}", t)
            null
        }

        val hookController = TabBarHookController(remotePrefs) { msg -> logInfo(msg) }
        controller = hookController

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
            logErr("Failed hooking Activity.onResume: ${t.message}", t)
        }

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
            logErr("Failed hooking ViewGroup.addView: ${t.message}", t)
        }
    }
}
