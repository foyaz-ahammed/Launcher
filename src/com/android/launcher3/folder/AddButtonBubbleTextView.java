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

package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import com.android.launcher3.BubbleTextView;

/**
 * 등록부의 `추가`단추에 해당한 View클라스
 *
 * @see AddButtonDrawable
 */
public class AddButtonBubbleTextView extends BubbleTextView {

    public AddButtonBubbleTextView(Context context) {
        this(context, null);
    }

    public AddButtonBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AddButtonBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        AddButtonDrawable drawable = new AddButtonDrawable(context, getIconSize(), getIconSize(), 15);
        drawable.setParentTextView(this);
        setIcon(drawable);
    }

    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
    }
}
