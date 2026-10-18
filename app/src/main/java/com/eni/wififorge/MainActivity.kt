package com.eni.wififorge

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var scanner: WifiScanner
    private lateinit var engine: BruteEngine
    private lateinit var logView: TextView
    private var lastScan: List<android.net.wifi.ScanResult> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 1)

        scanner = WifiScanner(this)
        logView = findViewById(R.id.txtLog)
        engine = BruteEngine(scanner) { msg -> runOnUiThread { appendLog(msg) } }

        val list = findViewById<ListView>(R.id.listNets)
        findViewById<Button>(R.id.btnScan).setOnClickListener {
            lifecycleScope.launch {
                lastScan = scanner.scanNetworks()
                list.adapter = ArrayAdapter(this@MainActivity,
                    android.R.layout.simple_list_item_1,
                    lastScan.map { "${it.SSID}  (${it.level} dBm)" })
                appendLog("Найдено сетей: ${lastScan.size}")
            }
        }

        list.setOnItemClickListener { _, _, position, _ ->
            findViewById<EditText>(R.id.inputTarget).setText(lastScan[position].SSID)
        }

        findViewById<Button>(R.id.btnAttack).setOnClickListener {
            val target = findViewById<EditText>(R.id.inputTarget).text.toString()
            if (target.isBlank()) { appendLog("Укажи SSID цели"); return@setOnClickListener }

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            engine.start(target, lifecycleScope)
        }
    }

    private fun appendLog(msg: String) {
        // Новые строки добавляются в начало, чтобы всегда был виден прогресс
        logView.text = "$msg\n" + logView.text
    }
}
