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

import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.View;
import com.android.launcher3.pageindicators.rd.animation.type.AnimationType;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;
import com.android.launcher3.pageindicators.rd.draw.data.Orientation;

public class MeasureController {

    public Pair<Integer, Integer> measureViewSize(@NonNull Indicator indicator, int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        int count = indicator.getCount();
        int radius = indicator.getRadius();
        int stroke = indicator.getStroke();

        int padding = indicator.getPadding();
        int paddingLeft = indicator.getPaddingLeft();
        int paddingTop = indicator.getPaddingTop();
        int paddingRight = indicator.getPaddingRight();
        int paddingBottom = indicator.getPaddingBottom();

        int circleDiameterPx = radius * 2;
        int desiredWidth = 0;
        int desiredHeight = 0;

        int width;
        int height;

        Orientation orientation = indicator.getOrientation();
        if (count != 0) {
            int diameterSum = circleDiameterPx * count;
            int strokeSum = (stroke * 2) * count;

            int paddingSum = padding * (count - 1);
            int w = diameterSum + strokeSum + paddingSum;
            int h = circleDiameterPx + stroke;

            if (orientation == Orientation.HORIZONTAL) {
                desiredWidth = w;
                desiredHeight = h;

            } else {
                desiredWidth = h;
                desiredHeight = w;
            }
        }

        if (indicator.getAnimationType() == AnimationType.DROP) {
            if (orientation == Orientation.HORIZONTAL) {
                desiredHeight *= 2;
            } else {
                desiredWidth *= 2;
            }
        }

        int horizontalPadding = paddingLeft + paddingRight;
        int verticalPadding = paddingTop + paddingBottom;

        if (orientation == Orientation.HORIZONTAL) {
            desiredWidth += horizontalPadding;
            desiredHeight += verticalPadding;

        } else {
            desiredWidth += horizontalPadding;
            desiredHeight += verticalPadding;
        }

        if (widthMode == View.MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == View.MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == View.MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == View.MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        if (width < 0) {
            width = 0;
        }

        if (height < 0) {
            height = 0;
        }

        indicator.setWidth(width);
        indicator.setHeight(height);

        return new Pair<>(width, height);
    }
}
