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

package com.android.launcher3.groupselect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragLayer;

public class GroupIconView extends View {
    Paint mPaint;
    Bitmap mBitmap;

    int mTranslationX;
    int mTranslationY;

    public GroupIconView(Launcher launcher, Bitmap bitmap, int[] position, float scale) {
        super(launcher);
        mBitmap = bitmap;
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

        mTranslationX = position[0];
        mTranslationY = position[1];

        setScaleX(scale);
        setScaleY(scale);
        setElevation(getResources().getDimension(R.dimen.drag_elevation));
    }

    public void show(){
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(0, 0);
        lp.width = mBitmap.getWidth();
        lp.height = mBitmap.getHeight();
        lp.customPosition = true;
        setLayoutParams(lp);

        setTranslationX(mTranslationX);
        setTranslationY(mTranslationY);
        invalidate();
    }

    public void setScale(float scale){
        setScaleX(scale);
        setScaleY(scale);
        invalidate();
    }

    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }
}
