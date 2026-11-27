package com.muratbek.instaliquidglass.ipc

import android.content.Context
import android.content.SharedPreferences
import com.muratbek.instaliquidglass.model.GlassConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArrayList

object PrefsManager {
    private const val PREF_GROUP = "liquid_glass_prefs"

    @Volatile
    private var xposedService: XposedService? = null

    @Volatile
    private var localPrefs: SharedPreferences? = null

    private val statusListeners = CopyOnWriteArrayList<(Boolean, String) -> Unit>()

    fun initApp(context: Context) {
        val safeCtx = context.createDeviceProtectedStorageContext()
        localPrefs = safeCtx.getSharedPreferences(PREF_GROUP, Context.MODE_PRIVATE)

        try {
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    xposedService = service
                    val remote = service.getRemotePreferences(PREF_GROUP)
                    if (remote != null && remote.all.isEmpty() && localPrefs != null) {
                        GlassConfig.fromPrefs(localPrefs).writeTo(remote.edit())
                    }
                    val info = "${service.frameworkName} ${service.frameworkVersion} (API ${service.apiVersion})"
                    statusListeners.forEach { it(true, info) }
                }

                override fun onServiceDied(service: XposedService) {
                    xposedService = null
                    statusListeners.forEach { it(false, "Сервис LSPosed отключён") }
                }
            })
        } catch (_: Throwable) {
        }
    }

    fun isServiceBound(): Boolean = xposedService != null

    fun getStatusText(): String {
        val s = xposedService
        return if (s != null) {
            "${s.frameworkName} ${s.frameworkVersion} • API ${s.apiVersion} (Активен)"
        } else {
            "Ожидание LSPosed 2.2.1+ (Активируйте модуль в менеджере)"
        }
    }

    fun addStatusListener(listener: (Boolean, String) -> Unit) {
        statusListeners.add(listener)
        listener(isServiceBound(), getStatusText())
    }

    fun removeStatusListener(listener: (Boolean, String) -> Unit) {
        statusListeners.remove(listener)
    }

    fun loadConfig(): GlassConfig {
        val remote = try {
            xposedService?.getRemotePreferences(PREF_GROUP)
        } catch (_: Throwable) {
            null
        }
        return if (remote != null && remote.all.isNotEmpty()) {
            GlassConfig.fromPrefs(remote)
        } else {
            GlassConfig.fromPrefs(localPrefs)
        }
    }

    fun saveConfig(config: GlassConfig) {
        localPrefs?.edit()?.let { config.writeTo(it) }
        try {
            xposedService?.getRemotePreferences(PREF_GROUP)?.edit()?.let {
                config.writeTo(it)
            }
        } catch (_: Throwable) {
        }
    }
}
