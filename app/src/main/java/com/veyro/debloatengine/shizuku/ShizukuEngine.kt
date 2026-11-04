package com.veyro.debloatengine.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.veyro.debloatengine.data.model.CommandResult
import com.veyro.debloatengine.data.model.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ShizukuEngine — rootless ADB execution layer.
 *
 * Owns the Shizuku binder lifecycle, permission flow, and synchronous
 * shell command execution. All reversible debloat commands are issued
 * through [exec].
 */
object ShizukuEngine {

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val REQUEST_CODE = 1001

    private val _state = MutableStateFlow(ShizukuState.NOT_INSTALLED)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshState() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refreshState() }
    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            if (requestCode == REQUEST_CODE) {
                _state.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ShizukuState.READY
                } else {
                    ShizukuState.PERMISSION_DENIED
                }
            }
        }

    private var listenersRegistered = false

    fun registerListeners(context: Context) {
        if (!listenersRegistered) {
            try {
                Shizuku.addBinderReceivedListener(binderReceivedListener)
                Shizuku.addBinderDeadListener(binderDeadListener)
                Shizuku.addRequestPermissionResultListener(permissionResultListener)
                listenersRegistered = true
            } catch (_: Exception) { /* binder not ready yet */ }
        }
        refreshState(context)
    }

    fun unregisterListeners() {
        if (!listenersRegistered) return
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Exception) { }
        listenersRegistered = false
    }

    fun refreshState(context: Context? = null) {
        val ping = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        if (!ping) {
            val installed = context?.let { isPackageInstalled(it, SHIZUKU_PACKAGE) } ?: false
            _state.value = if (installed) ShizukuState.STOPPED else ShizukuState.NOT_INSTALLED
            return
        }
        val granted = try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (_: Exception) { false }
        _state.value = if (granted) ShizukuState.READY else ShizukuState.PERMISSION_DENIED
    }

    fun requestPermission() {
        try {
            if (Shizuku.isPreV11()) {
                _state.value = ShizukuState.PERMISSION_DENIED
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (_: Exception) {
            _state.value = ShizukuState.PERMISSION_DENIED
        }
    }

    val isReady: Boolean get() = _state.value == ShizukuState.READY

    /**
     * Executes a shell command through the Shizuku process binder.
     * Suspend + IO dispatcher; safe to call from any coroutine.
     */
    suspend fun exec(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (_state.value != ShizukuState.READY) {
            return@withContext CommandResult(-1, "", "Shizuku is not connected or permission denied.")
        }
        try {
            // Shizuku.newProcess is hidden in the public API — invoke reflectively.
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process
            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) stdout.append(line).append('\n')
            while (stderrReader.readLine().also { line = it } != null) stderr.append(line).append('\n')
            val exit = process.waitFor()
            CommandResult(exit, stdout.toString().trim(), stderr.toString().trim())
        } catch (e: Exception) {
            CommandResult(-1, "", "Execution error: ${e.localizedMessage}")
        }
    }

    /**
     * Applies a system-wide animation speed to all three scales
     * (window / transition / animator).
     */
    suspend fun setAnimationScale(scale: Float): CommandResult {
        val s = String.format("%.2f", scale)
        return exec(
            "settings put global window_animation_scale $s && " +
                "settings put global transition_animation_scale $s && " +
                "settings put global animator_duration_scale $s"
        )
    }

    /** Reads the current animation scale (window scale is the canonical one). */
    suspend fun readAnimationScale(): Float {
        val res = exec("settings get global window_animation_scale")
        return res.combined.toFloatOrNull() ?: 1.0f
    }

    // ------------------------------------------------------------------
    // Per-app control actions (used by package detail sheets & modes)
    // ------------------------------------------------------------------

    /** Kills all cached/background processes — foreground apps are untouched. */
    suspend fun killBackgroundProcesses(): CommandResult =
        exec("am kill-all")

    suspend fun forceStop(packageName: String): CommandResult =
        exec("am force-stop $packageName")

    /** Freeze: suspended + cannot run. Fully reversible with [unsuspend]. */
    suspend fun freeze(packageName: String): CommandResult =
        exec("cmd package suspend $packageName")

    suspend fun unfreeze(packageName: String): CommandResult =
        exec("cmd package unsuspend $packageName")

    suspend fun disable(packageName: String): CommandResult =
        exec("pm disable-user --user 0 $packageName")

    suspend fun enable(packageName: String): CommandResult =
        exec("pm enable $packageName")

    /** Launches an app by package id (monkey resolves its launcher activity). */
    suspend fun launchApp(packageName: String): CommandResult =
        exec("monkey -p $packageName -c android.intent.category.LAUNCHER 1")

    /** System battery saver toggle (part of Eco Mode). */
    suspend fun setBatterySaver(enabled: Boolean): CommandResult =
        exec("settings put global low_power ${if (enabled) 1 else 0}")

    /**
     * Silent in-place APK install through Shizuku (`pm install -r`).
     * Used by the updater when Shizuku is active.
     */
    suspend fun silentInstall(apkPath: String): CommandResult =
        exec("pm install -r \"$apkPath\"")

    private fun isPackageInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
