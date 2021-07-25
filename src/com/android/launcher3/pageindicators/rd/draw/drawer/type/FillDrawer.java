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
import com.android.launcher3.pageindicators.rd.animation.data.type.FillAnimationValue;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;

public class FillDrawer extends BaseDrawer {

    private Paint strokePaint;

    public FillDrawer(@NonNull Paint paint, @NonNull Indicator indicator) {
        super(paint, indicator);

        strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);
    }

    public void draw(
            @NonNull Canvas canvas,
            @NonNull Value value,
            int position,
            int coordinateX,
            int coordinateY) {

        if (!(value instanceof FillAnimationValue)) {
            return;
        }

        FillAnimationValue v = (FillAnimationValue) value;
        int color = indicator.getUnselectedColor();
        float radius = indicator.getRadius();
        int stroke = indicator.getStroke();

        int selectedPosition = indicator.getSelectedPosition();
        int selectingPosition = indicator.getSelectingPosition();
        int lastSelectedPosition = indicator.getLastSelectedPosition();

        if (indicator.isInteractiveAnimation()) {
            if (position == selectingPosition) {
                color = v.getColor();
                radius = v.getRadius();
                stroke = v.getStroke();

            } else if (position == selectedPosition) {
                color = v.getColorReverse();
                radius = v.getRadiusReverse();
                stroke = v.getStrokeReverse();
            }

        } else {
            if (position == selectedPosition) {
                color = v.getColor();
                radius = v.getRadius();
                stroke = v.getStroke();

            } else if (position == lastSelectedPosition) {
                color = v.getColorReverse();
                radius = v.getRadiusReverse();
                stroke = v.getStrokeReverse();
            }
        }

        strokePaint.setColor(color);
        strokePaint.setStrokeWidth(indicator.getStroke());
        canvas.drawCircle(coordinateX, coordinateY, indicator.getRadius(), strokePaint);

        strokePaint.setStrokeWidth(stroke);
        canvas.drawCircle(coordinateX, coordinateY, radius, strokePaint);
    }
}
