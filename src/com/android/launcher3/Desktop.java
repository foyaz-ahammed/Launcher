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

package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.android.launcher3.assistant.AssistViewsContainer;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.touch.OverScroll;

/**
 * Assist Page, Home화면을 포함하고 있는 layout
 * @see R.layout#launcher
 */
public class Desktop extends LinearLayout implements Insettable {

    Launcher mLauncher; //Main Activity
    AssistViewsContainer mAssistant;        //Assist page
    DragLayer mDragLayer;                   //Home 화면
    ValueAnimator mDesktopAnimator = null;  //Home 화면을 오른쪽으로 끌었을때 리용되는 Animator

    /*-- Touch사건을 처리하기 위한 변수들 --*/
    boolean mTouchDownHandled = false;
    boolean mFirstTouchMove = true;
    boolean mWasInOverScroll = false;
    float mCurrentX = 0;
    float mDownMotionX = 0;
    float mDownMotionY = 0;
    float mLastMotionX = 0;
    float mLastMotionXRemainder = 0;

    /*-- 끌기할때 fling을 감지하기 위한 변수들 --*/
    protected final static int TOUCH_STATE_REST = 0;
    protected final static int TOUCH_STATE_NOT_REST = 1;
    protected int mTouchState = TOUCH_STATE_REST;
    private static final int FLING_THRESHOLD_VELOCITY = 100;
    private VelocityTracker mVelocityTracker;
    private final int mFlingThresholdVelocity;
    private final int mMaximumVelocity;

    private boolean mInterceptTouchAllowed = true;  //Touch사건 가로채기를 허용하겠는가?
    private boolean isOnAssistant;  //현재 Assist page가 보여진 상태인가? (false 이면 Home화면이 보이는 상태이다.)

    public Desktop(Context context) {
        this(context, null);
    }

    public Desktop(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Desktop(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //Main Activity얻기
        mLauncher = Launcher.getLauncher(context);

        //Fling 곁수들 계산
        float density = getResources().getDisplayMetrics().density;
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * density);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    /**
     * Animator 창조
     */
    public void createDesktopAnimator(){
        mDesktopAnimator = ValueAnimator.ofFloat(0, 1);
        mDesktopAnimator.setInterpolator(new LinearInterpolator());

        mDesktopAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                setAnimatorProgress(value);
            }
        });

        mDesktopAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                mWasInOverScroll = false;
                mDesktopAnimator = null;
            }
        });
    }

    /**
     * Animator 속성함수
     * @param value 속성값
     */
    public void setAnimatorProgress(float value){
        //움직임거리계산
        final int width = getWidth();
        final int distance = (int)(value * width);

        //Scroll넘침이 아닐때
        if(!mWasInOverScroll) {
            //자식 View들을 움직인다.
            mAssistant.setTranslationX(distance);
            mDragLayer.setTranslationX(distance);
            mDragLayer.invalidate();

            //Home 화면의 투명정도를 설정한다.
            float fraction = (float) (1 - Math.pow(1 - value, 4));
            float alpha = 1 - fraction;
            mDragLayer.setAlphaToChildren(alpha);
        }

        //Scroll넘침일때
        else {
            //자식 View들을 움직인다.
            mAssistant.setTranslationX(width + distance);
            mDragLayer.setTranslationX(width + distance);
            mDragLayer.invalidate();
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        //자식 View들 얻기
        mAssistant = findViewById(R.id.assistant);
        mDragLayer = findViewById(R.id.drag_layer);

        mAssistant.setDesktop(this);

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //처음에 Home 화면이 보이도록 한다. (layout 구조상 Assiat page가 먼저 보이도록 되있다.)
                final int width = getWidth();
                scrollTo(width ,0);

                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    /**
     * 편차거리만큼 화면을 좌우로 이동시킨다.
     * @param offsetX 편차
     */
    public void moveWithOffset(int offsetX){
        if(mDesktopAnimator == null)
            createDesktopAnimator();
        else if(mDesktopAnimator.isPaused()){
            mDesktopAnimator = null;
            createDesktopAnimator();
        }

        //Animator 속성값 계산
        final float width = getWidth();
        final float fraction = offsetX/width;

        //Animator의 속성함수를 호출하여 화면을 이동시킨다.
        mDesktopAnimator.setCurrentFraction(fraction);
    }

    /**
     * scroll넘침을 처리한다.
     * @param amount scroll 넙침거리
     */
    public void overScroll(int amount){
        //Animator 창조
        if(mDesktopAnimator == null) {
            createDesktopAnimator();
        } else if(mDesktopAnimator.isPaused()){
            mDesktopAnimator = null;
            createDesktopAnimator();
        }

        //Animator 속성값 계산
        final float width = getWidth();
        final float fraction = amount/width;

        //Animator의 속성함수를 호출하여 화면을 이동시킨다.
        mDesktopAnimator.setCurrentFraction(fraction);
    }

    /**
     * Animation을 진행한다.
     * @param isFling Fling(빠른 끌기)인가
     * @param flingDirectionLeft Fling방향, true: 왼쪽, false: 오른쪽
     */
    public void playCurrentAnimation(boolean isFling, boolean flingDirectionLeft){
        if(mDesktopAnimator != null) {
            if(mWasInOverScroll)
            {
                mDesktopAnimator.reverse();
                return;
            }

            if(isFling){
                if(!flingDirectionLeft) {
                    mDesktopAnimator.start();
                    isOnAssistant = true;
                }
                else {
                    mDesktopAnimator.reverse();
                    isOnAssistant = false;
                }
            }
            else {
                float currentFraction = mDesktopAnimator.getAnimatedFraction();

                if (currentFraction >= 0.5f) {
                    mDesktopAnimator.start();
                    isOnAssistant = true;
                }
                else {
                    mDesktopAnimator.reverse();
                    isOnAssistant = false;
                }
            }
        }
    }

    /**
     * 현재 보이는 화면이 Assist page인가를 돌려준다.
     * @return true: Assist page가 보임, false: Home 화면이 보임
     */
    public boolean getOnAssistant() {
        return isOnAssistant;
    }

    /**
     * Touch 사건 가로채기 (Home화면에서의 touch사건 처리)
     * @param ev MotionEvent
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev){
        if(mInterceptTouchAllowed) {
            int action = ev.getAction();
            float x = ev.getX();
            float y = ev.getY();

            switch (action){
                case  MotionEvent.ACTION_DOWN:  //끌기시작
                    mDownMotionX = x;
                    mDownMotionY = y;
                    mLastMotionX = x;
                    mLastMotionXRemainder = 0;
                    mFirstTouchMove = true;
                    mCurrentX = 0;

                    if(ev.getX() < mAssistant.getTranslationX())
                        mTouchDownHandled = true;
                    break;
                case MotionEvent.ACTION_MOVE:   //끌기중
                    if(mTouchDownHandled) {
                        if (mFirstTouchMove) {
                            mFirstTouchMove = false;

                            //Scroll 방향이 수직/수평인가 검사
                            float dx = Math.abs(x - mDownMotionX);
                            float dy = Math.abs(y - mDownMotionY);
                            if (dy > dx || dx == 0) {
                                return super.onInterceptTouchEvent(ev);
                            }

                            return true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: //끌기끝
                    mFirstTouchMove = true;
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Touch사건을 처리한다. (Assist page의 touch사건 처리)
     * @param event MotionEvent
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        handleTouchEvent(event);
        return true;
    }

    /**
     * Touch사건처리함수 (Assist page)
     * @param ev MotionEvent
     */
    public void handleTouchEvent(MotionEvent ev){

        acquireVelocityTrackerAndAddMovement(ev);
        final float x = ev.getX();
        final float y = ev.getY();

        switch (ev.getActionMasked()){
            case MotionEvent.ACTION_DOWN:   //끌기시작
                //변수들 초기화
                mDownMotionX = x;
                mDownMotionY = y;
                mLastMotionX = x;
                mLastMotionXRemainder = 0;

                mTouchState = TOUCH_STATE_NOT_REST;

                mCurrentX = 0;
                if(mDesktopAnimator != null){
                    mDesktopAnimator.pause();
                    mCurrentX = mAssistant.getTranslationX() - getWidth();
                }
                mFirstTouchMove = true;
                break;

            case MotionEvent.ACTION_MOVE:   //끌기중
                mTouchState = TOUCH_STATE_NOT_REST;
                if(mTouchDownHandled) {
                    final int width = getWidth();

                    final float deltaX = x - (mLastMotionX + mLastMotionXRemainder);
                    mCurrentX = mCurrentX + deltaX;

                    mLastMotionX = x;
                    mLastMotionXRemainder = deltaX - (int) deltaX;

                    if (mCurrentX > 0) {
                        int dampedAmount = OverScroll.dampedScroll(Math.abs(mCurrentX), getMeasuredWidth());

                        //이미 진행중이던 Animation은 중지
                        if(mDesktopAnimator != null && mWasInOverScroll){
                            mDesktopAnimator.end();
                            mDesktopAnimator = null;
                        }

                        mWasInOverScroll = true;
                        overScroll(dampedAmount);
                    }
                    else {
                        //이미 진행중이던 Animation은 중지
                        if(mDesktopAnimator != null && mWasInOverScroll){
                            mDesktopAnimator.end();
                            mDesktopAnimator = null;
                        }

                        mWasInOverScroll = false;
                        moveWithOffset((int) (width + mCurrentX));
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:     //끌기종료
                mTouchState = TOUCH_STATE_REST;
                if(mTouchDownHandled) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

                    int velocityX = (int) velocityTracker.getXVelocity(0);
                    boolean isFling = shouldFlingForVelocity(Math.abs(velocityX));
                    boolean isVelocityXLeft = velocityX < 0;

                    playCurrentAnimation(isFling, isVelocityXLeft);
                    releaseVelocityTracker();

                    mTouchDownHandled = false;
                }
                mFirstTouchMove = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                releaseVelocityTracker();
                mFirstTouchMove = true;
                break;
        }
    }

    /**
     * 현재 화면이 끌기하여 이동중인가 아닌가 를 돌려준다.
     */
    public boolean isTouchActive() {
        return mTouchState != TOUCH_STATE_REST;
    }

    /**
     * 움직임들을 기록한다. (이로부터 끌기속도를 계산한다.)
     * @param ev MotionEvent
     */
    public void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    /**
     * 기록된 움직임들을 모두 없앤다.
     */
    void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * Touch사건 가로채기를 허용하겠는가를 설정한다.
     * @param allowed 허용
     */
    public void setInterceptTouchAllowed(boolean allowed){
        mInterceptTouchAllowed = allowed;
    }

    /**
     * 끌기속도로부터 fling인가 아닌가를 판별한다.
     * @param velocityX 끌기속도
     */
    protected boolean shouldFlingForVelocity(int velocityX) {
        return Math.abs(velocityX) > mFlingThresholdVelocity;
    }

    @Override
    public void setInsets(Rect insets) {
        //insets 를 자식 view들에도 전달
        mAssistant.setInsets(insets);
        mDragLayer.setInsets(insets);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //Assist page와 Home화면의 크기를 화면의 크기와 꼭같게 설정한다.
        mAssistant.measure(widthMeasureSpec, heightMeasureSpec);
        mDragLayer.measure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * @return Assist Page를 돌려준다.
     */
    public AssistViewsContainer getAssistViewsContainer(){
        return mAssistant;
    }
}
