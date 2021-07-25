/*
 *     Copyright (C) 2020 Lawnchair Team.
 *
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

package com.android.launcher3.allapps.alphabetsindexfastscrollrecycler;

import java.util.*

class Helpers {
    companion object {
        fun sectionsHelper(sections: MutableList<String>, test: ArrayList<String>): HashMap<Int, Int> {
            val mapOfSections = hashMapOf<Int, Int>()
            var lastFound = 0
            test.forEachIndexed { index, s ->
                if (sections.any { it == s }) {
                    val value = sections.indexOfFirst { it == s }
                    mapOfSections[index] = value
                    lastFound = value
                } else {
                    mapOfSections[index] = lastFound
                }
            }
            return mapOfSections
        }
    }
}