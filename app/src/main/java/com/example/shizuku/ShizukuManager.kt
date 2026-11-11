package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuState {
    NOT_INSTALLED,
    INSTALLED_NOT_RUNNING,
    RUNNING_NOT_AUTHORIZED,
    AUTHORIZED_ACTIVE,
    BINDER_DEAD
}

class ShizukuManager private constructor(private val context: Context) {

    private val _shizukuState = MutableStateFlow(ShizukuState.NOT_INSTALLED)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Initializing Shizuku...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _shizukuUid = MutableStateFlow(-1)
    val shizukuUid: StateFlow<Int> = _shizukuUid.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d("ShizukuManager", "Shizuku binder received")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w("ShizukuManager", "Shizuku binder died")
        _shizukuState.value = ShizukuState.BINDER_DEAD
        _statusMessage.value = "Shizuku service died or was stopped by system. Please restart via Wireless Debugging."
        _shizukuUid.value = -1
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    _statusMessage.value = "Shizuku permission granted! Elevated ADB shell access active."
                } else {
                    _statusMessage.value = "Shizuku permission denied by user."
                }
                refreshStatus()
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Exception) {
            Log.e("ShizukuManager", "Error registering Shizuku listeners", e)
        }
        refreshStatus()
    }

    fun isInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun refreshStatus() {
        if (!isInstalled()) {
            _shizukuState.value = ShizukuState.NOT_INSTALLED
            _statusMessage.value = "Shizuku app not found on this device."
            _shizukuUid.value = -1
            return
        }

        val isRunning = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }

        if (!isRunning) {
            _shizukuState.value = ShizukuState.INSTALLED_NOT_RUNNING
            _statusMessage.value = "Shizuku installed but not running. Follow the Wireless Debugging guide to start."
            _shizukuUid.value = -1
            return
        }

        val hasPermission = try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Exception) {
            false
        }

        if (hasPermission) {
            _shizukuState.value = ShizukuState.AUTHORIZED_ACTIVE
            val uid = try { Shizuku.getUid() } catch (_: Exception) { 2000 }
            _shizukuUid.value = uid
            val role = if (uid == 0) "ROOT (uid=0)" else "ADB SHELL (uid=$uid)"
            _statusMessage.value = "Shizuku Active & Connected ($role)"
        } else {
            _shizukuState.value = ShizukuState.RUNNING_NOT_AUTHORIZED
            _statusMessage.value = "Shizuku is running. Tap 'Request Shizuku Permission' to allow ADB access."
            _shizukuUid.value = -1
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.isPreV11()) {
                _statusMessage.value = "Legacy Shizuku version detected. Please update Shizuku."
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _statusMessage.value = "Permission is already granted."
                refreshStatus()
                return
            }
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
        } catch (e: Exception) {
            Log.e("ShizukuManager", "Error requesting Shizuku permission", e)
            _statusMessage.value = "Failed to request permission: ${e.message}"
        }
    }

    companion object {
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_SHIZUKU = 4091

        @Volatile
        private var instance: ShizukuManager? = null

        fun getInstance(context: Context): ShizukuManager {
            return instance ?: synchronized(this) {
                instance ?: ShizukuManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
