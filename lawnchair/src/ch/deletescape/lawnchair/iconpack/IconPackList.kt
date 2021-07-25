/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.deletescape.lawnchair.iconpack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import com.android.launcher3.LauncherModel
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.util.LooperExecutor
import com.google.android.apps.nexuslauncher.utils.ActionIntentFilter

/**
 * Icon pack 목록 관리를 위한 class
 */
class IconPackList(private val context: Context, private val manager: IconPackManager) {

    private val prefs = Utilities.getLawnchairPrefs(context)

    private val loadedPacks = HashMap<String, LoadedPack>() // 읽은 icon pack 목록
    val appliedPacks = ArrayList<IconPack>() // 현재 적용된 icon pack 목록

    private val default by lazy { DefaultLoadedPack() }

    // 초기화
    init {
        reloadPacks()
    }

    /**
     * 주어진 theme 목록에 관하여 icon pack 목록 갱신
     * @param packs theme 이름 목록
     * @param changedTheme Theme 이 갱신되였는지 판별
     */
    fun onPackListUpdated(packs: List<String>, changedTheme: Boolean = false) {
        LooperExecutor(LauncherModel.getIconPackLooper()).execute {
            loadedPacks.values.forEach {
                if (!packs.contains(it.packageName)) {
                    it.unregister()
                }
            }

            appliedPacks.clear()

            val newPacks = HashMap<String, LoadedPack>()
            packs.forEach { pack ->
                val loadedPack = loadedPacks.getOrPut(pack) {
                    loadPack(pack).apply {
                        iconPack.ensureInitialLoadComplete()
                        register()
                    }
                }
                newPacks[pack] = loadedPack
                appliedPacks.add(loadedPack.iconPack)
            }

            loadedPacks.clear()
            loadedPacks.putAll(newPacks)

            manager.onPacksUpdated(changedTheme)
        }
    }

    /**
     * 주어진 theme 관련 icon pack 읽기
     * @param packageName 읽을 theme 이름
     */
    private fun loadPack(packageName: String) = if (!TextUtils.isEmpty(packageName))
        LoadedPackImpl(packageName) else default

    fun getPack(packageName: String, keep: Boolean): IconPack {
        if (keep) {
            return loadedPacks.getOrPut(packageName) {
                loadPack(packageName).apply { register() }
            }.iconPack
        }
        loadedPacks[packageName]?.let { return it.iconPack }
        return IconPackImpl(context, packageName)
    }

    fun onDateChanged() {
        loadedPacks.values.forEach { it.iconPack.onDateChanged() }
    }

    /**
     * 해당 Theme 에 관한 icon pack 목록 다시 얻기
     */
    fun reloadPacks() {
        setPackList(prefs.iconPacks.getList())
    }

    /**
     * icon pack 목록 갱신
     * @param packs 갱신할 icon pack 과 련관된 theme 목록
     */
    private fun setPackList(packs: List<String>) {
//        onPackListUpdated(packs.filter { IconPackManager.isPackProvider(context, it) })
        onPackListUpdated(packs, false)
    }

    /**
     * 현재 적용된 icon pack 목록의 iterator 를 얻는 함수
     */
    fun iterator() = appliedPacks.iterator()

    /**
     * 현재 적용된 icon pack 얻기
     */
    fun currentPack() = if (!appliedPacks.isEmpty()) appliedPacks[0] else default.iconPack

    fun getAvailablePacks(): MutableSet<PackInfo> {
        val pm = context.packageManager
        val packs = HashSet<PackInfo>()
        IconPackManager.ICON_INTENTS.forEach { intent ->
            pm.queryIntentActivities(Intent(intent), 0)
                    .mapTo(packs) { PackInfo.forPackage(context, it.activityInfo.packageName) }
        }
        return packs
    }

    /**
     * icon pack 정보를 담고 있는 기초 class
     */
    abstract class PackInfo(val context: Context, val packageName: String) : Comparable<PackInfo> {

        abstract val displayName: String
        abstract val displayIcon: Drawable

        abstract fun load() : IconPack

        override fun equals(other: Any?): Boolean {
            return other is PackInfo && packageName == other.packageName
        }

        override fun hashCode(): Int {
            return packageName.hashCode()
        }

        override fun compareTo(other: PackInfo): Int {
            return displayName.compareTo(other.displayName)
        }

        companion object {

            fun forPackage(context: Context, packageName: String): PackInfo {
                if (TextUtils.isEmpty(packageName)) return DefaultPackInfo(context)
                return PackInfoImpl(context, packageName)
            }
        }
    }

    /**
     * 표준 icon pack 정보를 담고 있는 class
     */
    class DefaultPackInfo(context: Context) : PackInfo(context, "") {

        override val displayIcon by lazy { context.getDrawable(R.mipmap.ic_launcher)!! }
        override val displayName by lazy { context.resources.getString(R.string.icon_pack_default)!! }

        override fun load() = IconPackManager.getInstance(context).defaultPack
    }

    /**
     * icon pack 정보를 담고 있는 class
     */
    class PackInfoImpl(context: Context, packageName: String) : PackInfo(context, packageName) {

        private val applicationInfo by lazy {
//            context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            context.packageManager.getApplicationInfo("com.android.themes", PackageManager.GET_META_DATA)
        }

        override val displayIcon by lazy {
            context.packageManager.getApplicationIcon(applicationInfo)!!
        }

        override val displayName by lazy {
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        }

        override fun load() = IconPackImpl(context, packageName)
    }

    abstract inner class LoadedPack(protected var pack: IconPack) {

        val packageName get() = pack.packPackageName
        val iconPack get() = pack

        open fun register() {

        }

        open fun unregister() {

        }

        open fun reloadPack() {
            pack = IconPackImpl(context, packageName)
        }
    }

    inner class LoadedPackImpl(pack: IconPack) : LoadedPack(pack) {

        constructor(packageName: String) : this(IconPackImpl(context, packageName))

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_CHANGED) {
                    reloadPack()
                    iconPack.ensureInitialLoadComplete()
                    manager.onPacksUpdated()
                } else {
                    reloadPacks()
                }
            }
        }

        override fun register() {
            super.register()

            context.registerReceiver(updateReceiver, ActionIntentFilter.newInstance(packageName,
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_FULLY_REMOVED))
        }

        override fun unregister() {
            super.unregister()

            context.unregisterReceiver(updateReceiver)
        }

        override fun reloadPack() {
            super.reloadPack()

            pack = IconPackImpl(context, packageName)
        }
    }

    inner class DefaultLoadedPack : LoadedPack(manager.defaultPack) {

        override fun register() {

        }

        override fun unregister() {

        }

        override fun reloadPack() {

        }
    }
}
