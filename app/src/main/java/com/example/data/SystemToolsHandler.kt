package com.example.data

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class SystemToolsHandler(private val context: Context) {

    private var isTorchOn = false

    fun handleFunctionCall(name: String, argsJson: JSONObject): Pair<String, String> {
        return try {
            when (name) {
                "openWebsite" -> {
                    val url = argsJson.optString("url", "")
                    val result = openWebsite(url)
                    Pair("openWebsite", result)
                }
                "getSystemStatus" -> {
                    val status = getSystemStatus()
                    Pair("getSystemStatus", status)
                }
                "toggleTorch" -> {
                    val enable = argsJson.optBoolean("enable", !isTorchOn)
                    val res = setTorch(enable)
                    Pair("toggleTorch", res)
                }
                "getTimeAndDate" -> {
                    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm:ss z", Locale.getDefault())
                    val timeString = sdf.format(Date())
                    Pair("getTimeAndDate", "Current system time: $timeString")
                }
                else -> {
                    Pair(name, "Executed tool $name with parameters: $argsJson")
                }
            }
        } catch (e: Exception) {
            Log.e("SystemToolsHandler", "Error handling tool $name", e)
            Pair(name, "Error executing protocol: ${e.localizedMessage}")
        }
    }

    fun openWebsite(rawUrl: String): String {
        var url = rawUrl.trim()
        if (url.isEmpty()) return "Error: No URL provided."
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            "Successfully opened $url in browser, Sir."
        } catch (e: Exception) {
            "Unable to launch browser for $url: ${e.message}"
        }
    }

    fun getSystemStatus(): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = batteryManager?.isCharging ?: false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val networkType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "High-Speed Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular 5G/LTE"
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true -> "Online (Ethernet/VPN)"
            else -> "Offline"
        }

        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableGb = availableBytes / (1024 * 1024 * 1024)
        val totalGb = totalBytes / (1024 * 1024 * 1024)

        return "JARVIS Telemetry Diagnostics:\n" +
                "• Power Core: $batteryLevel% ${if (isCharging) "[CHARGING - ARC REACTOR ENGAGED]" else "[DISCHARGING]"}\n" +
                "• Uplink Status: $networkType\n" +
                "• Storage Matrix: ${availableGb}GB free of ${totalGb}GB\n" +
                "• Device Unit: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n" +
                "• Security Protocol: Stark Industries Quantum Link Active"
    }

    private fun setTorch(enable: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return "Torch unit not detected."
            cameraManager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            if (enable) "Illumination engaged, Sir." else "Illumination disengaged."
        } catch (e: Exception) {
            "Torch control failure: ${e.message}"
        }
    }
}
