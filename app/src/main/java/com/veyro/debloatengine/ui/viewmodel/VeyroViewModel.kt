package com.veyro.debloatengine.ui.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veyro.debloatengine.data.model.AppItem
import com.veyro.debloatengine.data.model.BatchProgress
import com.veyro.debloatengine.data.model.DebloatStrategy
import com.veyro.debloatengine.data.model.DownloadState
import com.veyro.debloatengine.data.model.RamInfo
import com.veyro.debloatengine.data.model.ShizukuState
import com.veyro.debloatengine.data.model.StorageInfo
import com.veyro.debloatengine.data.model.UpdateInfo
import com.veyro.debloatengine.data.repository.DebloatRepository
import com.veyro.debloatengine.data.repository.PerformanceModeManager
import com.veyro.debloatengine.data.repository.PerfMode
import com.veyro.debloatengine.data.repository.RemovalLog
import com.veyro.debloatengine.data.repository.SystemTweaksRepository
import com.veyro.debloatengine.data.repository.ToolsRepository
import com.veyro.debloatengine.shizuku.ShizukuEngine
import com.veyro.debloatengine.update.UpdateManager
import com.veyro.debloatengine.update.UpdateNotifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VeyroUiState(
    val isScanning: Boolean = false,
    val storageInfo: StorageInfo = StorageInfo(),
    val ramInfo: RamInfo = RamInfo(),
    val isFlushingCache: Boolean = false,
    val flushMessage: String? = null,
    val allPackages: List<AppItem> = emptyList(),
    val shizukuState: ShizukuState = ShizukuState.NOT_INSTALLED,
    val animationScale: Float = 1.0f,
    val scaleApplyMessage: String? = null,
    val selectedPackages: Set<String> = emptySet(),
    val batch: BatchProgress = BatchProgress(),
    val removedCount: Int = 0,
    val strategy: DebloatStrategy = DebloatStrategy.FREEZE,
    // Engine modes
    val currentMode: PerfMode = PerfMode.ECO,
    val modeLog: List<String> = emptyList(),
    val isApplyingMode: Boolean = false,
    val selectedGame: String? = null,
    // System tweaks: DNS / refresh rate / wake-locks
    val dnsState: SystemTweaksRepository.DnsState = SystemTweaksRepository.DnsState(),
    val dnsMessage: String? = null,
    val refreshState: SystemTweaksRepository.RefreshState = SystemTweaksRepository.RefreshState(),
    val refreshMessage: String? = null,
    val wakeLockCut: Set<String> = emptySet(),
    val removalEntries: List<RemovalLog.Entry> = emptyList(),
    // Tools lab
    val battery: ToolsRepository.BatteryInfo = ToolsRepository.BatteryInfo(),
    val thermal: ToolsRepository.ThermalInfo = ToolsRepository.ThermalInfo("NONE", ""),
    val swap: ToolsRepository.SwapInfo = ToolsRepository.SwapInfo(),
    val sensorsOff: Boolean = false,
    val overlays: List<ToolsRepository.OverlayInfo> = emptyList(),
    val isCompiling: Boolean = false,
    val toolsLog: List<String> = emptyList(),
    val toolsNotice: String? = null,
    // Gaming overdrive
    val game720p: Boolean = false,
    val gameTouchBoost: Boolean = false,
    val resolution: ToolsRepository.ResolutionInfo = ToolsRepository.ResolutionInfo("", ""),
    val customGames: Set<String> = emptySet(),
    // Review-before-run confirmation
    val pendingBatchTargets: List<AppItem> = emptyList(),
    // Update system
    val updateInfo: UpdateInfo? = null,
    val showUpdateBanner: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val downloadState: DownloadState = DownloadState.IDLE,
    val downloadPercent: Int = 0
) {
    val cpuKillers: List<AppItem>
        get() = allPackages.filter {
            !it.isProtected && (
                    it.reason.contains("daemon", true) ||
                            it.reason.contains("Telemetry", true) ||
                            it.reason.contains("CPU", true)
                    )
        }
    val ramHogs: List<AppItem>
        get() = allPackages.filter {
            !it.isProtected && (
                    it.reason.contains("RAM", true) || it.reason.contains("Bloat tagged", true)
                    )
        }
    val installedGames: List<AppItem>
        get() = allPackages.filter {
            it.isInstalled && (it.isGame || customGames.contains(it.packageName))
        }
}

class VeyroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DebloatRepository()
    private val _uiState = MutableStateFlow(VeyroUiState())
    val uiState: StateFlow<VeyroUiState> = _uiState.asStateFlow()

    private var downloadId: Long = -1L

    init {
        viewModelScope.launch {
            ShizukuEngine.state.collect { state ->
                _uiState.update { it.copy(shizukuState = state) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(animationScale = ShizukuEngine.readAnimationScale()) }
        }
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("veyro_modes", Context.MODE_PRIVATE)
        _uiState.update {
            it.copy(
                currentMode = PerformanceModeManager.current(app),
                customGames = app.getSharedPreferences("veyro_modes", Context.MODE_PRIVATE)
                    .getStringSet("custom_games", emptySet()) ?: emptySet(),
                game720p = prefs.getBoolean("game_720p", false),
                gameTouchBoost = prefs.getBoolean("game_touch", false)
            )
        }
        checkForUpdates(showBannerOnly = true)
    }

    // ------------------------------------------------------------------
    // Data loading
    // ------------------------------------------------------------------
    fun refreshData() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            // Metrics are local and instant — show them first so gauges never sit at 0%.
            _uiState.update {
                it.copy(storageInfo = repository.getStorageInfo(), ramInfo = repository.getRamInfo(context))
            }
            val packages = repository.scanPackages(context)
            val removed = RemovalLog.load(context)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    allPackages = packages,
                    removedCount = removed.size,
                    removalEntries = removed,
                    selectedPackages = emptySet(),
                    wakeLockCut = SystemTweaksRepository.loadCutPackages(context)
                )
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    dnsState = SystemTweaksRepository.readDnsState(),
                    refreshState = SystemTweaksRepository.readRefreshState(context)
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Shizuku
    // ------------------------------------------------------------------
    fun requestShizukuPermission() = ShizukuEngine.requestPermission()

    // ------------------------------------------------------------------
    // Storage Unchoker
    // ------------------------------------------------------------------
    fun flushSystemCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFlushingCache = true, flushMessage = null) }
            val res = repository.flushSystemCache()
            val msg = if (res.isSuccess) {
                "System I/O cache flushed — flash speed restored."
            } else {
                "Cache flush failed: ${res.stderr.ifBlank { "Shizuku permission required" }}"
            }
            _uiState.update {
                it.copy(
                    isFlushingCache = false,
                    flushMessage = msg,
                    storageInfo = repository.getStorageInfo()
                )
            }
        }
    }

    fun clearFlushMessage() = _uiState.update { it.copy(flushMessage = null) }

    // ------------------------------------------------------------------
    // System Animation Tweaker
    // ------------------------------------------------------------------
    fun applyAnimationScale(scale: Float) {
        viewModelScope.launch {
            val res = ShizukuEngine.setAnimationScale(scale)
            _uiState.update {
                if (res.isSuccess) {
                    it.copy(animationScale = scale, scaleApplyMessage = "Animation speed set to ${"%.1f".format(scale)}x")
                } else {
                    it.copy(scaleApplyMessage = "Failed: ${res.stderr.ifBlank { "Shizuku not ready" }}")
                }
            }
        }
    }

    fun clearScaleMessage() = _uiState.update { it.copy(scaleApplyMessage = null) }

    // ------------------------------------------------------------------
    // Debloat / Restore engine
    // ------------------------------------------------------------------
    fun toggleSelection(packageName: String) {
        _uiState.update { state ->
            val target = state.allPackages.find { it.packageName == packageName }
            if (target?.isProtected == true) return@update state
            val sel = state.selectedPackages.toMutableSet()
            if (!sel.add(packageName)) sel.remove(packageName)
            state.copy(selectedPackages = sel)
        }
    }

    fun selectAll(packages: List<AppItem>) {
        _uiState.update { state ->
            state.copy(selectedPackages = packages.filter { !it.isProtected }.map { it.packageName }.toSet())
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedPackages = emptySet()) }

    fun setStrategy(strategy: DebloatStrategy) {
        _uiState.update { it.copy(strategy = strategy) }
    }

    /** Explicit action chips (Freeze/Disable/Remove) — bypasses the strategy selector. */
    fun runBatchWithStrategy(context: Context, targets: List<AppItem>, strategy: DebloatStrategy) {
        if (targets.isEmpty() || _uiState.value.batch.isRunning) return
        _uiState.update { it.copy(strategy = strategy) }
        runBatch(context, targets)
    }

    fun runBatch(context: Context, targets: List<AppItem>) {
        if (targets.isEmpty() || _uiState.value.batch.isRunning) return
        val strategy = _uiState.value.strategy
        val verb = when (strategy) {
            DebloatStrategy.FREEZE -> "FREEZED"
            DebloatStrategy.DISABLE -> "DISABLED"
            DebloatStrategy.UNINSTALL -> "REMOVED"
        }
        viewModelScope.launch {
            val logs = mutableListOf(
                "Veyro batch (${strategy.label}) — ${targets.size} target(s)"
            )
            _uiState.update {
                it.copy(
                    batch = BatchProgress(isRunning = true, total = targets.size, logs = logs.toList())
                )
            }
            targets.forEachIndexed { index, item ->
                _uiState.update {
                    it.copy(batch = it.batch.copy(currentPackage = item.appName, index = index + 1))
                }
                val (ok, log) = repository.debloatPackage(context, item, strategy)
                logs += "[${index + 1}/${targets.size}] ${item.appName}: ${if (ok) verb else "FAILED"}\n$log"
                _uiState.update { it.copy(batch = it.batch.copy(logs = logs.toList())) }
            }
            logs += "Batch complete. All actions logged — fully reversible via Restore."
            _uiState.update {
                it.copy(
                    batch = it.batch.copy(isRunning = false, finished = true),
                    selectedPackages = emptySet()
                )
            }
            refreshData()
        }
    }

    fun restoreSingle(context: Context, packageName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(batch = BatchProgress(isRunning = true, total = 1, index = 1, currentPackage = packageName))
            }
            repository.restorePackage(context, packageName)
            _uiState.update { it.copy(batch = it.batch.copy(isRunning = false, finished = true)) }
            refreshData()
        }
    }

    fun restoreAll(context: Context) {
        viewModelScope.launch {
            val entries = RemovalLog.load(context)
            if (entries.isEmpty()) return@launch
            _uiState.update {
                it.copy(batch = BatchProgress(isRunning = true, total = entries.size, logs = listOf("Restoring ${entries.size} package(s)…")))
            }
            entries.forEachIndexed { i, entry ->
                _uiState.update { it.copy(batch = it.batch.copy(currentPackage = entry.appName, index = i + 1)) }
                val (_, log) = repository.restorePackage(context, entry.packageName)
                _uiState.update {
                    it.copy(batch = it.batch.copy(logs = it.batch.logs + "[$i/${entries.size}] ${entry.appName}: RESTORED\n$log"))
                }
            }
            _uiState.update {
                it.copy(
                    batch = it.batch.copy(isRunning = false, finished = true, logs = it.batch.logs + "Restore All complete."),
                    removedCount = 0
                )
            }
            refreshData()
        }
    }

    fun dismissBatchDialog() = _uiState.update { it.copy(batch = BatchProgress()) }

    // ------------------------------------------------------------------
    // Engine modes (Eco / Performance / Gaming) + game boost
    // ------------------------------------------------------------------
    fun applyMode(context: Context, mode: PerfMode) {
        if (_uiState.value.isApplyingMode) return
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingMode = true, modeLog = listOf("Applying ${mode.title}…")) }
            val log = PerformanceModeManager.apply(context, mode, _uiState.value.ramHogs)
            _uiState.update {
                it.copy(
                    isApplyingMode = false,
                    currentMode = mode,
                    modeLog = log,
                    animationScale = mode.animationScale
                )
            }
            refreshData()
        }
    }

    fun selectGame(packageName: String?) {
        _uiState.update { it.copy(selectedGame = packageName) }
    }

    fun addCustomGame(pkg: String) {
        val prefs = getApplication<Application>().getSharedPreferences("veyro_modes", Context.MODE_PRIVATE)
        val updated = prefs.getStringSet("custom_games", emptySet())!!.toMutableSet().apply { add(pkg) }
        prefs.edit().putStringSet("custom_games", updated).apply()
        _uiState.update { it.copy(customGames = updated) }
    }

    fun removeCustomGame(pkg: String) {
        val prefs = getApplication<Application>().getSharedPreferences("veyro_modes", Context.MODE_PRIVATE)
        val updated = prefs.getStringSet("custom_games", emptySet())!!.toMutableSet().apply { remove(pkg) }
        prefs.edit().putStringSet("custom_games", updated).apply()
        _uiState.update { it.copy(customGames = updated) }
    }

    fun loadCustomGames() {
        val prefs = getApplication<Application>().getSharedPreferences("veyro_modes", Context.MODE_PRIVATE)
        _uiState.update { it.copy(customGames = prefs.getStringSet("custom_games", emptySet()) ?: emptySet()) }
    }

    /** Gaming Mode: clear RAM + freeze hogs + animations off + launch the game. */
    fun boostAndLaunch(context: Context) {
        val game = _uiState.value.selectedGame ?: return
        if (_uiState.value.isApplyingMode) return
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingMode = true, modeLog = listOf("Gaming boost for $game…")) }

            // Game Overdrive pre-launch tweaks
            val overdriveLog = mutableListOf<String>()
            val state = _uiState.value
            if (state.game720p) {
                val res = ToolsRepository.readResolution()
                if (!res.overridden && res.native.isNotBlank()) {
                    ToolsRepository.downscaleTarget(res.native)?.let { (w, h) ->
                        val r = ToolsRepository.setResolution(w, h)
                        overdriveLog += if (r.exitCode == 0) "Resolution downgraded to ${w}x$h (FPS boost)" else "Downscale failed"
                    }
                }
            }
            if (state.gameTouchBoost) {
                val r = ToolsRepository.setCompetitiveTouch(true)
                overdriveLog += if (r.exitCode == 0) "Competitive touch latency applied" else "Touch tweak failed"
            }

            val log = PerformanceModeManager.apply(
                context, PerfMode.GAMING, _uiState.value.ramHogs, launchPackage = game
            )
            _uiState.update {
                it.copy(
                    isApplyingMode = false,
                    currentMode = PerfMode.GAMING,
                    modeLog = overdriveLog + log,
                    animationScale = PerfMode.GAMING.animationScale
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Feature 4/5/7 — system tweaks
    // ------------------------------------------------------------------
    fun setPrivateDns(provider: SystemTweaksRepository.DnsProvider) {
        viewModelScope.launch {
            val res = SystemTweaksRepository.setPrivateDns(provider)
            _uiState.update {
                if (res.exitCode == 0) {
                    it.copy(
                        dnsState = SystemTweaksRepository.DnsState(enabled = true, host = provider.host),
                        dnsMessage = "${provider.label} Private DNS active — ads & trackers blocked OS-wide"
                    )
                } else {
                    it.copy(dnsMessage = "Failed: ${res.stderr.ifBlank { "Shizuku not ready" }}")
                }
            }
        }
    }

    fun disablePrivateDns() {
        viewModelScope.launch {
            val res = SystemTweaksRepository.disablePrivateDns()
            _uiState.update {
                if (res.exitCode == 0) {
                    it.copy(
                        dnsState = SystemTweaksRepository.DnsState(enabled = false, host = null),
                        dnsMessage = "Private DNS filter disabled"
                    )
                } else {
                    it.copy(dnsMessage = "Failed: ${res.stderr.ifBlank { "Shizuku not ready" }}")
                }
            }
        }
    }

    fun forcePeakRefreshRate() {
        viewModelScope.launch {
            val peak = _uiState.value.refreshState.devicePeakHz
            val res = SystemTweaksRepository.forcePeakRefreshRate(peak)
            _uiState.update {
                if (res.exitCode == 0) {
                    it.copy(
                        refreshState = it.refreshState.copy(currentMinHz = peak, currentPeakHz = peak),
                        refreshMessage = "Display locked at ${peak.toInt()}Hz everywhere"
                    )
                } else {
                    it.copy(refreshMessage = "Failed: ${res.stderr.ifBlank { "Shizuku not ready" }}")
                }
            }
        }
    }

    fun restoreAutoRefreshRate() {
        viewModelScope.launch {
            val res = SystemTweaksRepository.restoreAutoRefreshRate()
            _uiState.update {
                if (res.exitCode == 0) {
                    it.copy(
                        refreshState = it.refreshState.copy(currentMinHz = 0f, currentPeakHz = 0f),
                        refreshMessage = "OEM auto refresh management restored"
                    )
                } else {
                    it.copy(refreshMessage = "Failed: ${res.stderr.ifBlank { "Shizuku not ready" }}")
                }
            }
        }
    }

    fun forceRefreshRate(hz: Int) {
        viewModelScope.launch {
            val res = SystemTweaksRepository.forcePeakRefreshRate(hz.toFloat())
            _uiState.update {
                if (res.exitCode == 0) {
                    it.copy(
                        refreshState = it.refreshState.copy(currentMinHz = hz.toFloat(), currentPeakHz = hz.toFloat()),
                        refreshMessage = "Display locked at ${hz}Hz everywhere"
                    )
                } else {
                    it.copy(refreshMessage = "Failed: ${res.stderr.ifBlank { "Shizuku not ready" }}")
                }
            }
        }
    }

    fun cutWakeLock(context: Context, item: AppItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(batch = BatchProgress(isRunning = true, total = 1, index = 1, currentPackage = item.appName))
            }
            val (ok, log) = SystemTweaksRepository.cutWakeLock(context, item)
            _uiState.update {
                it.copy(
                    batch = it.batch.copy(
                        isRunning = false,
                        finished = true,
                        logs = listOf(
                            "Wake-lock cut ${item.appName}: ${if (ok) "DONE" else "FAILED"}",
                            log
                        ),
                    ),
                    wakeLockCut = SystemTweaksRepository.loadCutPackages(context)
                )
            }
        }
    }

    fun restoreWakeLock(context: Context, pkg: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(batch = BatchProgress(isRunning = true, total = 1, index = 1, currentPackage = pkg))
            }
            val (_, log) = SystemTweaksRepository.restoreWakeLock(context, pkg)
            _uiState.update {
                it.copy(
                    batch = it.batch.copy(isRunning = false, finished = true, logs = listOf("Wake-locks restored for $pkg", log)),
                    wakeLockCut = SystemTweaksRepository.loadCutPackages(context)
                )
            }
        }
    }

    fun cutAllWakeLocks(context: Context, targets: List<AppItem>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(batch = BatchProgress(isRunning = true, total = targets.size, logs = listOf("Cutting wake-locks on ${targets.size} apps…")))
            }
            targets.forEachIndexed { i, item ->
                _uiState.update { it.copy(batch = it.batch.copy(currentPackage = item.appName, index = i + 1)) }
                val (ok, log) = SystemTweaksRepository.cutWakeLock(context, item)
                _uiState.update {
                    it.copy(batch = it.batch.copy(logs = it.batch.logs + "[${i + 1}/${targets.size}] ${item.appName}: ${if (ok) "CUT" else "FAILED"}\n$log"))
                }
            }
            _uiState.update {
                it.copy(
                    batch = it.batch.copy(isRunning = false, finished = true, logs = it.batch.logs + "Done — these apps can no longer wake your CPU."),
                    wakeLockCut = SystemTweaksRepository.loadCutPackages(context)
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Tools Lab (Feature lab) — read-only data works without Shizuku where
    // the OS allows it; write actions require Shizuku and show a notice.
    // ------------------------------------------------------------------
    fun refreshTools() {
        viewModelScope.launch {
            val ready = ShizukuEngine.isReady
            if (ready) {
                _uiState.update {
                    it.copy(
                        battery = ToolsRepository.readBattery(),
                        thermal = ToolsRepository.readThermal(),
                        swap = ToolsRepository.readSwap(),
                        sensorsOff = ToolsRepository.readSensorsOff(),
                        overlays = ToolsRepository.listOverlays(),
                        resolution = ToolsRepository.readResolution(),
                        toolsNotice = null
                    )
                }
            } else {
                // Graceful read-only fallbacks that need no shell.
                val localBattery = localBatteryInfo()
                val localSwap = localSwapInfo()
                _uiState.update {
                    it.copy(
                        battery = localBattery,
                        thermal = ToolsRepository.ThermalInfo("UNKNOWN", ""),
                        swap = localSwap,
                        sensorsOff = false,
                        overlays = emptyList(),
                        resolution = ToolsRepository.ResolutionInfo("", ""),
                        toolsNotice = "Basic readouts shown locally — connect Shizuku (tutorial above) for deep hardware data and changes."
                    )
                }
            }
        }
    }

    private fun localBatteryInfo(): ToolsRepository.BatteryInfo = try {
        val bm = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val sticky = getApplication<Application>().registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val tempC = (sticky?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltageMv = sticky?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val currentMa = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000f
        val healthCode = sticky?.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, 0) ?: 0
        val health = when (healthCode) {
            android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            else -> "Unknown"
        }
        ToolsRepository.BatteryInfo(
            level = level,
            temperatureC = tempC,
            voltageMv = voltageMv,
            currentMa = currentMa,
            health = health
        )
    } catch (_: Exception) { ToolsRepository.BatteryInfo() }

    private fun localSwapInfo(): ToolsRepository.SwapInfo = try {
        val text = java.io.File("/proc/meminfo").readText()
        fun mb(name: String): Int =
            text.lineSequence().firstOrNull { it.startsWith(name) }
                ?.substringAfter(':')?.trim()?.split(' ')?.firstOrNull()?.toLongOrNull()
                ?.div(1024L)?.toInt() ?: 0
        ToolsRepository.SwapInfo(swapTotalMb = mb("SwapTotal"), swapFreeMb = mb("SwapFree"))
    } catch (_: Exception) { ToolsRepository.SwapInfo() }

    /** Guard for write actions attempted without Shizuku. */
    private fun gate(action: () -> Unit) {
        if (ShizukuEngine.isReady) action()
        else _uiState.update {
            it.copy(toolsNotice = "Shizuku is not connected — open the Tutorial at the top of Tools to set it up in ~1 minute.")
        }
    }

    fun runSpeedCompiler() = gate {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompiling = true, toolsLog = listOf("Compiling speed-profile for all apps…")) }
            val res = ToolsRepository.compileSpeedProfile()
            _uiState.update {
                it.copy(
                    isCompiling = false,
                    toolsLog = listOf(
                        if (res.exitCode == 0) "Speed-profile compiled — cold starts will feel snappier."
                        else "Compile failed: ${res.stderr.ifBlank { res.combined }}"
                    )
                )
            }
        }
    }

    fun toggleSensors(off: Boolean) = gate {
        viewModelScope.launch {
            val res = ToolsRepository.setSensorsOff(off)
            _uiState.update {
                if (res.exitCode == 0) it.copy(sensorsOff = off) else it
            }
        }
    }

    fun flushLogs() = gate {
        viewModelScope.launch {
            val res = ToolsRepository.flushLogs()
            _uiState.update {
                it.copy(
                    toolsLog = listOf(
                        if (res.exitCode == 0) "Logs cleared, buffer shrunk to 64K." else "Flush failed: ${res.stderr}"
                    )
                )
            }
        }
    }

    fun setRamExpansion(gb: Int) = gate {
        viewModelScope.launch {
            val res = ToolsRepository.setRamExpansionSizeGb(gb)
            _uiState.update {
                it.copy(
                    toolsLog = listOf(
                        if (res.exitCode == 0) "RAM Expansion set to ${gb}GB — reboot to apply."
                        else "RAM Expansion write failed: ${res.stderr.ifBlank { res.combined }}"
                    )
                )
            }
        }
    }

    fun toggleOverlay(overlay: ToolsRepository.OverlayInfo, enable: Boolean) = gate {
        viewModelScope.launch {
            val res = ToolsRepository.setOverlayEnabled(overlay.packageName, enable)
            _uiState.update {
                it.copy(
                    overlays = ToolsRepository.listOverlays(),
                    toolsLog = listOf(
                        (if (enable) "Enabled " else "Disabled ") + overlay.packageName +
                            if (res.exitCode != 0) " (error: ${res.stderr})" else ""
                    )
                )
            }
        }
    }

    fun setGame720p(enabled: Boolean) {
        getApplication<Application>().getSharedPreferences("veyro_modes", Context.MODE_PRIVATE)
            .edit().putBoolean("game_720p", enabled).apply()
        _uiState.update { it.copy(game720p = enabled) }
    }

    fun setGameTouchBoost(enabled: Boolean) {
        getApplication<Application>().getSharedPreferences("veyro_modes", Context.MODE_PRIVATE)
            .edit().putBoolean("game_touch", enabled).apply()
        viewModelScope.launch {
            ToolsRepository.setCompetitiveTouch(enabled)
            _uiState.update { it.copy(gameTouchBoost = enabled) }
        }
    }

    fun resetResolution() {
        viewModelScope.launch {
            ToolsRepository.resetResolution()
            _uiState.update { it.copy(resolution = ToolsRepository.readResolution()) }
        }
    }

    fun clearAppCaches(context: Context, targets: List<AppItem>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(batch = BatchProgress(isRunning = true, total = targets.size, logs = listOf("Clearing caches…")))
            }
            targets.forEachIndexed { i, item ->
                _uiState.update { it.copy(batch = it.batch.copy(currentPackage = item.appName, index = i + 1)) }
                val res = ToolsRepository.clearAppCache(item.packageName)
                _uiState.update {
                    it.copy(batch = it.batch.copy(logs = it.batch.logs + "[${i + 1}/${targets.size}] ${item.appName}: ${if (res.exitCode == 0) "CACHE CLEARED" else "FAILED"}"))
                }
            }
            _uiState.update {
                it.copy(batch = it.batch.copy(isRunning = false, finished = true), selectedPackages = emptySet())
            }
        }
    }

    // ------------------------------------------------------------------
    // Per-app control actions (detail sheet)
    // ------------------------------------------------------------------
    enum class AppAction { FORCE_STOP, FREEZE, UNFREEZE, DISABLE, ENABLE, BOOST_AND_LAUNCH, CUT_WAKELOCK, BACKUP_APK, BLOCK_BG_DATA }

    fun runAppAction(context: Context, item: AppItem, action: AppAction) {
        viewModelScope.launch {
            when (action) {
                AppAction.BOOST_AND_LAUNCH -> {
                    _uiState.update {
                        it.copy(batch = BatchProgress(isRunning = true, total = 1, index = 1, currentPackage = item.appName))
                    }
                    val log = PerformanceModeManager.apply(
                        context, PerfMode.GAMING, _uiState.value.ramHogs, launchPackage = item.packageName
                    )
                    _uiState.update {
                        it.copy(
                            batch = it.batch.copy(isRunning = false, finished = true, logs = log),
                            currentMode = PerfMode.GAMING,
                            animationScale = PerfMode.GAMING.animationScale
                        )
                    }
                    return@launch
                }
                AppAction.CUT_WAKELOCK -> {
                    cutWakeLock(context, item)
                    return@launch
                }
                AppAction.BACKUP_APK -> {
                    _uiState.update {
                        it.copy(batch = BatchProgress(isRunning = true, total = 1, index = 1, currentPackage = item.appName))
                    }
                    val res = ToolsRepository.backupApk(item)
                    _uiState.update {
                        it.copy(
                            batch = it.batch.copy(
                                isRunning = false,
                                finished = true,
                                logs = listOf(
                                    "Backup ${item.appName}: ${if (res.exitCode == 0) "SAVED" else "FAILED"}",
                                    res.combined
                                )
                            )
                        )
                    }
                    return@launch
                }
                AppAction.BLOCK_BG_DATA -> {
                    _uiState.update {
                        it.copy(batch = BatchProgress(isRunning = true, total = 1, index = 1, currentPackage = item.appName))
                    }
                    val res = ToolsRepository.blockBackgroundData(item.packageName, true)
                    _uiState.update {
                        it.copy(
                            batch = it.batch.copy(
                                isRunning = false,
                                finished = true,
                                logs = listOf(
                                    "BG-data blocked ${item.appName}: ${if (res.exitCode == 0) "DONE" else "FAILED"}",
                                    res.combined
                                )
                            )
                        )
                    }
                    return@launch
                }
                else -> Unit
            }
            _uiState.update {
                it.copy(batch = BatchProgress(isRunning = true, total = 1, index = 1, currentPackage = item.appName))
            }
            val res = when (action) {
                AppAction.FORCE_STOP -> ShizukuEngine.forceStop(item.packageName)
                AppAction.FREEZE -> ShizukuEngine.freeze(item.packageName)
                AppAction.UNFREEZE -> ShizukuEngine.unfreeze(item.packageName)
                AppAction.DISABLE -> ShizukuEngine.disable(item.packageName)
                AppAction.ENABLE -> ShizukuEngine.enable(item.packageName)
                else -> return@launch
            }
            val label = when (action) {
                AppAction.FORCE_STOP -> "Force stop"
                AppAction.FREEZE -> "Freeze"
                AppAction.UNFREEZE -> "Unfreeze"
                AppAction.DISABLE -> "Disable"
                AppAction.ENABLE -> "Enable"
                else -> "Action"
            }
            _uiState.update {
                it.copy(
                    batch = it.batch.copy(
                        isRunning = false,
                        finished = true,
                        logs = listOf(
                            "$label ${item.appName}: ${if (res.isSuccess) "DONE" else "FAILED"}",
                            res.combined
                        )
                    )
                )
            }
            refreshData()
        }
    }

    // ------------------------------------------------------------------
    // Review-before-run flow
    // ------------------------------------------------------------------
    fun requestBatchReview(targets: List<AppItem>) {
        if (targets.isNotEmpty()) {
            _uiState.update { it.copy(pendingBatchTargets = targets) }
        }
    }

    fun confirmPendingBatch(context: Context) {
        val targets = _uiState.value.pendingBatchTargets
        _uiState.update { it.copy(pendingBatchTargets = emptyList()) }
        runBatch(context, targets)
    }

    fun cancelPendingBatch() {
        _uiState.update { it.copy(pendingBatchTargets = emptyList()) }
    }

    // ------------------------------------------------------------------
    // GitHub Auto-Update system
    // ------------------------------------------------------------------
    fun checkForUpdates(showBannerOnly: Boolean) {
        viewModelScope.launch {
            val update = UpdateManager.checkForUpdate()
            if (update != null) {
                UpdateNotifier.notifyUpdateAvailable(getApplication(), update.versionName)
                _uiState.update {
                    it.copy(
                        updateInfo = update,
                        showUpdateBanner = true,
                        showUpdateDialog = if (showBannerOnly) (update.mandatory && it.showUpdateDialog) else true
                    )
                }
            } else {
                _uiState.update { it.copy(updateInfo = null, showUpdateBanner = false) }
            }
        }
    }

    fun showUpdateDialog() = _uiState.update { it.copy(showUpdateDialog = true, showUpdateBanner = false) }
    fun dismissUpdateBanner() = _uiState.update { it.copy(showUpdateBanner = false) }
    fun dismissUpdateDialog() = _uiState.update { it.copy(showUpdateDialog = false, downloadState = DownloadState.IDLE, downloadPercent = 0) }

    fun startUpdateDownload() {
        val update = _uiState.value.updateInfo ?: return
        val context = getApplication<Application>()
        downloadId = UpdateManager.startDownload(context, update)
        _uiState.update { it.copy(downloadState = DownloadState.DOWNLOADING, downloadPercent = 0) }
        pollDownloadProgress()
    }

    private fun pollDownloadProgress() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            while (_uiState.value.downloadState == DownloadState.DOWNLOADING) {
                val (bytes, total) = UpdateManager.queryProgress(context, downloadId)
                when {
                    bytes == 1L && total == 1L -> {
                        _uiState.update { it.copy(downloadState = DownloadState.DOWNLOADED, downloadPercent = 100) }
                    }
                    total > 0 && bytes >= 0 -> {
                        _uiState.update { it.copy(downloadPercent = (bytes * 100 / total).toInt().coerceIn(0, 100)) }
                    }
                    bytes < 0 -> {
                        val status = UpdateManager.downloadStatus(context, downloadId)
                        if (status == DownloadManager.STATUS_FAILED) {
                            _uiState.update { it.copy(downloadState = DownloadState.FAILED) }
                        }
                    }
                }
                delay(400)
            }
        }
    }

    fun openSystemInstaller() {
        val ok = UpdateManager.openInstaller(getApplication())
        if (!ok) {
            _uiState.update { it.copy(downloadState = DownloadState.FAILED) }
        }
    }

    fun silentInstallUpdate() {
        viewModelScope.launch {
            val res = UpdateManager.silentInstallViaShizuku(getApplication())
            if (res.isSuccess) {
                _uiState.update { it.copy(showUpdateDialog = false, downloadState = DownloadState.IDLE) }
            }
        }
    }
}
