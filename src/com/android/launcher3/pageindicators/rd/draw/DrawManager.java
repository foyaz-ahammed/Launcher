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

package com.android.launcher3.pageindicators.rd.draw;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import com.android.launcher3.pageindicators.rd.animation.data.Value;
import com.android.launcher3.pageindicators.rd.draw.controller.AttributeController;
import com.android.launcher3.pageindicators.rd.draw.controller.DrawController;
import com.android.launcher3.pageindicators.rd.draw.controller.MeasureController;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;

public class DrawManager {

	private Indicator indicator;
	private DrawController drawController;
	private MeasureController measureController;
	private AttributeController attributeController;

	public DrawManager() {
		this.indicator = new Indicator();
		this.drawController = new DrawController(indicator);
		this.measureController = new MeasureController();
		this.attributeController = new AttributeController(indicator);
	}

	@NonNull
	public Indicator indicator() {
		if (indicator == null) {
			indicator = new Indicator();
		}

		return indicator;
	}

	public void setClickListener(@Nullable DrawController.ClickListener listener) {
		drawController.setClickListener(listener);
	}

	public void touch(@Nullable MotionEvent event) {
		drawController.touch(event);
	}

	public void updateValue(@Nullable Value value) {
		drawController.updateValue(value);
	}

	public void draw(@NonNull Canvas canvas) {
		drawController.draw(canvas);
	}

	public Pair<Integer, Integer> measureViewSize(int widthMeasureSpec, int heightMeasureSpec) {
		return measureController.measureViewSize(indicator, widthMeasureSpec, heightMeasureSpec);
	}

	public void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
		attributeController.init(context, attrs);
	}
}
