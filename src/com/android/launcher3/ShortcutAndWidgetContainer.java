/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static android.view.MotionEvent.ACTION_DOWN;
import static com.android.launcher3.CellLayout.CAMERA_DISTANCE_SCALE;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import android.view.ViewTreeObserver;
import ch.deletescape.lawnchair.LawnchairPreferences;
import ch.deletescape.lawnchair.settings.ui.SettingsActivity;
import com.android.launcher3.CellLayout.ContainerType;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderPagedView;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import org.jetbrains.annotations.NotNull;

public class ShortcutAndWidgetContainer extends ViewGroup implements LawnchairPreferences.OnPreferenceChangeListener {
    static final String TAG = "ShortcutAndWidgetContainer";
    public static final int MAX_COUNT_HOTSEAT = 5;
    private static final int HOTSEAT_PADDING = 5;

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private final int[] mTmpCellXY = new int[2];

    @ContainerType private final int mContainerType;
    private final WallpaperManager mWallpaperManager;

    private int mCellWidth;
    private int mCellHeight;

    private int mCountX;

    private final Launcher mLauncher;
    private boolean mInvertIfRtl = false;

    private final LawnchairPreferences mPrefs;

    //Values for transition effects
    private float xFraction = 0;
    private ViewTreeObserver.OnPreDrawListener mPreDrawListener = null;

    public ShortcutAndWidgetContainer(Context context, @ContainerType int containerType) {
        super(context);
        mLauncher = Launcher.getLauncher(context);
        mWallpaperManager = WallpaperManager.getInstance(context);
        mContainerType = containerType;
        mPrefs = Utilities.getLawnchairPrefs(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPrefs.addOnPreferenceChangeListener(SettingsActivity.ALLOW_OVERLAP_PREF, this);
    }

    @Override
    public void onValueChanged(@NotNull String key, @NotNull LawnchairPreferences prefs, boolean force) {
        setClipChildren(!prefs.getAllowOverlap());
        setClipToPadding(!prefs.getAllowOverlap());
        setClipToOutline(!prefs.getAllowOverlap());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPrefs.removeOnPreferenceChangeListener(SettingsActivity.ALLOW_OVERLAP_PREF, this);
    }

    public void setCellDimensions(int cellWidth, int cellHeight, int countX, int countY) {
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
        mCountX = countX;
    }

    public View getChildAt(int x, int y) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

            if ((lp.cellX <= x) && (x < lp.cellX + lp.cellHSpan) &&
                    (lp.cellY <= y) && (y < lp.cellY + lp.cellVSpan)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSpecSize, heightSpecSize);

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child);
            }
        }
    }

    public void setupLp(View child) {
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        if (child instanceof LauncherAppWidgetHostView) {
            DeviceProfile profile = mLauncher.getDeviceProfile();
            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX,
                    profile.appWidgetScale.x, profile.appWidgetScale.y);
        } else {
            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX);
        }
    }

    // Set whether or not to invert the layout horizontally if the layout is in RTL mode.
    public void setInvertIfRtl(boolean invert) {
        mInvertIfRtl = invert;
    }

    public int getCellContentHeight() {
        return Math.min(getMeasuredHeight(),
                mLauncher.getDeviceProfile().getCellHeight(mContainerType));
    }

    public void measureChild(View child) {
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        final DeviceProfile profile = mLauncher.getDeviceProfile();

        if (child instanceof LauncherAppWidgetHostView) {
            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX,
                    profile.appWidgetScale.x, profile.appWidgetScale.y);
            // Widgets have their own padding
        } else {
            lp.setup(mCellWidth, mCellHeight, invertLayoutHorizontally(), mCountX);

            //Change start position for hotseat layout
            if(isHotseat()){
                int childCount = getChildCount();
                lp.x = lp.leftMargin + getHotSeatChildStartPos(getColumnIndex(lp.cellX, 0), mCellWidth, childCount);
            }
            // Center the icon/folder
            int cHeight = getCellContentHeight();
            int cellPaddingY = (int) Math.max(0, ((lp.height - cHeight) / 2f));
            int cellPaddingX = mContainerType == CellLayout.WORKSPACE
                    ? profile.workspaceCellPaddingXPx
                    : (int) (profile.edgeMarginPx / 2f);
            child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, 0);
        }
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        child.measure(childWidthMeasureSpec, childheightMeasureSpec);
    }

    public boolean invertLayoutHorizontally() {
        return mInvertIfRtl && Utilities.isRtl(getResources());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                if (child instanceof LauncherAppWidgetHostView) {
                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) child;

                    // Scale and center the widget to fit within its cells.
                    DeviceProfile profile = mLauncher.getDeviceProfile();
                    float scaleX = profile.appWidgetScale.x;
                    float scaleY = profile.appWidgetScale.y;

                    lahv.setScaleToFit(Math.min(scaleX, scaleY));
                    lahv.setTranslationForCentering(-(lp.width - (lp.width * scaleX)) / 2.0f,
                            -(lp.height - (lp.height * scaleY)) / 2.0f);
                }

                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                if (lp.dropped) {
                    lp.dropped = false;

                    final int[] cellXY = mTmpCellXY;
                    getLocationOnScreen(cellXY);
                    mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                            WallpaperManager.COMMAND_DROP,
                            cellXY[0] + childLeft + lp.width / 2,
                            cellXY[1] + childTop + lp.height / 2, 0, null);
                }
            }
        }

        //Set layout area again for hot seat layout
        if(isHotseat() && count > 0 && count <= 5) {
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if(child.getVisibility() != GONE) {
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                    int columnIndex = getColumnIndex(lp.cellX, 0);
                    int childLeft = getHotSeatChildStartPos(columnIndex, lp.width, getChildCount());
                    int childTop = lp.y;
                    child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == ACTION_DOWN && getAlpha() == 0) {
            // Dont let children handle touch, if we are not visible.
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    //functions added by rsj
    public boolean isHotseat() {
        return mContainerType == CellLayout.HOTSEAT;
    }

    public boolean isDesktop() {
        return mContainerType == CellLayout.WORKSPACE;
    }

    public boolean isFolder() {
        return getParent().getParent() instanceof FolderPagedView;
    }

    public int getColumnIndex(int cellX, int cellY) {
        int count = 0;
        for (int i = 0; i < cellX; i++){
            if(getChildAt(i, cellY) != null)
                count ++;
        }
        return count;
    }

    public static int getHotSeatChildStartPos(int columnIndex, int cellWidth, int childCount) {
        int screenWidth = cellWidth*MAX_COUNT_HOTSEAT;
        final float distanceCell;
        if(childCount < 5)
            distanceCell = (float)(screenWidth - cellWidth*(MAX_COUNT_HOTSEAT - 1) - 2*HOTSEAT_PADDING)/(MAX_COUNT_HOTSEAT - 1);
        else
            distanceCell = 0;
        final float cellStartPos = (screenWidth - cellWidth*childCount - distanceCell*(childCount - 1))/2;
        return (int)((cellWidth + distanceCell) * columnIndex + cellStartPos);
    }

    /**
     * Hotseat인 경우 빈 자리들을 없앤다.
     *
     * 실지 동작은 자식View의 LayoutParams의 cellX값을 순차대로 설정하는것이다.
     */
    public void removeEmptySpaceOnHotSeat() {
        if(!isHotseat())
            return;

        int childCount = getChildCount();
        int []columnIndexArr = new int[childCount];
        int i;

        for (i = 0; i < childCount; i ++) {
            columnIndexArr[i] = getColumnIndex(((CellLayout.LayoutParams)getChildAt(i).getLayoutParams()).cellX, 0);
        }
        for (i = 0; i < childCount; i ++) {
            View v = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
            lp.cellX = columnIndexArr[i];
        }
    }

    public boolean shouldDrawCheck() {
        if(!mLauncher.drawCheck())
            return false;
        if(isDesktop())
            return true;
        if(isFolder()) {
            View parent = (View) getParent().getParent().getParent().getParent();
            if(parent instanceof Folder){
                Folder folderParent = (Folder)parent;
                return folderParent.isInDesktop();
            }
        }
        return false;
    }

    //Property set for transition animation
    //Cube
    public void setCube(float fraction) {
        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
        setRotationY(90 * fraction);
        setPivotX(0);
        setPivotY(getHeight() / 2);
    }

    public void setCubeBack(float fraction) {
        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
        setRotationY(90 * fraction);
        setPivotY(getHeight() / 2);
        setPivotX(getWidth());
    }

    //Translation
    public void setXFraction(float fraction) {
        this.xFraction = fraction;
        if (getWidth() == 0) {
            if (mPreDrawListener == null) {
                mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(
                                mPreDrawListener);
                        setXFraction(xFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
            }
            return;
        }
        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
    }

    //Perspective
    public void setZoomSlideHorizontal(float fraction) {
        setTranslationX(getWidth() * fraction);
        setPivotX(getWidth() / 2);
        setPivotY(getHeight() / 2);
    }

    public void setLeftScale(float fraction) {
        setScaleX(fraction);
        setScaleY(fraction);
        setPivotX(0);
        setPivotY(getHeight() / 2);
    }

    //Cascade
    public void setCenterScale(float fraction) {
        setScaleX(fraction);
        setScaleY(fraction);
        setPivotX(getWidth() / 2);
        setPivotY(getHeight() / 2);
    }

    //Squeeze
    public void setAccordionPivotZero(float fraction) {
        setScaleX(fraction);
        setPivotX(0);
    }

    public void setAccordionPivotWidth(float fraction) {
        setScaleX(fraction);
        setPivotX(getWidth());
    }

    //Flip Over
    public void setTableHorizontalPivotZero(float fraction) {
        setRotationY(90 * fraction);
        setPivotX(0);
        setPivotY(getHeight() / 2);
    }

    public void setTableHorizontalPivotWidth(float fraction) {
        setRotationY(-90 * fraction);
        setPivotX(getWidth());
        setPivotY(getHeight() / 2);
    }

    //Rotate
    public void setHorizontalFullRotationY(float fraction){
        setRotationY(180 * fraction);
        setPivotX(getWidth()/2);

        setCameraDistance(getWidth() * CAMERA_DISTANCE_SCALE);
    }

    //Windmill
    public void setRotateDown(float fraction) {
        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
        setRotation(20 * fraction);
        setPivotY(getHeight());
        setPivotX(getWidth() / 2);
    }

    public void initializeProperties(){
        //Most animations end with changing translation, rotation, alpha values
        setTranslationX(0);

        setRotationX(0);
        setRotationY(0);
        setRotation(0);

        setScaleX(1);
        setScaleY(1);

        setAlpha(1);
    }
}
