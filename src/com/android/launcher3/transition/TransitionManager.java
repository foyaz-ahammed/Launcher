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

import static com.android.launcher3.transition.TransitionUtility.TRANSITION_CASCADE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_CUBE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_DEFAULT;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_FLIP_OVER;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_PERSPECTIVE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_ROTATE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_SQUEEZE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_WINDMILL;

import android.content.Context;
import androidx.annotation.AnimatorRes;
import com.android.launcher3.R;
import com.android.launcher3.transition.TransitionUtility.TransitionType;

/**
 * 페지전환에 필요한 animation의 resource들을 관리하는 클라스이다.
 */
public class TransitionManager {
    Context mContext;
    @TransitionType int mCurrentTransition;
    @AnimatorRes int[] mAnimationResource = new int[4];

    public TransitionManager(Context context) {
        mContext = context;
        loadTransitionPrefAndResource();
    }

    /**
     * Preference에서 전환설정을 얻고 animation resource 들을 불러들인다.
     */
    public void loadTransitionPrefAndResource() {
        mCurrentTransition = TransitionUtility.getTransitionPref(mContext);

        switch (mCurrentTransition){
            case TRANSITION_DEFAULT:
                setAnimationResources(R.animator.transition_default_left_in, R.animator.transition_default_left_out, R.animator.transition_default_right_in, R.animator.transition_default_right_out);
                break;
            case TRANSITION_PERSPECTIVE:
                setAnimationResources(R.animator.transition_perspective_left_in, R.animator.transition_perspective_left_out, R.animator.transition_perspective_right_in, R.animator.transition_perspective_right_out);
                break;
            case TRANSITION_SQUEEZE:
                setAnimationResources(R.animator.transition_squeeze_left_in, R.animator.transition_squeeze_left_out, R.animator.transition_squeeze_right_in, R.animator.transition_squeeze_right_out);
                break;
            case TRANSITION_CUBE:
                setAnimationResources(R.animator.transition_cube_left_in, R.animator.transition_cube_left_out, R.animator.transition_cube_right_in, R.animator.transition_cube_right_out);
                break;
            case TRANSITION_FLIP_OVER:
                setAnimationResources(R.animator.transition_flip_over_left_in, R.animator.transition_flip_over_left_out, R.animator.transition_flip_over_right_in, R.animator.transition_flip_over_right_out);
                break;
            case TRANSITION_ROTATE:
                setAnimationResources(R.animator.transition_rotate_left_in, R.animator.transition_rotate_left_out, R.animator.transition_rotate_right_in, R.animator.transition_rotate_right_out);
                break;
            case TRANSITION_CASCADE:
                setAnimationResources(R.animator.transition_cascade_left_in, R.animator.transition_cascade_left_out, R.animator.transition_cascade_right_in, R.animator.transition_cascade_right_out);
                break;
            case TRANSITION_WINDMILL:
                setAnimationResources(R.animator.transition_windmill_left_in, R.animator.transition_windmill_left_out, R.animator.transition_windmill_right_in, R.animator.transition_windmill_right_out);
                break;
        }
    }

    /**
     * Animation resource들을 설정한다.
     * @param left_in   왼쪽페지가 보여지는것
     * @param left_out  왼쪽페지가 사라지는것
     * @param right_in  오른쪽페지가 보여지는것
     * @param right_out 오른쪽페지가 사라지는것
     */
    private void setAnimationResources(@AnimatorRes int left_in, @AnimatorRes int left_out, @AnimatorRes int right_in, @AnimatorRes int right_out) {
        mAnimationResource[0] = left_in;
        mAnimationResource[1] = left_out;
        mAnimationResource[2] = right_in;
        mAnimationResource[3] = right_out;
    }

    /*---- Animation resource 들을 보여준다. ----*/
    public int getAnimationLeftIn() {
        return mAnimationResource[0];
    }
    public int getAnimationLeftOut() {
        return mAnimationResource[1];
    }
    public int getAnimationRightIn() {
        return mAnimationResource[2];
    }
    public int getAnimationRightOut() {
        return mAnimationResource[3];
    }
}
