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

package com.android.launcher3.pageindicators.rd.draw.controller;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import com.android.launcher3.pageindicators.rd.animation.data.Value;
import com.android.launcher3.pageindicators.rd.animation.type.AnimationType;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;
import com.android.launcher3.pageindicators.rd.draw.drawer.Drawer;
import com.android.launcher3.pageindicators.rd.utils.CoordinatesUtils;

public class DrawController {

	private Value value;
	private Drawer drawer;
	private Indicator indicator;
	private ClickListener listener;

	public interface ClickListener {

		void onIndicatorClicked(int position);
	}

	public DrawController(@NonNull Indicator indicator) {
		this.indicator = indicator;
		this.drawer = new Drawer(indicator);
	}

	public void updateValue(@Nullable Value value) {
        this.value = value;
    }

	public void setClickListener(@Nullable ClickListener listener) {
		this.listener = listener;
	}

	public void touch(@Nullable MotionEvent event) {
		if (event == null) {
			return;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				onIndicatorTouched(event.getX(), event.getY());
				break;
			default:
		}
	}

	private void onIndicatorTouched(float x, float y) {
		if (listener != null) {
			int position = CoordinatesUtils.getPosition(indicator, x, y);
			if (position >= 0) {
				listener.onIndicatorClicked(position);
			}
		}
	}

	public void draw(@NonNull Canvas canvas) {
        int count = indicator.getCount();

        for (int position = 0; position < count; position++) {
            int coordinateX = CoordinatesUtils.getXCoordinate(indicator, position);
            int coordinateY = CoordinatesUtils.getYCoordinate(indicator, position);
            drawIndicator(canvas, position, coordinateX, coordinateY);
        }
    }

    private void drawIndicator(
            @NonNull Canvas canvas,
            int position,
            int coordinateX,
            int coordinateY) {

        boolean interactiveAnimation = indicator.isInteractiveAnimation();
        int selectedPosition = indicator.getSelectedPosition();
        int selectingPosition = indicator.getSelectingPosition();
        int lastSelectedPosition = indicator.getLastSelectedPosition();

        boolean selectedItem = !interactiveAnimation && (position == selectedPosition || position == lastSelectedPosition);
        boolean selectingItem = interactiveAnimation && (position == selectedPosition || position == selectingPosition);
        boolean isSelectedItem = selectedItem | selectingItem;
        drawer.setup(position, coordinateX, coordinateY);

        if (value != null && isSelectedItem) {
            drawWithAnimation(canvas);
        } else {
            drawer.drawBasic(canvas, isSelectedItem);
        }
    }

    private void drawWithAnimation(@NonNull Canvas canvas) {
        AnimationType animationType = indicator.getAnimationType();
        switch (animationType) {
            case NONE:
                drawer.drawBasic(canvas, true);
                break;

            case COLOR:
                drawer.drawColor(canvas, value);
                break;

            case SCALE:
                drawer.drawScale(canvas, value);
                break;

            case WORM:
                drawer.drawWorm(canvas, value);
                break;

            case SLIDE:
                drawer.drawSlide(canvas, value);
                break;

            case FILL:
                drawer.drawFill(canvas, value);
                break;

            case THIN_WORM:
                drawer.drawThinWorm(canvas, value);
                break;

            case DROP:
                drawer.drawDrop(canvas, value);
                break;

            case SWAP:
                drawer.drawSwap(canvas, value);
                break;

            case SCALE_DOWN:
                drawer.drawScaleDown(canvas, value);
                break;
        }
    }
}
