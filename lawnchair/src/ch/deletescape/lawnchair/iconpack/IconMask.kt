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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import ch.deletescape.lawnchair.toBitmap
import com.android.launcher3.FastBitmapDrawable
import com.android.launcher3.LauncherAppState
import com.android.launcher3.Utilities
import com.android.launcher3.graphics.FixedScaleDrawable

/**
 * Icon Mask 관련 class
 */
class IconMask {
    // Mask 상태여부
    val hasMask by lazy { validBacks.isNotEmpty() || validMasks.isNotEmpty() || validUpons.isNotEmpty() }
    var onlyMaskLegacy: Boolean = false
    var iconScale = 1f // icon scale 정도
    val matrix = Matrix()
    val paint = Paint()

    val iconBackEntries = ArrayList<IconPackImpl.Entry>()
    val iconMaskEntries = ArrayList<IconPackImpl.Entry>()
    val iconUponEntries = ArrayList<IconPackImpl.Entry>()

    private val validBacks by lazy { iconBackEntries.filter { it.isAvailable } }
    private val validMasks by lazy { iconMaskEntries.filter { it.isAvailable } }
    private val validUpons by lazy { iconUponEntries.filter { it.isAvailable } }

    /**
     * Mask 를 씌운 icon 을 얻는 함수
     * @param context The application context
     * @param baseIcon 기초 icon
     * @param key 얻으려는 icon 에 해당하는 app 정보
     */
    fun getIcon(context: Context, baseIcon: Drawable, key: Any?): Drawable {
        synchronized(matrix) {
            val iconBack = getFromList(validBacks, key)
            val iconMask = getFromList(validMasks, key)
            val iconUpon = getFromList(validUpons, key)
            val scale = getScale(iconBack)

            var adaptiveBackground: Drawable? = null
            // 수용가능한 해상도를 얻기 위한 로직
            var size = (LauncherAppState.getIDP(context).iconBitmapSize * (3 - scale)).toInt()
            if (Utilities.ATLEAST_OREO && iconBack?.drawable is AdaptiveIconCompat) {
                size += (size * AdaptiveIconCompat.getExtraInsetFraction()).toInt()
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // app icon 그리기
            val iconBitmapSize = LauncherAppState.getIDP(context).iconBitmapSize
            var bb = baseIcon.toBitmap(fallbackSize = iconBitmapSize)!!
            if (!bb.isMutable) bb = bb.copy(bb.config, true)
            matrix.setScale((size * scale) / bb.width, (size * scale) / bb.height)
            matrix.postTranslate((size / 2) * (1 - scale), (size / 2) * (1 - scale))
            canvas.drawBitmap(bb, matrix, paint)
            matrix.reset()

            // app icon 에 Mask 적용
            if (iconMask != null) {
                iconMask.drawable.toBitmap(fallbackSize = iconBitmapSize)?.let {
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                    matrix.setScale(size.toFloat() / it.width, size.toFloat() / it.height)
                    Log.e("IconMask getIcon ", "componentname is "+key+" iconMask width is "+it.width+" iconMask scale value is "+size.toFloat() / it.width)
                    canvas.drawBitmap(it, matrix, paint)
                    matrix.reset()
                }
                paint.reset()
            }

            // iconBack 그리기
            if (iconBack != null) {
                val drawable = iconBack.drawable
                if (Utilities.ATLEAST_OREO && drawable is AdaptiveIconCompat) {
                    adaptiveBackground = drawable.background
                } else {
                    drawable.toBitmap()!!.let {
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                        matrix.setScale(size.toFloat() / it.width, size.toFloat() / it.height)
                        Log.e("IconMask getIcon ", "componentname is "+key+" iconBack width is "+it.width+" iconBack scale value is "+size.toFloat() / it.width)
                        canvas.drawBitmap(it, matrix, paint)
                        matrix.reset()
                    }
                    paint.reset()
                }
            }

            // iconUpon 그리기
            if (iconUpon != null) {
                iconUpon.drawable.toBitmap()!!.let {
                    matrix.setScale(size.toFloat() / it.width, size.toFloat() / it.height)
                    Log.e("IconMask getIcon ", "componentname is "+key+" iconUpon width is "+it.width+" iconUpon scale value is "+size.toFloat() / it.width)
                    canvas.drawBitmap(it, matrix, paint)
                    matrix.reset()
                }
            }
            if (adaptiveBackground != null) {
                if (onlyMaskLegacy && baseIcon is AdaptiveIconCompat) {
                    return baseIcon
                }
                return AdaptiveIconCompat(adaptiveBackground, FastBitmapDrawable(bitmap))
            }
            return FastBitmapDrawable(bitmap)
        }

    }

    /**
     * Mask 를 씌우기 위한 scale 정도를 얻는 함수
     */
    private fun getScale(iconBack: IconPackImpl.Entry?): Float {
        return if (Utilities.ATLEAST_OREO && iconBack?.drawable is AdaptiveIconCompat) {
            iconScale - (1f - FixedScaleDrawable.LEGACY_ICON_SCALE)
        } else {
            iconScale
        }
    }

    /**
     * 목록에서 주어진 key 해당하는 값을 얻는 함수
     */
    private fun <T> getFromList(list: List<T>, key: Any?): T? {
        if (list.isEmpty()) return null
        return list[Math.abs(key.hashCode()) % list.size]
    }
}
