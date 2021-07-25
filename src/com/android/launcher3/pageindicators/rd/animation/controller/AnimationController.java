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

package com.android.launcher3.pageindicators.rd.animation.controller;

import android.support.annotation.NonNull;
import com.android.launcher3.pageindicators.rd.animation.type.AnimationType;
import com.android.launcher3.pageindicators.rd.animation.type.BaseAnimation;
import com.android.launcher3.pageindicators.rd.draw.data.Indicator;
import com.android.launcher3.pageindicators.rd.draw.data.Orientation;
import com.android.launcher3.pageindicators.rd.utils.CoordinatesUtils;

public class AnimationController {

    private ValueController valueController;
    private ValueController.UpdateListener listener;

    private BaseAnimation runningAnimation;
    private Indicator indicator;

    private float progress;
    private boolean isInteractive;

    public AnimationController(@NonNull Indicator indicator, @NonNull ValueController.UpdateListener listener) {
        this.valueController = new ValueController(listener);
        this.listener = listener;
        this.indicator = indicator;
    }

    public void interactive(float progress) {
        this.isInteractive = true;
        this.progress = progress;
        animate();
    }

    public void basic() {
        this.isInteractive = false;
        this.progress = 0;
        animate();
    }

    public void end() {
        if (runningAnimation != null) {
            runningAnimation.end();
        }
    }

    private void animate() {
        AnimationType animationType = indicator.getAnimationType();
        switch (animationType) {
            case NONE:
                listener.onValueUpdated(null);
                break;

            case COLOR:
                colorAnimation();
                break;

            case SCALE:
                scaleAnimation();
                break;

            case WORM:
                wormAnimation();
                break;

            case FILL:
                fillAnimation();
                break;

            case SLIDE:
                slideAnimation();
                break;

            case THIN_WORM:
                thinWormAnimation();
                break;

            case DROP:
                dropAnimation();
                break;

            case SWAP:
                swapAnimation();
                break;

            case SCALE_DOWN:
                scaleDownAnimation();
                break;
        }
    }

    private void colorAnimation() {
        int selectedColor = indicator.getSelectedColor();
        int unselectedColor = indicator.getUnselectedColor();
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .color()
                .with(unselectedColor, selectedColor)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void scaleAnimation() {
        int selectedColor = indicator.getSelectedColor();
        int unselectedColor = indicator.getUnselectedColor();
        int radiusPx = indicator.getRadius();
        float scaleFactor = indicator.getScaleFactor();
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .scale()
                .with(unselectedColor, selectedColor, radiusPx, scaleFactor)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void wormAnimation() {
        int fromPosition = indicator.isInteractiveAnimation() ? indicator.getSelectedPosition() : indicator.getLastSelectedPosition();
        int toPosition = indicator.isInteractiveAnimation() ? indicator.getSelectingPosition() : indicator.getSelectedPosition();

        int from = CoordinatesUtils.getCoordinate(indicator, fromPosition);
        int to = CoordinatesUtils.getCoordinate(indicator, toPosition);
        boolean isRightSide = toPosition > fromPosition;

        int radiusPx = indicator.getRadius();
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .worm()
                .with(from, to, radiusPx, isRightSide)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void slideAnimation() {
        int fromPosition = indicator.isInteractiveAnimation() ? indicator.getSelectedPosition() : indicator.getLastSelectedPosition();
        int toPosition = indicator.isInteractiveAnimation() ? indicator.getSelectingPosition() : indicator.getSelectedPosition();

        int from = CoordinatesUtils.getCoordinate(indicator, fromPosition);
        int to = CoordinatesUtils.getCoordinate(indicator, toPosition);
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .slide()
                .with(from, to)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void fillAnimation() {
        int selectedColor = indicator.getSelectedColor();
        int unselectedColor = indicator.getUnselectedColor();
        int radiusPx = indicator.getRadius();
        int strokePx = indicator.getStroke();
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .fill()
                .with(unselectedColor, selectedColor, radiusPx, strokePx)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void thinWormAnimation() {
        int fromPosition = indicator.isInteractiveAnimation() ? indicator.getSelectedPosition() : indicator.getLastSelectedPosition();
        int toPosition = indicator.isInteractiveAnimation() ? indicator.getSelectingPosition() : indicator.getSelectedPosition();

        int from = CoordinatesUtils.getCoordinate(indicator, fromPosition);
        int to = CoordinatesUtils.getCoordinate(indicator, toPosition);
        boolean isRightSide = toPosition > fromPosition;

        int radiusPx = indicator.getRadius();
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .thinWorm()
                .with(from, to, radiusPx, isRightSide)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void dropAnimation() {
        int fromPosition = indicator.isInteractiveAnimation() ? indicator.getSelectedPosition() : indicator.getLastSelectedPosition();
        int toPosition = indicator.isInteractiveAnimation() ? indicator.getSelectingPosition() : indicator.getSelectedPosition();

        int widthFrom = CoordinatesUtils.getCoordinate(indicator, fromPosition);
        int widthTo = CoordinatesUtils.getCoordinate(indicator, toPosition);

        int paddingTop = indicator.getPaddingTop();
        int paddingLeft = indicator.getPaddingLeft();
        int padding = indicator.getOrientation() == Orientation.HORIZONTAL ? paddingTop : paddingLeft;

        int radius = indicator.getRadius();
        int heightFrom = radius * 3 + padding;
        int heightTo = radius + padding;

        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .drop()
                .duration(animationDuration)
                .with(widthFrom, widthTo, heightFrom, heightTo, radius);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void swapAnimation() {
        int fromPosition = indicator.isInteractiveAnimation() ? indicator.getSelectedPosition() : indicator.getLastSelectedPosition();
        int toPosition = indicator.isInteractiveAnimation() ? indicator.getSelectingPosition() : indicator.getSelectedPosition();

        int from = CoordinatesUtils.getCoordinate(indicator, fromPosition);
        int to = CoordinatesUtils.getCoordinate(indicator, toPosition);
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .swap()
                .with(from, to)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }

    private void scaleDownAnimation() {
        int selectedColor = indicator.getSelectedColor();
        int unselectedColor = indicator.getUnselectedColor();
        int radiusPx = indicator.getRadius();
        float scaleFactor = indicator.getScaleFactor();
        long animationDuration = indicator.getAnimationDuration();

        BaseAnimation animation = valueController
                .scaleDown()
                .with(unselectedColor, selectedColor, radiusPx, scaleFactor)
                .duration(animationDuration);

        if (isInteractive) {
            animation.progress(progress);
        } else {
            animation.start();
        }

        runningAnimation = animation;
    }
}

