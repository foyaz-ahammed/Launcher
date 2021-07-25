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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import ch.deletescape.lawnchair.asNonEmpty
import ch.deletescape.lawnchair.lawnchairPrefs
import ch.deletescape.lawnchair.override.AppInfoProvider
import ch.deletescape.lawnchair.override.CustomInfoProvider
import ch.deletescape.lawnchair.runOnMainThread
import ch.deletescape.lawnchair.util.extensions.d
import com.android.launcher3.*
import com.android.launcher3.shortcuts.ShortcutInfoCompat
import com.android.launcher3.util.ComponentKey
import java.util.*
import kotlin.collections.HashMap

/**
 * Icon pack 관리를 위한 class
 */
class IconPackManager(private val context: Context) {

    val prefs = context.lawnchairPrefs
    private val appInfoProvider = AppInfoProvider.getInstance(context)
    val defaultPack = DefaultPack(context)
    val uriPack = UriIconPack(context)
    var dayOfMonth = 0 // 현재 날자, Calendar icon 을 위한 날자
        set(value) {
            if (value != field) {
                field = value
                onDateChanged()
            }
        }

    val packList = IconPackList(context, this)
    val defaultPackProvider = PackProvider(privateObj, "")

    private val listeners: MutableSet<() -> Unit> = mutableSetOf()

    // 초기화
    init {
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            }
        }, IntentFilter(Intent.ACTION_DATE_CHANGED).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            if (!Utilities.ATLEAST_NOUGAT) {
                addAction(Intent.ACTION_TIME_TICK)
            }
        }, null, Handler(LauncherModel.getWorkerLooper()))
    }

    /**
     * 날자가 변경되였을때 호출되는 함수
     */
    private fun onDateChanged() {
        packList.onDateChanged()
    }

    /**
     * Icon pack 을 얻는 함수
     * @param name icon pack 관련 package name
     * @param put 보관여부
     * @return 얻어진 icon pack
     */
    private fun getIconPackInternal(name: String, put: Boolean = true, load: Boolean = false): IconPack? {
        if (name == defaultPack.packPackageName) return defaultPack
        if (name == uriPack.packPackageName) return uriPack
        return if (isPackProvider(context, name)) {
            packList.getPack(name, put).apply {
                if (load) {
                    ensureInitialLoadComplete()
                }
            }
        } else null
    }

    /**
     * Icon pack 을 얻는 함수
     * @param name icon pack 관련 package name
     * @param put 보관여부
     * @return 얻어진 icon pack
     */
    fun getIconPack(name: String, put: Boolean = true, load: Boolean = false): IconPack? {
        return getIconPackInternal(name, put, load)
    }

    /**
     * Icon pack 을 얻는 함수
     * @param packProvider icon pack 관련 package name 을 포함하는 PackProvider 객체
     * @param put 보관여부
     * @return 얻어진 icon pack
     */
    fun getIconPack(packProvider: PackProvider, put: Boolean = true, load: Boolean = false): IconPack {
        return getIconPackInternal(packProvider.name, put, load)!!
    }

    /**
     * icon 얻는 함수
     * @param launcherActivityInfo 해당 icon 관련 app 정보
     * @param iconDpi icon drawable dpi
     * @param flattenDrawable icon 이 흐린 화상을 얻기여부
     * @param itemInfo 항목정보
     * @return icon drawable
     */
    fun getIcon(launcherActivityInfo: LauncherActivityInfo,
                iconDpi: Int, flattenDrawable: Boolean, itemInfo: ItemInfo?,
                iconProvider: LawnchairIconProvider?): Drawable {
        val customEntry = CustomInfoProvider.forItem<ItemInfo>(context, itemInfo)?.getIcon(itemInfo!!)
                ?: appInfoProvider.getCustomIconEntry(launcherActivityInfo)
        val customPack = customEntry?.run {
            getIconPackInternal(packPackageName)
        }
        if (customPack != null) {
            customPack.getIcon(launcherActivityInfo, iconDpi,
                    flattenDrawable, customEntry, iconProvider)?.let { icon -> return icon }
        }
        packList.iterator().forEach { pack ->
            pack.getIcon(launcherActivityInfo, iconDpi,
                    flattenDrawable, null, iconProvider)?.let { return it }
        }
        return defaultPack.getIcon(launcherActivityInfo, iconDpi, flattenDrawable, null, iconProvider)
    }

    /**
     * Shortcut icon 을 얻는 함수
     * @param shortcutInfo shortcut 정보
     * @param iconDpi icon drawable dpi
     * @return 얻어진 short icon drawable
     */
    fun getIcon(shortcutInfo: ShortcutInfoCompat, iconDpi: Int): Drawable? {
        packList.iterator().forEach { pack ->
            pack.getIcon(shortcutInfo, iconDpi)?.let { return it }
        }
        return defaultPack.getIcon(shortcutInfo, iconDpi)
    }

    /**
     * icon 생성함수
     * @param icon 기초 icon bitmap
     * @param itemInfo 항목정보
     */
    fun newIcon(icon: Bitmap, itemInfo: ItemInfo, drawableFactory: LawnchairDrawableFactory): FastBitmapDrawable {
        val key = itemInfo.targetComponent?.let { ComponentKey(it, itemInfo.user) }
        val customEntry = CustomInfoProvider.forItem<ItemInfo>(context, itemInfo)?.getIcon(itemInfo)
                ?: key?.let { appInfoProvider.getCustomIconEntry(it) }
        val customPack = customEntry?.run { getIconPackInternal(packPackageName) }
        if (customPack != null) {
            customPack.newIcon(icon, itemInfo, customEntry, drawableFactory)?.let { return it }
        }
        packList.iterator().forEach { pack ->
            pack.newIcon(icon, itemInfo, customEntry, drawableFactory)?.let { return it }
        }
        return defaultPack.newIcon(icon, itemInfo, customEntry, drawableFactory)
    }

    fun maskSupported(): Boolean = packList.appliedPacks.any { it.supportsMasking() }

    /**
     *  pack 목록에서 주어진 key 에 관한 iconPack entry 를 얻는 함수
     */
    fun getEntryForComponent(component: ComponentKey): IconPack.Entry? {
        packList.iterator().forEach {
            val entry = it.getEntryForComponent(component)
            if (entry != null) return entry
        }
        return defaultPack.getEntryForComponent(component)
    }

    fun getPackProviders(): Set<PackProvider> {
        val pm = context.packageManager
        val packs = HashSet<PackProvider>()
        ICON_INTENTS.forEach { intent -> pm.queryIntentActivities(Intent(intent), PackageManager.GET_META_DATA).forEach {
            packs.add(PackProvider(privateObj, it.activityInfo.packageName))
        } }
        return packs
    }

    /**
     * 목록에서 해당 listener 추가함수
     * @param listener 추가하려는 listener
     */
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
        if (!packList.appliedPacks.isEmpty()) {
            listener.invoke()
        }
    }

    /**
     * 목록에서 해당 listener 제거함수
     * @param listener 제거하려는 listener
     */
    fun removeListener(listener: () -> Unit) {
        listeners.remove (listener)
    }

    /**
     * icon pack 이 갱신되였을때 icon 재읽기
     * @param changedTheme theme 변경여부
     */
    fun onPacksUpdated(changedTheme: Boolean = false) {
        context.lawnchairPrefs.reloadIcons(changedTheme)
        runOnMainThread { listeners.forEach { it.invoke() } }
    }

    /**
     * icon 정보를 담고 있는 data class
     */
    data class CustomIconEntry(val packPackageName: String, val icon: String? = null, val arg: String? = null) {

        fun toPackString(): String {
            return "$packPackageName"
        }

        override fun toString(): String {
            return "$packPackageName|${icon ?: ""}|${arg ?: ""}"
        }

        companion object {
            fun fromString(string: String): CustomIconEntry {
                return fromNullableString(string)!!
            }

            fun fromNullableString(string: String?): CustomIconEntry? {
                if (string == null) return null
                if (string.contains("|")) {
                    val parts = string.split("|")
                    if (parts[0].contains("/")) return parseLegacy(string)
                    if (parts.size == 1) {
                        return CustomIconEntry(parts[0])
                    }
                    return CustomIconEntry(parts[0], parts[1].asNonEmpty(), parts[2].asNonEmpty())
                }
                return parseLegacy(string)
            }

            private fun parseLegacy(string: String): CustomIconEntry? {
                val parts = string.split("/")
                val icon = TextUtils.join("/", parts.subList(1, parts.size))
                if (parts[0] == "lawnchairUriPack" && !icon.isNullOrBlank()) {
                    val iconParts = icon.split("|")
                    return CustomIconEntry(parts[0], iconParts[0].asNonEmpty(), iconParts[1].asNonEmpty())
                }
                return CustomIconEntry(parts[0], icon.asNonEmpty())
            }
        }
    }

    data class IconPackInfo(val packageName: String, val icon: Drawable, val label: CharSequence) {
        companion object {
            fun fromResolveInfo(info: ResolveInfo, pm: PackageManager) = IconPackInfo(info.activityInfo.packageName, info.loadIcon(pm), info.loadLabel(pm))
        }
    }

    class PackProvider(obj: Any, val name: String) : Parcelable {

        constructor(parcel: Parcel) : this(privateObj, parcel.readString()!!)

        init {
            if (obj !== privateObj) {
                UnsupportedOperationException("Cannot create PackProvider outside of IconPackManager")
            }
        }

        override fun equals(other: Any?): Boolean {
            return (other as? PackProvider)?.name == name
        }

        override fun hashCode() = name.hashCode()

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PackProvider> {
            override fun createFromParcel(parcel: Parcel): PackProvider {
                return PackProvider(parcel)
            }

            override fun newArray(size: Int): Array<PackProvider?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {

        const val TAG = "IconPackManager"
        private val privateObj = Object()

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: IconPackManager? = null

        fun getInstance(context: Context): IconPackManager {
            if (INSTANCE == null) {
                INSTANCE = IconPackManager(context.applicationContext)
            }
            return INSTANCE!!
        }

        val ICON_INTENTS = arrayOf(
                "com.fede.launcher.THEME_ICONPACK",
                "com.anddoes.launcher.THEME",
                "com.teslacoilsw.launcher.THEME",
                "com.gau.go.launcherex.theme",
                "org.adw.launcher.THEMES",
                "net.oneplus.launcher.icons.ACTION_PICK_ICON",
                "ch.deletescape.lawnchair.ICONPACK")

        internal fun isPackProvider(context: Context, packageName: String?): Boolean {
            if (packageName != null && !packageName.isEmpty()) {
                return ICON_INTENTS.firstOrNull { context.packageManager.queryIntentActivities(
                        Intent(it).setPackage(packageName), PackageManager.GET_META_DATA).iterator().hasNext() } != null
            }
            return false
        }
    }
}
