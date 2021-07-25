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

package com.android.launcher3.allapps;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import com.android.launcher3.R;

public class AllAppsScrollView extends RelativeLayout {
    private AllAppsContainerView mParent;
    private AllAppsRecyclerView mRecyclerView;

    public AllAppsScrollView(Context context) {
        this(context, null);
    }

    public AllAppsScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public View RemoveAndAddNewView(){
        removeView(findViewById(R.id.apps_view));
        View newView = LayoutInflater.from(getContext()).inflate(R.layout.all_apps_rv_layout, this, false);
        addView(newView);
        return newView;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
       if(mParent != null){

           float scrollShift = mParent.getScrollShift();
           int currentScrollPosition = 0;

           if(mRecyclerView != null && mRecyclerView.getApps() != null)
               currentScrollPosition = mRecyclerView.getCurrentScrollY();

           if(scrollShift < 0) {
               child.setTop((int) scrollShift);
           }
           else {
               child.setTop(0);
               int saveCount = canvas.save();
               canvas.translate(0, Math.max((int) (scrollShift - currentScrollPosition), 0));
               boolean result = super.drawChild(canvas, child, drawingTime);
               canvas.restoreToCount(saveCount);
               return result;
           }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev){
        return super.onInterceptTouchEvent(ev);
    }

    //Set index scroller area
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        if(mParent == null){
            mParent = (AllAppsContainerView) getParent();

            //Get navigation bar height, and set bottom margin same as this value
            int navBarHeight = mParent.getNavBarHeight();
            RelativeLayout.LayoutParams lp = (LayoutParams) getLayoutParams();
            lp.bottomMargin = navBarHeight;
        }
        if(mRecyclerView == null) {
            mRecyclerView = findViewById(R.id.apps_list_view);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
