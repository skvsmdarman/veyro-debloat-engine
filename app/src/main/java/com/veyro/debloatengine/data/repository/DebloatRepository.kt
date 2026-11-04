package com.veyro.debloatengine.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import com.veyro.debloatengine.data.model.AppItem
import com.veyro.debloatengine.data.model.CommandResult
import com.veyro.debloatengine.data.model.DebloatRule
import com.veyro.debloatengine.data.model.DebloatStrategy
import com.veyro.debloatengine.data.model.RamInfo
import com.veyro.debloatengine.data.model.StorageInfo
import com.veyro.debloatengine.data.model.Tier
import com.veyro.debloatengine.shizuku.ShizukuEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The "Lag Destroyer" core.
 *
 * Scans installed packages, classifies them into the engine categories
 * (CPU Sleep-Lock Killers, RAM Hogs, Storage bloat), enforces strict
 * safety locks, and executes reversible shell operations via Shizuku.
 */
class DebloatRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---------------------------------------------------------------------
    // CPU & Sleep-Lock Killers — persistent wake-lock / tracking daemons
    // ---------------------------------------------------------------------
    private val cpuKillers = listOf(
        // Xiaomi / Redmi / POCO
        "com.miui.analytics", "com.miui.daemon", "com.miui.msa.global",
        "com.miui.hybrid", "com.miui.msa", "com.miui.bugreport",
        "com.miui.systemAdSolution", "com.xiaomi.joyose", "com.xiaomi.powerkeeper",
        "com.miui.powerkeeper", "com.xiaomi.onetrack", "com.xiaomi.mi_connect_service",
        // OnePlus / Oppo (ColorOS)
        "com.oplus.obrain", "com.oplus.logkit", "com.oplus.statistics.rom",
        "com.oplus.battery", "com.oplus.onetrace", "com.nearme.statistics.rom",
        "com.nearme.atlas", "com.coloros.oppoguardelf", "com.oplus.exunit",
        // Vivo
        "com.vivo.daemonService", "com.vivo.stats", "com.iqoo.secure",
        // Huawei / Honor
        "com.huawei.systemmanager", "com.huawei.hwidcore",
        // Partner bloat daemons
        "com.facebook.services", "com.facebook.appmanager", "com.facebook.system",
        "com.facebook.katana.push", "com.amazon.ftvd"
    )

    // Fuzzy markers: any package whose id contains one of these is a CPU killer too.
    private val cpuKillerMarkers = listOf(
        "analytics", "onetrack", "logkit", "obrain", "bugreport",
        "statistics.rom", "ads", "msa", "msa.global"
    )

    // ---------------------------------------------------------------------
    // RAM Relief Mode — resident background pushers / duplicate media apps
    // ---------------------------------------------------------------------
    private val ramHogs = listOf(
        // Xiaomi ecosystem
        "com.miui.videoplayer", "com.miui.player", "com.miui.gallery.editor",
        "com.xiaomi.mipicks", "com.mi.globalminithunder", "com.miui.hybrid.accessory",
        "com.xiaomi.glgm", "com.miui.newhome", "com.miui.player.local",
        "com.xiaomi.vipaccount", "com.miui.bugreport", "com.miui.cloudservice",
        "com.miui.compass", "com.miui.fm", "com.milink.service",
        // Oppo / OnePlus
        "com.oneplus.membership", "com.oplus.games", "com.oplus.market",
        "com.heytap.browser", "com.heytap.market", "com.heytap.themestore",
        "com.nearme.statistics.rom", "com.finshell.fin",
        // Samsung
        "com.samsung.android.game.gamehome", "com.samsung.android.game.gametools",
        "com.samsung.android.bixby.agent", "com.samsung.android.bixby.wakeup",
        "com.samsung.android.themestore", "com.samsung.android.tvplus",
        "com.samsung.android.arzone", "com.samsung.android.spay",
        // Misc partner
        "com.amazon.mShop.android.shopping", "com.booking",
        "com.ss.android.ugc.trill", "com.netflix.mediaclient",
        "com.netflix.partner.activation"
    )

    private val ramHogTags = setOf("Ads", "Carrier/Partner", "Duplicate App", "Bloatware")

    // ---------------------------------------------------------------------
    // Safety Locks — never removable, regardless of what any rule says
    // ---------------------------------------------------------------------
    private val lockedPackages = setOf(
        // Core telephony / connectivity
        "com.android.phone", "com.android.server.telecom", "com.android.dialer",
        "com.google.android.dialer", "com.android.incallui", "com.android.stk",
        "com.android.mms", "com.google.android.apps.messaging",
        "com.android.providers.telephony", "com.android.providers.contacts",
        "com.android.providers.media", "com.android.providers.downloads",
        // Connectivity radios
        "com.android.wifi", "com.android.networkstack", "com.android.bluetooth",
        "com.android.settings", "com.android.systemui", "android",
        "com.android.keyguard", "com.android.nfc",
        // Input & launcher
        "com.google.android.inputmethod.latin", "com.android.inputmethod.latin",
        "com.android.inputmethod.pinyin",
        // Camera
        "com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera",
        "com.oneplus.camera", "com.miui.camera", "com.oplus.camera",
        "com.samsung.android.camera", "com.honor.camera",
        // Default launcher (any system launcher id containing "launcher" is locked in code)
        "com.miui.home", "com.google.android.apps.nexuslauncher",
        "com.oneplus.launcher", "com.oplus.launcher", "com.sec.android.app.launcher",
        // Google core
        "com.google.android.gms", "com.google.android.gsf", "com.google.android.gsf.login",
        "com.android.vending", "com.google.android.webview", "com.google.android.tts",
        // OEM security centers
        "com.miui.securitycenter", "com.oplus.battery", "com.coloros.safecenter",
        "com.oppo.market",
        // Brand / account infrastructure
        "com.xiaomi.account", "com.miui.cloudservice", "com.oplus.accounts",
        "com.samsung.android.mobileservice", "com.amazon.identity.auth.device",
        // This engine itself
        "com.veyro.debloatengine"
    )

    private var loadedRules: Map<String, DebloatRule> = emptyMap()

    // ------------------------------------------------------------------
    // Rule loading
    // ------------------------------------------------------------------
    private fun ensureRules(context: Context) {
        if (loadedRules.isNotEmpty()) return
        try {
            val text = context.assets.open("debloat_list.json").bufferedReader().use { it.readText() }
            loadedRules = jsonParser.decodeFromString<List<DebloatRule>>(text)
                .associateBy { it.packageName }
        } catch (_: Exception) {
            loadedRules = emptyMap()
        }
    }

    // ------------------------------------------------------------------
    // Device metrics
    // ------------------------------------------------------------------
    fun getStorageInfo(): StorageInfo = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        StorageInfo(
            totalBytes = stat.blockCountLong * stat.blockSizeLong,
            availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        )
    } catch (_: Exception) { StorageInfo() }

    fun getRamInfo(context: Context): RamInfo = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        RamInfo(totalBytes = mem.totalMem, availableBytes = mem.availMem)
    } catch (_: Exception) { RamInfo() }

    // ------------------------------------------------------------------
    // Package scanning & classification
    // ------------------------------------------------------------------
    suspend fun scanPackages(context: Context): List<AppItem> = withContext(Dispatchers.IO) {
        ensureRules(context)
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (_: Exception) { emptyList<ApplicationInfo>() }

        installed.map { info ->
            val pkg = info.packageName
            val appName = try {
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) { pkg }

            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val protected = isLocked(pkg, isSystem)
            val rule = loadedRules[pkg]

            val cpuReason = matchCpuKiller(pkg)
            val ramReason = matchRamHog(pkg, rule?.tags ?: emptyList())

            val tier = when {
                protected -> Tier.PROTECTED
                rule != null -> parseTier(rule.tier)
                cpuReason != null || ramReason != null -> Tier.SAFE
                isSystem -> Tier.PROBABLY_SAFE
                else -> Tier.SAFE
            }

            val reason = when {
                cpuReason != null -> cpuReason
                ramReason != null -> ramReason
                else -> ""
            }

            AppItem(
                packageName = pkg,
                appName = appName,
                isInstalled = info.enabled || isSystem,
                isEnabled = info.enabled,
                tier = tier,
                tags = rule?.tags ?: listOfNotNull(if (isSystem) "System App" else "User App"),
                description = rule?.description?.takeIf { it.isNotBlank() }
                    ?: when {
                        cpuReason != null -> "Background tracker / wake-lock daemon burning CPU cycles in your sleep."
                        ramReason != null -> "Resident background app holding RAM and pushing notifications."
                        isSystem -> "System component application."
                        else -> "User installed application."
                    },
                warning = rule?.warning?.takeIf { it.isNotBlank() }
                    ?: when {
                        protected -> "LOCKED: essential system component. Removal disabled."
                        cpuReason != null -> "Safe to remove. Stops background telemetry & wake-locks."
                        ramReason != null -> "Safe to remove. Frees resident RAM."
                        tier == Tier.PROBABLY_SAFE -> "Caution: system component. Removal may affect a feature."
                        else -> "Safe to debloat."
                    },
                isProtected = protected,
                isSystemApp = isSystem,
                reason = reason,
                isGame = isLikelyGame(pkg, info),
                iconDrawable = try { pm.getApplicationIcon(info) } catch (_: Exception) { null }
            )
        }.sortedWith(compareBy<AppItem> { it.tier.level }.thenBy { it.appName.lowercase() })
    }

    /** CPU Sleep-Lock Killers present on this device. */
    fun filterCpuKillers(all: List<AppItem>): List<AppItem> =
        all.filter { !it.isProtected && matchCpuKiller(it.packageName) != null }

    /** RAM Hogs present on this device. */
    fun filterRamHogs(all: List<AppItem>): List<AppItem> =
        all.filter { !it.isProtected && matchRamHog(it.packageName, it.tags) != null }

    private fun matchCpuKiller(pkg: String): String? {
        val exact = cpuKillers.firstOrNull { pkg == it }
        if (exact != null) return "Tracking / wake-lock daemon (CPU + battery drain)"
        val marker = cpuKillerMarkers.firstOrNull { pkg.contains(it, ignoreCase = true) }
        if (marker != null && pkg !in lockedPackages) {
            return "Telemetry marker \"$marker\" detected"
        }
        return null
    }

    private fun matchRamHog(pkg: String, tags: List<String>): String? {
        if (pkg in ramHogs) return "Resident RAM hog (background pusher)"
        if (tags.any { it in ramHogTags }) return "Bloat tagged: ${tags.first { it in ramHogTags }}"
        return null
    }

    // ------------------------------------------------------------------
    // Engine operations (all reversible)
    // ------------------------------------------------------------------

    /**
     * Storage Unchoker: safe system cache flush.
     * `pm trim-caches 1000G` frees I/O cache without touching personal data,
     * logins or photos. Restores flash write throughput on near-full drives.
     */
    suspend fun flushSystemCache(): CommandResult =
        ShizukuEngine.exec("pm trim-caches 1000G")

    /**
     * Strategy-aware, always reversible debloat of one package.
     *
     * FREEZE    → force-stop + suspend (app & data untouched, can never run)
     * DISABLE   → pm disable-user (installed + data kept, icon hidden)
     * UNINSTALL → pm uninstall -k --user 0, falling back to disable then suspend
     *
     * Every success is written to [RemovalLog] with its method so the
     * Restore engine (install-existing + enable + unsuspend) reverses all
     * three strategies with one click.
     */
    suspend fun debloatPackage(
        context: Context,
        item: AppItem,
        strategy: DebloatStrategy = DebloatStrategy.FREEZE
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (item.isProtected) return@withContext Pair(false, "LOCKED: ${item.packageName} is safety-locked and cannot be removed.")
        val pkg = item.packageName
        val log = StringBuilder()
        val method = strategy.name.lowercase()

        suspend fun record(ok: Boolean): Pair<Boolean, String> {
            if (ok) {
                RemovalLog.add(
                    context,
                    RemovalLog.Entry(pkg, item.appName, item.reason.ifBlank { "Debloated" }, System.currentTimeMillis(), method)
                )
            }
            return Pair(ok, log.toString())
        }

        when (strategy) {
            DebloatStrategy.FREEZE -> {
                log.appendLine("> am force-stop $pkg (stops it before freezing)")
                ShizukuEngine.exec("am force-stop $pkg")
                log.appendLine("> cmd package suspend $pkg")
                val res = ShizukuEngine.exec("cmd package suspend $pkg")
                log.appendLine(res.combined.ifBlank { "exit ${res.exitCode}" })
                val ok = res.isSuccess || res.combined.contains("suspended", ignoreCase = true) ||
                    res.combined.contains("already suspended", ignoreCase = true)
                return@withContext record(ok)
            }
            DebloatStrategy.DISABLE -> {
                log.appendLine("> am force-stop $pkg")
                ShizukuEngine.exec("am force-stop $pkg")
                log.appendLine("> pm disable-user --user 0 $pkg")
                val res = ShizukuEngine.exec("pm disable-user --user 0 $pkg")
                log.appendLine(res.combined.ifBlank { "exit ${res.exitCode}" })
                val ok = res.isSuccess || res.combined.contains("new state: disabled", ignoreCase = true)
                if (!ok) {
                    log.appendLine("Disable refused — falling back to suspend")
                    val r2 = ShizukuEngine.exec("cmd package suspend $pkg")
                    log.appendLine(r2.combined.ifBlank { "exit ${r2.exitCode}" })
                    return@withContext record(r2.isSuccess || r2.combined.contains("suspended", ignoreCase = true))
                }
                return@withContext record(true)
            }
            DebloatStrategy.UNINSTALL -> {
                // Backup vault: extract the clean APK to /sdcard/Veyro/Backups/ first
                // so the removal is always 100% restorable, even offline.
                val backup = ToolsRepository.backupApk(item)
                log.appendLine(if (backup.exitCode == 0) "APK backed up to /sdcard/Veyro/Backups/" else "Backup skipped: ${backup.stderr}")

                log.appendLine("> pm uninstall -k --user 0 $pkg (data kept, user-0 only)")
                var res = ShizukuEngine.exec("pm uninstall -k --user 0 $pkg")
                log.appendLine(res.combined.ifBlank { "exit ${res.exitCode}" })
                if (res.isSuccess && !res.combined.contains("Failure", ignoreCase = true)) {
                    return@withContext record(true)
                }
                log.appendLine("Uninstall refused — falling back to disable")
                res = ShizukuEngine.exec("pm disable-user --user 0 $pkg")
                log.appendLine(res.combined.ifBlank { "exit ${res.exitCode}" })
                if (res.isSuccess || res.combined.contains("new state: disabled", ignoreCase = true)) {
                    return@withContext record(true)
                }
                log.appendLine("Disable refused — falling back to suspend")
                res = ShizukuEngine.exec("cmd package suspend $pkg")
                log.appendLine(res.combined.ifBlank { "exit ${res.exitCode}" })
                return@withContext record(
                    res.isSuccess || res.combined.contains("suspended", ignoreCase = true)
                )
            }
        }
    }

    /**
     * Reversible restore: `cmd package install-existing` + `pm enable`
     * + `cmd package unsuspend`.
     */
    suspend fun restorePackage(context: Context, packageName: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val log = StringBuilder()
            log.appendLine("> cmd package install-existing $packageName")
            var res = ShizukuEngine.exec("cmd package install-existing $packageName")
            log.appendLine(res.combined)

            log.appendLine("> pm enable $packageName")
            res = ShizukuEngine.exec("pm enable $packageName")
            log.appendLine(res.combined)

            log.appendLine("> cmd package unsuspend $packageName")
            res = ShizukuEngine.exec("cmd package unsuspend $packageName")
            log.appendLine(res.combined)

            RemovalLog.remove(context, packageName)
            Pair(true, log.toString())
        }

    /** Restore everything the engine removed (1-click). */
    suspend fun restoreAll(context: Context): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            RemovalLog.load(context).map { entry ->
                val (_, log) = restorePackage(context, entry.packageName)
                Pair(entry.packageName, log)
            }
        }

    // ------------------------------------------------------------------
    // Safety lock evaluation
    // ------------------------------------------------------------------
    /**
     * Broader game detection: many OEM skins don't tag CATEGORY_GAME, so we
     * also check known publisher/title patterns. Users can add stragglers
     * manually in the Game Focus Engine.
     */
    private fun isLikelyGame(pkg: String, info: ApplicationInfo): Boolean {
        if (info.category == ApplicationInfo.CATEGORY_GAME) return true
        if (info.flags and ApplicationInfo.FLAG_IS_GAME != 0) return true
        val markers = listOf(
            "game", "pubg", "tencent.ig", "activision", "garena", "mihoyo",
            "hoyoverse", "supercell", "miniclip", "innersloth", "roblox",
            "minecraft", "mojang", "zeptolab", "king.com", "dreamgames",
            "playrix", "kungfu", "netease.game", "yxwm", "sigong"
        )
        return markers.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isLocked(pkg: String, isSystem: Boolean): Boolean {
        if (pkg in lockedPackages) return true
        if (pkg.startsWith("com.android.providers.")) return true
        if (isSystem && pkg.contains("launcher", ignoreCase = true)) return true
        if (isSystem && pkg.contains("keyguard", ignoreCase = true)) return true
        if (pkg.contains("fingerprint", ignoreCase = true) || pkg.contains("faceunlock", ignoreCase = true)) return true
        return false
    }

    private fun parseTier(tierStr: String): Tier = when (tierStr.uppercase()) {
        "SAFE" -> Tier.SAFE
        "PROBABLY_SAFE", "PROBABLY SAFE", "ADVANCED" -> Tier.PROBABLY_SAFE
        "RISKY", "EXPERT" -> Tier.RISKY
        "PROTECTED", "ESSENTIAL" -> Tier.PROTECTED
        else -> Tier.SAFE
    }
}
