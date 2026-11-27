package com.muratbek.instaliquidglass.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.muratbek.instaliquidglass.ipc.PrefsManager
import com.muratbek.instaliquidglass.model.GlassConfig

class MainActivity : AppCompatActivity() {

    private lateinit var config: GlassConfig
    private lateinit var previewView: GlassPreviewView
    private lateinit var statusText: TextView
    private lateinit var controlsContainer: LinearLayout

    private val statusListener: (Boolean, String) -> Unit = { active, text ->
        runOnUiThread {
            statusText.text = if (active) "✅ $text" else "⚠️ $text"
            statusText.setTextColor(if (active) 0xFF4ADE80.toInt() else 0xFFFBBF24.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = PrefsManager.loadConfig()

        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(0xFF0A0D12.toInt())
            isFillViewport = true
        }

        val rootCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(32))
        }
        rootScroll.addView(rootCol)

        // Header Title
        val title = TextView(this).apply {
            text = "Insta Liquid Glass"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text = "iOS 26 Fluid Bottom Bar • Instagram 457+ • LSPosed API 102"
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
        }
        statusText = TextView(this).apply {
            textSize = 13f
            setPadding(0, dp(6), 0, dp(14))
        }
        rootCol.addView(title)
        rootCol.addView(subtitle)
        rootCol.addView(statusText)

        // Interactive Live Preview
        previewView = GlassPreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
            ).apply {
                bottomMargin = dp(14)
            }
        }
        previewView.applyConfig(config)
        rootCol.addView(previewView)

        // Quick Action Row (Restart Instagram + Presets)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }

        val btnRestartIg = MaterialButton(this).apply {
            text = "Перезапустить IG"
            setOnClickListener { restartInstagram() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        }
        val btnBackup = MaterialButton(this).apply {
            text = "Экспорт / Импорт"
            setOnClickListener { showJsonDialog() }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            }
        }
        actionRow.addView(btnRestartIg)
        actionRow.addView(btnBackup)
        rootCol.addView(actionRow)

        // Presets Horizontal Strip
        val presetScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        }
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val presets = listOf(
            "iOS 26 Crystal" to GlassConfig.presetIos26Crystal(),
            "Midnight Obsidian" to GlassConfig.presetMidnightObsidian(),
            "Frosted Ice" to GlassConfig.presetFrostedIce(),
            "Neon Aurora" to GlassConfig.presetNeonAurora()
        )
        presets.forEach { (label, presetCfg) ->
            val chip = MaterialButton(this).apply {
                text = label
                textSize = 12f
                setOnClickListener {
                    updateAndSave(presetCfg.copy(enabled = config.enabled))
                    rebuildControls()
                    Toast.makeText(this@MainActivity, "Пресет: $label", Toast.LENGTH_SHORT).show()
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
            }
            presetRow.addView(chip)
        }
        presetScroll.addView(presetRow)
        rootCol.addView(presetScroll)

        controlsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootCol.addView(controlsContainer)

        rebuildControls()
        setContentView(rootScroll)
    }

    override fun onStart() {
        super.onStart()
        PrefsManager.addStatusListener(statusListener)
    }

    override fun onStop() {
        PrefsManager.removeStatusListener(statusListener)
        super.onStop()
    }

    private fun updateAndSave(newConfig: GlassConfig) {
        config = newConfig
        previewView.applyConfig(newConfig)
        PrefsManager.saveConfig(newConfig)
    }

    private fun rebuildControls() {
        controlsContainer.removeAllViews()
        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        fun addCard(title: String, block: LinearLayout.() -> Unit) {
            val card = MaterialCardView(this).apply {
                setCardBackgroundColor(0xFF131923.toInt())
                radius = 18f * density
                strokeWidth = dp(1)
                strokeColor = 0xFF1E293B.toInt()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(14) }
            }
            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }
            val header = TextView(this).apply {
                text = title
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFF38BDF8.toInt())
                setPadding(0, 0, 0, dp(8))
            }
            inner.addView(header)
            inner.block()
            card.addView(inner)
            controlsContainer.addView(card)
        }

        fun LinearLayout.addSwitch(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
            val sw = MaterialSwitch(this@MainActivity).apply {
                text = label
                isChecked = checked
                setTextColor(0xFFE2E8F0.toInt())
                textSize = 14f
                setOnCheckedChangeListener { _, isChecked -> onChanged(isChecked) }
            }
            addView(sw)
        }

        fun LinearLayout.addSlider(
            label: String,
            value: Float,
            from: Float,
            to: Float,
            unit: String = "",
            onChanged: (Float) -> Unit
        ) {
            val lbl = TextView(this@MainActivity).apply {
                text = "$label: ${"%.2f".format(value)}$unit"
                setTextColor(0xFFCBD5E1.toInt())
                textSize = 13f
                setPadding(0, dp(6), 0, 0)
            }
            val slider = Slider(this@MainActivity).apply {
                valueFrom = from
                valueTo = to
                this.value = value.coerceIn(from, to)
                addOnChangeListener { _, v, fromUser ->
                    lbl.text = "$label: ${"%.2f".format(v)}$unit"
                    if (fromUser) onChanged(v)
                }
            }
            addView(lbl)
            addView(slider)
        }

        addCard("Основные настройки") {
            addSwitch("Включить Liquid Glass в Instagram", config.enabled) {
                updateAndSave(config.copy(enabled = it))
            }
            addSwitch("Адаптивный контраст поверх Reels (SurfaceView)", config.reelsAdaptiveGlass) {
                updateAndSave(config.copy(reelsAdaptiveGlass = it))
            }
            addSwitch("Скрыть текстовые подписи вкладок (стиль iOS)", config.hideLabels) {
                updateAndSave(config.copy(hideLabels = it))
            }
            addSwitch("Скрыть кнопку создания (+)", config.hideCreateTab) {
                updateAndSave(config.copy(hideCreateTab = it))
            }
            addSwitch("Уменьшать панель при прокрутке ленты", config.compactOnScroll) {
                updateAndSave(config.copy(compactOnScroll = it))
            }
        }

        addCard("Оптика стекла и преломление (AGSL)") {
            addSlider("Радиус размытия (Blur)", config.blurRadiusDp, 0f, 60f, " dp") {
                updateAndSave(config.copy(blurRadiusDp = it))
            }
            addSlider("Прозрачность тонировки", config.tintAlpha, 0.05f, 0.85f) {
                updateAndSave(config.copy(tintAlpha = it))
            }
            addSlider("Насыщенность фона (Vibrancy)", config.saturationBoost, 0.8f, 2.0f, "x") {
                updateAndSave(config.copy(saturationBoost = it))
            }
            addSlider("Преломление линзы по краям", config.lensRefraction, 0f, 1.0f) {
                updateAndSave(config.copy(lensRefraction = it))
            }
            addSlider("Яркость блика рамки", config.specularBorderAlpha, 0f, 1.0f) {
                updateAndSave(config.copy(specularBorderAlpha = it))
            }
            addSlider("Толщина стеклянной кромки", config.borderWidthDp, 0f, 3.0f, " dp") {
                updateAndSave(config.copy(borderWidthDp = it))
            }
        }

        addCard("Геометрия плавающей капсулы") {
            addSlider("Скругление углов", config.cornerRadiusDp, 12f, 40f, " dp") {
                updateAndSave(config.copy(cornerRadiusDp = it))
            }
            addSlider("Отступы по бокам", config.marginHorizontalDp, 4f, 36f, " dp") {
                updateAndSave(config.copy(marginHorizontalDp = it))
            }
            addSlider("Отступ снизу", config.marginBottomDp, 2f, 36f, " dp") {
                updateAndSave(config.copy(marginBottomDp = it))
            }
            addSlider("Высота панели", config.barHeightDp, 48f, 76f, " dp") {
                updateAndSave(config.copy(barHeightDp = it))
            }
            addSlider("Масштаб иконок", config.iconScale, 0.75f, 1.30f, "x") {
                updateAndSave(config.copy(iconScale = it))
            }
        }

        addCard("Физика пилюли и жесты") {
            addSwitch("Плавающая стеклянная пилюля активной вкладки", config.pillEnabled) {
                updateAndSave(config.copy(pillEnabled = it))
            }
            addSwitch("Эффект резинки и подсветка при нажатии", config.touchRubberBand) {
                updateAndSave(config.copy(touchRubberBand = it))
            }
            addSwitch("Переключение вкладок свайпом по панели", config.dragToSwitchTab) {
                updateAndSave(config.copy(dragToSwitchTab = it))
            }
            addSlider("Яркость пилюли", config.pillAlpha, 0.05f, 0.70f) {
                updateAndSave(config.copy(pillAlpha = it))
            }
            addSlider("Пружинистость анимации (Bounce)", config.pillBounce, 0.30f, 0.95f) {
                updateAndSave(config.copy(pillBounce = it))
            }
        }
    }

    private fun restartInstagram() {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop com.instagram.android"))
            val code = proc.waitFor()
            if (code == 0) {
                Toast.makeText(this, "Instagram остановлен. Запускаем...", Toast.LENGTH_SHORT).show()
                packageManager.getLaunchIntentForPackage("com.instagram.android")?.let { startActivity(it) }
                return
            }
        } catch (_: Throwable) {
        }
        // Fallback: open App Info page so user can tap Force Stop
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:com.instagram.android")
            }
            startActivity(intent)
        } catch (_: Throwable) {
            Toast.makeText(this, "Закройте Instagram вручную", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showJsonDialog() {
        val input = EditText(this).apply {
            setText(config.toJson())
            textSize = 11f
            setTextColor(Color.WHITE)
        }
        AlertDialog.Builder(this)
            .setTitle("JSON Конфигурация")
            .setView(input)
            .setPositiveButton("Применить") { _, _ ->
                try {
                    val imported = GlassConfig.fromJson(input.text.toString())
                    updateAndSave(imported)
                    rebuildControls()
                    Toast.makeText(this, "Конфигурация применена!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка JSON: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNeutralButton("Копировать") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("InstaLiquidGlass", input.text.toString()))
                Toast.makeText(this, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }
}
