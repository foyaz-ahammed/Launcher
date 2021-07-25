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

package com.android.launcher3.transition;

import android.content.Context;
import androidx.annotation.IntDef;
import com.android.launcher3.Utilities;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 전환형태들을 정의한다.
 * Preference에 전환형식을 보관하고 Preference로부터 전환형식을 불러들인다.
 */
public final class TransitionUtility {
    public static final int TRANSITION_DEFAULT = 0;
    public static final int TRANSITION_PERSPECTIVE = 1;
    public static final int TRANSITION_SQUEEZE = 2;
    public static final int TRANSITION_CUBE = 3;
    public static final int TRANSITION_FLIP_OVER = 4;
    public static final int TRANSITION_ROTATE = 5;
    public static final int TRANSITION_CASCADE = 6;
    public static final int TRANSITION_WINDMILL = 7;

    public static final String PREF_TRANSITION = "pref_transition";

    @IntDef({TRANSITION_DEFAULT,
            TRANSITION_PERSPECTIVE,
            TRANSITION_SQUEEZE,
            TRANSITION_CUBE,
            TRANSITION_FLIP_OVER,
            TRANSITION_ROTATE,
            TRANSITION_CASCADE,
            TRANSITION_WINDMILL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransitionType {}

    /**
     * Preference로부터 전환형식을 얻어서 돌려준다..
     * @param context Context
     */
    public static int getTransitionPref(Context context) {
        return Utilities.getPrefs(context).getInt(PREF_TRANSITION, TRANSITION_DEFAULT);
    }

    /**
     * Preference에 전환형식을 설정한다.
     * @param context Context
     * @param transitionValue {@link TransitionType}
     */
    public static void setTransitionPref(Context context, @TransitionType int transitionValue) {
        Utilities.getPrefs(context).edit().putInt(PREF_TRANSITION, transitionValue).apply();
    }
}
