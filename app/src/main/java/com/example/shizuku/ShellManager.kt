package com.example.shizuku

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShellExecutionResult(
    val command: String,
    val isSuccess: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long,
    val usedShizuku: Boolean,
    val isRoot: Boolean
)

class ShellManager private constructor(private val context: Context) {
    private val shizukuManager = ShizukuManager.getInstance(context)

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _lastResult = MutableStateFlow<ShellExecutionResult?>(null)
    val lastResult: StateFlow<ShellExecutionResult?> = _lastResult.asStateFlow()

    suspend fun executeCommand(command: String, timeoutMs: Long = 8000L): ShellExecutionResult {
        return withContext(Dispatchers.IO) {
            _isExecuting.value = true
            val startTime = System.currentTimeMillis()
            var usedShizuku = false
            var isRoot = false

            try {
                withTimeout(timeoutMs) {
                    val process = if (shizukuManager.shizukuState.value == ShizukuState.AUTHORIZED_ACTIVE) {
                        usedShizuku = true
                        isRoot = (shizukuManager.shizukuUid.value == 0)
                        try {
                            val method = Shizuku::class.java.getDeclaredMethod(
                                "newProcess",
                                Array<String>::class.java,
                                Array<String>::class.java,
                                String::class.java
                            ).apply { isAccessible = true }
                            method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                        } catch (e: Exception) {
                            Log.w("ShellManager", "Shizuku process reflection fallback to runtime exec", e)
                            Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                        }
                    } else {
                        // Standard local fallback shell
                        Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                    }

                    val stdoutBuilder = StringBuilder()
                    val stderrBuilder = StringBuilder()

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errReader = BufferedReader(InputStreamReader(process.errorStream))

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdoutBuilder.appendLine(line)
                    }
                    reader.close()

                    while (errReader.readLine().also { line = it } != null) {
                        stderrBuilder.appendLine(line)
                    }
                    errReader.close()

                    val exitCode = process.waitFor()
                    val duration = System.currentTimeMillis() - startTime
                    val isSuccess = (exitCode == 0)

                    val result = ShellExecutionResult(
                        command = command,
                        isSuccess = isSuccess,
                        exitCode = exitCode,
                        stdout = stdoutBuilder.toString().trim(),
                        stderr = stderrBuilder.toString().trim(),
                        executionTimeMs = duration,
                        usedShizuku = usedShizuku,
                        isRoot = isRoot
                    )
                    _lastResult.value = result
                    result
                }
            } catch (e: Exception) {
                Log.e("ShellManager", "Command execution failed: $command", e)
                val duration = System.currentTimeMillis() - startTime
                val result = ShellExecutionResult(
                    command = command,
                    isSuccess = false,
                    exitCode = -1,
                    stdout = "",
                    stderr = "Execution Exception: ${e.message}",
                    executionTimeMs = duration,
                    usedShizuku = usedShizuku,
                    isRoot = isRoot
                )
                _lastResult.value = result
                result
            } finally {
                _isExecuting.value = false
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ShellManager? = null

        fun getInstance(context: Context): ShellManager {
            return instance ?: synchronized(this) {
                instance ?: ShellManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
