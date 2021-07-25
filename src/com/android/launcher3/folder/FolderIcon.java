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

package com.android.launcher3.folder;

import static com.android.launcher3.folder.ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;
import static com.android.launcher3.folder.PreviewItemManager.INITIAL_ITEM_ANIMATION_DURATION;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import ch.deletescape.lawnchair.LawnchairLauncher;
import ch.deletescape.lawnchair.LawnchairUtilsKt;
import ch.deletescape.lawnchair.gestures.BlankGestureHandler;
import ch.deletescape.lawnchair.gestures.GestureController;
import ch.deletescape.lawnchair.gestures.GestureHandler;
import ch.deletescape.lawnchair.gestures.RunnableGestureHandler;
import ch.deletescape.lawnchair.gestures.handlers.ViewSwipeUpGestureHandler;
import ch.deletescape.lawnchair.groups.DrawerFolderInfo;
import com.android.launcher3.Alarm;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.CheckLongPressHelper;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.FolderInfo.FolderListener;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.Launcher;
import com.android.launcher3.Launcher.OnResumeCallback;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.OnAlarmListener;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.SimpleOnStylusPressListener;
import com.android.launcher3.StylusEventHelper;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.badge.BadgeRenderer;
import com.android.launcher3.badge.FolderBadgeInfo;
import com.android.launcher3.dragndrop.BaseItemDragListener;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.graphics.BitmapInfo;
import com.android.launcher3.groupselect.GroupIconView;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 등록부아이콘
 *
 * 등록부에 있는 App 아이콘들을 현시한다.(최대 4개)
 * 등록부아이콘을 누르면 등록부({@link Folder}) View가 펼쳐진다.
 */
public class FolderIcon extends FrameLayout implements FolderListener, OnResumeCallback {
    @Thunk Launcher mLauncher;
    @Thunk Folder mFolder;
    private FolderInfo mInfo;
    @Thunk static boolean sStaticValuesDirty = true;

    private CheckLongPressHelper mLongPressHelper;
    private StylusEventHelper mStylusEventHelper;

    static final int DROP_IN_ANIMATION_DURATION = 400;

    // Flag whether the folder should open itself when an item is dragged over is enabled.
    public static final boolean SPRING_LOADING_ENABLED = true;

    // Delay when drag enters until the folder opens, in miliseconds.
    private static final int ON_OPEN_DELAY = 800;

    @Thunk BubbleTextView mFolderName;

    PreviewBackground mBackground = new PreviewBackground();
    private boolean mBackgroundIsVisible = true;

    FolderIconPreviewVerifier mPreviewVerifier;
    ClippedFolderIconLayoutRule mPreviewLayoutRule;
    private PreviewItemManager mPreviewItemManager;
    private PreviewItemDrawingParams mTmpParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    private List<BubbleTextView> mCurrentPreviewItems = new ArrayList<>();

    boolean mAnimating = false;
    private Rect mTempBounds = new Rect();

    private float mSlop;

    private Alarm mOpenAlarm = new Alarm();

    private FolderBadgeInfo mBadgeInfo = new FolderBadgeInfo();
    private BadgeRenderer mBadgeRenderer;
    private float mBadgeScale;
    private Point mTempSpaceForBadgeOffset = new Point();

    private GestureHandler mSwipeUpHandler;

    public boolean isCustomIcon = false;
    private boolean mIsTextVisible = true;

    private static final Property<FolderIcon, Float> BADGE_SCALE_PROPERTY
            = new Property<FolderIcon, Float>(Float.TYPE, "badgeScale") {
        @Override
        public Float get(FolderIcon folderIcon) {
            return folderIcon.mBadgeScale;
        }

        @Override
        public void set(FolderIcon folderIcon, Float value) {
            folderIcon.mBadgeScale = value;
            folderIcon.invalidate();
        }
    };

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FolderIcon(Context context) {
        super(context);
        init();
    }

    private void init() {
        mLongPressHelper = new CheckLongPressHelper(this);
        mStylusEventHelper = new StylusEventHelper(new SimpleOnStylusPressListener(this), this);
        mPreviewLayoutRule = new ClippedFolderIconLayoutRule();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mPreviewItemManager = new PreviewItemManager(this);
    }

    public static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group,
            FolderInfo folderInfo) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean error = INITIAL_ITEM_ANIMATION_DURATION >= DROP_IN_ANIMATION_DURATION;
        if (error) {
            throw new IllegalStateException("DROP_IN_ANIMATION_DURATION must be greater than " +
                    "INITIAL_ITEM_ANIMATION_DURATION, as sequencing of adding first two items " +
                    "is dependent on this");
        }

        DeviceProfile grid = launcher.getDeviceProfile();
        FolderIcon icon = (FolderIcon) LayoutInflater.from(group.getContext())
                .inflate(resId, group, false);

        icon.setClipToPadding(false);
        icon.mFolderName = icon.findViewById(R.id.folder_icon_name);
        icon.mFolderName.setText(folderInfo.getIconTitle());
        icon.mFolderName.setCompoundDrawablePadding(0);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) icon.mFolderName.getLayoutParams();
        if (folderInfo instanceof DrawerFolderInfo) {
            lp.topMargin = grid.allAppsIconSizePx + grid.allAppsIconDrawablePaddingPx;
            icon.mBackground = new PreviewBackground(true);
            ((DrawerFolderInfo) folderInfo).getAppsStore().registerFolderIcon(icon);
        } else {
            lp.topMargin = grid.iconSizePx + grid.iconDrawablePaddingPx;
        }

        icon.setTag(folderInfo);
        icon.setOnClickListener(ItemClickHandler.INSTANCE);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        icon.mBadgeRenderer = launcher.getDeviceProfile().mBadgeRenderer;
        icon.setContentDescription(launcher.getString(R.string.folder_name_format, folderInfo.title));
        Folder folder = Folder.fromXml(launcher);
        folder.setDragController(launcher.getDragController());
        folder.setFolderIcon(icon);
        folder.bind(folderInfo);
        icon.setFolder(folder);
        icon.setAccessibilityDelegate(launcher.getAccessibilityDelegate());

        folderInfo.addListener(icon);

        icon.setOnFocusChangeListener(launcher.mFocusHandler);
        icon.onIconChanged();
        return icon;
    }

    public void unbind() {
        if (mInfo != null) {
            mInfo.removeListener(this);
            mInfo.removeListener(mFolder);
            mInfo = null;
        }
    }

    @Override
    public void onIconChanged() {
        applySwipeUpAction(mInfo);
        setOnClickListener(mInfo.isCoverMode() ?
                ItemClickHandler.FOLDER_COVER_INSTANCE : ItemClickHandler.INSTANCE);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFolderName.getLayoutParams();
        DeviceProfile grid = mLauncher.getDeviceProfile();
        mFolderName.setTag(null);

        if (mInfo.useIconMode(mLauncher)) {
            lp.topMargin = 0;
            if (isInAppDrawer()) {
                mFolderName.setCompoundDrawablePadding(grid.allAppsIconDrawablePaddingPx);
            } else {
                mFolderName.setCompoundDrawablePadding(grid.iconDrawablePaddingPx);
            }

            isCustomIcon = true;

            if (mInfo.isCoverMode()) {
                ItemInfoWithIcon coverInfo = mInfo.getCoverInfo();
                mFolderName.setTag(coverInfo);
                mFolderName.applyIcon(coverInfo);
                applyCoverBadgeState(coverInfo, false);
            } else {
                BitmapInfo info = BitmapInfo.fromBitmap(
                        Utilities.drawableToBitmap(mInfo.getIcon(getContext())));
                mFolderName.applyIcon(info);
                mFolderName.applyBadgeState(mInfo, false);
            }
            mBackground.setStartOpacity(0f);
        } else {
            if (isInAppDrawer()) {
                lp.topMargin = grid.allAppsIconSizePx + grid.allAppsIconDrawablePaddingPx;
            } else {
                lp.topMargin = grid.iconSizePx + grid.iconDrawablePaddingPx;
            }
            mFolderName.setCompoundDrawablePadding(0);
            mFolderName.applyBadgeState(mInfo, false);

            isCustomIcon = false;
            mFolderName.clearIcon();
            mBackground.setStartOpacity(1f);
        }
        mFolderName.setText(mInfo.getIconTitle());
        requestLayout();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public Folder getFolder() {
        return mFolder;
    }

    private void setFolder(Folder folder) {
        mFolder = folder;
        mPreviewVerifier = new FolderIconPreviewVerifier(mLauncher.getDeviceProfile().inv);
        updatePreviewItems(false);
    }

    private boolean willAcceptItem(ItemInfo item) {
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) &&
                item != mInfo && !mFolder.isOpen());
    }

    public boolean acceptDrop(ItemInfo dragInfo) {
        return !mFolder.isDestroyed() && willAcceptItem(dragInfo);
    }

    public void addItem(ShortcutInfo item) {
        addItem(item, true);
    }

    public void addItem(ShortcutInfo item, boolean animate) {
        mInfo.add(item, animate);
    }

    public void removeItem(ShortcutInfo item, boolean animate) {
        mInfo.remove(item, animate);
    }

    public void onDragEnter(ItemInfo dragInfo) {
        if (mFolder.isDestroyed() || !willAcceptItem(dragInfo)) return;
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        CellLayout cl = (CellLayout) getParent().getParent();

        mBackground.animateToAccept(cl, lp.cellX, lp.cellY);
        mOpenAlarm.setOnAlarmListener(mOnOpenListener);
        if (SPRING_LOADING_ENABLED &&
                ((dragInfo instanceof AppInfo)
                        || (dragInfo instanceof ShortcutInfo)
                        || (dragInfo instanceof PendingAddShortcutInfo))) {
            mOpenAlarm.setAlarm(ON_OPEN_DELAY);
        }
    }

    OnAlarmListener mOnOpenListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            mFolder.beginExternalDrag();
            mFolder.animateOpen();
        }
    };

    public Drawable prepareCreateAnimation(final View destView) {
        return mPreviewItemManager.prepareCreateAnimation(destView);
    }

    public void performCreateAnimation(final ShortcutInfo destInfo, final View destView,
            final ShortcutInfo srcInfo, final DragView srcView, Rect dstRect,
            float scaleRelativeToDragLayer) {
        prepareCreateAnimation(destView);
        addItem(destInfo);
        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        mPreviewItemManager.createFirstItemAnimation(false /* reverse */, null)
                .start();

        // This will animate the dragView (srcView) into the new folder
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1,
                false /* itemReturnedOnFailedDrop */);
    }

    public void performDestroyAnimation(Runnable onCompleteRunnable) {
        // This will animate the final item in the preview to be full size.
        mPreviewItemManager.createFirstItemAnimation(true /* reverse */, onCompleteRunnable)
                .start();
    }

    public void onDragExit() {
        mBackground.animateToRest();
        mOpenAlarm.cancelAlarm();
    }

    private void onDrop(final ShortcutInfo item, View animateView, Rect finalRect,
            float scaleRelativeToDragLayer, int index,
            boolean itemReturnedOnFailedDrop) {
        item.cellX = -1;
        item.cellY = -1;

        // Typically, the animateView corresponds to the DragView; however, if this is being done
        // after a configuration activity (ie. for a Shortcut being dragged from AllApps) we
        // will not have a view to animate
        if (animateView != null) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            Rect from = new Rect();
            dragLayer.getViewRectRelativeToSelf(animateView, from);
            Rect to = finalRect;
            if (to == null && !isInAppDrawer()) {
                to = new Rect();
                Workspace workspace = mLauncher.getWorkspace();
                // Set cellLayout and this to it's final state to compute final animation locations
                workspace.setFinalTransitionTransform();
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to);
                // Finished computing final animation locations, restore current state
                setScaleX(scaleX);
                setScaleY(scaleY);
                workspace.resetTransitionTransform();
            }

            int numItemsInPreview = Math.min(MAX_NUM_ITEMS_IN_PREVIEW, index + 1);
            boolean itemAdded = false;
            if (itemReturnedOnFailedDrop || index >= MAX_NUM_ITEMS_IN_PREVIEW) {
                List<BubbleTextView> oldPreviewItems = new ArrayList<>(mCurrentPreviewItems);
                addItem(item, false);
                mCurrentPreviewItems.clear();
                mCurrentPreviewItems.addAll(getPreviewItems());

                if (!oldPreviewItems.equals(mCurrentPreviewItems)) {
                    for (int i = 0; i < mCurrentPreviewItems.size(); ++i) {
                        if (mCurrentPreviewItems.get(i).getTag().equals(item)) {
                            // If the item dropped is going to be in the preview, we update the
                            // index here to reflect its position in the preview.
                            index = i;
                        }
                    }

                    mPreviewItemManager.hidePreviewItem(index, true);
                    mPreviewItemManager.onDrop(oldPreviewItems, mCurrentPreviewItems, item);
                    itemAdded = true;
                } else {
                    removeItem(item, false);
                }
            }

            if (!itemAdded) {
                addItem(item);
            }

            if (isInAppDrawer()) {
                return;
            }
            int[] center = new int[2];
            float scale = getLocalCenterForIndex(index, numItemsInPreview, center);
            center[0] = (int) Math.round(scaleRelativeToDragLayer * center[0]);
            center[1] = (int) Math.round(scaleRelativeToDragLayer * center[1]);

            to.offset(center[0] - animateView.getMeasuredWidth() / 2,
                    center[1] - animateView.getMeasuredHeight() / 2);

            float finalAlpha = index < MAX_NUM_ITEMS_IN_PREVIEW ? 0.5f : 0f;

            float finalScale = scale * scaleRelativeToDragLayer;
            dragLayer.animateView(animateView, from, to, finalAlpha,
                    1, 1, finalScale, finalScale, DROP_IN_ANIMATION_DURATION,
                    Interpolators.DEACCEL_2, Interpolators.ACCEL_2,
                    null, DragLayer.ANIMATION_END_DISAPPEAR, null);

            mFolder.hideItem(item);

            if (!itemAdded) mPreviewItemManager.hidePreviewItem(index, true);
            final int finalIndex = index;
            postDelayed(new Runnable() {
                public void run() {
                    mPreviewItemManager.hidePreviewItem(finalIndex, false);
                    mFolder.showItem(item);
                    invalidate();
                }
            }, DROP_IN_ANIMATION_DURATION);
        } else {
            addItem(item);
        }
    }

    public void onDrop(DragObject d, boolean itemReturnedOnFailedDrop) {
        ShortcutInfo item;
        if (d.dragInfo instanceof AppInfo) {
            // Came from all apps -- make a copy
            item = ((AppInfo) d.dragInfo).makeShortcut();
        } else if (d.dragSource instanceof BaseItemDragListener){
            // Came from a different window -- make a copy
            item = new ShortcutInfo((ShortcutInfo) d.dragInfo);
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
        mFolder.notifyDrop();
        onDrop(item, d.dragView, null, 1.0f, mInfo.contents.size(),
                itemReturnedOnFailedDrop);
    }

    //No drag view, and has simply one parameter shortcut info
    public void onDrop(ShortcutInfo item, boolean itemReturnedFailedOnDrop){
        if(item != null){
            mFolder.notifyDrop();
            onDrop(item, null, null, 1.0f, mInfo.contents.size(), itemReturnedFailedOnDrop);
        }
    }

    public void onDrop(GroupIconView dragView, ShortcutInfo item, boolean itemReturnedFailedOnDrop){
        if(item != null){
            mFolder.notifyDrop();
            onDrop(item, dragView, null, 1.0f, mInfo.contents.size(), itemReturnedFailedOnDrop);
        }
    }

    public void setBadgeInfo(FolderBadgeInfo badgeInfo) {
        updateBadgeScale(mBadgeInfo.hasBadge(), badgeInfo.hasBadge());
        mBadgeInfo = badgeInfo;
    }

    public ShortcutInfo getCoverInfo() {
        return mInfo.getCoverInfo();
    }

    public void applyCoverBadgeState(ItemInfo itemInfo, boolean animate) {
        mFolderName.applyBadgeState(itemInfo, animate);
    }

    public ClippedFolderIconLayoutRule getLayoutRule() {
        return mPreviewLayoutRule;
    }

    /**
     * Sets mBadgeScale to 1 or 0, animating if wasBadged or isBadged is false
     * (the badge is being added or removed).
     */
    private void updateBadgeScale(boolean wasBadged, boolean isBadged) {
        float newBadgeScale = isBadged ? 1f : 0f;
        // Animate when a badge is first added or when it is removed.
        if ((wasBadged ^ isBadged) && isShown()) {
            createBadgeScaleAnimator(newBadgeScale).start();
        } else {
            mBadgeScale = newBadgeScale;
            invalidate();
        }
    }

    public Animator createBadgeScaleAnimator(float... badgeScales) {
        return ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, badgeScales);
    }

    public boolean hasBadge() {
        return mBadgeInfo != null && mBadgeInfo.hasBadge();
    }

    private float getLocalCenterForIndex(int index, int curNumItems, int[] center) {
        mTmpParams = mPreviewItemManager.computePreviewItemDrawingParams(
                Math.min(MAX_NUM_ITEMS_IN_PREVIEW, index), curNumItems, mTmpParams);

        mTmpParams.transX += mBackground.basePreviewOffsetX;
        mTmpParams.transY += mBackground.basePreviewOffsetY;

        float intrinsicIconSize = mPreviewItemManager.getIntrinsicIconSize();
        float offsetX = mTmpParams.transX + (mTmpParams.scale * intrinsicIconSize) / 2;
        float offsetY = mTmpParams.transY + (mTmpParams.scale * intrinsicIconSize) / 2;

        center[0] = Math.round(offsetX);
        center[1] = Math.round(offsetY);
        return mTmpParams.scale;
    }

    public void setFolderBackground(PreviewBackground bg) {
        mBackground = bg;
        mBackground.setInvalidateDelegate(this);
    }

    public void setBackgroundVisible(boolean visible) {
        mBackgroundIsVisible = visible;
        invalidate();
    }

    public PreviewBackground getFolderBackground() {
        return mBackground;
    }

    public PreviewItemManager getPreviewItemManager() {
        return mPreviewItemManager;
    }

    /**
     * 그리기 함수
     * @param canvas Canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mBackgroundIsVisible) {
            mPreviewItemManager.recomputePreviewDrawingParams();

            if (!mBackground.drawingDelegated() && !isCustomIcon) {
                mBackground.drawBackground(canvas);
            }
        } else if (!isCustomIcon || mInfo.container == Favorites.CONTAINER_HOTSEAT) return;

        if (isCustomIcon) {
            return;
        }

        if (mFolder == null) return;
        if (mFolder.getItemCount() == 0 && !mAnimating) return;

        final int saveCount = canvas.save();
        canvas.clipPath(mBackground.getClipPath());

        mPreviewItemManager.draw(canvas);

        canvas.restoreToCount(saveCount);

        if (!mBackground.drawingDelegated()) {
            mBackground.drawBackgroundStroke(canvas);
        }

        drawBadge(canvas);
    }

    public void drawBadge(Canvas canvas) {
        if ((mBadgeInfo != null && mBadgeInfo.hasBadge()) || mBadgeScale > 0) {
            int offsetX = mBackground.getOffsetX();
            int offsetY = mBackground.getOffsetY();
            int previewSize = (int) (mBackground.previewSize * mBackground.mScale);
            mTempBounds.set(offsetX, offsetY, offsetX + previewSize, offsetY + previewSize);

            // If we are animating to the accepting state, animate the badge out.
            float badgeScale = Math.max(0, mBadgeScale - mBackground.getScaleProgress());
            mTempSpaceForBadgeOffset.set(getWidth() - mTempBounds.right, mTempBounds.top);
            mBadgeRenderer.draw(canvas, mBackground.getBadgeColor(), mTempBounds,
                    badgeScale, mTempSpaceForBadgeOffset, mBadgeInfo.getNotificationCount());
        }
    }

    public void setTextVisible(boolean visible) {
        mIsTextVisible = visible;
        mFolderName.setTextVisibility(visible);
    }

    public boolean getTextVisible() {
        return mIsTextVisible;
    }

    /**
     * Returns the list of preview items displayed in the icon.
     */
    public List<BubbleTextView> getPreviewItems() {
        return getPreviewItemsOnPage(0);
    }

    /**
     * Returns the list of "preview items" on {@param page}.
     */
    public List<BubbleTextView> getPreviewItemsOnPage(int page) {
        mPreviewVerifier.setFolderInfo(mFolder.getInfo());

        List<BubbleTextView> itemsToDisplay = new ArrayList<>();
        List<BubbleTextView> itemsOnPage = mFolder.getItemsOnPage(page);
        int numItems = itemsOnPage.size();
        for (int rank = 0; rank < numItems; ++rank) {
            if (mPreviewVerifier.isItemInPreview(page, rank)) {
                itemsToDisplay.add(itemsOnPage.get(rank));
            }

            if (itemsToDisplay.size() == MAX_NUM_ITEMS_IN_PREVIEW) {
                break;
            }
        }
        return itemsToDisplay;
    }

    public void verifyHighRes() {
        int processedItemCount = 0;
        List<BubbleTextView> itemsOnPage = mFolder.getItemsOnPage(0);
        int numItems = itemsOnPage.size();
        for (int rank = 0; rank < numItems; ++rank) {
            if (mPreviewVerifier.isItemInPreview(0, rank)) {
                BubbleTextView item = itemsOnPage.get(rank);
                item.verifyHighRes(info -> {
                    item.reapplyItemInfo(info);
                    updatePreviewItems(false);
                    invalidate();
                });
                processedItemCount++;
            }

            if (processedItemCount == MAX_NUM_ITEMS_IN_PREVIEW) {
                break;
            }
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return mPreviewItemManager.verifyDrawable(who) || super.verifyDrawable(who);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mInfo.useIconMode(mLauncher) && isInAppDrawer()) {
            DeviceProfile grid = mLauncher.getDeviceProfile();
            int drawablePadding = grid.allAppsIconDrawablePaddingPx;

            Paint.FontMetrics fm = mFolderName.getPaint().getFontMetrics();
            int cellHeightPx = mFolderName.getIconSize() + drawablePadding +
                    (int) Math.ceil(fm.bottom - fm.top);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setPadding(getPaddingLeft(), (height - cellHeightPx) / 2, getPaddingRight(),
                    getPaddingBottom());
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onItemsChanged(boolean animate) {
        if (mInfo.isCoverMode()) {
            onIconChanged();
            mFolderName.setText(mInfo.getIconTitle());
        }
        updatePreviewItems(animate);
        invalidate();
        requestLayout();
    }

    public void updatePreviewItems(boolean animate) {
        mPreviewItemManager.updatePreviewItems(animate);
        mCurrentPreviewItems.clear();
        mCurrentPreviewItems.addAll(getPreviewItems());
    }

    @Override
    public void prepareAutoUpdate() {
    }

    @Override
    public void onAdd(ShortcutInfo item, int rank) {
        boolean wasBadged = mBadgeInfo.hasBadge();
        mBadgeInfo.addBadgeInfo(mLauncher.getBadgeInfoForItem(item));
        boolean isBadged = mBadgeInfo.hasBadge();
        updateBadgeScale(wasBadged, isBadged);
        invalidate();
        requestLayout();
    }

    @Override
    public void onRemove(ShortcutInfo item, boolean folderRemove) {
        boolean wasBadged = mBadgeInfo.hasBadge();
        mBadgeInfo.subtractBadgeInfo(mLauncher.getBadgeInfoForItem(item));
        boolean isBadged = mBadgeInfo.hasBadge();
        updateBadgeScale(wasBadged, isBadged);
        invalidate();
        requestLayout();
    }

    @Override
    public void onTitleChanged(CharSequence title) {
        mFolderName.setText(mInfo.getIconTitle());
        applySwipeUpAction(mInfo);
        setContentDescription(getContext().getString(R.string.folder_name_format, title));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP

        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            int left = getPaddingLeft();
            int right = getWidth() - getPaddingRight();
            int top = getPaddingTop();
            int bottom = getPaddingTop() + getWidth() + (int) (mFolderName.getTextSize());

            if (!(event.getX() >= left && event.getX() <= right && event.getY() >= top
                    && event.getY() <= bottom))
                return false;
        }

        boolean result = super.onTouchEvent(event);

        // Check for a stylus button press, if it occurs cancel any long press checks.
        if (mStylusEventHelper.onMotionEvent(event)) {
            mLongPressHelper.cancelLongPress();
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLongPressHelper.cancelLongPress();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!Utilities.pointInView(this, event.getX(), event.getY(), mSlop)) {
                    mLongPressHelper.cancelLongPress();
                }
                break;
        }

        Launcher launcher = LawnchairUtilsKt.getLauncherOrNull(getContext());
        if (launcher instanceof LawnchairLauncher && mSwipeUpHandler != null) {
            ((LawnchairLauncher) launcher).getGestureController()
                    .setSwipeUpOverride(mSwipeUpHandler, event.getDownTime());
        }

        return result;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mLongPressHelper.cancelLongPress();
    }

    public void removeListeners() {
        mInfo.removeListener(this);
        mInfo.removeListener(mFolder);
    }

    public void clearLeaveBehindIfExists() {
        if (isInAppDrawer()) {
            return;
        }
        ((CellLayout.LayoutParams) getLayoutParams()).canReorder = true;
        if (mInfo != null && mInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            CellLayout cl = (CellLayout) getParent().getParent();
            cl.clearFolderLeaveBehind();
        }
    }

    public void drawLeaveBehindIfExists() {
        if (isInAppDrawer()) {
            return;
        }
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        // While the folder is open, the position of the icon cannot change.
        lp.canReorder = false;
        if (mInfo != null && mInfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            CellLayout cl = (CellLayout) getParent().getParent();
            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
        }
    }

    public void onFolderClose(int currentPage) {
        mPreviewItemManager.onFolderClose(currentPage);
    }

    private void applySwipeUpAction(FolderInfo info) {
        if (info.isCoverMode()) {
            mSwipeUpHandler = new RunnableGestureHandler(getContext(), () -> ItemClickHandler.INSTANCE.onClick(this));
        } else {
            mSwipeUpHandler = GestureController.Companion.createGestureHandler(
                    getContext(), info.swipeUpAction, new BlankGestureHandler(getContext(), null));
        }
        if (mSwipeUpHandler instanceof BlankGestureHandler) {
            mSwipeUpHandler = null;
        } else {
            mSwipeUpHandler = new ViewSwipeUpGestureHandler(this, mSwipeUpHandler);
        }
    }

    private float mIconScale = 1f;

    public void setIconScale(float scale) {
        mIconScale = scale;
        invalidate();
    }

    public float getIconScale() {
        return mIconScale;
    }

    public static final Property<FolderIcon, Float> ICON_SCALE_PROPERTY =
            new Property<FolderIcon, Float>(Float.class, "iconScale") {
                @Override
                public Float get(FolderIcon icon) {
                    return icon.getIconScale();
                }

                @Override
                public void set(FolderIcon icon, Float scale) {
                    icon.setIconScale(scale);
                }
            };

    public boolean isInAppDrawer() {
        return mInfo instanceof DrawerFolderInfo;
    }

    public boolean isCoverMode() {
        return mInfo.isCoverMode();
    }

    public BubbleTextView getFolderName() {
        return mFolderName;
    }

    public void setStayPressed(boolean stayPressed) {
        mFolderName.setStayPressed(stayPressed);
    }

    @Override
    public void onLauncherResume() {
        // Reset the pressed state of icon that was locked in the press state while activity
        // was launching
        setStayPressed(false);
    }

    public void clearPressedBackground() {
        setStayPressed(false);
    }

    public void updateIconBadges(Set<PackageUserKey> updatedBadges, PackageUserKey tmpKey) {
        FolderBadgeInfo folderBadgeInfo = new FolderBadgeInfo();
        for (ShortcutInfo si : mInfo.contents) {
            folderBadgeInfo.addBadgeInfo(mLauncher.getBadgeInfoForItem(si));
        }
        setBadgeInfo(folderBadgeInfo);

        if (isCoverMode()) {
            ShortcutInfo coverInfo = getCoverInfo();
            if (tmpKey.updateFromItemInfo(coverInfo) &&
                    updatedBadges.contains(tmpKey)) {
                applyCoverBadgeState(coverInfo, true);
            }
        }
    }
}