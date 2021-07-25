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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.pageindicators.rd.animation.type.AnimationType;
import com.android.launcher3.pageindicators.rd.animation.type.BaseAnimation;
import com.android.launcher3.pageindicators.rd.animation.type.ColorAnimation;
import com.android.launcher3.pageindicators.rd.animation.type.FillAnimation;
import com.android.launcher3.pageindicators.rd.animation.type.ScaleAnimation;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;
import com.android.launcher3.pageindicators.rd.draw.data.Orientation;
import com.android.launcher3.pageindicators.rd.draw.data.RtlMode;
import com.android.launcher3.pageindicators.rd.utils.DensityUtils;
import com.android.launcher3.R;

public class AttributeController {

    private Indicator indicator;

    public AttributeController(@NonNull Indicator indicator) {
        this.indicator = indicator;
    }

    public void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PageIndicatorView, 0, 0);
        initCountAttribute(typedArray);
        initColorAttribute(typedArray);
        initAnimationAttribute(typedArray);
        initSizeAttribute(typedArray);
        typedArray.recycle();
    }

    private void initCountAttribute(@NonNull TypedArray typedArray) {
        int viewPagerId = typedArray.getResourceId(R.styleable.PageIndicatorView_piv_viewPager, View.NO_ID);
        boolean autoVisibility = typedArray.getBoolean(R.styleable.PageIndicatorView_piv_autoVisibility, true);
        boolean dynamicCount = typedArray.getBoolean(R.styleable.PageIndicatorView_piv_dynamicCount, false);
        int count = typedArray.getInt(R.styleable.PageIndicatorView_piv_count, Indicator.COUNT_NONE);

        if (count == Indicator.COUNT_NONE) {
            count = Indicator.DEFAULT_COUNT;
        }

        int position = typedArray.getInt(R.styleable.PageIndicatorView_piv_select, 0);
        if (position < 0) {
            position = 0;
        } else if (count > 0 && position > count - 1) {
            position = count - 1;
        }

        indicator.setViewPagerId(viewPagerId);
        indicator.setAutoVisibility(autoVisibility);
        indicator.setDynamicCount(dynamicCount);
        indicator.setCount(count);

        indicator.setSelectedPosition(position);
        indicator.setSelectingPosition(position);
        indicator.setLastSelectedPosition(position);
    }


    private void initColorAttribute(@NonNull TypedArray typedArray) {
        int unselectedColor = typedArray.getColor(R.styleable.PageIndicatorView_piv_unselectedColor, Color.parseColor(
                ColorAnimation.DEFAULT_UNSELECTED_COLOR));
        int selectedColor = typedArray.getColor(R.styleable.PageIndicatorView_piv_selectedColor, Color.parseColor(
                ColorAnimation.DEFAULT_SELECTED_COLOR));

        indicator.setUnselectedColor(unselectedColor);
        indicator.setSelectedColor(selectedColor);
    }

    private void initAnimationAttribute(@NonNull TypedArray typedArray) {
        boolean interactiveAnimation = typedArray.getBoolean(R.styleable.PageIndicatorView_piv_interactiveAnimation, false);
        int animationDuration = typedArray.getInt(R.styleable.PageIndicatorView_piv_animationDuration, BaseAnimation.DEFAULT_ANIMATION_TIME);
        if (animationDuration < 0) {
            animationDuration = 0;
        }

        int animIndex = typedArray.getInt(R.styleable.PageIndicatorView_piv_animationType, AnimationType.NONE.ordinal());
        AnimationType animationType = getAnimationType(animIndex);

        int rtlIndex = typedArray.getInt(R.styleable.PageIndicatorView_piv_rtl_mode, RtlMode.Off.ordinal());
        RtlMode rtlMode = getRtlMode(rtlIndex);

        indicator.setAnimationDuration(animationDuration);
        indicator.setInteractiveAnimation(interactiveAnimation);
        indicator.setAnimationType(animationType);
        indicator.setRtlMode(rtlMode);
    }

    private void initSizeAttribute(@NonNull TypedArray typedArray) {
        int orientationIndex = typedArray.getInt(R.styleable.PageIndicatorView_piv_orientation, Orientation.HORIZONTAL.ordinal());
        Orientation orientation;

        if (orientationIndex == 0) {
            orientation = Orientation.HORIZONTAL;
        } else {
            orientation = Orientation.VERTICAL;
        }

        int radius = (int) typedArray.getDimension(R.styleable.PageIndicatorView_piv_radius, DensityUtils
                .dpToPx(Indicator.DEFAULT_RADIUS_DP));
        if (radius < 0) {
            radius = 0;
        }

        int padding = (int) typedArray.getDimension(R.styleable.PageIndicatorView_piv_padding, DensityUtils
                .dpToPx(Indicator.DEFAULT_PADDING_DP));
        if (padding < 0) {
            padding = 0;
        }

        float scaleFactor = typedArray.getFloat(R.styleable.PageIndicatorView_piv_scaleFactor, ScaleAnimation.DEFAULT_SCALE_FACTOR);
        if (scaleFactor < ScaleAnimation.MIN_SCALE_FACTOR) {
            scaleFactor = ScaleAnimation.MIN_SCALE_FACTOR;

        } else if (scaleFactor > ScaleAnimation.MAX_SCALE_FACTOR) {
            scaleFactor = ScaleAnimation.MAX_SCALE_FACTOR;
        }

        int stroke = (int) typedArray.getDimension(R.styleable.PageIndicatorView_piv_strokeWidth, DensityUtils
                .dpToPx(FillAnimation.DEFAULT_STROKE_DP));
        if (stroke > radius) {
            stroke = radius;
        }

        if (indicator.getAnimationType() != AnimationType.FILL) {
            stroke = 0;
        }

        indicator.setRadius(radius);
        indicator.setOrientation(orientation);
        indicator.setPadding(padding);
        indicator.setScaleFactor(scaleFactor);
        indicator.setStroke(stroke);
    }

    private AnimationType getAnimationType(int index) {
        switch (index) {
            case 0:
                return AnimationType.NONE;
            case 1:
                return AnimationType.COLOR;
            case 2:
                return AnimationType.SCALE;
            case 3:
                return AnimationType.WORM;
            case 4:
                return AnimationType.SLIDE;
            case 5:
                return AnimationType.FILL;
            case 6:
                return AnimationType.THIN_WORM;
            case 7:
                return AnimationType.DROP;
            case 8:
                return AnimationType.SWAP;
            case 9:
                return AnimationType.SCALE_DOWN;
        }

        return AnimationType.NONE;
    }

    private RtlMode getRtlMode(int index) {
        switch (index) {
            case 0:
                return RtlMode.On;
            case 1:
                return RtlMode.Off;
            case 2:
                return RtlMode.Auto;
        }

        return RtlMode.Auto;
    }
}
