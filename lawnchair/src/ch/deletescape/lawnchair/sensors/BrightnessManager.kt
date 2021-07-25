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

package ch.deletescape.lawnchair.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import ch.deletescape.lawnchair.ensureOnMainThread
import ch.deletescape.lawnchair.useApplicationContext
import ch.deletescape.lawnchair.util.SingletonHolder

/**
 * 밝기변화를 감지하는 감지하여 알려주는 클라스
 * @see SensorEventListener
 * @see Sensor
 */
class BrightnessManager private constructor(context: Context): SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    //Listener 들
    private val listeners = mutableSetOf<OnBrightnessChangeListener>()
    private val shouldListen get() = listeners.isNotEmpty()
    private var isListening = false

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    /**
     * Sensor 변화가 일어났을때 호출된다.
     * @param event SensorEvent
     */
    override fun onSensorChanged(event: SensorEvent) {
        //밝기를 얻어서 listener들에 알려준다.
        val illuminance = event.values[0]
        for (listener in listeners) {
            listener.onBrightnessChanged(illuminance)
        }
    }

    /**
     * Sensor Listener 를 등록한다.
     */
    fun startListening() {
        if (shouldListen && !isListening) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    /**
     * Sensor Listener 를 등록해제한다.
     */
    fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
    }

    /**
     * listener추가
     * @param listener OnBrightnessChangeListener
     */
    fun addListener(listener: OnBrightnessChangeListener) {
        listeners.add(listener)
        startListening()
    }

    /**
     * listener삭제
     * @param listener OnBrightnessChangeListener
     */
    fun removeListener(listener: OnBrightnessChangeListener) {
        listeners.remove(listener)
        if (!shouldListen) {
            stopListening()
        }
    }

    //BrightnessManager instance
    companion object : SingletonHolder<BrightnessManager, Context>(ensureOnMainThread(useApplicationContext(::BrightnessManager)))

    interface OnBrightnessChangeListener {
        fun onBrightnessChanged(illuminance: Float)
    }
}