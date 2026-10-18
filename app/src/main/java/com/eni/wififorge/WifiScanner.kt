package com.eni.wififorge

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.ScanResult

class WifiScanner(private val ctx: Context) {

    private val wifi: WifiManager =
        ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun scanNetworks(): List<ScanResult> {
        wifi.startScan()
        return wifi.scanResults
            .sortedByDescending { it.level }
            .distinctBy { it.SSID }
            .filter { it.SSID.isNotBlank() }
    }

    fun tryConnect(ssid: String, password: String): Boolean {
        val cfg = android.net.wifi.WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
            allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK)
        }
        val netId = wifi.addNetwork(cfg)
        if (netId == -1) return false
        wifi.disconnect()
        val enabled = wifi.enableNetwork(netId, true)
        wifi.reconnect()

        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 4000) {
            val info = wifi.connectionInfo
            if (info != null && info.networkId == netId &&
                info.supplicantState == android.net.wifi.SupplicantState.COMPLETED) {
                return true
            }
            Thread.sleep(200)
        }
        wifi.removeNetwork(netId)
        return false
    }
}
