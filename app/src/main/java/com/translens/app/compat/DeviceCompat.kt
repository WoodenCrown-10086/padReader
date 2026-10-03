package com.translens.app.compat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * 厂商 ROM 差异封装：识别 ROM、检查/跳转关键权限与后台白名单设置。
 *
 * 国产 ROM（华为鸿蒙 / 小米 / OPPO / vivo / 荣耀）对悬浮窗、后台保活、
 * 电池优化各有一套设置路径。这里做"尽力跳转 + 兜底到应用详情页"，
 * 具体路径以机型为准，接入真机后可按需补充深链。
 */
object DeviceCompat {

    enum class Rom { HUAWEI, HONOR, XIAOMI, OPPO, VIVO, SAMSUNG, GENERIC }

    val rom: Rom by lazy { detectRom() }

    private fun detectRom(): Rom = when (Build.MANUFACTURER.lowercase()) {
        "huawei" -> Rom.HUAWEI
        "honor" -> Rom.HONOR
        "xiaomi", "redmi" -> Rom.XIAOMI
        "oppo", "oneplus", "realme" -> Rom.OPPO
        "vivo", "iqoo" -> Rom.VIVO
        "samsung" -> Rom.SAMSUNG
        else -> Rom.GENERIC
    }

    /** 是否已授予悬浮窗权限。 */
    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** 跳转悬浮窗授权页。 */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** 跳转无障碍设置页（用户手动开启本服务）。 */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** 跳转电池优化白名单列表页。 */
    fun openBatteryOptimizationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** 直接请求忽略本应用的电池优化（Android 6+）。 */
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** 应用详情页（自启动 / 关联启动 / 后台白名单等厂商设置的统一兜底入口）。 */
    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 厂商「自启动」设置：多数 ROM 无公开 Intent，先统一跳应用详情页，
     * 用户手动找「自启动 / 关联启动」开关。
     */
    fun openAutoStartSettings(context: Context) = openAppDetailsSettings(context)
}
