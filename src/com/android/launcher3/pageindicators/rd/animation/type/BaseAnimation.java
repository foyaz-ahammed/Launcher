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

package com.android.launcher3.pageindicators.rd.animation.type;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.android.launcher3.pageindicators.rd.animation.controller.ValueController;

public abstract class BaseAnimation<T extends Animator> {

    public static final int DEFAULT_ANIMATION_TIME = 350;
    protected long animationDuration = DEFAULT_ANIMATION_TIME;

    protected ValueController.UpdateListener listener;
    protected T animator;

    public BaseAnimation(@Nullable ValueController.UpdateListener listener) {
        this.listener = listener;
        animator = createAnimator();
    }

    @NonNull
    public abstract T createAnimator();

    public abstract BaseAnimation progress(float progress);

    public BaseAnimation duration(long duration) {
        animationDuration = duration;

        if (animator instanceof ValueAnimator) {
            animator.setDuration(animationDuration);
        }

        return this;
    }

    public void start() {
        if (animator != null && !animator.isRunning()) {
            animator.start();
        }
    }

    public void end() {
        if (animator != null && animator.isStarted()) {
            animator.end();
        }
    }
}
