package com.veyro.debloatengine.data.repository

import android.content.Context
import android.view.Display
import com.veyro.debloatengine.data.model.AppItem
import com.veyro.debloatengine.data.model.CommandResult
import com.veyro.debloatengine.shizuku.ShizukuEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OS-wide system tweaks (Feature 4 / 5 / 7):
 *  - Private DNS ad-block: encrypted, low-latency DNS filtering at socket
 *    level via the native Private DNS slot — no VPN app, no battery drain.
 *  - Display Smoother: locks peak/min refresh rate so OEM power saving
 *    can't drop apps to 60Hz.
 *  - Wake-Lock Cutter: strips WAKE_LOCK / RUN_IN_BACKGROUND appops from
 *    heavy social & shopping apps so they can't wake the CPU.
 */
object SystemTweaksRepository {

    // ------------------------------------------------------------------
    // Feature 4 — OS-wide AdBlock over Private DNS
    // ------------------------------------------------------------------
    enum class DnsProvider(val label: String, val host: String, val blurb: String) {
        ADGUARD("AdGuard", "dns.adguard-dns.com", "Blocks ads, trackers & phishing in every app"),
        CLOUDFLARE("Cloudflare", "1dot1dot1dot1.cloudflare-dns.com", "Raw speed + malware filter, near-zero latency")
    }

    data class DnsState(
        val enabled: Boolean = false,
        val host: String? = null
    )

    suspend fun readDnsState(): DnsState = withContext(Dispatchers.IO) {
        val mode = ShizukuEngine.exec("settings get global private_dns_mode").combined.trim()
        val host = ShizukuEngine.exec("settings get global private_dns_specifier").combined.trim()
        DnsState(
            enabled = mode == "hostname" && host.isNotBlank() && host != "null",
            host = host.takeIf { it.isNotBlank() && it != "null" && it != "none" }
        )
    }

    suspend fun setPrivateDns(provider: DnsProvider): CommandResult =
        ShizukuEngine.exec(
            "settings put global private_dns_mode hostname && " +
                "settings put global private_dns_specifier ${provider.host}"
        )

    /** Disables Private DNS filtering entirely (back to auto/off). */
    suspend fun disablePrivateDns(): CommandResult =
        ShizukuEngine.exec("settings put global private_dns_mode off")

    // ------------------------------------------------------------------
    // Feature 5 — Display Smoother (force peak refresh rate)
    // ------------------------------------------------------------------
    data class RefreshState(
        val devicePeakHz: Float = 60f,
        val currentMinHz: Float = 0f,
        val currentPeakHz: Float = 0f,
        val supportedRates: List<Int> = emptyList()
    ) {
        val forced: Boolean get() = currentPeakHz > 0f && currentMinHz >= devicePeakHz - 1f
    }

    fun devicePeakRefreshRate(context: Context): Float {
        val fromShell = runCatching {
            kotlinx.coroutines.runBlocking {
                ShizukuEngine.exec("dumpsys display | grep -iE 'supportedModes|refreshRate' | head -8")
            }.combined
        }.getOrDefault("")
        val shellRates = Regex("""(\d+\.?\d*)\s*Hz""")
            .findAll(fromShell)
            .mapNotNull { it.groupValues[1].toFloatOrNull() }
            .toList()
        if (shellRates.isNotEmpty()) return shellRates.max()
        return try {
            val display = context.getSystemService(Display::class.java)
            display?.supportedModes?.maxOf { it.refreshRate } ?: 60f
        } catch (_: Exception) { 60f }
    }

    /** All distinct whole refresh rates the panel supports (60 / 90 / 120 …). */
    suspend fun supportedRefreshRates(context: Context): List<Int> = withContext(Dispatchers.IO) {
        val dump = ShizukuEngine.exec("dumpsys display | grep -iE 'supportedModes|fps|refreshRate' | head -12")
        val rates = Regex("""(\d+\.?\d*)\s*Hz""")
            .findAll(dump.combined)
            .mapNotNull { it.groupValues[1].toFloatOrNull()?.toInt() }
            .filter { it in 24..240 }
            .toSortedSet()
        if (rates.isEmpty()) {
            // Fallback: app-side Display API
            try {
                val display = context.getSystemService(Display::class.java)
                display?.supportedModes?.forEach {
                    rates.add(it.refreshRate.toInt())
                }
            } catch (_: Exception) { }
        }
        rates.toList().sorted()
    }

    suspend fun readRefreshState(context: Context): RefreshState = withContext(Dispatchers.IO) {
        val rates = supportedRefreshRates(context)
        val min = ShizukuEngine.exec("settings get system min_refresh_rate").combined.trim().toFloatOrNull() ?: 0f
        val peak = ShizukuEngine.exec("settings get system peak_refresh_rate").combined.trim().toFloatOrNull() ?: 0f
        RefreshState(
            devicePeakHz = rates.maxOrNull()?.toFloat() ?: devicePeakRefreshRate(context),
            currentMinHz = min,
            currentPeakHz = peak,
            supportedRates = rates
        )
    }

    suspend fun forcePeakRefreshRate(hz: Float): CommandResult {
        val v = String.format("%.1f", hz)
        return ShizukuEngine.exec(
            "settings put system peak_refresh_rate $v && settings put system min_refresh_rate $v"
        )
    }

    /** Deletes the overrides — OEM auto refresh management resumes. */
    suspend fun restoreAutoRefreshRate(): CommandResult =
        ShizukuEngine.exec(
            "settings delete system peak_refresh_rate && settings delete system min_refresh_rate"
        )

    // ------------------------------------------------------------------
    // Feature 7 — Wake-Lock & Background Execution Cutter (AppOps)
    // ------------------------------------------------------------------

    /** Heavy social / shopping apps that hold wake-locks in your pocket. */
    private val wakeLockTargets = listOf(
        "com.facebook.katana", "com.facebook.orca", "com.facebook.services",
        "com.facebook.appmanager", "com.facebook.system",
        "com.instagram.android", "com.snapchat.android",
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.twitter.android", "com.x.android",
        "com.linkedin.android", "com.pinterest",
        "com.amazon.mShop.android.shopping", "com.amazon.ftvd",
        "com.flipkart.android", "com.myntra.android", "com.snapdeal.main",
        "com.spotify.music", "com.whatsapp", "com.whatsapp.w4b"
    )

    /** Installed wake-lock candidates, prioritized social/shopping first. */
    fun wakeLockCandidates(all: List<AppItem>): List<AppItem> =
        all.filter { item ->
            !item.isProtected && item.isInstalled && (
                    item.packageName in wakeLockTargets ||
                        (item.packageName.contains("facebook", true) && !item.isProtected)
                    )
        }.sortedBy { wakeLockTargets.indexOf(it.packageName).let { i -> if (i < 0) 999 else i } }

    private const val PREFS = "veyro_wakelock_log"
    private const val KEY = "cut"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadCutPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY, emptySet()) ?: emptySet()

    private fun recordCut(context: Context, pkg: String, cut: Boolean) {
        val updated = loadCutPackages(context).toMutableSet()
        if (cut) updated.add(pkg) else updated.remove(pkg)
        prefs(context).edit().putStringSet(KEY, updated).apply()
    }

    /**
     * Strips wake-lock + background execution from an app.
     * The app works perfectly while you use it — it just cannot wake the
     * CPU on its own anymore. Fully reversible via [restoreWakeLock].
     */
    suspend fun cutWakeLock(context: Context, item: AppItem): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val pkg = item.packageName
            val log = StringBuilder()
            log.appendLine("> cmd appops set $pkg RUN_IN_BACKGROUND ignore")
            var res = ShizukuEngine.exec("cmd appops set $pkg RUN_IN_BACKGROUND ignore")
            log.appendLine(res.combined.ifBlank { "ok" })
            log.appendLine("> cmd appops set $pkg WAKE_LOCK ignore")
            res = ShizukuEngine.exec("cmd appops set $pkg WAKE_LOCK ignore")
            log.appendLine(res.combined.ifBlank { "ok" })
            val ok = res.exitCode == 0
            if (ok) recordCut(context, pkg, true)
            Pair(ok, log.toString())
        }

    /** Restores wake-lock + background execution (appops allow). */
    suspend fun restoreWakeLock(context: Context, pkg: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val log = StringBuilder()
            log.appendLine("> cmd appops set $pkg RUN_IN_BACKGROUND allow")
            ShizukuEngine.exec("cmd appops set $pkg RUN_IN_BACKGROUND allow")
            log.appendLine("> cmd appops set $pkg WAKE_LOCK allow")
            val res = ShizukuEngine.exec("cmd appops set $pkg WAKE_LOCK allow")
            log.appendLine(res.combined.ifBlank { "ok" })
            recordCut(context, pkg, false)
            Pair(res.exitCode == 0, log.toString())
        }

    // ------------------------------------------------------------------
    // Scoped Storage Unblocker — orphaned /sdcard/Android/{data,obb} junk
    // ------------------------------------------------------------------
    data class OrphanedFolder(
        val path: String,
        val packageName: String,
        val kind: String, // "data" or "obb"
        val sizeMb: Long = -1L
    )

    /**
     * Shell (UID 2000) bypasses Scoped Storage, so we can see the folders
     * regular file managers can't. Any folder whose name is not an installed
     * package is orphaned junk left behind by an uninstalled app.
     */
    suspend fun listOrphanedFolders(installedPackages: Set<String>): List<OrphanedFolder> =
        withContext(Dispatchers.IO) {
            val out = ShizukuEngine.exec(
                "ls /sdcard/Android/data 2>/dev/null; echo '__OBB__'; ls /sdcard/Android/obb 2>/dev/null"
            )
            val orphans = mutableListOf<OrphanedFolder>()
            var section = "data"
            out.combined.lines().forEach { raw ->
                val line = raw.trim()
                when {
                    line == "__OBB__" -> section = "obb"
                    line.isBlank() || line.startsWith("ls:") -> Unit
                    else -> {
                        if (!installedPackages.contains(line) &&
                            line != "com.veyro.debloatengine" &&
                            !line.contains("..")
                        ) {
                            orphans += OrphanedFolder(
                                path = "/sdcard/Android/$section/$line",
                                packageName = line,
                                kind = section
                            )
                        }
                    }
                }
            }
            orphans
        }

    /** Sizes can be slow — only computed for the orphan list, in one shell call. */
    suspend fun measureOrphans(orphans: List<OrphanedFolder>): List<OrphanedFolder> =
        withContext(Dispatchers.IO) {
            if (orphans.isEmpty()) return@withContext orphans
            val cmd = orphans.joinToString(" && ") { "du -sk \"${it.path}\" 2>/dev/null" }
            val out = ShizukuEngine.exec(cmd)
            val sizes = Regex("""(\d+)\s+(/sdcard/Android/(?:data|obb)/\S+)""")
                .findAll(out.combined)
                .associate { it.groupValues[2] to (it.groupValues[1].toLongOrNull() ?: 0L) / 1024L }
            orphans.map { it.copy(sizeMb = sizes[it.path] ?: 0L) }
                .sortedByDescending { it.sizeMb }
        }

    suspend fun deleteOrphanedFolder(path: String): CommandResult =
        ShizukuEngine.exec("rm -rf \"$path\"")

    // ------------------------------------------------------------------
    // Status bar icon declutterer (secure icon_blacklist)
    // ------------------------------------------------------------------
    val STATUS_BAR_ICONS: Map<String, String> = linkedMapOf(
        "nfc" to "NFC",
        "volte" to "VoLTE",
        "vowifi" to "VoWiFi",
        "alarm_clock" to "Alarm",
        "bluetooth" to "Bluetooth",
        "hotspot" to "Hotspot",
        "managed_profile" to "Work profile",
        "rotate" to "Auto-rotate",
        "ethernet" to "Ethernet"
    )

    suspend fun readIconBlacklist(): Set<String> =
        ShizukuEngine.exec("settings get secure icon_blacklist")
            .combined.trim()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    suspend fun applyIconBlacklist(hidden: Set<String>): CommandResult =
        if (hidden.isEmpty()) {
            ShizukuEngine.exec("settings delete secure icon_blacklist")
        } else {
            ShizukuEngine.exec("settings put secure icon_blacklist \"${hidden.joinToString(",")}\"")
        }
}
