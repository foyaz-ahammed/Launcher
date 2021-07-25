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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;

/**
 * 8개의 전환형식단추들을 가지고 있는 수평방향 ScrollView
 *
 * 전체단추들을 절반으로 갈라서 한페지에 4개 단추씩 보여준다.
 * scroll이 끝나면 거기서 멈추는것이 아니라 첫페지 혹은 둘째페지로 이동한다.
 */
public class CustomizedHorizontalScrollView extends HorizontalScrollView {
    private static final int ONE_PAGE_SCROLL_DURATION = 1000;
    private static final int MIN_VELOCITY_VALUE = 500;

    //Scroll Animator(끌기가 끝났을때 진행된다.)
    private Animator mScrollAnimator;

    private boolean mIsDragging;        //끌기중인가?
    private boolean mIsFling = false;   //빠른 끌기인가?

    //Fling의 거리, 속도를 계산하는데 필요한 값들
    private float mPhysicalCoeff;
    private final float mFlingFriction = ViewConfiguration.getScrollFriction();
    private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private static final float INFLEXION = 0.35f;
    private static final float START_TENSION = 0.5f;
    private static final float END_TENSION = 1.0f;
    private static final float P1 = START_TENSION * INFLEXION;
    private static final float P2 = 1.0f - END_TENSION * (1.0f - INFLEXION);
    private static final int NB_SAMPLES = 100;
    private static final float[] SPLINE_POSITION = new float[NB_SAMPLES + 1];
    private static final float[] SPLINE_TIME = new float[NB_SAMPLES + 1];

    static {
        //Fling기초값들을 계산한다.
        float x_min = 0.0f;
        float y_min = 0.0f;
        for (int i = 0; i < NB_SAMPLES; i++) {
            final float alpha = (float) i / NB_SAMPLES;

            float x_max = 1.0f;
            float x, tx, coef;
            while (true) {
                x = x_min + (x_max - x_min) / 2.0f;
                coef = 3.0f * x * (1.0f - x);
                tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x;
                if (Math.abs(tx - alpha) < 1E-5) break;
                if (tx > alpha) x_max = x;
                else x_min = x;
            }
            SPLINE_POSITION[i] = coef * ((1.0f - x) * START_TENSION + x) + x * x * x;

            float y_max = 1.0f;
            float y, dy;
            while (true) {
                y = y_min + (y_max - y_min) / 2.0f;
                coef = 3.0f * y * (1.0f - y);
                dy = coef * ((1.0f - y) * START_TENSION + y) + y * y * y;
                if (Math.abs(dy - alpha) < 1E-5) break;
                if (dy > alpha) y_max = y;
                else y_min = y;
            }
            SPLINE_TIME[i] = coef * ((1.0f - y) * P1 + y * P2) + y * y * y;
        }
        SPLINE_POSITION[NB_SAMPLES] = SPLINE_TIME[NB_SAMPLES] = 1.0f;
    }

    public CustomizedHorizontalScrollView(Context context) {
        this(context, null);
    }

    public CustomizedHorizontalScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomizedHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getFlingValues(context);
    }

    /**
     * Fling값들 계산
     * @param context Context
     */
    private void getFlingValues(Context context){
        final float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning
    }

    /**
     * 끌기가 끝났을때 호출된다.
     * 현재 움직인 거리와 속도에 맞게 animation을 진행한다.
     */
    private void onDraggingEnded(){
        int pageWidth = getWidth();
        int currentScrollPos = getScrollX();

        int scrollInsidePage = currentScrollPos % pageWidth;
        final int finalPage; //최종적으로 이동할 페지

        //scroll위치가 페지너비의 절반보다 크면 둘째 페지, 작으면 첫페지로 이동한다.
        boolean smallerThanHalf = scrollInsidePage * 2 < pageWidth;
        if (smallerThanHalf) {
            finalPage = 0;
        } else {
            finalPage = 1;
        }

        //움직여야 할 거리, 지속시간을 계산한다.
        int targetScrollPos = pageWidth * finalPage;
        int duration = (Math.abs(targetScrollPos - currentScrollPos)
                * ONE_PAGE_SCROLL_DURATION) / pageWidth;

        //Animator를 창조하고 animation을 시작한다.
        mScrollAnimator = ObjectAnimator.ofInt(this, "scrollX", targetScrollPos);
        mScrollAnimator.setDuration(duration);
        mScrollAnimator.start();
    }

    private void doFling(int velocity){
        boolean directionRight = velocity > 0;
        int pageWidth = getWidth();
        int currentScrollPos = getScrollX();
        final int finalPage;

        if (!directionRight) {  //왼쪽방향이면 첫페지로
            finalPage = 0;
        } else {                //오른쪽방향이면 둘째 페지로
            finalPage = 1;
        }

        //목표위치, animation의 지속시간을 계산한다.
        int targetScrollPos = pageWidth * finalPage;
        float splineDuration = getSplineFlingDuration(velocity);
        double totalDistance = getSplineFlingDistance(velocity);
        int splineDistance = (int) (totalDistance * Math.signum(velocity));
        int finalPos = currentScrollPos + splineDistance;
        splineDuration *= adjustDuration(currentScrollPos, finalPos, targetScrollPos);

        //Animator를 창조하고 animation을 시작한다.
        mScrollAnimator = ObjectAnimator.ofInt(this, "scrollX", targetScrollPos);
        mScrollAnimator.setDuration((long) splineDuration);
        mScrollAnimator.start();
    }

    /**
     * Fling이 일어났을때 ScrollView의 표준 Fling동작대신 자체로 fling을 진행한다
     *
     * @see #doFling
     * @param velocityX 속도
     */
    @Override
    public void fling(int velocityX){
        if(Math.abs(velocityX) > MIN_VELOCITY_VALUE) {
            mIsFling = true;
            doFling(velocityX);
        }
    }

    private int getSplineFlingDuration(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * Math.exp(l / decelMinusOne));
    }
    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }
    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }
    private float adjustDuration(int start, int oldFinal, int newFinal) {
        final int oldDistance = oldFinal - start;
        final int newDistance = newFinal - start;
        final float x = Math.abs((float) newDistance / oldDistance);
        final int index = (int) (NB_SAMPLES * x);
        if (index < NB_SAMPLES) {
            final float x_inf = (float) index / NB_SAMPLES;
            final float x_sup = (float) (index + 1) / NB_SAMPLES;
            final float t_inf = SPLINE_TIME[index];
            final float t_sup = SPLINE_TIME[index + 1];
            return t_inf + (x - x_inf) / (x_sup - x_inf) * (t_sup - t_inf);
        }
        return 1;
    }

    /**
     * Touch사건 처리
     * @param ev MotionEvent
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {int action = ev.getAction();
        //끌기중
        if (action == MotionEvent.ACTION_MOVE) {
            mIsDragging = true;
            mIsFling = false;

            //Animation 이 이미 진행중이면 중지한다.
            if(mScrollAnimator != null && mScrollAnimator.isRunning()) {
                mScrollAnimator.pause();
            }
        }

        //끌기끝
        else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mIsDragging) {
                //끌기가 끝난후 fling이 일어날수 있으므로 약간의 지연을 주고 fling이 아닐때에 그에 따른 동작을 진행한다.
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(!mIsFling) {
                            onDraggingEnded();
                        }
                    }
                }, 10);
            }
            mIsDragging = false;
        }

        return super.onTouchEvent(ev);
    }
}
