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

import static android.view.Gravity.BOTTOM;
import static com.android.launcher3.LauncherAnimUtils.SPRING_LOADED_EXIT_DELAY;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.OPTIONS;
import static com.android.launcher3.compat.AccessibilityManagerCompat.sendCustomAccessibilityEvent;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.InputType;
import android.text.Selection;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import ch.deletescape.lawnchair.groups.DrawerFolderInfo;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Alarm;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.FolderInfo.FolderListener;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.OnAlarmListener;
import com.android.launcher3.PagedView;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace.ItemOperator;
import com.android.launcher3.accessibility.AccessibleDragListenerAdapter;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragController.DragListener;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.groupselect.GroupIconCell;
import com.android.launcher3.groupselect.GroupIconManager;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.userevent.nano.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.views.ClipPathView;
import com.android.launcher3.widget.PendingAddShortcutInfo;

import com.google.android.apps.nexuslauncher.CustomBottomSheet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 등록부아이콘({@link FolderIcon})을 눌러서 펼쳐지는 등록부 View
 *
 * @see R.layout#user_folder_icon_normalized
 */
public class Folder extends AbstractFloatingView implements DragSource,
        View.OnLongClickListener, DropTarget, FolderListener, TextView.OnEditorActionListener,
        View.OnFocusChangeListener, DragListener, ExtendedEditText.OnBackKeyListener,
        View.OnClickListener,
        ClipPathView {
    private static final String TAG = "Launcher.Folder";

    /**
     * We avoid measuring {@link #mContent} with a 0 width or height, as this
     * results in CellLayout being measured as UNSPECIFIED, which it does not support.
     */
    private static final int MIN_CONTENT_DIMEN = 5;

    //등록부의 여러가지 상태들
    static final int STATE_NONE = -1;       //초기상태(초기에 한번 리용된다.)
    static final int STATE_SMALL = 0;       //열렸다가 닫긴 상태
    static final int STATE_ANIMATING = 1;   //Animation진행중인 상태
    static final int STATE_OPEN = 2;        //열려있는 상태

    /**
     * Time for which the scroll hint is shown before automatically changing page.
     */
    public static final int SCROLL_HINT_DURATION = 500;
    public static final int RESCROLL_DELAY = PagedView.PAGE_SNAP_ANIMATION_DURATION + 150;
    private static final int SECOND_STEP_ANIMATION_DELAY = 300;

    public static final int SCROLL_NONE = -1;
    public static final int SCROLL_LEFT = 0;
    public static final int SCROLL_RIGHT = 1;

    /**
     * Fraction of icon width which behave as scroll region.
     */
    private static final float ICON_OVERSCROLL_WIDTH_FACTOR = 0.45f;

    private static final int FOLDER_NAME_ANIMATION_DURATION = 633;

    private static final int REORDER_DELAY = 0;
    private static final int ON_EXIT_CLOSE_DELAY = 400;
    private static final Rect sTempRect = new Rect();

    private static String sDefaultFolderName;
    private static String sHintText;

    private final Alarm mReorderAlarm = new Alarm();
    private final Alarm mOnExitAlarm = new Alarm();
    private final Alarm mOnScrollHintAlarm = new Alarm();
    @Thunk final Alarm mScrollPauseAlarm = new Alarm();

    @Thunk final ArrayList<View> mItemsInReadingOrder = new ArrayList<View>();

    private AnimatorSet mCurrentAnimator;

    protected final Launcher mLauncher;
    protected DragController mDragController;
    public FolderInfo mInfo;

    @Thunk FolderIcon mFolderIcon;

    @Thunk FolderPagedView mContent;
    public ExtendedEditText mFolderName;
    private PageIndicatorDots mPageIndicator;

    private View mFooter;
    private int mFooterHeight;
    private int mFolderNameHeight = 0;

    // Cell ranks used for drag and drop
    @Thunk int mTargetRank, mPrevTargetRank, mEmptyCellRank;

    boolean mFirstStepAnimationEnded = true;
    boolean mSecondStepAnimationEnded = true;
    boolean mFirstDrag = true;

    private int mRunningAnimatorCount = 0;

    @ViewDebug.ExportedProperty(category = "launcher",
            mapping = {
                    @ViewDebug.IntToString(from = STATE_NONE, to = "STATE_NONE"),
                    @ViewDebug.IntToString(from = STATE_SMALL, to = "STATE_SMALL"),
                    @ViewDebug.IntToString(from = STATE_ANIMATING, to = "STATE_ANIMATING"),
                    @ViewDebug.IntToString(from = STATE_OPEN, to = "STATE_OPEN"),
            })
    @Thunk int mState = STATE_NONE;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mRearrangeOnClose = false;
    boolean mItemsInvalidated = false;
    private View mCurrentDragView;
    private boolean mIsExternalDrag;
    private boolean mDragInProgress = false;
    private boolean mDeleteFolderOnDropCompleted = false;
    private boolean mSuppressFolderDeletion = false;
    private boolean mItemAddedBackToSelfViaIcon = false;
    @Thunk float mFolderIconPivotX;
    @Thunk float mFolderIconPivotY;
    private boolean mIsEditingName = false;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mDestroyed;

    @Thunk int mScrollHintDir = SCROLL_NONE;
    @Thunk int mCurrentScrollDir = SCROLL_NONE;

    public Path mClipPath;

    //Dragging cell count;
    private int mDragCellCount;

    //Rectangle area to store the initial position
    private Rect []mFolderIconPos = new Rect[1];

    //Linear layout containing paged view and footer
    View mMainFooterView;

    //Add Icon
    AddButtonBubbleTextView mAddButton = null;

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public Folder(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlwaysDrawnWithCacheEnabled(false);
        Resources res = getResources();

        if (sDefaultFolderName == null || sDefaultFolderName.isEmpty()) {
            sDefaultFolderName = res.getString(R.string.folder_name);
        }
        if (sHintText == null || sHintText.isEmpty()) {
            sHintText = res.getString(R.string.folder_hint_text);
        }
        mLauncher = Launcher.getLauncher(context);
        // We need this view to be focusable in touch mode so that when text editing of the folder
        // name is complete, we have something to focus on, thus hiding the cursor and giving
        // reliable behavior when clicking the text field (since it will always gain focus on click).
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.folder_content);
        mContent.setFolder(this);

        mPageIndicator = findViewById(R.id.folder_page_indicator);
        mFolderName = findViewById(R.id.folder_name);
        mFolderName.setOnBackKeyListener(this);
        mFolderName.setOnFocusChangeListener(this);

        if (!Utilities.ATLEAST_MARSHMALLOW) {
            // We disable action mode in older OSes where floating selection menu is not yet
            // available.
            mFolderName.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }

                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                public void onDestroyActionMode(ActionMode mode) {
                }

                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }
            });
        }
        mFolderName.setOnEditorActionListener(this);
        mFolderName.setSelectAllOnFocus(true);
        mFolderName.setInputType(mFolderName.getInputType()
                & ~InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                & ~InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mFolderName.forceDisableSuggestions(true);

        mFooter = findViewById(R.id.folder_footer);

        // We find out how tall footer wants to be (it is set to wrap_content), so that
        // we can allocate the appropriate amount of space for it.
        int measureSpec = MeasureSpec.UNSPECIFIED;
        mFooter.measure(measureSpec, measureSpec);
        mFooterHeight = mFooter.getMeasuredHeight();

        View settingsButton = findViewById(R.id.settings_button);
        if (Utilities.getLawnchairPrefs(mLauncher).getLockDesktop()) {
            settingsButton.setVisibility(View.GONE);
        } else {
            settingsButton.setOnClickListener(v -> {
                animateClosed();
                if (mInfo instanceof DrawerFolderInfo) {
                    ((DrawerFolderInfo) mInfo).showEdit(mLauncher);
                } else {
                    CustomBottomSheet.show(mLauncher, mInfo);
                }
            });
        }

        mMainFooterView = findViewById(R.id.main_footer);

        if(mFolderNameHeight == 0) {
            mFolderName.measure(0, 0);
            mFolderNameHeight = mFolderName.getMeasuredHeight();
        }
    }

    public boolean onLongClick(View v) {
        // Return if global dragging is not enabled
        if (!mLauncher.isDraggingEnabled()) return true;

        //Set text, and check icon invisible
        if(v instanceof BubbleTextView){
            ((BubbleTextView)v).setTextVisibility(false);
            ((BubbleTextView)v).setDragging(true);
        }

        return startDrag(v, new DragOptions());
    }

    public FolderPagedView getFolderPagedView(){
        return mContent;
    }
    public EditText getFolderNameView(){
        return mFolderName;
    }

    public boolean startDrag(View v, DragOptions options) {
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            ShortcutInfo item = (ShortcutInfo) tag;

            mEmptyCellRank = item.rank;
            mCurrentDragView = v;

            mDragController.addDragListener(this);
            if (options.isAccessibleDrag) {
                mDragController.addDragListener(new AccessibleDragListenerAdapter(
                        mContent, CellLayout.FOLDER_ACCESSIBILITY_DRAG) {

                    @Override
                    protected void enableAccessibleDrag(boolean enable) {
                        super.enableAccessibleDrag(enable);
                        mFooter.setImportantForAccessibility(enable
                                ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                                : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                    }
                });
            }


            //Remove the add button
            removeAddIcon();
            mLauncher.getWorkspace().beginDragShared(v, this, options);
        }

        return true;
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        if (dragObject.dragSource != this) {
            return;
        }

        if (isInAppDrawer()) {
            close(true);
        }
        mContent.removeItem(mCurrentDragView);

        if (dragObject.dragInfo instanceof ShortcutInfo) {
            mItemsInvalidated = true;

            // We do not want to get events for the item being removed, as they will get handled
            // when the drop completes
            try (SuppressInfoChanges s = new SuppressInfoChanges()) {

                //Remove from folder icon
                //First remove from folder
                for (int i = 0; i < mDragController.getGroupIconManager().getGroupIconCount(); i ++){
                    GroupIconCell iconCell = mDragController.getGroupIconManager().getGroupIconCell(i);
                    //internal drag
                    if(iconCell.isFolder() && iconCell.getFolder() == this) {
                        BubbleTextView view = iconCell.getCellView();
                        ShortcutInfo info = (ShortcutInfo) view.getTag();
                        View icon = getViewForInfo(info);
                        mContent.removeItem(icon);
                    }
                }

                //Then remove from folder icon
                for (int i = 0; i < mDragController.getGroupIconManager().getGroupIconCount(); i ++){
                    GroupIconCell iconCell = mDragController.getGroupIconManager().getGroupIconCell(i);
                    //internal drag
                    if(iconCell.isFolder() && iconCell.getFolder() == this) {
                        BubbleTextView view = iconCell.getCellView();
                        ShortcutInfo info = (ShortcutInfo) view.getTag();
                        mInfo.remove(info, true);
                    }
                }

                mInfo.remove((ShortcutInfo) dragObject.dragInfo, true);
            }
        }
        mDragInProgress = true;
        mItemAddedBackToSelfViaIcon = false;
    }

    @Override
    public void onDragEnd() {
        if (mIsExternalDrag && mDragInProgress) {
            completeDragExit();
        }
        mDragInProgress = false;
        mDragController.removeDragListener(this);

        mFirstDrag = true;

        //Insert the add button if the folder is open
        if(isOpen())
            insertAddIcon();
    }

    public boolean isEditingName() {
        return mIsEditingName;
    }

    public void startEditingFolderName() {
        post(new Runnable() {
            @Override
            public void run() {
                mFolderName.setHint("");
                mIsEditingName = true;
            }
        });
    }


    @Override
    public boolean onBackKey() {
        // Convert to a string here to ensure that no other state associated with the text field
        // gets saved.
        String newTitle = mFolderName.getText().toString();

        if(newTitle.isEmpty())
            newTitle = sDefaultFolderName;

        mInfo.setTitle(newTitle);
        mLauncher.getModelWriter().updateItemInDatabase(mInfo);

        sendCustomAccessibilityEvent(
                this, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                getContext().getString(R.string.folder_renamed, newTitle));

        // This ensures that focus is gained every time the field is clicked, which selects all
        // the text and brings up the soft keyboard if necessary.
        mFolderName.clearFocus();

        Selection.setSelection(mFolderName.getText(), 0, 0);
        mIsEditingName = false;

        //Set focus to invisible child(not hidden, alpha is just 0)
        findViewById(R.id.invisible_focus_layout).requestFocus();

        return true;
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            mFolderName.dispatchBackKey();
            return true;
        }
        return false;
    }

    public FolderIcon getFolderIcon() {
        return mFolderIcon;
    }

    @Override
    public void setClipPath(Path path) {
        mClipPath = path;
        invalidate();
    }


    public void setDragController(DragController dragController) {
        mDragController = dragController;
    }

    public void setFolderIcon(FolderIcon icon) {
        mFolderIcon = icon;
    }

    @Override
    protected void onAttachedToWindow() {
        // requestFocus() causes the focus onto the folder itself, which doesn't cause visual
        // effect but the next arrow key can start the keyboard focus inside of the folder, not
        // the folder itself.
        requestFocus();
        super.onAttachedToWindow();
        if (mFolderIcon != null && mFolderIcon.isCustomIcon && Utilities.getLawnchairPrefs(getContext()).getFolderBgColored()) {
            setBackgroundTintList(ColorStateList.valueOf(mFolderIcon.getFolderName().getBadgeColor()));
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // When the folder gets focus, we don't want to announce the list of items.
        return true;
    }

    @Override
    public View focusSearch(int direction) {
        // When the folder is focused, further focus search should be within the folder contents.
        return FocusFinder.getInstance().findNextFocus(this, null, direction);
    }

    /**
     * @return the FolderInfo object associated with this folder
     */
    public FolderInfo getInfo() {
        return mInfo;
    }

    void bind(FolderInfo info) {
        mInfo = info;
        ArrayList<ShortcutInfo> children = info.contents;
        children.sort(ITEM_POS_COMPARATOR);
        mContent.bindItems(children);

        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new DragLayer.LayoutParams(0, 0);
            lp.customPosition = true;
            setLayoutParams(lp);
        }
        centerAboutIcon();

        mItemsInvalidated = true;
        updateTextViewFocus();
        mInfo.addListener(this);

        if (mInfo.title.length() > 0) {
            mFolderName.setText(mInfo.title);
            mFolderName.setHint(null);
        } else {
            mFolderName.setText("");
            mFolderName.setHint(sHintText);
        }

        // In case any children didn't come across during loading, clean up the folder accordingly
        mFolderIcon.post(new Runnable() {
            public void run() {
                if (getItemCount() <= 1 && !isInAppDrawer()) {
                    replaceFolderWithFinalItem();
                }
            }
        });
    }

    /**
     * Creates a new UserFolder, inflated from R.layout.user_folder.
     *
     * @param launcher The main activity.
     *
     * @return A new UserFolder.
     */
    @SuppressLint("InflateParams")
    static Folder fromXml(Launcher launcher) {
        return (Folder) launcher.getLayoutInflater()
                .inflate(R.layout.user_folder_icon_normalized, null);
    }

    //return the current animation
    public AnimatorSet getCurrentAnimator(){
        return mCurrentAnimator;
    }

    private void startAnimation(final AnimatorSet a) {
        if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
            mCurrentAnimator.cancel();
        }
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mState = STATE_ANIMATING;
                mCurrentAnimator = a;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        a.start();
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     */
    public void animateOpen() {
        mLauncher.folderViewAnimation(true);

        Folder openFolder = getOpen(mLauncher);
        if (openFolder != null && openFolder != this) {
            // Close any open folder before opening a folder.
            openFolder.close(true);
        }

        mIsOpen = true;
        if (mFolderIcon.isCustomIcon) {
            mFolderIcon.mFolderName.setIconVisible(false);
        }

        DragLayer dragLayer = mLauncher.getDragLayer();
        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (getParent() == null) {
            dragLayer.addView(this);
            mDragController.addDropTarget(this);
        } else {
            if (FeatureFlags.IS_DOGFOOD_BUILD) {
                Log.e(TAG, "Opening folder (" + this + ") which already has a parent:"
                        + getParent());
            }
        }

        mContent.completePendingPageChanges();
        if (!mDragInProgress) {
            // Open on the first page.
            mContent.snapToPageImmediately(0);
        }

        // This is set to true in close(), but isn't reset to false until onDropCompleted(). This
        // leads to an inconsistent state if you drag out of the folder and drag back in without
        // dropping. One resulting issue is that replaceFolderWithFinalItem() can be called twice.
        mDeleteFolderOnDropCompleted = false;

        centerAboutIcon();

        AnimatorSet anim = new FolderAnimationManager(this, true /* isOpening */).getAnimator(mFolderIconPos);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFolderIcon.setBackgroundVisible(false);
                mFolderIcon.drawLeaveBehindIfExists();
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mState = STATE_OPEN;
                announceAccessibilityChanges();

                mLauncher.getUserEventDispatcher().resetElapsedContainerMillis("folder opened");
                mContent.setFocusOnFirstChild();
            }
        });

        // Footer animation
        if (mContent.getPageCount() > 1 && !mInfo.hasOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION)) {
            int footerWidth = mContent.getDesiredWidth()
                    - mFooter.getPaddingLeft() - mFooter.getPaddingRight();

            float textWidth =  mFolderName.getPaint().measureText(mFolderName.getText().toString());
            float translation = (footerWidth - textWidth) / 2;
            mFolderName.setTranslationX(mContent.mIsRtl ? -translation : translation);
            mPageIndicator.prepareEntryAnimation();

            // Do not update the flag if we are in drag mode. The flag will be updated, when we
            // actually drop the icon.
            final boolean updateAnimationFlag = !mDragInProgress;
            anim.addListener(new AnimatorListenerAdapter() {

                @SuppressLint("InlinedApi")
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFolderName.animate().setDuration(FOLDER_NAME_ANIMATION_DURATION)
                        .translationX(0)
                        .setInterpolator(AnimationUtils.loadInterpolator(
                                mLauncher, android.R.interpolator.fast_out_slow_in));
                    mPageIndicator.playEntryAnimation();

                    if (updateAnimationFlag) {
                        mInfo.setOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION, true,
                                mLauncher.getModelWriter());
                    }
                }
            });
        } else {
            mFolderName.setTranslationX(0);
        }

        mPageIndicator.stopAllAnimations();
        startAnimation(anim);

        // Make sure the folder picks up the last drag move even if the finger doesn't move.
        if (mDragController.isDragging()) {
            mDragController.forceTouchMove();
        }

        mContent.verifyVisibleHighResIcons(mContent.getNextPage());
    }

    public void beginExternalDrag() {
        mEmptyCellRank = mContent.allocateRankForNewItem();

        mDragCellCount = 0;
        if(isInDesktop() && mLauncher.drawCheck()){
            mDragCellCount = mDragController.getGroupIconManager().getGroupIconCount();
        }
        mDragCellCount ++;

        mIsExternalDrag = true;
        mDragInProgress = true;

        // Since this folder opened by another controller, it might not get onDrop or
        // onDropComplete. Perform cleanup once drag-n-drop ends.
        mDragController.addDragListener(this);
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_FOLDER) != 0;
    }

    @Override
    protected void handleClose(boolean animate) {
        mIsOpen = false;
        if (mFolderIcon.isCustomIcon) {
            mFolderIcon.mFolderName.setIconVisible(true);
        }

        if (isEditingName()) {
            mFolderName.dispatchBackKey();
        }

        if (mFolderIcon != null) {
            mFolderIcon.clearLeaveBehindIfExists();
        }

        //remove the add icon
        removeAddIcon();

        if (animate) {
            animateClosed();
        } else {
            closeComplete(false);
            post(this::announceAccessibilityChanges);
        }


        // Notify the accessibility manager that this folder "window" has disappeared and no
        // longer occludes the workspace items
        mLauncher.getDragLayer().sendAccessibilityEvent(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void animateClosed() {
        mLauncher.folderViewAnimation(false);

        for (int i = 0; i < mContent.getPageCount(); i ++){
            if(mContent.getPageAt(i) != null){
                CellLayout cellLayout = mContent.getPageAt(i);
                for (int j = 0; j < cellLayout.getShortcutsAndWidgets().getChildCount(); j ++){
                    View view = cellLayout.getShortcutsAndWidgets().getChildAt(j);
                    if(view instanceof BubbleTextView){
                        ((BubbleTextView)view).setChecked(false);
                    }
                }
            }
        }

        AnimatorSet a = new FolderAnimationManager(this, false /* isOpening */).getAnimator(mFolderIconPos);
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                closeComplete(true);
                announceAccessibilityChanges();
            }
        });
        startAnimation(a);
    }

    @Override
    protected Pair<View, String> getAccessibilityTarget() {
        return Pair.create(mContent, mIsOpen ? mContent.getAccessibilityDescription()
                : getContext().getString(R.string.folder_closed));
    }

    private void closeComplete(boolean wasAnimated) {
        // TODO: Clear all active animations.
        DragLayer parent = (DragLayer) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        mDragController.removeDropTarget(this);
        clearFocus();
        if (mFolderIcon != null) {
            mFolderIcon.setVisibility(View.VISIBLE);
            mFolderIcon.setBackgroundVisible(true);
            mFolderIcon.mFolderName.setTextVisibility(mFolderIcon.mFolderName.shouldTextBeVisible());
            if (wasAnimated) {
                mFolderIcon.mBackground.fadeInBackgroundShadow();
                mFolderIcon.mBackground.animateBackgroundStroke();
                mFolderIcon.onFolderClose(mContent.getCurrentPage());
                if (mFolderIcon.hasBadge()) {
                    mFolderIcon.createBadgeScaleAnimator(0f, 1f).start();
                }
                mFolderIcon.requestFocus();
            }
        }

        if (mRearrangeOnClose) {
            rearrangeChildren();
            mRearrangeOnClose = false;
        }
        if (getItemCount() <= 1) {
            if (!mDragInProgress && !mSuppressFolderDeletion && !isInAppDrawer()) {
                replaceFolderWithFinalItem();
            } else if (mDragInProgress) {
                mDeleteFolderOnDropCompleted = true;
            }
        }
        mSuppressFolderDeletion = false;
        clearDragInfo();
        mState = STATE_SMALL;
        mContent.setCurrentPage(0);
        if (mInfo instanceof DrawerFolderInfo) {
            ((DrawerFolderInfo) mInfo).onCloseComplete();
        }
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        final ItemInfo item = d.dragInfo;
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT));
    }

    public void onDragEnter(DragObject d) {
        mDragInProgress = true;
        mItemAddedBackToSelfViaIcon = false;

        mPrevTargetRank = -1;
        mOnExitAlarm.cancelAlarm();

        mDragCellCount = 0;
        if(isInDesktop() && mLauncher.drawCheck()){
            mDragCellCount = mDragController.getGroupIconManager().getGroupIconCount();
        }
        mDragCellCount ++;
    }

    OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            mContent.realTimeReorder(mEmptyCellRank, mTargetRank);
            mEmptyCellRank = mTargetRank;
        }
    };

    public boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    private int getTargetRank(DragObject d, float[] recycle) {
        recycle = d.getVisualCenter(recycle);
        recycle[1] -= mFolderNameHeight;
        return mContent.findNearestArea(
                (int) recycle[0] - getPaddingLeft(), (int) recycle[1] - getPaddingTop());
    }

    @Override
    public void onDragOver(DragObject d) {
        if (mScrollPauseAlarm.alarmPending()) {
            return;
        }

        final float[] r = new float[2];
        mTargetRank = getTargetRank(d, r);

        //If external drag, second step animation is not necessary, so do not call the function
        if(mFirstDrag && !mIsExternalDrag){
            secondStepAnimation();
            mFirstDrag = false;
            return;
        }
        mFirstDrag = false;

        if(!mFirstStepAnimationEnded || !mSecondStepAnimationEnded){
            //Stop first and second step animations
            mDragController.stopAllRunningAnimations();
            mFirstStepAnimationEnded = true;
            mSecondStepAnimationEnded = true;
            return;
        }

        if (mTargetRank != mPrevTargetRank) {
            mReorderAlarm.cancelAlarm();
            mReorderAlarm.setOnAlarmListener(mReorderAlarmListener);
            mReorderAlarm.setAlarm(REORDER_DELAY);
            mPrevTargetRank = mTargetRank;

            if (d.stateAnnouncer != null) {
                d.stateAnnouncer.announce(getContext().getString(R.string.move_to_position,
                        mTargetRank + 1));
            }
        }

        float x = r[0];
        int currentPage = mContent.getNextPage();

        float cellOverlap = mContent.getCurrentCellLayout().getCellWidth()
                * ICON_OVERSCROLL_WIDTH_FACTOR;
        boolean isOutsideLeftEdge = x < cellOverlap;
        boolean isOutsideRightEdge = x > (getWidth() - cellOverlap);

        if (currentPage > 0 && (mContent.mIsRtl ? isOutsideRightEdge : isOutsideLeftEdge)) {
            showScrollHint(SCROLL_LEFT, d);
        } else if (currentPage < (mContent.getPageCount() - 1)
                && (mContent.mIsRtl ? isOutsideLeftEdge : isOutsideRightEdge)) {
            showScrollHint(SCROLL_RIGHT, d);
        } else {
            mOnScrollHintAlarm.cancelAlarm();
            if (mScrollHintDir != SCROLL_NONE) {
                mContent.clearScrollHint();
                mScrollHintDir = SCROLL_NONE;
            }
        }
    }

    private void showScrollHint(int direction, DragObject d) {
        // Show scroll hint on the right
        if (mScrollHintDir != direction) {
            mContent.showScrollHint(direction);
            mScrollHintDir = direction;
        }

        // Set alarm for when the hint is complete
        if (!mOnScrollHintAlarm.alarmPending() || mCurrentScrollDir != direction) {
            mCurrentScrollDir = direction;
            mOnScrollHintAlarm.cancelAlarm();
            mOnScrollHintAlarm.setOnAlarmListener(new OnScrollHintListener(d));
            mOnScrollHintAlarm.setAlarm(SCROLL_HINT_DURATION);

            mReorderAlarm.cancelAlarm();
            mTargetRank = mEmptyCellRank;
        }
    }

    OnAlarmListener mOnExitAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            completeDragExit();
        }
    };

    public void completeDragExit() {
        if (isInAppDrawer()) {
            // This is faster and more straightforward than trying to get the dragged app reliably
            // back into the folder in any other way
            mLauncher.getAppsView().getApps().reset();
            return;
        }
        if (mIsOpen) {
            close(true);
            mRearrangeOnClose = true;
        } else if (mState == STATE_ANIMATING) {
            mRearrangeOnClose = true;
        } else {
            rearrangeChildren();
            clearDragInfo();
        }
    }

    private void clearDragInfo() {
        mCurrentDragView = null;
        mIsExternalDrag = false;
    }

    public void onDragExit(DragObject d) {
        // We only close the folder if this is a true drag exit, ie. not because
        // a drop has occurred above the folder.
        if (!d.dragComplete) {
            mOnExitAlarm.setOnAlarmListener(mOnExitAlarmListener);
            mOnExitAlarm.setAlarm(ON_EXIT_CLOSE_DELAY);
        }
        mReorderAlarm.cancelAlarm();

        mOnScrollHintAlarm.cancelAlarm();
        mScrollPauseAlarm.cancelAlarm();
        if (mScrollHintDir != SCROLL_NONE) {
            mContent.clearScrollHint();
            mScrollHintDir = SCROLL_NONE;
        }
    }

    /**
     * When performing an accessibility drop, onDrop is sent immediately after onDragEnter. So we
     * need to complete all transient states based on timers.
     */
    @Override
    public void prepareAccessibilityDrop() {
        if (mReorderAlarm.alarmPending()) {
            mReorderAlarm.cancelAlarm();
            mReorderAlarmListener.onAlarm(mReorderAlarm);
        }
    }

    public void onDropCompleted(final View target, final DragObject d,
            final boolean success) {

        if (success) {
            if (mDeleteFolderOnDropCompleted && !mItemAddedBackToSelfViaIcon && target != this) {
                replaceFolderWithFinalItem();
            }
        } else {

            List<BubbleTextView> tempViewList = new ArrayList<>();
            List<BubbleTextView> viewList = new ArrayList<>();
            // The drag failed, we need to return the item to the folder
            if(d != null) {
                ShortcutInfo info = (ShortcutInfo) d.dragInfo;
                View icon = (mCurrentDragView != null && mCurrentDragView.getTag() == info)
                        ? mCurrentDragView : mContent.createNewView(info);
                tempViewList.add((BubbleTextView) icon);
            }

            if(mDragController.getGroupIconManager().getGroupIconCount() > 0){
                mDragController.getGroupIconManager().getDraggingIconsFromSourceFolder(this, tempViewList);
            }

            //Then array with rank
            while (!tempViewList.isEmpty()) {
                int minRank = tempViewList.get(0).getRank();
                int minRankPosition = 0;
                for (int i = 1; i < tempViewList.size(); i++) {
                    int rank = tempViewList.get(i).getRank();
                    if (rank < minRank) {
                        minRank = rank;
                        minRankPosition = i;
                    }
                }

                //Add to new ViewList, and remove from old ViewList
                viewList.add(tempViewList.get(minRankPosition));
                tempViewList.remove(minRankPosition);
            }

            ArrayList<View> views = getItemsInReadingOrder();

            for(int i = 0; i < viewList.size(); i ++){
                BubbleTextView view = viewList.get(i);
                view.setChecked(false);

                ShortcutInfo itemInfo = (ShortcutInfo) view.getTag();
                views.add(itemInfo.rank, view);
                mContent.arrangeChildren(views, views.size());

                try (SuppressInfoChanges s = new SuppressInfoChanges()) {
                    mFolderIcon.onDrop(itemInfo, true /* itemReturnedOnFailedDrop */);
                }
            }

            mItemsInvalidated = true;

//            try (SuppressInfoChanges s = new SuppressInfoChanges()) {
//                mFolderIcon.onDrop(d, true /* itemReturnedOnFailedDrop */);
//            }
        }

        if (target != this) {
            if (mOnExitAlarm.alarmPending()) {
                mOnExitAlarm.cancelAlarm();
                if (!success) {
                    mSuppressFolderDeletion = true;
                }
                mScrollPauseAlarm.cancelAlarm();
            }
            completeDragExit();
        }

        mDeleteFolderOnDropCompleted = false;
        mDragInProgress = false;
        mItemAddedBackToSelfViaIcon = false;
        mCurrentDragView = null;

        // Reordering may have occured, and we need to save the new item locations. We do this once
        // at the end to prevent unnecessary database operations.
        updateItemLocationsInDatabaseBatch();

        // Use the item count to check for multi-page as the folder UI may not have
        // been refreshed yet.
        if (getItemCount() <= mContent.itemsPerPage()) {
            // Show the animation, next time something is added to the folder.
            mInfo.setOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION, false,
                    mLauncher.getModelWriter());
        }
    }

    public void onDropWithFailing(){
        onDropCompleted(null, null, false);
    }

    private void updateItemLocationsInDatabaseBatch() {
        ArrayList<View> list = getItemsInReadingOrder();
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        for (int i = 0; i < list.size(); i++) {
            View v = list.get(i);
            ItemInfo info = (ItemInfo) v.getTag();
            info.rank = i;
            items.add(info);
        }

        mLauncher.getModelWriter().moveItemsInDatabase(items, mInfo.id, 0);
    }

    public void notifyDrop() {
        if (mDragInProgress) {
            mItemAddedBackToSelfViaIcon = true;
        }
    }

    public boolean isDropEnabled() {
        return mState != STATE_ANIMATING;
    }

    private void centerAboutIcon() {
        DeviceProfile grid = mLauncher.getDeviceProfile();

        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        DragLayer parent = (DragLayer) mLauncher.findViewById(R.id.drag_layer);
        int width = getFolderWidth();
        int height = getFolderHeight();

        parent.getDescendantRectRelativeToSelf(mFolderIcon, sTempRect);
        int centerX = sTempRect.centerX();
        int centerY = sTempRect.centerY();
        int centeredLeft = centerX - width / 2;
        int centeredTop = centerY - height / 2;

        // We need to bound the folder to the currently visible workspace area
        if (mLauncher.getStateManager().getState().overviewUi) {
            mLauncher.getDragLayer().getDescendantRectRelativeToSelf(mLauncher.getOverviewPanel(),
                    sTempRect);
        } else {
            mLauncher.getWorkspace().getPageAreaRelativeToDragLayer(sTempRect);
        }
        int left = Math.min(Math.max(sTempRect.left, centeredLeft),
                sTempRect.right- width);
        int top = Math.min(Math.max(sTempRect.top, centeredTop),
                sTempRect.bottom - height);

        int distFromEdgeOfScreen = mLauncher.getWorkspace().getPaddingLeft() + getPaddingLeft();

        if (grid.isPhone && (grid.availableWidthPx - width) < 4 * distFromEdgeOfScreen) {
            // Center the folder if it is very close to being centered anyway, by virtue of
            // filling the majority of the viewport. ie. remove it from the uncanny valley
            // of centeredness.
            left = (grid.availableWidthPx - width) / 2;
        } else if (width >= sTempRect.width()) {
            // If the folder doesn't fit within the bounds, center it about the desired bounds
            left = sTempRect.left + (sTempRect.width() - width) / 2;
        }
        if (height >= sTempRect.height()) {
            // Folder height is greater than page height, center on page
            top = sTempRect.top + (sTempRect.height() - height) / 2;
        } else {
            // Folder height is less than page height, so bound it to the absolute open folder
            // bounds if necessary
            Rect folderBounds = grid.getAbsoluteOpenFolderBounds();
            left = Math.max(folderBounds.left, Math.min(left, folderBounds.right - width));
            top = Math.max(folderBounds.top, Math.min(top, folderBounds.bottom - height));
        }

        int folderPivotX = width / 2 + (centeredLeft - left);
        int folderPivotY = height / 2 + (centeredTop - top);
        setPivotX(folderPivotX);
        setPivotY(folderPivotY);

        mFolderIconPivotX = (int) (mFolderIcon.getMeasuredWidth() *
                (1.0f * folderPivotX / width));
        mFolderIconPivotY = (int) (mFolderIcon.getMeasuredHeight() *
                (1.0f * folderPivotY / height));

        //Get width, and height of parent draglayer
        int parentWidth = 0;
        int parentHeight = 0;
        int scaledWidth = (int)(width * 0.8f);
        int scaledHeight = (int)(height * 0.8f);
        if(mLauncher.isInState(NORMAL)){
            parentWidth = parent.getWidth();
            parentHeight = parent.getHeight();
            left = (parentWidth - width)/2;
            top = (parentHeight - height)/2;
        }
        else if(mLauncher.isInState(OPTIONS)){
            View view = mLauncher.getWorkspace().getPageAt(0);
            parentWidth = view.getWidth();
            parentHeight = view.getHeight();
            left = view.getLeft() + (parentWidth - scaledWidth)/2;
            top = view.getTop() + (parentHeight - scaledHeight)/2;
        }

        lp.width = width;
        lp.height = height;
        lp.x = left;
        lp.y = top;
    }



    public float getPivotXForIconAnimation() {
        return mFolderIconPivotX;
    }
    public float getPivotYForIconAnimation() {
        return mFolderIconPivotY;
    }

    private int getContentAreaHeight() {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        int maxContentAreaHeight = grid.availableHeightPx
                - grid.getTotalWorkspacePadding().y - mFooterHeight;
        int height = Math.min(maxContentAreaHeight,
                mContent.getDesiredHeight());
        return Math.max(height, MIN_CONTENT_DIMEN);
    }

    private int getContentAreaWidth() {
        return Math.max(mContent.getDesiredWidth(), MIN_CONTENT_DIMEN);
    }

    public int getFolderWidth() {
        return getPaddingLeft() + getPaddingRight() + mContent.getDesiredWidth();
    }

    public int getFolderHeight() {
        return getFolderHeight(getContentAreaHeight());
    }

    private int getFolderHeight(int contentAreaHeight) {
        return getPaddingTop() + getPaddingBottom() + contentAreaHeight + mFooterHeight + mFolderNameHeight;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int contentWidth = getContentAreaWidth();
        int contentHeight = getContentAreaHeight();

        int contentAreaWidthSpec = MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY);
        int contentAreaHeightSpec = MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY);

        mContent.setFixedSize(contentWidth, contentHeight);
        mContent.measure(contentAreaWidthSpec, contentAreaHeightSpec);

        if (mContent.getChildCount() > 0) {
            int cellIconGap = (mContent.getPageAt(0).getCellWidth()
                    - mLauncher.getDeviceProfile().iconSizePx) / 2;
            mFooter.setPadding(mContent.getPaddingLeft() + cellIconGap,
                    mFooter.getPaddingTop(),
                    mContent.getPaddingRight() + cellIconGap,
                    mFooter.getPaddingBottom());
        }
        mFooter.measure(contentAreaWidthSpec,
                MeasureSpec.makeMeasureSpec(mFooterHeight, MeasureSpec.EXACTLY));

        int folderWidth = getPaddingLeft() + getPaddingRight() + contentWidth;
        int folderHeight = getFolderHeight(contentHeight);
        setMeasuredDimension(folderWidth, folderHeight);
    }

    /**
     * Rearranges the children based on their rank.
     */
    public void rearrangeChildren() {
        rearrangeChildren(-1);
    }

    /**
     * Rearranges the children based on their rank.
     * @param itemCount if greater than the total children count, empty spaces are left at the end,
     * otherwise it is ignored.
     */
    public void rearrangeChildren(int itemCount) {
        ArrayList<View> views = getItemsInReadingOrder();
        mContent.arrangeChildren(views, Math.max(itemCount, views.size()));
        mItemsInvalidated = true;
    }

    public int getItemCount() {
        return mContent.getItemCount();
    }

    @Thunk public void replaceFolderWithFinalItem() {
        // Add the last remaining child to the workspace in place of the folder
        Runnable onCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                int itemCount = mInfo.contents.size();
                if (itemCount <= 1) {
                    View newIcon = null;

                    if (itemCount == 1) {
                        // Move the item from the folder to the workspace, in the position of the
                        // folder
                        CellLayout cellLayout = mLauncher.getCellLayout(mInfo.container,
                                mInfo.screenId);
                        ShortcutInfo finalItem = mInfo.contents.remove(0);
                        newIcon = mLauncher.createShortcut(cellLayout, finalItem);
                        mLauncher.getModelWriter().addOrMoveItemInDatabase(finalItem,
                                mInfo.container, mInfo.screenId, mInfo.cellX, mInfo.cellY);
                    }

                    // Remove the folder
                    mLauncher.removeItem(mFolderIcon, mInfo, true /* deleteFromDb */);
                    if (mFolderIcon instanceof DropTarget) {
                        mDragController.removeDropTarget((DropTarget) mFolderIcon);
                    }

                    if (newIcon != null) {
                        // We add the child after removing the folder to prevent both from existing
                        // at the same time in the CellLayout.  We need to add the new item with
                        // addInScreenFromBind() to ensure that hotseat items are placed correctly.
                        mLauncher.getWorkspace().addInScreenFromBind(newIcon, mInfo);

                        // Focus the newly created child
                        newIcon.requestFocus();
                    }
                }
            }
        };
        View finalChild = mContent.getLastItem();
        if (finalChild != null) {
            mFolderIcon.performDestroyAnimation(onCompleteRunnable);
        } else {
            onCompleteRunnable.run();
        }
        mDestroyed = true;
    }

    public boolean isDestroyed() {
        return mDestroyed;
    }

    // This method keeps track of the first and last item in the folder for the purposes
    // of keyboard focus
    public void updateTextViewFocus() {
        final View firstChild = mContent.getFirstItem();
        final View lastChild = mContent.getLastItem();
        if (firstChild != null && lastChild != null) {
            mFolderName.setNextFocusDownId(lastChild.getId());
            mFolderName.setNextFocusRightId(lastChild.getId());
            mFolderName.setNextFocusLeftId(lastChild.getId());
            mFolderName.setNextFocusUpId(lastChild.getId());
            // Hitting TAB from the folder name wraps around to the first item on the current
            // folder page, and hitting SHIFT+TAB from that item wraps back to the folder name.
            mFolderName.setNextFocusForwardId(firstChild.getId());
            // When clicking off the folder when editing the name, this Folder gains focus. When
            // pressing an arrow key from that state, give the focus to the first item.
            this.setNextFocusDownId(firstChild.getId());
            this.setNextFocusRightId(firstChild.getId());
            this.setNextFocusLeftId(firstChild.getId());
            this.setNextFocusUpId(firstChild.getId());
            // When pressing shift+tab in the above state, give the focus to the last item.
            setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    boolean isShiftPlusTab = keyCode == KeyEvent.KEYCODE_TAB &&
                            event.hasModifiers(KeyEvent.META_SHIFT_ON);
                    if (isShiftPlusTab && Folder.this.isFocused()) {
                        return lastChild.requestFocus();
                    }
                    return false;
                }
            });
        }
    }

    public void onDrop(DragObject d, DragOptions options) {
        while (mEmptyCellRank > 0 && mContent.getChildViewByRank(mEmptyCellRank - 1) == null)
            mEmptyCellRank --;

        if(!mFirstStepAnimationEnded || !mSecondStepAnimationEnded){
            //Stop first and second step animations
            mDragController.stopAllRunningAnimations();
            mFirstStepAnimationEnded = true;
            mSecondStepAnimationEnded = true;
        }

        // If the icon was dropped while the page was being scrolled, we need to compute
        // the target location again such that the icon is placed of the final page.
        if (!mContent.rankOnCurrentPage(mEmptyCellRank)) {
            // Reorder again.
            mTargetRank = getTargetRank(d, null);

            // Rearrange items immediately.
            mReorderAlarmListener.onAlarm(mReorderAlarm);

            mOnScrollHintAlarm.cancelAlarm();
            mScrollPauseAlarm.cancelAlarm();
        }
        mContent.completePendingPageChanges();

        PendingAddShortcutInfo pasi = d.dragInfo instanceof PendingAddShortcutInfo
                ? (PendingAddShortcutInfo) d.dragInfo : null;
        ShortcutInfo pasiSi = pasi != null ? pasi.activityInfo.createShortcutInfo() : null;
        if (pasi != null && pasiSi == null) {
            // There is no ShortcutInfo, so we have to go through a configuration activity.
            pasi.container = mInfo.id;
            pasi.rank = mEmptyCellRank;

            mLauncher.addPendingItem(pasi, pasi.container, pasi.screenId, null, pasi.spanX,
                    pasi.spanY);
            d.deferDragViewCleanupPostAnimation = false;
            mRearrangeOnClose = true;
        } else {
            final ShortcutInfo si;
            if (pasiSi != null) {
                si = pasiSi;
            } else if (d.dragInfo instanceof AppInfo) {
                // Came from all apps -- make a copy.
                si = ((AppInfo) d.dragInfo).makeShortcut();
            } else {
                // ShortcutInfo
                si = (ShortcutInfo) d.dragInfo;
            }

            View currentDragView;
            if (mIsExternalDrag) {
                currentDragView = mContent.createAndAddViewWithRank(si, mEmptyCellRank);

                // Actually move the item in the database if it was an external drag. Call this
                // before creating the view, so that ShortcutInfo is updated appropriately.
                mLauncher.getModelWriter().addOrMoveItemInDatabase(
                        si, mInfo.id, 0, si.cellX, si.cellY);

                // We only need to update the locations if it doesn't get handled in
                // #onDropCompleted.
                if (d.dragSource != this) {
                    updateItemLocationsInDatabaseBatch();
                }
                mIsExternalDrag = false;
            } else {
                currentDragView = mCurrentDragView;
                mContent.addViewForRank(currentDragView, si, mEmptyCellRank);
            }

            if (d.dragView.hasDrawn()) {
                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, currentDragView, null);
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                currentDragView.setVisibility(VISIBLE);
            }

            //Play animation only page number is same as main drag view's page number
            int oldPage = 0;
            if(mCurrentDragView instanceof BubbleTextView){
                oldPage = ((BubbleTextView)mCurrentDragView).getRank() / mContent.mMaxItemsPerPage;
            }

            //Start dropping of group icons
            GroupIconManager groupIconManager = mDragController.getGroupIconManager();
            for (int i = 0; i < groupIconManager.getGroupIconCount(); i ++){
                GroupIconCell iconCell = groupIconManager.getGroupIconCell(i);
                BubbleTextView view = iconCell.getCellView();
                ShortcutInfo info = (ShortcutInfo) view.getTag();

                int newRank = mEmptyCellRank + i + 1;
                int newPage = newRank / mContent.mMaxItemsPerPage;

                //internal drag
                if(iconCell.isFolder() && iconCell.getFolder() == this) {
                    mContent.addViewForRank(view, info, newRank);
                    ((ShortcutAndWidgetContainer)view.getParent()).measureChild(view);

                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams)view.getLayoutParams();
                    int childLeft = lp.x;
                    int childTop = lp.y;
                    view.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                    view.setChecked(false);
                    groupIconManager.playAnimationGetOut(iconCell.getGroupIconDragView(), view, info.cellX, info.cellY,
                            oldPage != newPage, this);
                }
                //external drag
                else{
                    View targetView = mContent.createAndAddViewWithRank(info, newRank);
                    mLauncher.getModelWriter().addOrMoveItemInDatabase(
                            info, mInfo.id, 0, info.cellX, info.cellY);
                    updateItemLocationsInDatabaseBatch();

                    if(targetView instanceof BubbleTextView)
                        ((BubbleTextView)targetView).setChecked(false);

                    ((ShortcutAndWidgetContainer)targetView.getParent()).measureChild(targetView);
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams)targetView.getLayoutParams();
                    int childLeft = lp.x;
                    int childTop = lp.y;
                    targetView.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);
                    iconCell.getGroupIconDragView().setScale(getScaleX());

                    groupIconManager.playAnimationGetOut(iconCell.getGroupIconDragView(), targetView, info.cellX, info.cellY,
                            oldPage != newPage, this);
                }
            }

            mItemsInvalidated = true;
            rearrangeChildren();

            // Temporarily suppress the listener, as we did all the work already here.
            try (SuppressInfoChanges s = new SuppressInfoChanges()) {
                mInfo.add(si, false);
            }

        }

        // Clear the drag info, as it is no longer being dragged.
        mDragInProgress = false;

        if (mContent.getPageCount() > 1) {
            // The animation has already been shown while opening the folder.
            mInfo.setOption(FolderInfo.FLAG_MULTI_PAGE_ANIMATION, true, mLauncher.getModelWriter());
        }

        if(!mLauncher.isInState(OPTIONS))
            mLauncher.getStateManager().goToState(NORMAL, SPRING_LOADED_EXIT_DELAY);
        if (d.stateAnnouncer != null) {
            d.stateAnnouncer.completeAction(R.string.item_moved);
        }
    }

    public void addAppIcon(ShortcutInfo si, int rank){
        mContent.createAndAddViewWithRank(si, rank);
        mLauncher.getModelWriter().addOrMoveItemInDatabase(
                si, mInfo.id, 0, si.cellX, si.cellY);
        updateItemLocationsInDatabaseBatch();
    }

    // This is used so the item doesn't immediately appear in the folder when added. In one case
    // we need to create the illusion that the item isn't added back to the folder yet, to
    // to correspond to the animation of the icon back into the folder. This is
    public void hideItem(ShortcutInfo info) {
        View v = getViewForInfo(info);
        v.setVisibility(INVISIBLE);
    }
    public void showItem(ShortcutInfo info) {
        View v = getViewForInfo(info);
        v.setVisibility(VISIBLE);
    }

    @Override
    public void onAdd(ShortcutInfo item, int rank) {
        View view = mContent.createAndAddViewForRank(item, rank);
        mLauncher.getModelWriter().addOrMoveItemInDatabase(item, mInfo.id, 0, item.cellX,
                item.cellY);

        ArrayList<View> items = new ArrayList<>(getItemsInReadingOrder());
        items.add(rank, view);
        mContent.arrangeChildren(items, items.size());
        mItemsInvalidated = true;
    }

    public void onRemove(ShortcutInfo item, boolean folderRemove) {
        if (isInAppDrawer()) {
            return;
        }
        mItemsInvalidated = true;
        View v = getViewForInfo(item);
        mContent.removeItem(v);
        if (mState == STATE_ANIMATING) {
            mRearrangeOnClose = true;
        } else {
            rearrangeChildren();
        }
        if (getItemCount() <= 1) {
            if (mIsOpen) {
                close(true);
            } else {
                //When group dragging outside, do not remove the folder
                if(folderRemove) {
                    replaceFolderWithFinalItem();
                }
            }
        }
    }

    private View getViewForInfo(final ShortcutInfo item) {
        return mContent.iterateOverItems(new ItemOperator() {

            @Override
            public boolean evaluate(ItemInfo info, View view) {
                return info == item;
            }
        });
    }

    @Override
    public void onItemsChanged(boolean animate) {
        updateTextViewFocus();
    }

    @Override
    public void prepareAutoUpdate() {
        close(false);
    }

    public void onTitleChanged(CharSequence title) {
        mFolderName.setText(title);
    }

    public ArrayList<View> getItemsInReadingOrder() {
        if (mItemsInvalidated) {
            mItemsInReadingOrder.clear();
            mContent.iterateOverItems(new ItemOperator() {

                @Override
                public boolean evaluate(ItemInfo info, View view) {
                    mItemsInReadingOrder.add(view);
                    return false;
                }
            });
            mItemsInvalidated = false;
        }
        return mItemsInReadingOrder;
    }

    public void iterateOverItems(ItemOperator op) {
        mContent.iterateOverItems(op);
    }

    public List<BubbleTextView> getItemsOnPage(int page) {
        ArrayList<View> allItems = getItemsInReadingOrder();
        int lastPage = mContent.getPageCount() - 1;
        int totalItemsInFolder = allItems.size();
        int itemsPerPage = mContent.itemsPerPage();
        int numItemsOnCurrentPage = page == lastPage
                ? totalItemsInFolder - (itemsPerPage * page)
                : itemsPerPage;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + numItemsOnCurrentPage, allItems.size());

        List<BubbleTextView> itemsOnCurrentPage = new ArrayList<>(numItemsOnCurrentPage);
        for (int i = startIndex; i < endIndex; ++i) {
            itemsOnCurrentPage.add((BubbleTextView) allItems.get(i));
        }
        return itemsOnCurrentPage;
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (v == mFolderName) {
            if (hasFocus) {
                startEditingFolderName();
            } else {
                mFolderName.dispatchBackKey();
            }
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
    }

    @Override
    public void fillInLogContainerData(View v, ItemInfo info, Target target, Target targetParent) {
        target.gridX = info.cellX;
        target.gridY = info.cellY;
        target.pageIndex = mContent.getCurrentPage();
        targetParent.containerType = ContainerType.FOLDER;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mClipPath != null) {
            int save = canvas.save();
            canvas.clipPath(mClipPath);
            super.draw(canvas);
            canvas.restoreToCount(save);
        } else {
            super.draw(canvas);
        }
    }

    @Override
    public void onClick(View v) {
        if(v == mAddButton){
            //On click add button, so open the extended folder
            ExtendedFolder extendedFolder = new ExtendedFolder(getContext(), this);

            Objects.requireNonNull(extendedFolder.getWindow()).setGravity(BOTTOM);

            extendedFolder.show();
        }
    }

    private class OnScrollHintListener implements OnAlarmListener {

        private final DragObject mDragObject;

        OnScrollHintListener(DragObject object) {
            mDragObject = object;
        }

        /**
         * Scroll hint has been shown long enough. Now scroll to appropriate page.
         */
        @Override
        public void onAlarm(Alarm alarm) {
            if (mCurrentScrollDir == SCROLL_LEFT) {
                mContent.scrollLeft();
                mScrollHintDir = SCROLL_NONE;
            } else if (mCurrentScrollDir == SCROLL_RIGHT) {
                mContent.scrollRight();
                mScrollHintDir = SCROLL_NONE;
            } else {
                // This should not happen
                return;
            }
            mCurrentScrollDir = SCROLL_NONE;

            // Pause drag event until the scrolling is finished
            mScrollPauseAlarm.setOnAlarmListener(new OnScrollFinishedListener(mDragObject));
            mScrollPauseAlarm.setAlarm(RESCROLL_DELAY);
        }
    }

    private class OnScrollFinishedListener implements OnAlarmListener {

        private final DragObject mDragObject;

        OnScrollFinishedListener(DragObject object) {
            mDragObject = object;
        }

        /**
         * Page scroll is complete.
         */
        @Override
        public void onAlarm(Alarm alarm) {
            // Reorder immediately on page change.
            onDragOver(mDragObject);
        }
    }

    // Compares item position based on rank and position giving priority to the rank.
    public static final Comparator<ItemInfo> ITEM_POS_COMPARATOR = new Comparator<ItemInfo>() {

        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            if (lhs.rank != rhs.rank) {
                return lhs.rank - rhs.rank;
            } else if (lhs.cellY != rhs.cellY) {
                return lhs.cellY - rhs.cellY;
            } else {
                return lhs.cellX - rhs.cellX;
            }
        }
    };

    /**
     * Temporary resource held while we don't want to handle info changes
     */
    private class SuppressInfoChanges implements AutoCloseable {

        SuppressInfoChanges() {
            mInfo.removeListener(Folder.this);
        }

        @Override
        public void close() {
            mInfo.addListener(Folder.this);
            updateTextViewFocus();
        }
    }

    /**
     * Returns a folder which is already open or null
     */
    public static Folder getOpen(Launcher launcher) {
        return getOpenView(launcher, TYPE_FOLDER);
    }

    public static Folder getCurrentFolder(Launcher launcher){
        DragLayer dragLayer = launcher.getDragLayer();
        if (dragLayer != null) {
            for (int i = dragLayer.getChildCount() - 1; i >= 0; i--) {
                View child = dragLayer.getChildAt(i);
                if (child instanceof Folder) {
                    return (Folder)child;
                }
            }
        }
        return null;
    }

    @Override
    public void logActionCommand(int command) {
        mLauncher.getUserEventDispatcher().logActionCommand(
                command, getFolderIcon(), ContainerType.FOLDER);
    }

    @Override
    public boolean onBackPressed() {
        if (isEditingName()) {
            mFolderName.dispatchBackKey();
        } else {
            super.onBackPressed();
        }
        return true;
    }

    public void removeAddIcon(){
        if(mAddButton != null){
            if(mAddButton.getParent() != null){
                ShortcutAndWidgetContainer parent = ((ShortcutAndWidgetContainer)mAddButton.getParent());
                parent.removeView(mAddButton);

                //If the last page had only add button, remove the last page too
                if(parent.getChildCount() == 0){
                    CellLayout cellLayout = (CellLayout)parent.getParent();

                    ((ViewGroup)(parent.getParent().getParent())).removeView(cellLayout);
                }

                //Notify that a child is removed
                mItemsInvalidated = true;
            }
        }
    }

    public void insertAddIcon(){
        //This function is called twice some times, so skip on second and further calls
        if(mAddButton != null && mAddButton.getParent() != null)
            return;

        //Create add button
        if(mAddButton == null){
            mAddButton = (AddButtonBubbleTextView) inflate(getContext(), R.layout.add_button_in_folder, null);
            mAddButton.setOnClickListener(this);
        }

        int lastPageIndex = mContent.getPageCount() - 1;
        if(lastPageIndex >= 0) {
            CellLayout lastPageCellLayout = mContent.getPageAt(lastPageIndex);
            int itemsPerPage = mContent.getGridCountX() * mContent.getGridCountY();
            int index = lastPageCellLayout.getShortcutsAndWidgets().getChildCount() % itemsPerPage;

            if(index == 0) {
                mContent.createAndAddNewPage();
                lastPageCellLayout = mContent.getPageAt(lastPageIndex + 1);
            }

            int cellX = index % mContent.getGridCountX();
            int cellY = index / mContent.getGridCountX();

            CellLayout.LayoutParams lp = new CellLayout.LayoutParams(cellX, cellY, 1, 1);
            mAddButton.setLayoutParams(lp);

            lastPageCellLayout.addViewToCellLayout(mAddButton, -1, -1 , lp, false);
            lastPageCellLayout.getShortcutsAndWidgets().measureChild(mAddButton);
            mAddButton.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
        }
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            DragLayer dl = mLauncher.getDragLayer();

            if (isEditingName()) {
                if (!dl.isEventOverView(mFolderName, ev)) {
                    mFolderName.dispatchBackKey();
                    return true;
                }
                return false;
            } else if (!dl.isEventOverView(this, ev)) {
                if (mLauncher.getAccessibilityDelegate().isInAccessibleDrag()) {
                    // Do not close the container if in drag and drop.
                    if (!dl.isEventOverView(mLauncher.getDropTargetBar(), ev)) {
                        return true;
                    }
                } else {
                    mLauncher.getUserEventDispatcher().logActionTapOutside(
                            LoggerUtils.newContainerTarget(ContainerType.FOLDER));
                    close(true);
                    return true;
                }
            }
        }
        return false;
    }

    public void secondStepAnimation(){
        mRunningAnimatorCount = 0;

        //Rearrange children
        int itemCount = mContent.getItemCount();
        int missingItemCount = mDragCellCount;

        //if main view is not checked, then the main view is invisible, and not removed, so set it 0
        if(mDragController.getGroupIconManager().getMainView() == null && !mIsExternalDrag){
            missingItemCount = 0;
        }

        int maxItemPos = itemCount + missingItemCount;

        List<View> viewList = new ArrayList<>();
        for (int i = 0; i < maxItemPos; i ++){
            viewList.add(mContent.getChildViewByRank(i));
        }

        List<Integer> tempIndexArray = new ArrayList<Integer>();
        List<Integer> indexArray = new ArrayList<Integer>();
        int k = 0;

        //Calculate new array
        for (int i = 0; i < maxItemPos; i ++){
            View item = viewList.get(i);
            if(item != null){
                tempIndexArray.add(i);
            }
        }
        for (int i = 0; i < maxItemPos; i ++){
            if(i >= mTargetRank && i < mTargetRank + missingItemCount){
                indexArray.add(-1);
            }
            else {
                if(k < tempIndexArray.size())
                    indexArray.add(tempIndexArray.get(k ++));
            }
        }

        //Move to new position with animation
        for (int i = 0; i < maxItemPos; i ++){
            View item = viewList.get(i);
            if(item == null)
                continue;
            int newPos = indexArray.lastIndexOf(i);

            if(i == newPos)
                continue;

            ItemInfo itemInfo = (ItemInfo) item.getTag();
            itemInfo.rank = newPos;

            int oldPageNumber = i / mContent.mMaxItemsPerPage;
            int pageNumber = newPos / mContent.mMaxItemsPerPage;
            int indexInPage = newPos % mContent.mMaxItemsPerPage;

            int cellX = indexInPage % mContent.getGridCountX();
            int cellY = indexInPage / mContent.getGridCountX();

            itemInfo.cellX = cellX;
            itemInfo.cellY = cellY;

            if(oldPageNumber == pageNumber) {
                CellLayout cellLayout = (CellLayout) item.getParent().getParent();
                //Set start delay because this should play after first animation ends
                cellLayout.animateChildToPosition(item, cellX, cellY, 200, SECOND_STEP_ANIMATION_DELAY, true, true);

                //Add this animator to list
                if(item.getTag(R.string.group_drag_animate_to_pos) != null){
                    mRunningAnimatorCount ++;
                    Animator animator = (Animator) item.getTag(R.string.group_drag_animate_to_pos);
                    mDragController.addToRunningAnimatorList(animator);

                    //When all animation ends, set second animation ended true
                    animator.addListener(new AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mRunningAnimatorCount --;

                            if(mRunningAnimatorCount == 0){
                                mSecondStepAnimationEnded = true;
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                }
            }
            else{
                if(mContent.getPageAt(pageNumber) == null)
                    mContent.createAndAddNewPage();

                //Remove from old page, and add to new page
                Runnable endAction = new Runnable() {
                    final float oldTranslateX = item.getTranslationX();
                    @Override
                    public void run() {
                        mContent.mPendingAnimations.remove(item);
                        item.setTranslationX(oldTranslateX);
                        ((CellLayout) item.getParent().getParent()).removeView(item);
                        mContent.addViewForRank(item, (ShortcutInfo) item.getTag(), newPos);
                    }
                };

                item.animate()
                        .translationXBy(oldPageNumber < pageNumber ? item.getWidth() : - item.getWidth())
                        .setStartDelay(SECOND_STEP_ANIMATION_DELAY)
                        .setDuration(200)
                        .withEndAction(endAction);

                mContent.mPendingAnimations.put(item, endAction);
            }
        }
        viewList.clear();

        mSecondStepAnimationEnded = mRunningAnimatorCount == 0;
    }

    public List<ShortcutInfo> getShortCutInformationList(){
        List<ShortcutInfo> infoList = new ArrayList<>();

        for (int i = 0; i < mContent.getPageCount(); i ++){
            CellLayout cellLayout = mContent.getPageAt(i);
            ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();

            for (int j = 0; j < container.getChildCount(); j ++){
                View child = container.getChildAt(j);
                if(child instanceof BubbleTextView && child != mAddButton){
                    BubbleTextView textView = (BubbleTextView)child;

                    if(textView.getTag() instanceof ShortcutInfo){
                        ShortcutInfo shortcutInfo = (ShortcutInfo)(textView.getTag());
                        infoList.add(shortcutInfo);
                    }
                }
            }
        }
        return infoList;
    }

    public String getFolderName(){
        return mFolderName.getText().toString();
    }

    public int getIconCount(){
        int itemsPerPage = mContent.getGridCountX() * mContent.getGridCountY();
        int lastPage = mContent.getChildCount() - 1;
        int lastPageItemCount = ((CellLayout)mContent.getChildAt(lastPage)).getShortcutsAndWidgets().getChildCount();

        int result = lastPage * itemsPerPage + lastPageItemCount;

        //do not count add button
        if(mAddButton.getParent() != null)
            result --;

        return result;
    }

    public int getDragCellCount(){
        return Math.max(1, mDragCellCount);
    }

    public void setFirstStepAnimationEnded(boolean ended){
        mFirstStepAnimationEnded = ended;
    }

    public boolean isInAppDrawer() {
        return mInfo.container == ItemInfo.NO_ID;
    }

    public boolean isInDesktop() {
        return mInfo.isContainerDesktop();
    }

    public static String getDefaultFolderName() {
        return sDefaultFolderName;
    }

    public View getMainFooterView(){
        return mMainFooterView;
    }
}
