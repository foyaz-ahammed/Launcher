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
import com.android.launcher3.pageindicators.rd.animation.data.type.ThinWormAnimationValue;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;
import com.android.launcher3.pageindicators.rd.draw.data.Orientation;

public class ThinWormDrawer extends WormDrawer {

    public ThinWormDrawer(@NonNull Paint paint, @NonNull Indicator indicator) {
        super(paint, indicator);
    }

    public void draw(
            @NonNull Canvas canvas,
            @NonNull Value value,
            int coordinateX,
            int coordinateY) {

        if (!(value instanceof ThinWormAnimationValue)) {
            return;
        }

        ThinWormAnimationValue v = (ThinWormAnimationValue) value;
        int rectStart = v.getRectStart();
        int rectEnd = v.getRectEnd();
        int height = v.getHeight() / 2;

        int radius = indicator.getRadius();
        int unselectedColor = indicator.getUnselectedColor();
        int selectedColor = indicator.getSelectedColor();

        if (indicator.getOrientation() == Orientation.HORIZONTAL) {
            rect.left = rectStart;
            rect.right = rectEnd;
            rect.top = coordinateY - height;
            rect.bottom = coordinateY + height;

        } else {
            rect.left = coordinateX - height;
            rect.right = coordinateX + height;
            rect.top = rectStart;
            rect.bottom = rectEnd;
        }

        paint.setColor(unselectedColor);
        canvas.drawCircle(coordinateX, coordinateY, radius, paint);

        paint.setColor(selectedColor);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }
}
