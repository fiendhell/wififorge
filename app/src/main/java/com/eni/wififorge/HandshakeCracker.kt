package com.eni.wififorge

import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest

class HandshakeCracker(private val onLog: (String) -> Unit) {

    fun crackWithAircrack(capPath: String, wordlistPath: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val proc = ProcessBuilder(
                "su", "-c",
                "aircrack-ng -w $wordlistPath $capPath"
            ).redirectErrorStream(true).start()

            proc.inputStream.bufferedReader().forEachLine { line ->
                onLog(line)
            }
            proc.waitFor()
        }
    }

    fun verifyPassword(ssid: String, passphrase: String, capturedMic: ByteArray): Boolean {
        val pmk = pbkdf2(passphrase.toByteArray(), ssid.toByteArray(), 4096, 32)
        return pmk.contentEquals(capturedMic)
    }

    private fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLen: Int): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA1")
        val secret = javax.crypto.spec.SecretKeySpec(password, "HmacSHA1")
        mac.init(secret)
        val out = ByteArray(keyLen)
        var block = 1
        var offset = 0
        while (offset < keyLen) {
            mac.update(salt)
            mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(),
                (block ushr 8).toByte(), block.toByte()))
            var u = mac.doFinal()
            var t = u.copyOf()
            for (i in 1 until iterations) {
                mac.update(u)
                u = mac.doFinal()
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val chunk = minOf(keyLen - offset, 20)
            System.arraycopy(t, 0, out, offset, chunk)
            offset += chunk
            block++
        }
        return out
    }
}
