/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.folder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;

import android.view.ViewPropertyAnimator;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.PagedView;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace.ItemOperator;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.util.Thunk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FolderPagedView extends PagedView<PageIndicatorDots> {

    private static final String TAG = "FolderPagedView";

    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;

    /**
     * Fraction of the width to scroll when showing the next page hint.
     */
    private static final float SCROLL_HINT_FRACTION = 0.07f;

    private static final int[] sTmpArray = new int[2];

    public final boolean mIsRtl;

    private final LayoutInflater mInflater;
    private final ViewGroupFocusHelper mFocusIndicatorHelper;

    @Thunk final ArrayMap<View, Runnable> mPendingAnimations = new ArrayMap<>();

    @ViewDebug.ExportedProperty(category = "launcher")
    public final int mMaxItemsPerPage;

    private int mAllocatedContentSize;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountX;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountY;

    private Folder mFolder;

    public FolderPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mInflater = LayoutInflater.from(context);
        mIsRtl = Utilities.isRtl(getResources());
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        mFocusIndicatorHelper = new ViewGroupFocusHelper(this);

        //Get horizontal and vertical grid count
        mGridCountX = context.getResources().getInteger(R.integer.folder_grid_count_x);
        mGridCountY = context.getResources().getInteger(R.integer.folder_grid_count_y);

        mMaxItemsPerPage = mGridCountX * mGridCountY;
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
        mPageIndicator = folder.findViewById(R.id.folder_page_indicator);
        initParentViews(folder);
    }

    /**
     * Calculates the grid size such that {@param count} items can fit in the grid.
     * The grid size is calculated such that countY <= countX and countX = ceil(sqrt(count)) while
     * maintaining the restrictions of {@link #mMaxCountX} &amp; {@link #mMaxCountY}.
     *
     */
    public static void calculateGridSize(int count, int countX, int countY, int maxCountX,
            int maxCountY, int maxItemsPerPage, int[] out) {
        boolean done;
        int gridCountX = countX;
        int gridCountY = countY;

        if (count >= maxItemsPerPage) {
            gridCountX = maxCountX;
            gridCountY = maxCountY;
            done = true;
        } else {
            done = false;
        }

        while (!done) {
            int oldCountX = gridCountX;
            int oldCountY = gridCountY;
            if (gridCountX * gridCountY < count) {
                // Current grid is too small, expand it
                if ((gridCountX <= gridCountY || gridCountY == maxCountY)
                        && gridCountX < maxCountX) {
                    gridCountX++;
                } else if (gridCountY < maxCountY) {
                    gridCountY++;
                }
                if (gridCountY == 0) gridCountY++;
            } else if ((gridCountY - 1) * gridCountX >= count && gridCountY >= gridCountX) {
                gridCountY = Math.max(0, gridCountY - 1);
            } else if ((gridCountX - 1) * gridCountY >= count) {
                gridCountX = Math.max(0, gridCountX - 1);
            }
            done = gridCountX == oldCountX && gridCountY == oldCountY;
        }

        out[0] = gridCountX;
        out[1] = gridCountY;
    }

    /**
     * Sets up the grid size such that {@param count} items can fit in the grid.
     */
    public void setupContentDimensions(int count) {
        mAllocatedContentSize = count;
//        calculateGridSize(count, mGridCountX, mGridCountY, mMaxCountX, mMaxCountY, mMaxItemsPerPage,
//                sTmpArray);
//        mGridCountX = sTmpArray[0];
//        mGridCountY = sTmpArray[1];
//
//        // Update grid size
//        for (int i = getPageCount() - 1; i >= 0; i--) {
//            getPageAt(i).setGridSize(mGridCountX, mGridCountY);
//        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mFocusIndicatorHelper.draw(canvas);
        super.dispatchDraw(canvas);
    }

    /**
     * Binds items to the layout.
     */
    public void bindItems(ArrayList<ShortcutInfo> items) {
        ArrayList<View> icons = new ArrayList<>();
        for (ShortcutInfo item : items) {
            icons.add(createNewView(item));
        }
        arrangeChildren(icons, icons.size(), false);
    }

    public void allocateSpaceForRank(int rank) {
        ArrayList<View> views = new ArrayList<>(mFolder.getItemsInReadingOrder());
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
    }

    /**
     * Create space for a new item at the end, and returns the rank for that item.
     * Also sets the current page to the last page.
     */
    public int allocateRankForNewItem() {
        int rank = getItemCount();
        allocateSpaceForRank(rank);
        setCurrentPage(rank / mMaxItemsPerPage);
        return rank;
    }

    public View createAndAddViewForRank(ShortcutInfo item, int rank) {
        View icon = createNewView(item);
        allocateSpaceForRank(rank);
        addViewForRank(icon, item, rank);
        return icon;
    }

    //Same as above function createAndAddViewForRank except allocate space for rank
    public View createAndAddViewWithRank(ShortcutInfo item, int rank){
        View icon = createNewView(item);
        addViewForRank(icon, item, rank);
        return icon;
    }

    /**
     * Adds the {@param view} to the layout based on {@param rank} and updated the position
     * related attributes. It assumes that {@param item} is already attached to the view.
     */
    public void addViewForRank(View view, ShortcutInfo item, int rank) {
        int pagePos = rank % mMaxItemsPerPage;
        int pageNo = rank / mMaxItemsPerPage;

        item.rank = rank;
        item.cellX = pagePos % mGridCountX;
        item.cellY = pagePos / mGridCountX;

        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = item.cellX;
        lp.cellY = item.cellY;

        if(getPageAt(pageNo) == null)
            createAndAddNewPage();

        getPageAt(pageNo).addViewToCellLayout(
                view, -1, mFolder.mLauncher.getViewIdForItem(item), lp, true);
    }

    @SuppressLint("InflateParams")
    public View createNewView(ShortcutInfo item) {
        int layout = mFolder.isInAppDrawer() ? R.layout.all_apps_folder_application
                : R.layout.folder_application;
        final BubbleTextView textView = (BubbleTextView) mInflater.inflate(layout, null, false);
        textView.applyFromShortcutInfo(item);
        textView.setHapticFeedbackEnabled(false);
        textView.setOnClickListener(ItemClickHandler.INSTANCE);
        textView.setOnLongClickListener(mFolder);
        textView.setOnFocusChangeListener(mFocusIndicatorHelper);

        textView.setLayoutParams(new CellLayout.LayoutParams(
                item.cellX, item.cellY, item.spanX, item.spanY));
        return textView;
    }

    @Override
    public CellLayout getPageAt(int index) {
        return (CellLayout) getChildAt(index);
    }

    public CellLayout getCurrentCellLayout() {
        return getPageAt(getNextPage());
    }

    CellLayout createAndAddNewPage() {
        DeviceProfile grid = Launcher.getLauncher(getContext()).getDeviceProfile();
        CellLayout page = (CellLayout) mInflater.inflate(R.layout.folder_page, this, false);
        if (mFolder.isInAppDrawer()) {
            page.setCellDimensions(grid.allAppsFolderCellWidthPx, grid.allAppsFolderCellHeightPx);
        } else {
            page.setCellDimensions(grid.folderCellWidthPx, grid.folderCellHeightPx);
        }
        page.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        page.setInvertIfRtl(true);

        page.setGridSize(mGridCountX, mGridCountY);

        addView(page, -1, generateDefaultLayoutParams());
        return page;
    }

    @Override
    protected int getChildGap() {
        return getPaddingLeft() + getPaddingRight();
    }

    public void setFixedSize(int width, int height) {
        width -= (getPaddingLeft() + getPaddingRight());
        height -= (getPaddingTop() + getPaddingBottom());
        for (int i = getChildCount() - 1; i >= 0; i --) {
            ((CellLayout) getChildAt(i)).setFixedSize(width, height);
        }
    }

    public void removeItem(View v) {
        for (int i = getChildCount() - 1; i >= 0; i --) {
            getPageAt(i).removeView(v);
        }
    }

    public void removeAllItems(){
        for (int i = getChildCount() - 1; i >= 0; i --){
            getPageAt(i).removeAllAndSave();
        }
        super.removeAllViews();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mPageIndicator.setScroll(l, mMaxScrollX);
    }

    /**
     * Updates position and rank of all the children in the view.
     * It essentially removes all views from all the pages and then adds them again in appropriate
     * page.
     *
     * @param list the ordered list of children.
     * @param itemCount if greater than the total children count, empty spaces are left
     * at the end, otherwise it is ignored.
     *
     */
    public void arrangeChildren(ArrayList<View> list, int itemCount) {
        arrangeChildren(list, itemCount, true);
    }

    @SuppressLint("RtlHardcoded")
    private void arrangeChildren(ArrayList<View> list, int itemCount, boolean saveChanges) {
        ArrayList<CellLayout> pages = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout page = (CellLayout) getChildAt(i);
            page.removeAllViews();
            pages.add(page);
        }
        setupContentDimensions(itemCount);

        Iterator<CellLayout> pageItr = pages.iterator();
        CellLayout currentPage = null;

        int position = 0;
        int newX, newY, rank;

        FolderIconPreviewVerifier verifier = new FolderIconPreviewVerifier(
                Launcher.getLauncher(getContext()).getDeviceProfile().inv);
        rank = 0;
        for (int i = 0; i < itemCount; i++) {
            View v = list.size() > i ? list.get(i) : null;
            if (currentPage == null || position >= mMaxItemsPerPage) {
                // Next page
                if (pageItr.hasNext()) {
                    currentPage = pageItr.next();
                } else {
                    currentPage = createAndAddNewPage();
                }
                position = 0;
            }

            if (v != null) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                newX = position % mGridCountX;
                newY = position / mGridCountX;
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.cellX != newX || info.cellY != newY || info.rank != rank) {
                    info.cellX = newX;
                    info.cellY = newY;
                    info.rank = rank;
                    if (saveChanges) {
                        mFolder.mLauncher.getModelWriter().addOrMoveItemInDatabase(info,
                                mFolder.mInfo.id, 0, info.cellX, info.cellY);
                    }
                }
                lp.cellX = info.cellX;
                lp.cellY = info.cellY;
                currentPage.addViewToCellLayout(
                        v, -1, mFolder.mLauncher.getViewIdForItem(info), lp, true);

                if (verifier.isItemInPreview(rank) && v instanceof BubbleTextView) {
                    ((BubbleTextView) v).verifyHighRes();
                }
            }

            rank ++;
            position++;
        }

        // Remove extra views.
        boolean removed = false;
        while (pageItr.hasNext()) {
            removeView(pageItr.next());
            removed = true;
        }
        if (removed) {
            setCurrentPage(0);
        }

        setEnableOverscroll(getPageCount() > 1);

        // Update footer
        mPageIndicator.setVisibility(getPageCount() > 1 ? View.VISIBLE : View.GONE);
        // Set the gravity as LEFT or RIGHT instead of START, as START depends on the actual text.
    }

    public int getDesiredWidth() {
        return getPageCount() > 0 ?
                (getPageAt(0).getDesiredWidth() + getPaddingLeft() + getPaddingRight()) : 0;
    }

    public int getDesiredHeight()  {
        return  getPageCount() > 0 ?
                (getPageAt(0).getDesiredHeight() + getPaddingTop() + getPaddingBottom()) : 0;
    }

    public int getItemCount() {
        int lastPageIndex = getChildCount() - 1;
        if (lastPageIndex < 0) {
            // If there are no pages, nothing has yet been added to the folder.
            return 0;
        }
        return getPageAt(lastPageIndex).getShortcutsAndWidgets().getChildCount()
                + lastPageIndex * mMaxItemsPerPage;
    }

    /**
     * @return the rank of the cell nearest to the provided pixel position.
     */
    public int findNearestArea(int pixelX, int pixelY) {
        int pageIndex = getNextPage();
        CellLayout page = getPageAt(pageIndex);
        page.findNearestArea(pixelX, pixelY, 1, 1, sTmpArray);
        if (mFolder.isLayoutRtl()) {
            sTmpArray[0] = page.getCountX() - sTmpArray[0] - 1;
        }
        return Math.min(mAllocatedContentSize - 1,
                pageIndex * mMaxItemsPerPage + sTmpArray[1] * mGridCountX + sTmpArray[0]);
    }

    public View getFirstItem() {
        if (getChildCount() < 1) {
            return null;
        }
        ShortcutAndWidgetContainer currContainer = getCurrentCellLayout().getShortcutsAndWidgets();
        if (mGridCountX > 0) {
            return currContainer.getChildAt(0, 0);
        } else {
            return currContainer.getChildAt(0);
        }
    }

    public View getLastItem() {
        if (getChildCount() < 1) {
            return null;
        }
        ShortcutAndWidgetContainer currContainer = getCurrentCellLayout().getShortcutsAndWidgets();
        int lastRank = currContainer.getChildCount() - 1;
        if (mGridCountX > 0) {
            return currContainer.getChildAt(lastRank % mGridCountX, lastRank / mGridCountX);
        } else {
            return currContainer.getChildAt(lastRank);
        }
    }

    /**
     * Iterates over all its items in a reading order.
     * @return the view for which the operator returned true.
     */
    public View iterateOverItems(ItemOperator op) {
        for (int k = 0 ; k < getChildCount(); k++) {
            CellLayout page = getPageAt(k);
            for (int j = 0; j < page.getCountY(); j++) {
                for (int i = 0; i < page.getCountX(); i++) {
                    View v = page.getChildAt(i, j);
                    if ((v != null) && op.evaluate((ItemInfo) v.getTag(), v)) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    public String getAccessibilityDescription() {
        return getContext().getString(R.string.folder_opened, mGridCountX, mGridCountY);
    }

    /**
     * Sets the focus on the first visible child.
     */
    public void setFocusOnFirstChild() {
        View firstChild = getCurrentCellLayout().getChildAt(0, 0);
        if (firstChild != null) {
            firstChild.requestFocus();
        }
    }

    @Override
    protected void notifyPageSwitchListener(int prevPage) {
        super.notifyPageSwitchListener(prevPage);
        if (mFolder != null) {
            mFolder.updateTextViewFocus();
        }
    }

    /**
     * Scrolls the current view by a fraction
     */
    public void showScrollHint(int direction) {
        float fraction = (direction == Folder.SCROLL_LEFT) ^ mIsRtl
                ? -SCROLL_HINT_FRACTION : SCROLL_HINT_FRACTION;
        int hint = (int) (fraction * getWidth());
        int scroll = getScrollForPage(getNextPage()) + hint;
        int delta = scroll - getScrollX();
        if (delta != 0) {
            mScroller.setInterpolator(Interpolators.DEACCEL);
            mScroller.startScroll(getScrollX(), 0, delta, 0, Folder.SCROLL_HINT_DURATION);
            invalidate();
        }
    }

    public void clearScrollHint() {
        if (getScrollX() != getScrollForPage(getNextPage())) {
            snapToPage(getNextPage());
        }
    }

    /**
     * Finish animation all the views which are animating across pages
     */
    public void completePendingPageChanges() {
        if (!mPendingAnimations.isEmpty()) {
            ArrayMap<View, Runnable> pendingViews = new ArrayMap<>(mPendingAnimations);
            for (Map.Entry<View, Runnable> e : pendingViews.entrySet()) {
                e.getKey().animate().cancel();
                e.getValue().run();
            }
        }
    }

    public boolean rankOnCurrentPage(int rank) {
        int p = rank / mMaxItemsPerPage;
        return p == getNextPage();
    }

    @Override
    protected void onPageBeginTransition() {
        super.onPageBeginTransition();
        // Ensure that adjacent pages have high resolution icons
        verifyVisibleHighResIcons(getCurrentPage() - 1);
        verifyVisibleHighResIcons(getCurrentPage() + 1);
    }

    /**
     * Ensures that all the icons on the given page are of high-res
     */
    public void verifyVisibleHighResIcons(int pageNo) {
        CellLayout page = getPageAt(pageNo);
        if (page != null) {
            ShortcutAndWidgetContainer parent = page.getShortcutsAndWidgets();
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                BubbleTextView icon = ((BubbleTextView) parent.getChildAt(i));
                icon.verifyHighRes();
                // Set the callback back to the actual icon, in case
                // it was captured by the FolderIcon
                Drawable d = icon.getCompoundDrawables()[1];
                if (d != null) {
                    d.setCallback(icon);
                }
            }
        }
    }

    public int getAllocatedContentSize() {
        return mAllocatedContentSize;
    }

    /**
     * Reorders the items such that the {@param empty} spot moves to {@param target}
     */
    public void realTimeReorder(int empty, int target) {
        completePendingPageChanges();
        int delay = 0;
        float delayAmount = START_VIEW_REORDER_DELAY;

        // Animation only happens on the current page.
        int pageToAnimate = getNextPage();

        int pageT = target / mMaxItemsPerPage;
        int pagePosT = target % mMaxItemsPerPage;

        if (pageT != pageToAnimate) {
            Log.e(TAG, "Cannot animate when the target cell is invisible");
        }
        int pagePosE = empty % mMaxItemsPerPage;
        int pageE = empty / mMaxItemsPerPage;

        int startPos, endPos;
        int moveStart, moveEnd;
        int direction;

        if (target == empty) {
            // No animation
            return;
        } else if (target > empty) {
            // Items will move backwards to make room for the empty cell.
            direction = 1;

            // If empty cell is in a different page, move them instantly.
            if (pageE < pageToAnimate) {
                moveStart = empty;
                // Instantly move the first item in the current page.
                moveEnd = pageToAnimate * mMaxItemsPerPage;
                // Animate the 2nd item in the current page, as the first item was already moved to
                // the last page.
                startPos = 0;
            } else {
                moveStart = moveEnd = -1;
                startPos = pagePosE;
            }

            endPos = pagePosT;
        } else {
            // The items will move forward.
            direction = -1;

            if (pageE > pageToAnimate) {
                // Move the items immediately.
                moveStart = empty;
                // Instantly move the last item in the current page.
                moveEnd = (pageToAnimate + 1) * mMaxItemsPerPage - 1;

                // Animations start with the second last item in the page
                startPos = mMaxItemsPerPage - 1;
            } else {
                moveStart = moveEnd = -1;
                startPos = pagePosE;
            }

            endPos = pagePosT;
        }

        if ((endPos - startPos) * direction <= 0) {
            // No animation
            return;
        }

        CellLayout page = getPageAt(pageToAnimate);

        int maxItemCount = getItemCount() + mFolder.getDragCellCount();
        int dragCellCount = mFolder.getDragCellCount();

        List<Integer> oldIndexArray = new ArrayList<>();
        List<Integer> newIndexArray = new ArrayList<>();
        int i;

        List<View> viewList = new ArrayList<>();
        for (i = 0; i < maxItemCount; i ++){
            viewList.add(getChildViewByRank(i));
        }

        for (i = 0; i < empty; i ++){
            oldIndexArray.add(i);
        }
        for (i = 0; i < dragCellCount; i ++){
            oldIndexArray.add(-1);
        }
        for (i = empty; i < maxItemCount; i ++){
            oldIndexArray.add(i);
        }

        for (i = 0; i < target; i ++){
            newIndexArray.add(i);
        }
        for (i = 0; i < dragCellCount; i ++){
            newIndexArray.add(-1);
        }
        for (i = target; i < maxItemCount; i ++){
            newIndexArray.add(i);
        }

        for (i = 0; i < maxItemCount; i ++){
            View view = viewList.get(i);
            if(view == null) {
                continue;
            }

            ItemInfo itemInfo = (ItemInfo) view.getTag();
            int oldIndex = itemInfo.rank;
            int newIndex = newIndexArray.indexOf(oldIndexArray.get(i));

            if(itemInfo.rank != newIndex) {
                itemInfo.rank = newIndex;

                CellLayout cellLayout = (CellLayout) view.getParent().getParent();
                int oldPageNumber = oldIndex / mMaxItemsPerPage;
                int newPageNumber = newIndex / mMaxItemsPerPage;
                int newIndexInsidePage = newIndex % mMaxItemsPerPage;

                if(oldPageNumber == newPageNumber){
                    if (cellLayout.animateChildToPosition(view, newIndexInsidePage % mGridCountX,
                            newIndexInsidePage / mGridCountX,
                            REORDER_ANIMATION_DURATION, delay, true, true)) {
                        if(page == cellLayout) {
                            delay += delayAmount;
                            delayAmount *= VIEW_REORDER_DELAY_FACTOR;
                        }
                    }
                }

                else{
                    //Create new page if necessary
                    if(getPageAt(newPageNumber) == null)
                        createAndAddNewPage();

                    boolean pageDirection = newPageNumber > oldPageNumber;
                    Runnable endAction = new Runnable() {
                        final float oldTranslateX = view.getTranslationX();
                        @Override
                        public void run() {
                            mPendingAnimations.remove(view);
                            view.setTranslationX(oldTranslateX);
                            ((CellLayout) view.getParent().getParent()).removeView(view);
                            addViewForRank(view, (ShortcutInfo) view.getTag(), newIndex);
                        }
                    };

                    view.animate()
                            .translationXBy(pageDirection ? view.getWidth() : - view.getWidth())
                            .setDuration(REORDER_ANIMATION_DURATION)
                            .setStartDelay(delay)
                            .withEndAction(endAction);

                    mPendingAnimations.put(view, endAction);

                    if(page == cellLayout) {
                        delay += delayAmount;
                        delayAmount *= VIEW_REORDER_DELAY_FACTOR;
                    }
                }
            }
        }

        viewList.clear();
        oldIndexArray.clear();
        newIndexArray.clear();
    }


    View getChildViewByRank(int rank){
        int pageNo = rank / mMaxItemsPerPage;
        int posInPage = rank % mMaxItemsPerPage;
        int cellX = posInPage % mGridCountX;
        int cellY = posInPage / mGridCountX;

        CellLayout cellLayout = getPageAt(pageNo);
        if(cellLayout == null)
            return null;

        return cellLayout.getChildAt(cellX, cellY);
    }

    public void setCheckIconAlpha(float value){
        CellLayout currentPage = getPageAt(mCurrentPage);
        if(currentPage != null){
            ShortcutAndWidgetContainer shortcutAndWidgetContainer = currentPage.getShortcutsAndWidgets();
            for (int i = 0; i < shortcutAndWidgetContainer.getChildCount(); i ++){
                View child = shortcutAndWidgetContainer.getChildAt(i);
                if(child instanceof BubbleTextView){
                    BubbleTextView bubbleTextView = (BubbleTextView)child;
                    bubbleTextView.setCheckIconAlpha(value);
                }
            }
        }
    }

    public int getGridCountX(){
        return mGridCountX;
    }

    public int getGridCountY(){
        return mGridCountY;
    }

    public int itemsPerPage() {
        return mMaxItemsPerPage;
    }
}
