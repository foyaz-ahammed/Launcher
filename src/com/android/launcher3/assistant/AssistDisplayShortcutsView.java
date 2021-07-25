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

package com.android.launcher3.assistant;
import static com.android.launcher3.assistant.AssistQuickAccess.SHORTCUTS_MIN_COUNT;
import static com.android.launcher3.assistant.AssistQuickAccess.SHORTCUTS_PER_ROW;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.Toast;
import ch.deletescape.lawnchair.adaptive.IconShape;
import ch.deletescape.lawnchair.adaptive.IconShapeManager;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import java.util.ArrayList;
import java.util.List;

public class AssistDisplayShortcutsView extends RelativeLayout implements View.OnTouchListener, View.OnLongClickListener {
    //Animators' duration
    private static final int DRAG_VIEW_ANIMATE_DURATION = 100;
    private static final int OTHER_VIEWS_ANIMATE_DURATION = 300;

    private static final int DRAG_VIEW_LONG_PRESS_DURATION = 200;

    //Elevation of dragview to always show dragview over other views, this value should be big
    private static final int DRAG_VIEW_ELEVATION = 1000;

    IconShape mIconShape;
    Path mIconShapePath;
    Paint mPaint = new Paint();

    float []mShapePositions = new float[SHORTCUTS_PER_ROW];
    int mShapeCount = SHORTCUTS_PER_ROW;

    Launcher mLauncher;
    LayoutInflater mLayoutInflater;
    AssistQuickAccess mQuickAccess;

    //ShortcutInfo List to display
    private List<ShortcutInfo> mShortcutInfoList = new ArrayList<>();
    private List<AppInfo> mAppInfoList = new ArrayList<>();
    private List<BubbleTextView> mShortcutViews = new ArrayList<>();

    private Rect []mViewsArea = new Rect[SHORTCUTS_PER_ROW];
    private int []mRankArr = new int[SHORTCUTS_PER_ROW];

    private int mItemStartPadding = 0;

    /*-- Dragging view information --*/
    boolean mDragStarted = false;
    BubbleTextView mDragView = null;

    //When dragging, max elevation is using, and when dragging ends, elevation should be recovered, so save original elevation value
    float mLastElevation = 0;

    int mEmptyCell = -1;
    Rect mEmptyCellRect = new Rect();
    int mOffsetX = 0;
    int mOffsetY = 0;

    //Move animator list
    List<Animator> mAnimatorList = new ArrayList<>();

    //Down position
    int mDownMotionX;
    int mDownMotionY;
    int mCurrentMotionX;
    int mCurrentMotionY;

    public AssistDisplayShortcutsView(Context context) {
        this(context, null);
    }

    public AssistDisplayShortcutsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistDisplayShortcutsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = Launcher.getLauncher(context);
        mLayoutInflater = LayoutInflater.from(context);

        mIconShape = IconShapeManager.Companion.getInstance(context).getIconShape();
        mIconShapePath = mIconShape.getMaskPath();

        //Initialize viewsArea, rank array
        for (int i = 0; i < SHORTCUTS_PER_ROW; i ++) {
            mRankArr[i] = i;
            mViewsArea[i] = new Rect();
        }
    }

    public void setQuickAccessView(AssistQuickAccess parent){
        mQuickAccess = parent;
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
                setShapePositions(getWidth());
            }
        });
    }

    public void setShortcutInfoList(List<ShortcutInfo> shortcutInfos){
        mShortcutInfoList = shortcutInfos;
        resetShortcutViews();
    }

    public void setAppInfoList(List<AppInfo> appInfos){
        mAppInfoList = appInfos;
    }

    public String getAppName(ShortcutInfo shortcutInfo){
        for (int i = 0; i < mAppInfoList.size(); i ++){
            AppInfo appInfo = mAppInfoList.get(i);
            if(appInfo.getPackageName().equals(shortcutInfo.getPackageName()))
                return appInfo.title.toString();
        }
        return "";
    }

    @SuppressLint("ClickableViewAccessibility")
    public void resetShortcutViews(){
        //Remove from parent, and clear the shortcut views
        for (int i = 0; i < mShortcutViews.size(); i ++){
            BubbleTextView child = mShortcutViews.get(i);
            if(child != null && child.getParent() != null && child.getParent() == this){
                removeView(child);
            }
        }
        mShortcutViews.clear();

        //Then create views, and add
        for (int i = 0; i < mShortcutInfoList.size(); i ++){
            BubbleTextView child = (BubbleTextView) mLayoutInflater.inflate(R.layout.folder_icon_to_add, this, false);
            child.setTextColor(Color.WHITE);

            ShortcutInfo info = mShortcutInfoList.get(i);
            child.getLayoutParams().width = mLauncher.getDeviceProfile().cellWidthPx;
            child.getLayoutParams().height = mLauncher.getDeviceProfile().folderCellHeightPx + 100;
            child.setClickable(true);
            child.applyFromShortcutInfo(info);
            child.setQuickAccessAppName(getAppName(info));

            //Use minus button to show
            child.setIconFromQuickAccess(true, false);

            int finalI = i;
            child.setOnClickListener(v -> {
                removeItem(finalI);
            });
            child.setLongPressTimeout(DRAG_VIEW_LONG_PRESS_DURATION);
            child.setOnLongClickListener(this);
            child.setOnTouchListener(this);

            addView(child);
            mShortcutViews.add(child);
        }

        //Then set layout position of each shortcut views
        requestLayout();
    }

    public void addItem(ShortcutInfo info){
        ShortcutInfo clonedInfo = new ShortcutInfo(info);
        mShortcutInfoList.add(clonedInfo);

        resetShortcutViews();
    }

    public void removeItem(int i){
        //If the remaining item count is min count, do not remove and make a toast
        if(getDisplayingItemCount() <= SHORTCUTS_MIN_COUNT){
            Toast toast = Toast.makeText(getContext(),
                    getContext().getResources().getString(R.string.shortcut_count_min_warn),
                    Toast.LENGTH_SHORT);

            toast.show();
            return;
        }

        BubbleTextView child = mShortcutViews.get(i);
        if(child == null)
            return;

        mShortcutInfoList.remove(i);
        resetShortcutViews();

        ShortcutInfo info = (ShortcutInfo)(child.getTag());
        mQuickAccess.addItemToAllShortcutView(info);
    }

    public int getDisplayingItemCount(){
        return mShortcutViews.size();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        //Child width including padding
        float childWidthWithPadding = (getWidth() + 0f) / SHORTCUTS_PER_ROW;

        for (int i = 0; i < mShortcutViews.size(); i ++){
            BubbleTextView textView = mShortcutViews.get(i);

            int width = textView.getLayoutParams().width;
            int height = textView.getLayoutParams().height;
            int startPadding = (int)((childWidthWithPadding - width)/2);

            if(mItemStartPadding != startPadding) {
                mItemStartPadding = startPadding;
                mQuickAccess.setCommonPadding(mItemStartPadding);
            }

            int left = (int)(childWidthWithPadding * i) + startPadding;
            int top = 0;

            mViewsArea[i].set(left, top, left + width, top + height);
            textView.layout(left, top, left + width, top + height);
        }
    }

    public List<ShortcutInfo> getShortcutInfoList(){
        return mShortcutInfoList;
    }

    public void setShapePositions(int width){
        float childWidth = (width + 0f) / SHORTCUTS_PER_ROW;

        RectF rectF = new RectF();
        mIconShapePath.computeBounds(rectF, true);

        float shapeSize = rectF.width();
        float startPadding = (childWidth - shapeSize) / 2;

        for (int i = 0; i < mShapeCount; i ++){
            mShapePositions[i] = i * childWidth + startPadding;
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);

        int outlineColor = mLauncher.getResources().getColor(R.color.quick_access_shortcuts_outline_color);
        mPaint.setColor(outlineColor);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(5);
        mPaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 100));

        int drawablePadding = mLauncher.getDeviceProfile().iconDrawablePaddingPx;
        int showedItemCount = mShortcutViews.size();
        for (int i = showedItemCount; i < mShapeCount; i ++){
            canvas.save();
            canvas.translate(mShapePositions[i], drawablePadding);
            canvas.drawPath(mIconShapePath, mPaint);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v instanceof BubbleTextView){
            BubbleTextView childView = (BubbleTextView)v;
            int action = event.getAction();

            if(action == MotionEvent.ACTION_DOWN){
                mDragView = childView;

                int []pos = new int[2];
                Utilities.getDescendantCoordRelativeToAncestor(mDragView, this, pos, false);
                mDownMotionX = (int) (pos[0] + event.getX());
                mDownMotionY = (int) (pos[1] + event.getY());

                mCurrentMotionX = mDownMotionX;
                mCurrentMotionY = mDownMotionY;
            }

            else if(action == MotionEvent.ACTION_UP){
                mDragStarted = false;
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        int action = event.getAction();

        switch (action){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                return super.onInterceptTouchEvent(event);
            case MotionEvent.ACTION_MOVE:
                return true;
        }

        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(!mDragStarted)
            return super.onTouchEvent(event);

        int x = (int) event.getX();
        int y = (int) event.getY();

        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_MOVE:
                if(x != mCurrentMotionX || y != mCurrentMotionY){
                    mCurrentMotionX = x;
                    mCurrentMotionY = y;
                    moveDragView();

                    //Calculate
                    int foundCell = findNearestCell();
                    if(foundCell != -1 && foundCell != mEmptyCell){
                        //Update rank array
                        int[] tempRankArr = new int[SHORTCUTS_PER_ROW];
                        refreshRank(foundCell, tempRankArr);

                        //Initialize animator list
                        mAnimatorList.clear();

                        //Move views
                        for (int i = 0; i < mShortcutViews.size(); i ++){
                            if(mRankArr[i] != tempRankArr[i])
                            {
                                BubbleTextView childView = mShortcutViews.get(i);
                                if(childView != mDragView) {
                                    int rankedIndex = findInArray(i, tempRankArr);
                                    if (rankedIndex != -1) {
                                        int calculatedTranslationX =
                                                mViewsArea[rankedIndex].left - mViewsArea[i].left;

                                        //Create animators, and add to list
                                        Animator animator = ObjectAnimator
                                                .ofFloat(childView, "translationX",
                                                        calculatedTranslationX);
                                        animator.setDuration(OTHER_VIEWS_ANIMATE_DURATION);
                                        mAnimatorList.add(animator);
                                    }
                                }
                            }
                        }

                        //Play animations of the animator list
                        for (int i = 0; i < mAnimatorList.size(); i ++)
                        {
                            Animator animator = mAnimatorList.get(i);
                            if(animator != null)
                                animator.start();
                        }

                        mEmptyCell = foundCell;
                        mRankArr = tempRankArr.clone();

                        //Then copy all values of tempRankArr to rankArr
                        System.arraycopy(tempRankArr, 0, mRankArr, 0, SHORTCUTS_PER_ROW);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if(mDragStarted){
                    int dragViewIndex = getIndexOfDragView();
                    int rankedIndex = findInArray(dragViewIndex, mRankArr);
                    int calculatedTranslationX =
                            mViewsArea[rankedIndex].left - mViewsArea[dragViewIndex].left;

                    //Create drag view animator

                    final float oldTransX = mDragView.getTranslationX();
                    final float oldTransY = mDragView.getTranslationY();
                    final float newTransX = calculatedTranslationX;
                    final float newTransY = 0;

                    ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
                    animator.addUpdateListener(new AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float value = (float)animation.getAnimatedValue();
                            float transX = oldTransX + (newTransX - oldTransX) * value;
                            float transY = oldTransY + (newTransY - oldTransY) * value;

                            mDragView.setTranslationX(transX);
                            mDragView.setTranslationY(transY);
                        }
                    });

                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            doEndAction();
                        }
                    });

                    animator.setDuration(DRAG_VIEW_ANIMATE_DURATION);
                    animator.start();
                    mAnimatorList.add(animator);

                    mDragStarted = false;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mDragStarted = false;
                break;
        }

        return true;
    }

    @Override
    public boolean onLongClick(View v) {
        if(v instanceof BubbleTextView){
            BubbleTextView childView = (BubbleTextView)v;
            mDragStarted = true;
            mDragView = childView;

            //Get drag view position, set it as empty cell index},
            mEmptyCell = getIndexOfDragView();
            if(mEmptyCell != -1){
                mEmptyCellRect.set(mViewsArea[mEmptyCell]);
                mOffsetX = mDownMotionX - mEmptyCellRect.left;
                mOffsetY = mDownMotionY - mEmptyCellRect.top;
            }

            //End all running animations
            for (int i = 0; i < mAnimatorList.size(); i ++)
            {
                Animator animator = mAnimatorList.get(i);
                if(animator != null && animator.isRunning())
                    animator.end();
            }

            //Save original elevation, and set max elevation of dragview
            mLastElevation = mDragView.getElevation();
            mDragView.setElevation(DRAG_VIEW_ELEVATION);
        }
        return false;
    }

    private void doEndAction(){
        int i = 0;

        //Copy shortcuts with ranked order
        List<ShortcutInfo> tempShortcuts = new ArrayList<>();
        for (i = 0; i < mShortcutInfoList.size(); i ++){
            int value = mRankArr[i];
            ShortcutInfo info = mShortcutInfoList.get(value);
            tempShortcuts.add(info);
        }

        //And then reset shortcut info array with tempShortcuts
        mShortcutInfoList.clear();
        for (i = 0; i < tempShortcuts.size(); i ++){
            mShortcutInfoList.add(tempShortcuts.get(i));
        }
        tempShortcuts.clear();

        //Then reset rank array
        for (i = 0; i < SHORTCUTS_PER_ROW; i ++)
            mRankArr[i] = i;

        //Recover elevation of dragview
        mDragView.setElevation(mLastElevation);

        //Finally, reset shortcut views
        resetShortcutViews();
    }

    private void refreshRank(int newEmptyCell, int[] resultRankArr){
        int i = 0;

        if(newEmptyCell < mEmptyCell){
            for (i = 0; i < newEmptyCell; i ++)
                resultRankArr[i] = mRankArr[i];
            for (i = mEmptyCell + 1; i < SHORTCUTS_PER_ROW; i ++)
                resultRankArr[i] = mRankArr[i];

            resultRankArr[newEmptyCell] = mRankArr[mEmptyCell];
            for (i = newEmptyCell; i < mEmptyCell; i ++)
                resultRankArr[i + 1] = mRankArr[i];
        }

        else{
            for (i = 0; i < mEmptyCell; i ++)
                resultRankArr[i] = mRankArr[i];
            for (i = newEmptyCell + 1; i < SHORTCUTS_PER_ROW; i ++)
                resultRankArr[i] = mRankArr[i];

            resultRankArr[newEmptyCell] = mRankArr[mEmptyCell];
            for (i = newEmptyCell; i > mEmptyCell; i --)
                resultRankArr[i - 1] = mRankArr[i];
        }
    }

    private int findNearestCell(){
        int xDiff = mCurrentMotionX - mDownMotionX;
        int yDiff = mCurrentMotionY - mDownMotionY;
        Rect dragRect = new Rect(mEmptyCellRect.left + xDiff, mEmptyCellRect.top + yDiff, mEmptyCellRect.right + xDiff, mEmptyCellRect.bottom + yDiff);

        List<Integer> foundCells = new ArrayList<>();
        for (int i = 0; i < mShortcutViews.size(); i ++){
            //check the position is inside the view rect
            Rect rect = mViewsArea[i];
            if(haveCommonArea(dragRect, rect)) {
                foundCells.add(i);
            }
        }

        //If no cell found, return -1
        if(foundCells.size() == 0)
            return -1;

        //If one cell found, return it
        if(foundCells.size() == 1)
            return foundCells.get(0);

        /*-- Then get right one of found cells --*/
        //The cells are placed horizontally on one row, so found cell size is at most 2
        int firstIndex = foundCells.get(0);
        int secondIndex = foundCells.get(1);

        int firstCommonDistance = commonHorizontalDistance(mViewsArea[firstIndex], dragRect);
        int secondCommonDistance = commonHorizontalDistance(mViewsArea[secondIndex], dragRect);

        if(firstCommonDistance > secondCommonDistance)
            return firstIndex;
        return secondIndex;
    }

    public static int commonHorizontalDistance(Rect firstRect, Rect secondRect){
        if(firstRect.right >= secondRect.left && firstRect.right <= secondRect.right)
            return firstRect.right - secondRect.left;
        if(firstRect.left >= secondRect.left && firstRect.left <= secondRect.right)
            return secondRect.right - firstRect.left;
        return 0;
    }

    public static int findInArray(int value, int[] array){
        for (int i = 0; i < array.length; i ++) {
            if (array[i] == value)
                return i;
        }
        return -1;
    }

    public static boolean haveCommonArea(Rect firstRect, Rect secondRect){
        if(firstRect.right < secondRect.left)
            return false;
        if(firstRect.left > secondRect.right)
            return false;
        if(firstRect.bottom < secondRect.top)
            return false;
        if(firstRect.top > secondRect.bottom)
            return false;
        return true;
    }

    //Index of dragview
    private int getIndexOfDragView(){
        return getIndexOfView(mDragView);
    }

    public int getIndexOfView(BubbleTextView view){
        for (int i = 0; i < mShortcutViews.size(); i ++) {
            if (mShortcutViews.get(i) == view)
                return i;
        }

        return -1;
    }

    private void moveDragView(){
        int translationX = mCurrentMotionX - mDownMotionX;
        int translationY = mCurrentMotionY - mDownMotionY;

        if(mDragView != null) {
            mDragView.setTranslationX(translationX);
            mDragView.setTranslationY(translationY);
        }
    }
}
