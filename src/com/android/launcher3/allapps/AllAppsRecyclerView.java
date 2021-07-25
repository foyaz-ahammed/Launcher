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
package com.android.launcher3.allapps;

import static android.view.View.MeasureSpec.UNSPECIFIED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import ch.deletescape.lawnchair.colors.ColorEngine.ColorResolver;
import ch.deletescape.lawnchair.colors.ColorEngine.ResolveInfo;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BaseRecyclerView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.allapps.alphabetsindexfastscrollrecycler.IndexFastScrollRecyclerSection;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider;
import com.android.launcher3.userevent.nano.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.android.launcher3.views.RecyclerViewFastScroller;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * A RecyclerView with custom fast scroll support for the all apps view.
 */
public class AllAppsRecyclerView extends BaseRecyclerView implements LogContainerProvider {

    private AlphabeticalAppsList mApps;
    public AllAppsFastScrollHelper mFastScrollHelper;
    private final int mNumAppsPerRow;

    // The specific view heights that we use to calculate scroll
    private SparseIntArray mViewHeights = new SparseIntArray();
    private SparseIntArray mCachedScrollPositions = new SparseIntArray();

    // The empty-search result background
    private AllAppsBackgroundDrawable mEmptySearchBackground;
    private int mEmptySearchBackgroundTopOffset;

    private float mSpringShift;
    private IndexFastScrollRecyclerSection mScroller;
    private Paint mPaint = new Paint();

    private int mFixedHeight = 0;

    //Values for drawing the apps heading label
    private String mHeadingAppsLabel;
    private int mHeadingLeftPadding;
    private int mHeadingTopPadding;
    private int mHeadingLabelHeight;
    private float mHeadingLabelTextSize;
    private char prevLetter = '#';

    public AllAppsRecyclerView(Context context) {
        this(context, null);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr);
        Resources res = getResources();
        mEmptySearchBackgroundTopOffset = res.getDimensionPixelSize(
                R.dimen.all_apps_empty_search_bg_top_offset);
        mNumAppsPerRow = LauncherAppState.getIDP(context).numColsDrawer;

        //Set values for drawing heading label
        mHeadingAppsLabel = getResources().getString(R.string.all_app_label).toUpperCase();
        mHeadingLabelHeight = (int)getResources().getDimension(R.dimen.header_label_height);
        mHeadingLeftPadding = (int)getResources().getDimension(R.dimen.header_left_padding);
        mHeadingTopPadding = (int)getResources().getDimension(R.dimen.header_top_padding);
        mHeadingLabelTextSize = getResources().getDimension(R.dimen.header_label_text_size);
    }

    public int getHeadingLabelHeight(){
        return mHeadingLabelHeight;
    }

    private int getNavigationBarHeight(){
        if(getParent() instanceof AllAppsContainerView)
            return ((AllAppsContainerView)getParent()).getNavBarHeight();
        return 0;
    }

    /**
     * Sets the list of apps in this view, used to determine the fastscroll position.
     */
    public void setApps(AlphabeticalAppsList apps, boolean usingTabs) {
        mApps = apps;
        mFastScrollHelper = new AllAppsFastScrollHelper(this, apps);
    }

    public AlphabeticalAppsList getApps() {
        return mApps;
    }

    private void updatePoolSize() {
        DeviceProfile grid = Launcher.getLauncher(getContext()).getDeviceProfile();
        RecyclerView.RecycledViewPool pool = getRecycledViewPool();
        int approxRows = (int) Math.ceil(grid.availableHeightPx / grid.allAppsIconSizePx);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_EMPTY_SEARCH, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_ALL_APPS_DIVIDER, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_SEARCH_MARKET, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.VIEW_TYPE_ICON, approxRows * mNumAppsPerRow);

        mViewHeights.clear();
        mViewHeights.put(AllAppsGridAdapter.VIEW_TYPE_ICON, grid.allAppsCellHeightPx);
        mViewHeights.put(AllAppsGridAdapter.VIEW_TYPE_FOLDER, grid.allAppsCellHeightPx);
    }

    /**
     * Scrolls this recycler view to the top.
     */
    public void scrollToTop() {
        // Ensure we reattach the scrollbar if it was previously detached while fast-scrolling
        if (mScrollbar != null) {
            mScrollbar.reattachThumbToScroll();
        }
        scrollToPosition(0);
    }

    @Override
    public void onDraw(Canvas c) {
        // Draw the background
        if (mEmptySearchBackground != null && mEmptySearchBackground.getAlpha() > 0) {
            mEmptySearchBackground.draw(c);
        }

        if(getChildAt(0) != null){
            int topPadding = getChildAt(0).getTop();
            int color = getResources().getColor(R.color.apps_heading_label_text_color);
            mPaint.setColor(color);
            mPaint.setTextSize(mHeadingLabelTextSize);
            c.drawText(mHeadingAppsLabel, mHeadingLeftPadding, topPadding + mHeadingTopPadding + mHeadingLabelTextSize - mHeadingLabelHeight, mPaint);

            //Draw the background
            color = getResources().getColor(R.color.all_apps_background_color);
            mPaint.setColor(color);
            mPaint.setAlpha(40);

            //Set rectangle height max + spring height
            c.drawRect(0, topPadding, getWidth(), getHeight() + 500, mPaint);
            mPaint.setAlpha(255);
        }

        super.onDraw(c);
    }

    public void drawIndexScroller(Canvas c){
        if (mScroller != null)
            mScroller.draw(c);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mEmptySearchBackground || super.verifyDrawable(who);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateEmptySearchBackgroundBounds();
        updatePoolSize();
    }

    @Override
    public void fillInLogContainerData(View v, ItemInfo info, Target target, Target targetParent) {
        if (mApps.hasFilter()) {
            targetParent.containerType = ContainerType.SEARCHRESULT;
        } else {
            targetParent.containerType = ContainerType.ALLAPPS;
        }
    }

    public void onSearchResultsChanged() {
        // Always scroll the view to the top so the user can see the changed results
        scrollToTop();

        if (mApps.hasNoFilteredResults()) {
            if (mEmptySearchBackground == null) {
                mEmptySearchBackground = DrawableFactory.get(getContext())
                        .getAllAppsBackground(getContext());
                mEmptySearchBackground.setAlpha(0);
                mEmptySearchBackground.setCallback(this);
                updateEmptySearchBackgroundBounds();
            }
            mEmptySearchBackground.animateBgAlpha(1f, 150);
        } else if (mEmptySearchBackground != null) {
            // For the time being, we just immediately hide the background to ensure that it does
            // not overlap with the results
            mEmptySearchBackground.setBgAlpha(0f);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        boolean result = super.onInterceptTouchEvent(e);
        if (!result && e.getAction() == MotionEvent.ACTION_DOWN
                && mEmptySearchBackground != null && mEmptySearchBackground.getAlpha() > 0) {
            mEmptySearchBackground.setHotspot(e.getX(), e.getY());
        }
        return result;
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    @Override
    public PositionThumbInfo scrollToPositionAtProgress(float touchFraction) {
        int rowCount = mApps.getNumAppRows();
        if (rowCount == 0) {
            return new PositionThumbInfo("", 0);
        }

        // Stop the scroller if it is scrolling
        stopScroll();

        // Find the fastscroll section that maps to this touch fraction
        List<AlphabeticalAppsList.FastScrollSectionInfo> fastScrollSections =
                mApps.getFastScrollerSections();
        AlphabeticalAppsList.FastScrollSectionInfo lastInfo = fastScrollSections.get(0);
        for (int i = 1; i < fastScrollSections.size(); i++) {
            AlphabeticalAppsList.FastScrollSectionInfo info = fastScrollSections.get(i);
            if (info.touchFraction > touchFraction) {
                break;
            }
            lastInfo = info;
        }

        // Update the fast scroll
        int scrollY = getCurrentScrollY();
        int availableScrollHeight = getAvailableScrollHeight();
        mFastScrollHelper.smoothScrollToSection(scrollY, availableScrollHeight, lastInfo.fastScrollToItem.position, lastInfo.sectionName, false);
        return new PositionThumbInfo(lastInfo.sectionName, lastInfo.color);
    }

    /**
     * App 이름들에서 주어진 문자를 첫문자로 포함하는것이 있는지 확인
     * @param firstLetter 첫문자
     * @return 있으면 true
     */
    public boolean foundLetterStarting(String firstLetter){
        char c1 = firstLetter.charAt(0);
        if(c1 == '#')
            return true;
        List<AppInfo> mAppInfoList = mApps.getFilteredApps();
            for(int i = 0; i < mAppInfoList.size(); i ++){
                String sectionName = mApps.getIndexer().computeSectionName(mAppInfoList.get(i).title);
                char c2 = sectionName.charAt(0);
                if(c2 == c1){
                    return true;
                }
//                if(c2 > c1)
//                    return false;
            }

        return false;
    }

    /**
     * 주어진 첫글자와 매칭되는 app 이름을 가진 위치로 Scroll 하는 함수
     * @param firstLetter 첫글자
     */
    public void scrollToLetter(String firstLetter){
        List<AppInfo> mAppInfoList = mApps.getFilteredApps();

        char c1 = firstLetter.charAt(0);
        boolean found = false;
        boolean scrollToTop = c1 == '#';

        int resultPosition = 0;
        String resultSectionName = "";
        if(scrollToTop){
            found = true;
            resultSectionName = "#";
        }
        else{
            for(int i = 0; i < mAppInfoList.size(); i ++){
                String sectionName = mApps.getIndexer().computeSectionName(mAppInfoList.get(i).title);
                char c2 = sectionName.charAt(0);
                if(c2 == c1){
                    found = true;
                    resultPosition = i;
                    resultSectionName = sectionName;
                    break;
                }
//                if(c2 > c1)
//                    break;
            }
        }

        if(!found)
            return;

        // Update the fast scroll
        int scrollY = getCurrentScrollY();
        int availableScrollHeight = getAvailableScrollHeight();
        mFastScrollHelper.smoothScrollToSection(scrollY, availableScrollHeight, resultPosition, resultSectionName, scrollToTop);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mUseIndexScroller)
            createIndexScroller();
    }

    public void createIndexScroller(){
        mScroller = new IndexFastScrollRecyclerSection(getContext(), this);

        AllAppsContainerView allAppsView = null;
        if(getParent() instanceof  AllAppsContainerView){
            allAppsView = (AllAppsContainerView) getParent();
        }
        else if(getParent().getParent() instanceof AllAppsContainerView){
            allAppsView = (AllAppsContainerView) getParent().getParent();
        }
        if(allAppsView != null){
            allAppsView.setIndexScroller(mScroller);
        }

        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
                if(!mScroller.isIndexing() && !mFastScrollHelper.isSmoothScrolling()) {
                    onUpdateScrollbar(dy);
                }
            }
        });
    }

    public IndexFastScrollRecyclerSection getIndexScroller(){
        return mScroller;
    }

    @Override
    public void onFastScrollCompleted() {
        super.onFastScrollCompleted();
        mFastScrollHelper.onFastScrollCompleted();
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                mCachedScrollPositions.clear();
            }
        });
        if(!mUseIndexScroller)
            mFastScrollHelper.onSetAdapter((AllAppsGridAdapter) adapter);
        if (mScroller != null)
            mScroller.setAdapter(adapter);
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        // No bottom fading edge.
        return 0;
    }

    @Override
    protected boolean isPaddingOffsetRequired() {
        return true;
    }

    @Override
    protected int getTopPaddingOffset() {
        return -getPaddingTop();
    }

    /**
     * Updates the bounds for the scrollbar.
     */
    @Override
    public void onUpdateScrollbar(int dy) {
        if (mApps == null) {
            return;
        }

        if(mUseIndexScroller){
            //Do not include the label height of prediction app row and all apps row
            int lineNumber = (getCurrentScrollY() - mHeadingLabelHeight*2) / getChildAt(0).getHeight() - 1;
            if(lineNumber < 0) {
                if (Locale.getDefault().toLanguageTag().startsWith("ko")) {
                    if (prevLetter >= 'A' && prevLetter <= 'Z')
                        mScroller.changeAlphabet('ㄱ');
                    prevLetter = '#';
                }
                mScroller.setSelectionByLetter('#');
            } else {
                int index = Math.max(lineNumber, 0) * mNumAppsPerRow;
                AppInfo info = mApps.getApps().get(index);
                char firstLetter = mApps.getIndexer().computeSectionName(info.title).charAt(0);
                if (Locale.getDefault().toLanguageTag().startsWith("ko")) {
                    if ((prevLetter >= 'ㄱ' && prevLetter <= 'ㅎ') && (firstLetter >= 'A' && firstLetter <= 'Z')) {
                        mScroller.changeAlphabet(firstLetter);
                    }
                    if ((firstLetter >= 'ㄱ' && firstLetter <= 'ㅎ') && ((prevLetter >= 'A' && prevLetter <= 'Z') || mScroller.mSections.length > 17)) {
                        mScroller.changeAlphabet(firstLetter);
                    }
                    prevLetter = firstLetter;
                }
                mScroller.setSelectionByLetter(firstLetter);
            }
            return;
        }

        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();

        // Skip early if there are no items or we haven't been measured
        if (items.isEmpty() || mNumAppsPerRow == 0) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        // Skip early if, there no child laid out in the container.
        int scrollY = getCurrentScrollY();
        if (scrollY < 0) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        // Only show the scrollbar if there is height to be scrolled
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int availableScrollHeight = getAvailableScrollHeight();
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        if (mScrollbar.isThumbDetached()) {
            if (!mScrollbar.isDraggingThumb()) {
                // Calculate the current scroll position, the scrollY of the recycler view accounts
                // for the view padding, while the scrollBarY is drawn right up to the background
                // padding (ignoring padding)
                int scrollBarY = (int)
                        (((float) scrollY / availableScrollHeight) * availableScrollBarHeight);

                int thumbScrollY = mScrollbar.getThumbOffsetY();
                int diffScrollY = scrollBarY - thumbScrollY;
                if (diffScrollY * dy > 0f) {
                    // User is scrolling in the same direction the thumb needs to catch up to the
                    // current scroll position.  We do this by mapping the difference in movement
                    // from the original scroll bar position to the difference in movement necessary
                    // in the detached thumb position to ensure that both speed towards the same
                    // position at either end of the list.
                    if (dy < 0) {
                        int offset = (int) ((dy * thumbScrollY) / (float) scrollBarY);
                        thumbScrollY += Math.max(offset, diffScrollY);
                    } else {
                        int offset = (int) ((dy * (availableScrollBarHeight - thumbScrollY)) /
                                (float) (availableScrollBarHeight - scrollBarY));
                        thumbScrollY += Math.min(offset, diffScrollY);
                    }
                    thumbScrollY = Math.max(0, Math.min(availableScrollBarHeight, thumbScrollY));
                    mScrollbar.setThumbOffsetY(thumbScrollY);
                    if (scrollBarY == thumbScrollY) {
                        mScrollbar.reattachThumbToScroll();
                    }
                } else {
                    // User is scrolling in an opposite direction to the direction that the thumb
                    // needs to catch up to the scroll position.  Do nothing except for updating
                    // the scroll bar x to match the thumb width.
                    mScrollbar.setThumbOffsetY(thumbScrollY);
                }
            }
        } else {
            synchronizeScrollBarThumbOffsetToViewScroll(scrollY, availableScrollHeight);
        }
    }

    @Override
    public int getScrollbarTrackHeight() {
        return mScrollbar.getHeight() - getScrollBarTop() - getPaddingBottom() - getNavigationBarHeight();
    }

    @Override
    public boolean supportsFastScrolling() {
        // Only allow fast scrolling when the user is not searching, since the results are not
        // grouped in a meaningful order
        return !mApps.hasFilter();
    }

    @Override
    public int getCurrentScrollY() {
        // Return early if there are no items or we haven't been measured
        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();
        if (items.isEmpty() || mNumAppsPerRow == 0 || getChildCount() == 0) {
            return -1;
        }

        // Calculate the y and offset for the item
        View child = getChildAt(0);
        int position = getChildPosition(child);
        if (position == NO_POSITION) {
            return -1;
        }
        return getPaddingTop() +
                getCurrentScrollY(position, getLayoutManager().getDecoratedTop(child));
    }

    public int getCurrentScrollY(int position, int offset) {
        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();
        AlphabeticalAppsList.AdapterItem posItem = position < items.size() ?
                items.get(position) : null;
        int y = mCachedScrollPositions.get(position, -1);
        if (y < 0) {
            y = 0;
            for (int i = 0; i < position; i++) {
                AlphabeticalAppsList.AdapterItem item = items.get(i);
                if (AllAppsGridAdapter.isIconViewType(item.viewType)) {
                    // Break once we reach the desired row
                    if (posItem != null && posItem.viewType == item.viewType &&
                            posItem.rowIndex == item.rowIndex) {
                        break;
                    }
                    // Otherwise, only account for the first icon in the row since they are the same
                    // size within a row
                    if (item.rowAppIndex == 0) {
                        y += mViewHeights.get(item.viewType, 0);
                    }
                } else {
                    // Rest of the views span the full width
                    int elHeight = mViewHeights.get(item.viewType);
                    if (elHeight == 0) {
                        ViewHolder holder = findViewHolderForAdapterPosition(i);
                        if (holder == null) {
                            holder = getAdapter().createViewHolder(this, item.viewType);
                            getAdapter().onBindViewHolder(holder, i);
                            holder.itemView.measure(UNSPECIFIED, UNSPECIFIED);
                            elHeight = holder.itemView.getMeasuredHeight();

                            getRecycledViewPool().putRecycledView(holder);
                        } else {
                            elHeight = holder.itemView.getMeasuredHeight();
                        }
                    }
                    y += elHeight;
                }
            }
            mCachedScrollPositions.put(position, y);
        }
        return y - offset;
    }

    /**
     * Returns the available scroll height:
     *   AvailableScrollHeight = Total height of the all items - last page height
     */
    @Override
    protected int getAvailableScrollHeight() {
        return getPaddingTop() + getCurrentScrollY(Objects.requireNonNull(getAdapter()).getItemCount(), 0)
                - getHeight() + getPaddingBottom();
    }

    public int getScrollBarTop() {
        return getResources().getDimensionPixelOffset(R.dimen.all_apps_header_top_padding);
    }

    public RecyclerViewFastScroller getScrollbar() {
        return mScrollbar;
    }

    /**
     * Updates the bounds of the empty search background.
     */
    private void updateEmptySearchBackgroundBounds() {
        if (mEmptySearchBackground == null) {
            return;
        }

        // Center the empty search background on this new view bounds
        int x = (getMeasuredWidth() - mEmptySearchBackground.getIntrinsicWidth()) / 2;
        int y = mEmptySearchBackgroundTopOffset;
        mEmptySearchBackground.setBounds(x, y,
                x + mEmptySearchBackground.getIntrinsicWidth(),
                y + mEmptySearchBackground.getIntrinsicHeight());
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onColorChange(@NotNull ResolveInfo resolveInfo) {

    }

    public void setScrollbarColor(ColorResolver colorResolver) {
        if(mUseIndexScroller)
            return;
        mScrollbar.setColor(colorResolver.resolveColor(), colorResolver.computeForegroundColor());
    }
}
