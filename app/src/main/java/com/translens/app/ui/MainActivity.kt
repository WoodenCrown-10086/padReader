package com.translens.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.translens.app.R
import com.translens.app.compat.DeviceCompat
import com.translens.app.databinding.ActivityMainBinding
import com.translens.app.overlay.LensOverlayService
import com.translens.app.source.AccessibilityTextSource

/**
 * 配置/引导入口：检查并引导授权，然后启动透镜服务。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnToggle.setOnClickListener {
            if (DeviceCompat.hasOverlayPermission(this)) {
                LensOverlayService.start(this)
                finish() // 透镜已启动，退回桌面使用
            } else {
                DeviceCompat.openOverlaySettings(this)
            }
        }

        binding.btnAccessibility.setOnClickListener {
            DeviceCompat.openAccessibilitySettings(this)
        }

        binding.btnBattery.setOnClickListener {
            DeviceCompat.requestIgnoreBatteryOptimization(this)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val overlayOk = DeviceCompat.hasOverlayPermission(this)
        val accessibilityOk = AccessibilityTextSource.isEnabled()
        binding.tvStatus.text = buildString {
            append(getString(R.string.status_overlay)).append("：").append(if (overlayOk) "已开启" else "未开启")
            append('\n')
            append(getString(R.string.status_accessibility)).append("：").append(if (accessibilityOk) "已开启" else "未开启")
        }
    }
}
