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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.Xml
import android.widget.Toast
import ch.deletescape.lawnchair.adaptive.AdaptiveIconGenerator
import ch.deletescape.lawnchair.get
import ch.deletescape.lawnchair.toTitleCase
import ch.deletescape.lawnchair.util.extensions.d
import ch.deletescape.lawnchair.util.extensions.e
import com.android.launcher3.*
import com.android.launcher3.compat.LauncherAppsCompat
import com.android.launcher3.compat.UserManagerCompat
import com.android.launcher3.shortcuts.DeepShortcutManager
import com.android.launcher3.shortcuts.ShortcutInfoCompat
import com.android.launcher3.util.ComponentKey
import com.google.android.apps.nexuslauncher.CustomIconUtils
import com.google.android.apps.nexuslauncher.clock.CustomClock
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList

/**
 * IconPack 확장함수
 * Icon pack 과 관련된 함수들을 내포하고 있는 클라스
 */
class IconPackImpl(context: Context, packPackageName: String) : IconPack(context, packPackageName) {

    private val packComponents: MutableMap<ComponentName, Entry> = HashMap()
    // Calendar icon 정보를 담고 있는 Map
    private val packCalendars: MutableMap<ComponentName, String> = HashMap()
    private val packDirectClocks: MutableMap<ComponentName, String> = HashMap()
    // icon 정보들을 담고 있는 Map
    private val customPackComponents: MutableMap<ComponentName, String> = HashMap()
    private val packClocks: MutableMap<Int, CustomClock.Metadata> = HashMap()
    // Clock 관련 icon 정보를 담고 있는 Map
    private val customPackClocks: MutableMap<Int, CustomClock.Metadata> = HashMap()
    // 변하는 icon 들의 정보를 담고 있는 Map
    private val packDynamicDrawables: MutableMap<Int, DynamicDrawable.Metadata> = HashMap()
    // icon mask 관련 변수
    private var packMask: IconMask = IconMask()
    private val defaultPack = DefaultPack(context)
//    private val packResources = context.packageManager.getResourcesForApplication(packPackageName)
    // icon pack 정보를 가지고 있는 package 의 resource
    private val packResources = context.packageManager.getResourcesForApplication("com.android.themes")
    private val prefs by lazy { Utilities.getLawnchairPrefs(context) }
    override val entries get() = packComponents.values.toList()

    // 초기화
    init {
        if (prefs.showDebugInfo) {
            d("init pack $packPackageName on ${Looper.myLooper()!!.thread.name}", Throwable())
        }
        executeLoadPack()
    }

    override val packInfo = IconPackList.PackInfoImpl(context, packPackageName)

    /**
     * 날자가 변경되였을때 호출되는 callback
     * Calendar app icon 을 날자별로 변경시키기 위해 리용
     */
    override fun onDateChanged() {
        val apps = LauncherAppsCompat.getInstance(context)
        val model = LauncherAppState.getInstance(context).model
        val shortcutManager = DeepShortcutManager.getInstance(context)
        for (user in UserManagerCompat.getInstance(context).userProfiles) {
            packCalendars.keys.forEach {
                val pkg = it.packageName
                if (!apps.getActivityList(pkg, user).isEmpty()) {
                    CustomIconUtils.reloadIcon(shortcutManager, model, user, pkg)
                }
            }
        }
    }

    /**
     * Theme app 에서부터 해당 theme 에 관한 icon 들을 읽는 함수
     */
    override fun loadPack() {
        try {
            val startTime = System.currentTimeMillis()
            val compStart = "ComponentInfo{"
            val compStartlength = compStart.length
            val compEnd = "}"
            val compEndLength = compEnd.length

            val parseXml = getXml("appfilter", packPackageName) ?: throw IllegalStateException("parser is null")
            while (parseXml.next() != XmlPullParser.END_DOCUMENT) {
                if (parseXml.eventType == XmlPullParser.START_TAG) {
                    val name = parseXml.name
                    val isCalendar = name == "calendar"
                    val isClock = name == "clock"
                    when {
                        isClock || isCalendar || name == "item" -> {
                            var componentName: String? = parseXml[null, "component"]
                            val drawableName = parseXml[if (isCalendar) "prefix" else "drawable"]
                            if (componentName != null && drawableName != null) {
                                if (componentName.startsWith(compStart) && componentName.endsWith(compEnd)) {
                                    componentName = componentName.substring(compStartlength, componentName.length - compEndLength)
                                }
                                val parsed = ComponentName.unflattenFromString(componentName)
                                if (parsed != null) {
                                    if (isCalendar) {
                                        packCalendars[parsed] = drawableName
                                    } else if (isClock) {
                                        packDirectClocks[parsed] = drawableName
                                    } else {
                                        customPackComponents[parsed] = drawableName
                                    }
                                }
                            }
                        }
                        name == "dynamic-clock" -> {
                            val drawableName = parseXml["drawable"]
                            if (drawableName != null) {
                                val drawableId = customGetDrawableId(drawableName)
                                if (drawableId != 0) {
                                    customPackClocks[drawableId] = CustomClock.Metadata(
                                            if (parseXml["hourLayerIndex"] != null) parseXml.getAttributeValue(null, "hourLayerIndex").toInt() else -1,
                                            if (parseXml["minuteLayerIndex"] != null) parseXml.getAttributeValue(null, "minuteLayerIndex").toInt() else -1,
                                            if (parseXml["secondLayerIndex"] != null) parseXml.getAttributeValue(null, "secondLayerIndex").toInt() else -1,
                                            if (parseXml["defaultHour"] != null) parseXml.getAttributeValue(null, "defaultHour").toInt() else 0,
                                            if (parseXml["defaultMinute"] != null) parseXml.getAttributeValue(null, "defaultMinute").toInt() else 0,
                                            if (parseXml["defaultSecond"] != null) parseXml.getAttributeValue(null, "defaultSecond").toInt() else 0)
                                }
                            }
                        }
                        name == "scale" -> {
                            val scale = parseXml["factor"]!!.toFloat()
                            if (scale > 0x7f070000) {
                                packMask.iconScale = packResources.getDimension(scale.toInt())
                            } else {
                                packMask.iconScale = scale
                            }
                        }
                        name == "iconback" -> {
                            // TODO: handle packs with multiple masks
                            addImgsTo(parseXml, packMask.iconBackEntries)
                        }
                        name == "iconmask" -> {
                            addImgsTo(parseXml, packMask.iconMaskEntries)
                        }
                        name == "iconupon" -> {
                            addImgsTo(parseXml, packMask.iconUponEntries)
                        }
                        name == "config" -> {
                            val onlyMaskLegacy = parseXml["onlyMaskLegacy"]
                            if (!TextUtils.isEmpty(onlyMaskLegacy)) {
                                packMask.onlyMaskLegacy = onlyMaskLegacy!!.toBoolean()
                            }
                        }
                    }
                }
            }
            val endTime = System.currentTimeMillis()
            d("completed parsing pack $packPackageName in ${endTime - startTime}ms")
            return
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        Toast.makeText(context, "Failed to parse AppFilter", Toast.LENGTH_SHORT).show()
    }

    /**
     * 주어진 xml 에 Attribute 가 img 인것이 있으면 entry 에 추가하는 함수
     * @param parseXml 검색할 XmlPullParser
     * @param collection 얻은 img attribute 내용을 추가할 entry
     */
    private fun addImgsTo(parseXml: XmlPullParser, collection: MutableCollection<Entry>) {
        for (i in (0 until parseXml.attributeCount)) {
            if (parseXml.getAttributeName(i).startsWith("img")) {
                val drawableName = parseXml.getAttributeValue(i)
                if (!TextUtils.isEmpty(drawableName)) {
                    collection.add(Entry(drawableName))
                }
            }
        }
    }

    /**
     * pack 목록에서 해당 key 를 포함하는 entry 를 얻는 함수
     * @param key 얻으려는 entry 의 key 값
     * @return entry 값
     */
    override fun getEntryForComponent(key: ComponentKey): Entry? {
        val entry = packComponents[key.componentName]
        if (entry?.isAvailable != true) return null
        return entry
    }

    /**
     * 주어진 key 에 해당하는 icon mask entry 를 얻는 함수
     */
    override fun getMaskEntryForComponent(key: ComponentKey): IconPack.Entry? {
        if (!supportsMasking()) return null
        return MaskEntry(key)
    }

    /**
     * icon drawable 을 얻는 함수
     * @param entry 얻으려는 icon 정보를 포함하고 있는 entry
     * @param iconDpi icon drawable dpi
     * @return icon drawable
     */
    override fun getIcon(entry: IconPackManager.CustomIconEntry, iconDpi: Int): Drawable? {
        val drawableId = getDrawableId(entry.icon ?: return null)
        if (drawableId != 0) {
            try {
                var drawable = packResources.getDrawable(drawableId)
                if (Utilities.ATLEAST_OREO && packClocks.containsKey(drawableId)) {
                    drawable = CustomClock.getClock(context, drawable, packClocks[drawableId], iconDpi)
                } else if (packDynamicDrawables.containsKey(drawableId)) {
                    drawable = DynamicDrawable.getIcon(context, drawable, packDynamicDrawables[drawableId]!!, iconDpi)
                }
                if (prefs.adaptifyIconPacks) {
                    val gen = AdaptiveIconGenerator(context, drawable.mutate())
                    return gen.result
                }
                return drawable.mutate()
            } catch (ex: Resources.NotFoundException) {
                e("Can't get drawable for name ${entry.icon} ($drawableId)", ex)
            }
        }
        return null
    }

    /**
     * Theme app 의 asset 등록부에서 해당 icon 을 얻는 함수
     * @param launcherActivityInfo icon 에 해당하는 app 의 launcher activity 정보를 담고있는 객체
     * @param iconDpi icon drawable dpi
     * @param flattenDrawable icon 이 흐린 화상을 얻기여부
     * @param customIconEntry 얻으려는 icon 정보를 포함하고 있는 entry
     * @return icon drawable
     */
    override fun getIcon(launcherActivityInfo: LauncherActivityInfo, iconDpi: Int,
                         flattenDrawable: Boolean,
                         customIconEntry: IconPackManager.CustomIconEntry?,
                         iconProvider: LawnchairIconProvider?): Drawable? {
        ensureInitialLoadComplete()

        val component = launcherActivityInfo.componentName

        val assets = packResources.assets
        try {
            if (packCalendars.containsKey(component)) {
                val inputStream = assets.open("theme/$packPackageName/icons/"+packCalendars[component]+Calendar.getInstance().get(Calendar.DAY_OF_MONTH)+".png")
                return Drawable.createFromStream(inputStream, null).mutate()
            }
            if (packDirectClocks.containsKey(component)) {
                val drawableId = customGetDrawableId(packDirectClocks[component] ?: throw java.lang.IllegalStateException("packDirectClocks is null"))
                if (drawableId != 0) {
                    val drawable = AdaptiveIconCompat.wrap(packResources.getDrawableForDensity(drawableId, iconDpi)
                            ?: packResources.getDrawable(drawableId))
                    return CustomClock.getClock(context, drawable, customPackClocks[drawableId], iconDpi).mutate()
                }
            }
            if (customPackComponents.containsKey(component)) {
                val inputStream = assets.open("theme/$packPackageName/icons/"+customPackComponents[component]+".png")
                return Drawable.createFromStream(inputStream, null).mutate()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        if (prefs.iconPackMasking && packMask.hasMask) {
            val baseIcon = defaultPack.getIcon(launcherActivityInfo, iconDpi, flattenDrawable,
                    customIconEntry, iconProvider)
            Log.e("IconPackImpl getIcon ", "componentname is "+component+" baseicon height is "+baseIcon.intrinsicHeight+" width is "+baseIcon.intrinsicWidth)
            val icon = packMask.getIcon(context, baseIcon, launcherActivityInfo.componentName)
            if (prefs.adaptifyIconPacks) {
                val gen = AdaptiveIconGenerator(context, icon)
                return gen.result
            }
            return icon
        }

        return null

    }

    /**
     * Shortcut icon 을 얻는 함수
     * @param shortcutInfo shortcut 정보
     * @param iconDpi icon drawable dpi
     * @return 얻어진 shortcut icon drawable
     */
    override fun getIcon(shortcutInfo: ShortcutInfoCompat, iconDpi: Int): Drawable? {
        ensureInitialLoadComplete()

        if (prefs.iconPackMasking && packMask.hasMask) {
            val baseIcon = defaultPack.getIcon(shortcutInfo, iconDpi)
            if (baseIcon != null) {
                val icon = packMask.getIcon(context, baseIcon, shortcutInfo.activity)
                if (prefs.adaptifyIconPacks) {
                    val gen = AdaptiveIconGenerator(context, icon)
                    return gen.result
                }
                return icon
            }
        }

        return null
    }

    /**
     * icon 생성함수
     * @param icon 기초 icon bitmap
     * @param itemInfo 항목정보
     */
    override fun newIcon(icon: Bitmap, itemInfo: ItemInfo,
                         customIconEntry: IconPackManager.CustomIconEntry?,
                         drawableFactory: LawnchairDrawableFactory): FastBitmapDrawable? {
        ensureInitialLoadComplete()

        if (Utilities.ATLEAST_OREO && itemInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            val component = itemInfo.targetComponent
            if (packDirectClocks.containsKey(component)) {
                val id: Int = customGetDrawableId(packDirectClocks[component] ?: throw java.lang.IllegalStateException("packDirectClocks is null"))
                if (id != 0) {
                    val drawable = AdaptiveIconCompat.wrap(packResources.getDrawable(id))
                    return drawableFactory.customClockDrawer.drawIcon(icon, drawable, customPackClocks[id])
                }
            }
            if (customPackComponents.containsKey(component)) {
                return FastBitmapDrawable(icon)
            }
        }
        return null
    }

    /**
     * 모든 icon 을 얻는 함수
     */
    override fun getAllIcons(callback: (List<PackEntry>) -> Unit, cancel: () -> Boolean, filter: (item: String) -> Boolean) {
        var lastSend = System.currentTimeMillis() - 900
        var tmpList = ArrayList<PackEntry>()
        val sendResults = { force: Boolean ->
            val current = System.currentTimeMillis()
            if (force || current - lastSend >= 1000) {
                callback(tmpList)
                tmpList = ArrayList()
                lastSend = current
            }
        }
        var found = false
        val startTime = System.currentTimeMillis()
        var entry: Entry
        try {
            val parser = getXml("drawable")
            d("initialized parser for pack $packPackageName in ${System.currentTimeMillis() - startTime}ms")
            while (parser != null && parser.next() != XmlPullParser.END_DOCUMENT) {
                if (cancel()) return
                if (parser.eventType != XmlPullParser.START_TAG) continue
                if ("category" == parser.name) {
                    tmpList.add(CategoryTitle(parser["title"]!!))
                    sendResults(false)
                } else if ("item" == parser.name) {
                    val drawableName = parser["drawable"]!!
                    if (filter(drawableName)) {
                        val resId = getDrawableId(drawableName)
                        if (resId != 0) {
                            entry = Entry(drawableName, resId)
                            tmpList.add(entry)
                            sendResults(false)
                            found = true
                        }
                    }
                }
            }
            sendResults(true)
            if (found) {
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.getAllIcons(callback, cancel, filter)
    }

    override fun supportsMasking(): Boolean = packMask.hasMask

    /**
     * icon pack 정보를 담고 있는 xml 을 얻는 함수
     * @param name xml 이름
     * @param iconPack theme 이름
     */
    private fun getXml(name: String, iconPack: String): XmlPullParser? {
        val res: Resources
        try {
            res = context.packageManager.getResourcesForApplication("com.android.themes")
            val assets = res.assets
            val inputStream = assets.open("theme/$iconPack/$name.xml")

            val xmlPullParserFactory = XmlPullParserFactory.newInstance()
            xmlPullParserFactory.isNamespaceAware = true
            val xmlPullParser = xmlPullParserFactory.newPullParser()
            xmlPullParser.setInput(InputStreamReader(inputStream))
            return xmlPullParser

        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: IOException) {
        } catch (e: XmlPullParserException) {
        }
        return null
    }

    /**
     * 주어진 이름의 xml 을 얻는 함수
     * @param name 얻으려는 xml 이름
     */
    private fun getXml(name: String): XmlPullParser? {
        val res: Resources
        try {
            res = context.packageManager.getResourcesForApplication(packPackageName)
            val resourceId = res.getIdentifier(name, "xml", packPackageName)
            return if (0 != resourceId) {
                context.packageManager.getXml(packPackageName, resourceId, null)
            } else {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(res.assets.open("$name.xml"), Xml.Encoding.UTF_8.toString())
                parser
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: IOException) {
        } catch (e: XmlPullParserException) {
        }
        return null
    }

    /**
     * drawable 얻는 함수
     * @param name drawable 이름
     * @param density drawable density
     */
    fun getDrawable(name: String, density: Int): Drawable? {
        val id = getDrawableId(name)
        return try {
            if (id != 0) packResources.getDrawableForDensity(id, density) else null
        } catch (ex: Resources.NotFoundException) {
            e("Can't get drawable $id($name) from $packPackageName", ex)
            null
        }
    }

    private val idCache = mutableMapOf<String, Int>()

    /**
     * drawable 이름에 관한 id 를 얻는 함수
     */
    private fun getDrawableId(name: String) = packResources.getIdentifier(name, "drawable", packPackageName)// idCache.getOrPut(name) {    }

    /**
     * Theme app 에서 주어진 drawable 이름에 관한 id 를 얻는 함수
     */
    private fun customGetDrawableId(name: String) = packResources.getIdentifier(name, "drawable", "com.android.themes")// idCache.getOrPut(name) {    }

    /**
     * theme 등록부에서 theme app 의 asset 등록부에서 mask icon drawable 을 얻는 함수
     * @param name icon 이름
     */
    private fun getIconMask(name: String): Drawable? {
        val assets = packResources.assets
            var list = assets.list("theme/$packPackageName/icons")
            for (each in list) {
                if (("$name.png") == each) {
                    val inputStream = assets.open("theme/$packPackageName/icons/$name.png")
                    val b: Bitmap = BitmapFactory.decodeStream(inputStream)
                    b.density = Bitmap.DENSITY_NONE
                    return BitmapDrawable(b)
                }
            }
        return null
    }

    /**
     * icon entry 생성함수
     */
    fun createEntry(icon: Intent.ShortcutIconResource): Entry {
        val id = packResources.getIdentifier(icon.resourceName, null, null)
        val simpleName = packResources.getResourceEntryName(id)
        return Entry(simpleName, id)
    }

    /**
     * Icon mask 를 위한 entry class
     */
    inner class Entry(private val drawableName: String, val id: Int? = null) : IconPack.Entry() {

        override val displayName by lazy { drawableName.replace(Regex("""_+"""), " ").trim().toTitleCase() }
        override val identifierName = drawableName
        override val isAvailable by lazy { isHaveMaskType() }

        private val debugName get() = "$drawableName in $packPackageName"
        val drawableId: Int by lazy { id ?: getDrawableId(drawableName) }

        override fun drawableForDensity(density: Int): Drawable {
            if (!isAvailable) {
                throw IllegalStateException("Trying to access an unavailable entry $debugName")
            }
            try {
                return AdaptiveIconCompat.wrap(getIconMask(drawableName))
            } catch (e: Resources.NotFoundException) {
                throw Exception("Failed to get drawable $drawableId ($debugName)", e)
            }
        }

        override fun toCustomEntry() = IconPackManager.CustomIconEntry(packPackageName, drawableName)

        private fun isHaveMaskType(): Boolean {
            if (getIconMask(drawableName) == null)
                return false
            return true
        }
    }

    inner class MaskEntry(private val key: ComponentKey) : IconPack.Entry() {

        override val identifierName = key.toString()
        override val displayName = identifierName
        override val isAvailable = true

        override fun drawableForDensity(density: Int): Drawable {
            val baseIcon = defaultPack.getIcon(key, density)!!
            val icon = packMask.getIcon(context, baseIcon, key.componentName)
            if (prefs.adaptifyIconPacks) {
                val gen = AdaptiveIconGenerator(context, icon)
                return gen.result
            }
            return icon
        }

        override fun toCustomEntry() = IconPackManager.CustomIconEntry(packPackageName, key.toString(), "mask")
    }

}
