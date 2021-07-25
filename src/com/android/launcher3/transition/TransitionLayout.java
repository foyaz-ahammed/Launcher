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

import static com.android.launcher3.transition.TransitionUtility.TRANSITION_DEFAULT;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_PERSPECTIVE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_SQUEEZE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_CUBE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_FLIP_OVER;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_ROTATE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_CASCADE;
import static com.android.launcher3.transition.TransitionUtility.TRANSITION_WINDMILL;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import ch.deletescape.lawnchair.views.OptionsPanel;
import ch.deletescape.lawnchair.views.OptionsTextView;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;

/**
 * Home화면을 길게 누른다음 `전환`단추를 눌렀을때 펼쳐지는 layout
 *
 * @see TransitionLayout#show
 * @see R.layout#transition_layout
 */
public class TransitionLayout extends AbstractFloatingView implements View.OnClickListener {

    //Animation을 진행하는데 필요한 상수값들
    private static final float ANIMATE_START_SCALE = 1.2f;      //시작 scale(처음에는 확대된 상태이다.)
    private static final float ANIMATE_START_ALPHA = 0.3f;      //시작 alpha(처음에는 희미한 상태이다.)
    private static final int APPEAR_ANIMATION_DURATION = 200;   //Layout이 펼쳐지는 Animation의 지속시간

    /*
    * 미리보기 Animation(전환형식을 하나 선택했을때)
    * 두 단계로 진행된다.
    * 다음페지로 갔다가 다시 그 페지로 이행.
    * 혹은 현재 페지가 마지막페지일때에는 전페지로 갔다가 다시 그 페지로 이행
    */
    private static final int PREVIEW_ANIMATION_DURATION = 300;  //Animation의 지속시간
    private static final int PREVIEW_DELAY_ON_HALF = 100;       //Animation의 중간시점에서의 지연시간(다음/이전페지까지 완전히 도착했을때)

    Launcher mLauncher;
    Workspace mWorkspace;
    LinearLayout mScrollLayout; //여러가지 전환형식단추들을 가진 scroll할수 있는 layout
    CustomizedHorizontalScrollView mScrollView; //mScrollLayout의 부모(Custom한 ScrollView 클라스)

    int mItemSelectedColor;     //단추를 선택했을때의 색갈(아이콘, label)
    int mItemUnSelectedColor;   //단추를 선택안했을때의 색갈
    int mIconSize;              //아이콘크기

    //전환형식단추들
    OptionsTextView mDefault;       //기정(Slide)
    OptionsTextView mPerspective;   //원근
    OptionsTextView mSqueeze;       //압착
    OptionsTextView mCube;          //립방
    OptionsTextView mFlipOver;      //뒤집기
    OptionsTextView mRotate;        //회전
    OptionsTextView mCascade;       //계단
    OptionsTextView mWindMill;      //풍차

    //선택된 전환형식단추
    OptionsTextView mSelectedTransitionItem = null;

    //미리보기 animation을 진행하는 2개의 자식 View들 (CellLayout)
    CellLayout mWorkspaceFirstChild;
    CellLayout mWorkspaceSecondChild;

    ShortcutAndWidgetContainer mAnimateFirstView;
    ShortcutAndWidgetContainer mAnimateSecondView;

    //2개의 animator를 동작시킨다.(첫페지, 둘째 페지)
    AnimatorSet mFirstAnimatorSetForward = null;    //전진방향 - 첫 페지
    AnimatorSet mSecondAnimatorSetForward = null;   //전진방향 - 둘째 페지
    AnimatorSet mFirstAnimatorSetBack = null;       //후진방향 - 첫 페지
    AnimatorSet mSecondAnimatorSetBack = null;      //후진방향 - 둘째 페지
    boolean mPlayBackAnimationStartDelaying = false;    //중간지점에서 animation이 중지된 상태일동안 true로 설정됨 (PREVIEW_DELAY_ON_HALF)

    //Animation resource들을 관리하는 변수
    TransitionManager mTransitionManager;

    public TransitionLayout(Context context) {
        this(context, null);
    }

    public TransitionLayout(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransitionLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
        mWorkspace = mLauncher.getWorkspace();
        mTransitionManager = new TransitionManager(context);

        //선택된/선택안된 단추의 색갈들을 불러온다.
        mItemSelectedColor = mLauncher.getColor(R.color.transition_selected_color);
        mItemUnSelectedColor = mLauncher.getColor(R.color.transition_unselected_color);

        //아이콘크기를 불러온다.
        mIconSize = (int)getResources().getDimension(R.dimen.options_view_icon_size);
    }

    @Override
    protected void onFinishInflate(){
        super.onFinishInflate();

        //자식 View들 얻기
        mScrollLayout = findViewById(R.id.scroll_layout);
        mScrollView = findViewById(R.id.transition_scroll_view);

        int childWidth = mLauncher.getDragLayer().getWidth() / 4;
        for (int i = 0; i < mScrollLayout.getChildCount(); i ++){
            if(mScrollLayout.getChildAt(i) instanceof OptionsTextView){
                OptionsTextView optionsChild = (OptionsTextView) mScrollLayout.getChildAt(i);

                Rect bound = optionsChild.getCompoundDrawables()[1].getBounds();
                int start = (bound.width() - mIconSize)/2;
                bound.set(start, start, start + mIconSize, start + mIconSize);

                optionsChild.getCompoundDrawables()[1].setBounds(bound);

                optionsChild.setWidth(childWidth);
                optionsChild.setOnClickListener(this);
            }
        }

        //전환형식단추들
        mDefault = findViewById(R.id.transition_default_button);
        mPerspective = findViewById(R.id.transition_perspective_button);
        mSqueeze = findViewById(R.id.transition_squeeze_button);
        mCube = findViewById(R.id.transition_cube_button);
        mFlipOver = findViewById(R.id.transition_flip_over_button);
        mRotate = findViewById(R.id.transition_rotate_button);
        mCascade = findViewById(R.id.transition_cascade_button);
        mWindMill = findViewById(R.id.transition_windmill_button);

        //Preference에 설정된 전환형식을 얻는다.
        int transitionPref = TransitionUtility.getTransitionPref(getContext());

        //전환형식에 따라 선택될 단추, 페지를 계산한다.
        final int scrollViewPage;
        if(transitionPref == TRANSITION_DEFAULT) {
            mSelectedTransitionItem = mDefault;
            scrollViewPage = 0;
        }
        else if(transitionPref == TRANSITION_PERSPECTIVE) {
            mSelectedTransitionItem = mPerspective;
            scrollViewPage = 0;
        }
        else if(transitionPref == TRANSITION_SQUEEZE) {
            mSelectedTransitionItem = mSqueeze;
            scrollViewPage = 0;
        }
        else if(transitionPref == TRANSITION_CUBE) {
            mSelectedTransitionItem = mCube;
            scrollViewPage = 0;
        }
        else if(transitionPref == TRANSITION_FLIP_OVER) {
            mSelectedTransitionItem = mFlipOver;
            scrollViewPage = 1;
        }
        else if(transitionPref == TRANSITION_ROTATE) {
            mSelectedTransitionItem = mRotate;
            scrollViewPage = 1;
        }
        else if(transitionPref == TRANSITION_CASCADE) {
            mSelectedTransitionItem = mCascade;
            scrollViewPage = 1;
        }
        else if(transitionPref == TRANSITION_WINDMILL) {
            mSelectedTransitionItem = mWindMill;
            scrollViewPage = 1;
        }
        else{
            scrollViewPage = 2;
        }

        //해당 전환형식단추의 색갈을 설정한다.
        setTextAndDrawableColor(mSelectedTransitionItem, true);

        final TransitionLayout animateTransitionView = this;
        final OptionsPanel animateOptionsView = (OptionsPanel)mLauncher.getOptionsView();
        final ViewTreeObserver observer = getViewTreeObserver();

        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mScrollView.scrollTo(scrollViewPage * mScrollView.getWidth(), 0);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);

                //Transition Layout은 축소되면서 서서히 보이는 animation
                ObjectAnimator firstAnimator = ObjectAnimator.ofFloat(animateTransitionView, "scaleAlpha", 0, 1);
                firstAnimator.setDuration(APPEAR_ANIMATION_DURATION);
                firstAnimator.start();

                //Options Panel(배경화면, 위젯, 전환, 홈 설정)은 서서히 사라지는 animation
                ObjectAnimator secondAnimator = ObjectAnimator.ofFloat(animateOptionsView, "alpha", 1, 0f);
                secondAnimator.setDuration(APPEAR_ANIMATION_DURATION);
                secondAnimator.start();
            }
        });
    }

    /**
     * 전환형식단추의 색갈을 설정한다.(아이콘, 본문색)
     * @param textView 단추
     * @param selected 단추가 선택되였는가, 선택안되였는가?
     */
    private void setTextAndDrawableColor(TextView textView, boolean selected){
        int color = selected? mItemSelectedColor : mItemUnSelectedColor;
        textView.setTextColor(color);

        int resource = 0;
        if(textView == mDefault) {              //기정
            resource = selected ? R.drawable.launcher_edit_transition_defult_current
                    : R.drawable.launcher_edit_transition_defult_pressed;
        } else if(textView == mPerspective) {   //원근
            resource = selected ? R.drawable.launcher_edit_transition_perspective_current
                    : R.drawable.launcher_edit_transition_perspective_pressed;
        } else if(textView == mSqueeze) {       //압착
            resource = selected ? R.drawable.launcher_edit_transition_squeeze_current
                    : R.drawable.launcher_edit_transition_squeeze_pressed;
        } else if(textView == mCube) {          //립방
            resource = selected ? R.drawable.launcher_edit_transition_box_current
                    : R.drawable.launcher_edit_transition_box_pressed;
        } else if(textView == mFlipOver) {      //뒤집기
            resource = selected ? R.drawable.launcher_edit_transition_filpover_current
                    : R.drawable.launcher_edit_transition_filpover_pressed;
        } else if(textView == mRotate) {        //회전
            resource = selected ? R.drawable.launcher_edit_transition_rotate_current
                    : R.drawable.launcher_edit_transition_rotate_pressed;
        } else if(textView == mCascade) {       //계단
            resource = selected ? R.drawable.launcher_edit_transition_cascade_current
                    : R.drawable.launcher_edit_transition_cascade_pressed;
        } else if(textView == mWindMill) {      //풍차
            resource = selected ? R.drawable.launcher_edit_transition_windmill_current
                    : R.drawable.launcher_edit_transition_windmill_pressed;
        }

        //TextView의 Drawable을 설정한다.
        textView.setCompoundDrawablesWithIntrinsicBounds(0, resource, 0, 0);
        Drawable drawable = getResources().getDrawable(resource, null);
        int start = (drawable.getIntrinsicWidth() - mIconSize)/2;
        Rect bound = new Rect(start, start, start + mIconSize, start + mIconSize);
        textView.getCompoundDrawables()[1].setBounds(bound);
    }

    /**
     * Layout을 없앤다.
     * @param animate Animation을 주겠는가?
     */
    @Override
    public void handleClose(boolean animate) {
        mIsOpen = false;
        OptionsPanel optionsView = (OptionsPanel) mLauncher.getOptionsView();

        if(animate){
            //Animation 진행
            final TransitionLayout transitionView = this;

            //TransitionLayout은 확대되면서 서서히 없어지는 animation
            ObjectAnimator firstAnimator = ObjectAnimator.ofFloat(transitionView, "scaleAlpha", 1, 0);
            firstAnimator.addListener(new AnimatorListener(){
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLauncher.getDragLayer().removeView(transitionView);
                }

                @Override
                public void onAnimationStart(Animator animation) { }
                @Override
                public void onAnimationCancel(Animator animation) { }
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            firstAnimator.setDuration(APPEAR_ANIMATION_DURATION);
            firstAnimator.start();

            //OptionsView는 서서히 보여지는 animation
            ObjectAnimator secondAnimator = ObjectAnimator.ofFloat(optionsView, "alpha", 1.0f);
            secondAnimator.setDuration(APPEAR_ANIMATION_DURATION);
            secondAnimator.start();
        }
        else{
            //DragLayer에서 view를 제거한다.
            mLauncher.getDragLayer().removeView(this);
        }
    }

    @Override
    public void logActionCommand(int command) {
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_TRANSITION_BOTTOM_SHEET) != 0;
    }

    /**
     * `전환`단추를 눌렀을때 호출된다..
     * Transition Layout을 animation과 함께 현시해준다.
     *
     * @param launcher Launcher
     * @return 생성된 layout을 돌려준다.
     */
    public static TransitionLayout show(Launcher launcher) {
        //layout을 inflate한다.
        TransitionLayout sheet = (TransitionLayout) launcher.getLayoutInflater()
                .inflate(R.layout.transition_layout, launcher.getDragLayer(), false);
        sheet.mIsOpen = true;

        //Drag Layer에 layout을 추가한다.
        launcher.getDragLayer().addView(sheet);

        //생성된 layout을 돌려준다.
        return sheet;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    /**
     * Animation속성함수
     * Scale, Alpha값을 조절한다.
     *
     * @param value 속성값
     */
    @Keep
    public void setScaleAlpha(float value) {
        //Scale
        float scaleValue = ANIMATE_START_SCALE - value*(ANIMATE_START_SCALE - 1);
        setScaleX(scaleValue);
        setScaleY(scaleValue);

        //Alpha
        float alphaValue = ANIMATE_START_ALPHA + value*(1 - ANIMATE_START_ALPHA);
        setAlpha(alphaValue);
    }

    @Override
    public void onClick(View v) {
        if(v instanceof OptionsTextView) {
            //미리보기 animation이 이미 진행중이면 함수를 끝낸다.
            if(isPreviewAnimationRunning())
                return;

            //선택된 단추, 본래 선택되였던 단추들의 색갈을 갱신한다.
            if(mSelectedTransitionItem != null && mSelectedTransitionItem != v){
                setTextAndDrawableColor(mSelectedTransitionItem, false);
            }
            setTextAndDrawableColor((OptionsTextView)v, true);
            mSelectedTransitionItem = (OptionsTextView)v;

            final int transitionValue;  //전환형식
            if(v == mDefault)
                transitionValue = TRANSITION_DEFAULT;
            else if(v == mPerspective)
                transitionValue = TRANSITION_PERSPECTIVE;
            else if(v == mSqueeze)
                transitionValue = TRANSITION_SQUEEZE;
            else if(v == mCube)
                transitionValue = TRANSITION_CUBE;
            else if(v == mFlipOver)
                transitionValue = TRANSITION_FLIP_OVER;
            else if(v == mRotate)
                transitionValue = TRANSITION_ROTATE;
            else if(v == mCascade)
                transitionValue = TRANSITION_CASCADE;
            else
                transitionValue = TRANSITION_WINDMILL;

            //전환형식을 Preference 에 보관한다.
            TransitionUtility.setTransitionPref(getContext(), transitionValue);

            //전환형식이 갱신된데 맞게 animation xml 파일들을 불러들인다.
            mTransitionManager.loadTransitionPrefAndResource();
            mLauncher.getWorkspace().reloadTransitionPref();

            //미리보기 animation을 시작한다.
            playPreviewAnimation();
        }
    }

    /**
     * 미리보기 Animation 을 진행하기 위한 View들(첫페지, 두번째 페지)을 얻는다,
     * @return 현재가 마지막 페지이고 비였을때 true를 돌려준다.(Home화면을 길게 누르면 마지막에 빈페지가 생성된다.)
     */
    public boolean getChildrenForPreview() {
        return mWorkspace.getChildrenForPreview(this);
    }

    /**
     * 첫번째 자식 설정
     * @param view CellLayout
     */
    public void setFirstChild(CellLayout view) {
        mWorkspaceFirstChild = view;
    }

    /**
     * 두번째 자식설정
     * @param view CellLayout
     */
    public void setSecondChild(CellLayout view) {
        mWorkspaceSecondChild = view;
    }

    /**
     * 미리보기 animation 시작
     */
    private void playPreviewAnimation(){
        //Animation진행중에는 량옆페지(이전, 다음)들을 숨긴다.
        boolean isLastPageAndEmpty = getChildrenForPreview();
        mWorkspace.hideNeighbors();

        //Animator객체들을 초기화
        if(mFirstAnimatorSetForward != null) {
            mFirstAnimatorSetForward.removeAllListeners();
            mFirstAnimatorSetForward = null;
        }
        if(mSecondAnimatorSetForward != null) {
            mSecondAnimatorSetForward.removeAllListeners();
            mSecondAnimatorSetForward = null;
        }
        if(mFirstAnimatorSetBack != null) {
            mFirstAnimatorSetBack.removeAllListeners();
            mFirstAnimatorSetBack = null;
        }
        if(mSecondAnimatorSetBack != null) {
            mSecondAnimatorSetBack.removeAllListeners();
            mSecondAnimatorSetBack = null;
        }

        //Animation resource들을 얻는다.
        int transition_left_in = mTransitionManager.getAnimationLeftIn();
        int transition_left_out = mTransitionManager.getAnimationLeftOut();
        int transition_right_in = mTransitionManager.getAnimationRightIn();
        int transition_right_out = mTransitionManager.getAnimationRightOut();

        if(mWorkspaceFirstChild != null) {
            mAnimateFirstView = mWorkspaceFirstChild.getShortcutsAndWidgets();
            if(isLastPageAndEmpty && mWorkspaceSecondChild != null) {
                mWorkspaceFirstChild.removeChildView(mAnimateFirstView);
                mWorkspaceSecondChild.addView(mAnimateFirstView);
            }

            mFirstAnimatorSetForward = (AnimatorSet) AnimatorInflater
                    .loadAnimator(getContext(), isLastPageAndEmpty? transition_left_in:transition_left_out);
            setDurationToAnimatorSet(mFirstAnimatorSetForward, PREVIEW_ANIMATION_DURATION);
            mFirstAnimatorSetForward.setTarget(mAnimateFirstView);

            mFirstAnimatorSetBack = (AnimatorSet) AnimatorInflater
                    .loadAnimator(getContext(), isLastPageAndEmpty? transition_left_out:transition_left_in);
            setDurationToAnimatorSet(mFirstAnimatorSetBack, PREVIEW_ANIMATION_DURATION);
            mFirstAnimatorSetBack.setTarget(mAnimateFirstView);
        }

        if(mWorkspaceSecondChild != null) {
            mAnimateSecondView = mWorkspaceSecondChild.getShortcutsAndWidgets();
            if(!isLastPageAndEmpty && mWorkspaceFirstChild != null) {
                mWorkspaceSecondChild.removeChildView(mAnimateSecondView);
                mWorkspaceFirstChild.addView(mAnimateSecondView);
            }

            mSecondAnimatorSetForward = (AnimatorSet) AnimatorInflater
                    .loadAnimator(getContext(), isLastPageAndEmpty? transition_right_out:transition_right_in);
            setDurationToAnimatorSet(mSecondAnimatorSetForward, PREVIEW_ANIMATION_DURATION);
            mSecondAnimatorSetForward.setTarget(mAnimateSecondView);

            mSecondAnimatorSetBack = (AnimatorSet) AnimatorInflater
                    .loadAnimator(getContext(), isLastPageAndEmpty? transition_right_in:transition_right_out);
            setDurationToAnimatorSet(mSecondAnimatorSetBack, PREVIEW_ANIMATION_DURATION);
            mSecondAnimatorSetBack.setTarget(mAnimateSecondView);
        }

        //전진방향의 animation을 시작한다.
        playGoingForwardAnimation(isLastPageAndEmpty);
    }

    public void setDurationToAnimatorSet(AnimatorSet animatorSet, int newDuration){
        long currentDuration = animatorSet.getTotalDuration();
        for (int i = 0; i < animatorSet.getChildAnimations().size(); i ++){
            Animator animator = animatorSet.getChildAnimations().get(i);
            animator.setDuration((int)(animator.getDuration() * newDuration/(currentDuration + 0f)));
        }
    }

    /**
     * 전진방향 Animation을 진행한다.
     * @param isLastPageAndEmpty 현재페지가 마지막페지이고 비였는가?
     */
    public void playGoingForwardAnimation(boolean isLastPageAndEmpty){
        //Determine which animator to process end action
        boolean processEndOnFirstAnimation;
        AnimatorSet endProcessAnimator;
        if(mWorkspaceFirstChild == null)
            processEndOnFirstAnimation = false;
        else if(mWorkspaceSecondChild == null)
            processEndOnFirstAnimation = true;
        else{
            processEndOnFirstAnimation = mFirstAnimatorSetForward.getTotalDuration() > mSecondAnimatorSetForward.getTotalDuration();
        }

        if(processEndOnFirstAnimation)
            endProcessAnimator = mFirstAnimatorSetForward;
        else
            endProcessAnimator = mSecondAnimatorSetForward;

        if(mWorkspaceFirstChild != null){
            //set the same position as the first child
            mFirstAnimatorSetForward.start();
        }
        if(mWorkspaceSecondChild != null){
            mSecondAnimatorSetForward.start();
        }

        //Do end action
        endProcessAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mPlayBackAnimationStartDelaying = true;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mWorkspaceFirstChild != null){
                            mAnimateFirstView.initializeProperties();
                        }
                        if(mWorkspaceSecondChild != null) {
                            mAnimateSecondView.initializeProperties();
                        }

                        playGoingBackAnimation(isLastPageAndEmpty);
                        mPlayBackAnimationStartDelaying = false;
                    }
                }, PREVIEW_DELAY_ON_HALF);
            }

            @Override
            public void onAnimationStart(Animator animation) { }
            @Override
            public void onAnimationCancel(Animator animation) { }
            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
    }

    /**
     * 후진방향 Animation을 진행한다.(돌아올때)
     * @param isLastPageAndEmpty 현재페지가 마지막페지이고 비였는가?
     */
    public void playGoingBackAnimation(boolean isLastPageAndEmpty){
        //Determine which animator to process end action
        boolean processEndOnFirstAnimation;
        AnimatorSet endProcessAnimator;
        if(mWorkspaceFirstChild == null)
            processEndOnFirstAnimation = false;
        else if(mWorkspaceSecondChild == null)
            processEndOnFirstAnimation = true;
        else{
            processEndOnFirstAnimation = mFirstAnimatorSetBack.getTotalDuration() > mSecondAnimatorSetBack.getTotalDuration();
        }

        if(processEndOnFirstAnimation)
            endProcessAnimator = mFirstAnimatorSetBack;
        else
            endProcessAnimator = mSecondAnimatorSetBack;

        if(mWorkspaceFirstChild != null){
            mFirstAnimatorSetBack.start();
        }
        if(mWorkspaceSecondChild != null){
            mSecondAnimatorSetBack.start();
        }

        //Do end action
        endProcessAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(mWorkspaceFirstChild != null) {
                    mAnimateFirstView.initializeProperties();
                }
                if(mWorkspaceSecondChild != null) {
                    mAnimateSecondView.initializeProperties();
                }

                //Move the child view to original parent
                if(mWorkspaceFirstChild != null && mWorkspaceSecondChild != null){
                    if(isLastPageAndEmpty) {
                        mWorkspaceSecondChild.removeChildView(mAnimateFirstView);
                        mWorkspaceFirstChild.addView(mAnimateFirstView);
                    }
                    else {
                        mWorkspaceFirstChild.removeChildView(mAnimateSecondView);
                        mWorkspaceSecondChild.addView(mAnimateSecondView);
                    }
                }
                mWorkspace.showNeighbors();
            }

            @Override
            public void onAnimationStart(Animator animation) { }
            @Override
            public void onAnimationCancel(Animator animation) { }
            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
    }

    public boolean isPreviewAnimationRunning(){
        if(mFirstAnimatorSetForward != null && mFirstAnimatorSetForward.isRunning())
            return true;
        if(mSecondAnimatorSetForward != null && mSecondAnimatorSetForward.isRunning())
            return true;
        if(mFirstAnimatorSetBack != null && mFirstAnimatorSetBack.isRunning())
            return true;
        if(mSecondAnimatorSetBack != null && mSecondAnimatorSetBack.isRunning())
            return true;
        if(mPlayBackAnimationStartDelaying)
            return true;
        return false;
    }
}
