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

package com.android.launcher3.pageindicators.rd.animation.type;

import android.animation.IntEvaluator;
import android.animation.PropertyValuesHolder;
import android.support.annotation.NonNull;
import com.android.launcher3.pageindicators.rd.animation.controller.ValueController;

public class ScaleDownAnimation extends ScaleAnimation {

	public ScaleDownAnimation(@NonNull ValueController.UpdateListener listener) {
		super(listener);
	}

	@NonNull
	@Override
	protected PropertyValuesHolder createScalePropertyHolder(boolean isReverse) {
		String propertyName;
		int startRadiusValue;
		int endRadiusValue;

		if (isReverse) {
			propertyName = ANIMATION_SCALE_REVERSE;
			startRadiusValue = (int) (radius * scaleFactor);
			endRadiusValue = radius;
		} else {
			propertyName = ANIMATION_SCALE;
			startRadiusValue = radius;
			endRadiusValue = (int) (radius * scaleFactor);
		}

		PropertyValuesHolder holder = PropertyValuesHolder.ofInt(propertyName, startRadiusValue, endRadiusValue);
		holder.setEvaluator(new IntEvaluator());

		return holder;
	}
}

