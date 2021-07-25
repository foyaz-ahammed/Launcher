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

package com.android.launcher3.pageindicators.rd.draw.drawer.type;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import com.android.launcher3.pageindicators.rd.animation.data.Value;
import com.android.launcher3.pageindicators.rd.animation.data.type.SwapAnimationValue;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;
import com.android.launcher3.pageindicators.rd.draw.data.Orientation;

public class SwapDrawer extends BaseDrawer {

    public SwapDrawer(@NonNull Paint paint, @NonNull Indicator indicator) {
        super(paint, indicator);
    }

    public void draw(
            @NonNull Canvas canvas,
            @NonNull Value value,
            int position,
            int coordinateX,
            int coordinateY) {

        if (!(value instanceof SwapAnimationValue)) {
            return;
        }

        SwapAnimationValue v = (SwapAnimationValue) value;
        int selectedColor = indicator.getSelectedColor();
        int unselectedColor = indicator.getUnselectedColor();
        int radius = indicator.getRadius();

        int selectedPosition = indicator.getSelectedPosition();
        int selectingPosition = indicator.getSelectingPosition();
        int lastSelectedPosition = indicator.getLastSelectedPosition();

        int coordinate = v.getCoordinate();
        int color = unselectedColor;

        if (indicator.isInteractiveAnimation()) {
            if (position == selectingPosition) {
                coordinate = v.getCoordinate();
                color = selectedColor;

            } else if (position == selectedPosition) {
                coordinate = v.getCoordinateReverse();
                color = unselectedColor;
            }

        } else {
            if (position == lastSelectedPosition) {
                coordinate = v.getCoordinate();
                color = selectedColor;

            } else if (position == selectedPosition) {
                coordinate = v.getCoordinateReverse();
                color = unselectedColor;
            }
        }

        paint.setColor(color);
        if (indicator.getOrientation() == Orientation.HORIZONTAL) {
            canvas.drawCircle(coordinate, coordinateY, radius, paint);
        } else {
            canvas.drawCircle(coordinateX, coordinate, radius, paint);
        }
    }
}
