
package com.android.launcher3.pageindicators;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.pageindicators.rd.PageIndicatorView;
import com.android.launcher3.pageindicators.rd.animation.type.AnimationType;

/**
 * Home화면의 Workspace공간에서 리용하는 페지표시기 {@link PageIndicator}
 */
public class WorkspacePageIndicator extends PageIndicatorView implements Insettable, PageIndicator {

    private final Launcher mLauncher;
    private final float mSmallPadding;  //축소되였을때의 padding
    private final float mBigPadding;    //확대되였을때의 padding
    private float mTranslationX = 0;    //Canvas 이동거리

    public WorkspacePageIndicator(Context context) {
        this(context, null);
    }

    public WorkspacePageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspacePageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mLauncher = Launcher.getLauncher(context);

        //확대/축소되였을때의 padding값들을 얻는다.
        mSmallPadding = getResources().getDimension(R.dimen.page_indicator_small_padding);
        mBigPadding = getResources().getDimension(R.dimen.page_indicator_big_padding);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setPadding(mSmallPadding);
    }

    @Override
    public void setInsets(Rect insets) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();

        if (grid.isVerticalBarLayout()) {
            Rect padding = grid.workspacePadding;
            lp.leftMargin = padding.left + grid.workspaceCellPaddingXPx;
            lp.rightMargin = padding.right + grid.workspaceCellPaddingXPx;
            lp.bottomMargin = padding.bottom;
        } else {
            lp.leftMargin = lp.rightMargin = 0;
            lp.gravity = Gravity.BOTTOM;
            lp.bottomMargin = grid.hotseatBarSizePx + insets.bottom;
        }
        setLayoutParams(lp);
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
    }

    @Override
    public void setActiveMarker(int activePage) {
        setSelection(activePage);
    }

    /**
     * 페지개수 설정
     * @param numMarkers 페지개수
     */
    @Override
    public void setMarkersCount(int numMarkers) {
        setCount(numMarkers);
        makeCenterLayout();
    }

    public void pauseAnimations() {
    }

    public void updateShow() {
        boolean show = Utilities.getLawnchairPrefs(getContext()).getDockShowPageIndicator();
        if(show)
            setVisibility(VISIBLE);
        else
            setVisibility(INVISIBLE);
    }

    public void setShouldAutoHide(boolean shouldAutoHide) {
    }

    public void skipAnimationsToEnd() {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Canvas를 mTranslationX만큼 수평으로 이동시킨다.
        canvas.translate(mTranslationX, 0);
        super.onDraw(canvas);
    }

    /**
     * 확대/축소 animation을 진행한다.
     * @param expand true: 확대, false: 축소
     * @param duration 지속시간
     */
    public void playAnimation(boolean expand, int duration){
        //Padding 값을 계산한다.
        float startPadding = expand? mSmallPadding : mBigPadding;
        float endPadding = expand? mBigPadding : mSmallPadding;
        ValueAnimator animator = ValueAnimator.ofFloat(startPadding, endPadding);

        //Option State(Home화면이 long click된 상태)에서는 마지막에 빈 페지가 창조되고
        //Normal상태로 넘어갈때 마지막페지가 없어지므로 이때는 페지개수를 하나 줄인다.
        if(!expand && mLauncher.getWorkspace().isLastPageEmpty()){
            setCount(getCount() - 1);
        }

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (Float)valueAnimator.getAnimatedValue();
                setPadding(value);
                makeCenterLayout();
            }
        });
        animator.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animator) {
                setAnimationType(AnimationType.NONE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                setAnimationType(AnimationType.WORM);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                setAnimationType(AnimationType.WORM);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        animator.setDuration(duration);
        animator.start();
    }

    /**
     * 페지표시기를 확대/축소시킨다.
     * @param expanded true: 확대, false: 축소
     */
    public void setExpanded(boolean expanded){
        //Padding을 주고 중심위치로 이동한다.
        float expand = expanded? mBigPadding : mSmallPadding;
        setPadding(expand);
        makeCenterLayout();
    }

    /**
     * View를 중심위치로 이동시켜준다.
     */
    public void makeCenterLayout(){
        //화면의 너비와 페지표시기의 너비를 계산한다.
        int radius = getRadius();
        int padding = getPadding();
        int count = getCount();
        int usedWidth = (radius+padding)*(count - 1) + radius;
        int entireWidth = getWidth();
        if(entireWidth == 0){
            entireWidth = getResources().getDisplayMetrics().widthPixels;
        }

        //이동거리를 계산하고 재그리기를 진행한다.
        if(count > 0){
            mTranslationX = (entireWidth - usedWidth)*0.5f;
            invalidate();
        }
    }
}