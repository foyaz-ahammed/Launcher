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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Property;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import ch.deletescape.lawnchair.LawnchairLauncher;
import ch.deletescape.lawnchair.LawnchairPreferences;
import ch.deletescape.lawnchair.LawnchairUtilsKt;
import ch.deletescape.lawnchair.colors.ColorEngine;
import ch.deletescape.lawnchair.colors.ColorEngine.ResolveInfo;
import ch.deletescape.lawnchair.colors.ColorEngine.Resolvers;
import ch.deletescape.lawnchair.font.CustomFontManager;
import ch.deletescape.lawnchair.gestures.BlankGestureHandler;
import ch.deletescape.lawnchair.gestures.GestureController;
import ch.deletescape.lawnchair.gestures.GestureHandler;
import ch.deletescape.lawnchair.gestures.handlers.ViewSwipeUpGestureHandler;
import ch.deletescape.lawnchair.override.CustomInfoProvider;
import com.android.launcher3.IconCache.IconLoadRequest;
import com.android.launcher3.IconCache.ItemInfoUpdateReceiver;
import com.android.launcher3.Launcher.OnResumeCallback;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.badge.BadgeRenderer;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.graphics.BitmapInfo;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.graphics.IconPalette;
import com.android.launcher3.graphics.PreloadIconDrawable;
import com.android.launcher3.model.PackageItemInfo;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Workspace의 Shortcut, 등록부 아이콘, AppDrawer에서 App아이콘들을 보여주는데 리용한다.
 *
 * @see CellLayout
 */
public class BubbleTextView extends AppCompatTextView implements ItemInfoUpdateReceiver, OnResumeCallback,
        ColorEngine.OnColorChangeListener {

    private boolean mHideText;

    private static final int DISPLAY_WORKSPACE = 0;
    private static final int DISPLAY_ALL_APPS = 1;
    private static final int DISPLAY_FOLDER = 2;
    private static final int DISPLAY_DRAWER_FOLDER = 5;
    private static final float MARK_ICON_SCALE_FROM_PARENT = 0.3f;

    private static final int[] STATE_PRESSED = new int[] {android.R.attr.state_pressed};
    private boolean mIsChecked = false;
    private boolean mIsDragging = false;

    @Keep
    private float mCheckIconAlpha = 1.0f;

    private boolean mIconFromFolderDialog = false;
    private boolean mIconFromAssistApps = false;
    private boolean mIconFromQuickAccess = false;
    private String mAppName = "";
    private final Paint mQAPaint = new Paint();

    //Quick Access(Assist페지 -> 빠른 접근)에서 보여줄 아이콘형태(true: +, false: -)
    private boolean mQuickAccessIconType = false;
    private ColorFilter mIconFilter;

    private static final Property<BubbleTextView, Float> BADGE_SCALE_PROPERTY
            = new Property<BubbleTextView, Float>(Float.TYPE, "badgeScale") {
        @Override
        public Float get(BubbleTextView bubbleTextView) {
            return bubbleTextView.mBadgeScale;
        }

        @Override
        public void set(BubbleTextView bubbleTextView, Float value) {
            bubbleTextView.mBadgeScale = value;
            bubbleTextView.invalidate();
        }
    };

    public static final Property<BubbleTextView, Float> TEXT_ALPHA_PROPERTY
            = new Property<BubbleTextView, Float>(Float.class, "textAlpha") {
        @Override
        public Float get(BubbleTextView bubbleTextView) {
            return bubbleTextView.mTextAlpha;
        }

        @Override
        public void set(BubbleTextView bubbleTextView, Float alpha) {
            bubbleTextView.setTextAlpha(alpha);
        }
    };

    private final BaseDraggingActivity mActivity;
    private Drawable mIcon;
    private final boolean mCenterVertically;

    private final CheckLongPressHelper mLongPressHelper;
    private final StylusEventHelper mStylusEventHelper;
    private final float mSlop;

    private final boolean mLayoutHorizontal;
    private int mIconSize;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mIsIconVisible = true;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mTextColor;
    @ViewDebug.ExportedProperty(category = "launcher")
    private float mTextAlpha = 1;

    private BadgeInfo mBadgeInfo;
    private BadgeRenderer mBadgeRenderer;
    private int mBadgeColor;
    private float mBadgeScale;
    private boolean mForceHideBadge;
    private final Point mTempSpaceForBadgeOffset = new Point();
    private final Rect mTempIconBounds = new Rect();

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mStayPressed;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mIgnorePressedStateChange;
    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mDisableRelayout = false;

    private IconLoadRequest mIconLoadRequest;

    private GestureHandler mSwipeUpHandler;

    private ColorEngine colorEngine;

    public BubbleTextView(Context context) {
        this(context, null, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mActivity = LawnchairUtilsKt.getBaseDraggingActivityOrNull(context);
        if (mActivity == null) {
            mLayoutHorizontal = false;
            mIconSize = 0;
            mCenterVertically = true;
            mLongPressHelper = new CheckLongPressHelper(this);
            mStylusEventHelper = null;
            mSlop = 0;
            return;
        }
        DeviceProfile grid = mActivity.getDeviceProfile();
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.BubbleTextView, defStyle, 0);
        mLayoutHorizontal = a.getBoolean(R.styleable.BubbleTextView_layoutHorizontal, false);

        colorEngine = ColorEngine.getInstance(context);

        int display = a.getInteger(R.styleable.BubbleTextView_iconDisplay, DISPLAY_WORKSPACE);
        int defaultIconSize = grid.iconSizePx;
        LawnchairPreferences prefs = Utilities.getLawnchairPrefs(context);
        if (display == DISPLAY_WORKSPACE) {
            mHideText = prefs.getHideAppLabels();
            setTextSize(TypedValue.COMPLEX_UNIT_PX, isTextHidden() ? 0 : grid.iconTextSizePx);
            setCompoundDrawablePadding(grid.iconDrawablePaddingPx);
            int lines = prefs.getHomeLabelRows();
            setLineCount(lines);
            colorEngine.addColorChangeListeners(this, Resolvers.WORKSPACE_ICON_LABEL);
        } else if (display == DISPLAY_ALL_APPS) {
            mHideText = prefs.getHideAllAppsAppLabels();
            setTextSize(TypedValue.COMPLEX_UNIT_PX, isTextHidden() ? 0 : grid.allAppsIconTextSizePx);
            setCompoundDrawablePadding(grid.allAppsIconDrawablePaddingPx);
            defaultIconSize = grid.allAppsIconSizePx;
            int lines = prefs.getDrawerLabelRows();
            setLineCount(lines);
            colorEngine.addColorChangeListeners(this, Resolvers.ALLAPPS_ICON_LABEL);
        } else if (display == DISPLAY_FOLDER) {
            mHideText = prefs.getHideAppLabels();
            setTextSize(TypedValue.COMPLEX_UNIT_PX, isTextHidden() ? 0 : grid.folderChildTextSizePx);
            setCompoundDrawablePadding(grid.folderChildDrawablePaddingPx);
            defaultIconSize = grid.folderChildIconSizePx;
            int lines = prefs.getHomeLabelRows();
            setLineCount(lines);
        } else if (display == DISPLAY_DRAWER_FOLDER) {
            mHideText = prefs.getHideAllAppsAppLabels();
            setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    isTextHidden() ? 0 : grid.allAppsFolderChildTextSizePx);
            setCompoundDrawablePadding(grid.allAppsFolderChildDrawablePaddingPx);
            defaultIconSize = grid.allAppsFolderChildIconSizePx;
            int lines = prefs.getDrawerLabelRows();
            setLineCount(lines);
        }
        CustomFontManager customFontManager = CustomFontManager.Companion.getInstance(context);
        int customFontType = getCustomFontType(display);
        if (customFontType != -1) {
            customFontManager.setCustomFont(this, customFontType);
        } else {
            customFontManager.loadCustomFont(this, attrs);
        }
        mCenterVertically = a.getBoolean(R.styleable.BubbleTextView_centerVertically, false);

        mIconSize = a.getDimensionPixelSize(R.styleable.BubbleTextView_iconSizeOverride,
                defaultIconSize);
        a.recycle();

        mLongPressHelper = new CheckLongPressHelper(this);
        mStylusEventHelper = new StylusEventHelper(new SimpleOnStylusPressListener(this), this);

        setAccessibilityDelegate(mActivity.getAccessibilityDelegate());
        setTextAlpha(1f);
    }

    public void setLineCount(int lines) {
        setMaxLines(lines);
        setSingleLine(lines == 1);
        setEllipsize(TextUtils.TruncateAt.END);
        // This shouldn't even be needed, what is going on?!
        setLines(lines);
    }

    public void setColorResolver(String resolver) {
        colorEngine.removeColorChangeListeners(this);
        colorEngine.addColorChangeListeners(this, resolver);
    }

    protected int getCustomFontType(int display) {
        switch (display) {
            case DISPLAY_ALL_APPS:
                return CustomFontManager.FONT_ALL_APPS_ICON;
            case DISPLAY_FOLDER:
                return CustomFontManager.FONT_FOLDER_ICON;
            case DISPLAY_DRAWER_FOLDER:
                return CustomFontManager.FONT_DRAWER_FOLDER;
            default:
                return -1;
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        // Disable marques when not focused to that, so that updating text does not cause relayout.
        setEllipsize(focused ? TruncateAt.MARQUEE : TruncateAt.END);
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    /**
     * Resets the view so it can be recycled.
     */
    public void reset() {
        mBadgeInfo = null;
        mBadgeColor = Color.TRANSPARENT;
        mBadgeScale = 0f;
        mForceHideBadge = false;
    }

    public void applyFromShortcutInfo(ShortcutInfo info) {
        applyFromShortcutInfo(info, false);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, boolean promiseStateChanged) {
        applyIconAndLabel(info);
        applySwipeUpAction(info);
        setTag(info);
        if (promiseStateChanged || (info.hasPromiseIconUi())) {
            applyPromiseState(promiseStateChanged);
        }

        applyBadgeState(info, false /* animate */);
    }

    public void applyFromApplicationInfo(AppInfo info) {
        applyIconAndLabel(info);

        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();

        if (info instanceof PromiseAppInfo) {
            PromiseAppInfo promiseAppInfo = (PromiseAppInfo) info;
            applyProgressLevel(promiseAppInfo.level);
        }
        applyBadgeState(info, false /* animate */);
    }

    public void applyFromPackageItemInfo(PackageItemInfo info) {
        applyIconAndLabel(info);
        // We don't need to check the info since it's not a ShortcutInfo
        super.setTag(info);

        // Verify high res immediately
        verifyHighRes();
    }

    private void applyIconAndLabel(ItemInfoWithIcon info) {
        FastBitmapDrawable iconDrawable = DrawableFactory.get(getContext()).newIcon(info);
        iconDrawable.setParentTextView(this);
        mBadgeColor = IconPalette.getMutedColor(getContext(), info.iconColor, 0.54f);

        setIcon(iconDrawable);
        if (!isTextHidden())
            setText(getTitle(info));
        if (info.contentDescription != null) {
            setContentDescription(info.isDisabled()
                    ? getContext().getString(R.string.disabled_app_label, info.contentDescription)
                    : info.contentDescription);
        }
    }

    public void applyIcon(ItemInfoWithIcon info) {
        FastBitmapDrawable iconDrawable = DrawableFactory.get(getContext()).newIcon(info);
        iconDrawable.setParentTextView(this);
        mBadgeColor = IconPalette.getMutedColor(getContext(), info.iconColor, 0.54f);

        setIcon(iconDrawable);
    }

    public void applyIcon(BitmapInfo info) {
        FastBitmapDrawable iconDrawable = new FastBitmapDrawable(info);
        iconDrawable.setParentTextView(this);
        mBadgeColor = IconPalette.getMutedColor(getContext(), info.color, 0.54f);

        setIcon(iconDrawable);
    }

    private void applySwipeUpAction(ShortcutInfo info) {
        GestureHandler handler = GestureController.Companion.createGestureHandler(
                getContext(), info.swipeUpAction, new BlankGestureHandler(getContext(), null));
        if (handler instanceof BlankGestureHandler) {
            mSwipeUpHandler = null;
        } else {
            mSwipeUpHandler = new ViewSwipeUpGestureHandler(this, handler);
        }
    }

    private CharSequence getTitle(ItemInfo info) {
        CustomInfoProvider<ItemInfo> customInfoProvider = CustomInfoProvider.Companion.forItem(getContext(), info);
        if (customInfoProvider != null) {
            return customInfoProvider.getTitle(info);
        } else {
            return info.title;
        }
    }

    /**
     * Overrides the default long press timeout.
     */
    public void setLongPressTimeout(int longPressTimeout) {
        mLongPressHelper.setLongPressTimeout(longPressTimeout);
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    //Currently ignore refreshDrawableState
    @Override
    public void refreshDrawableState() {
        //if it is from folder dialog do not refresh the state
        if (!mIgnorePressedStateChange) {
            //do not call super function
            super.refreshDrawableState();
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mStayPressed) {
            mergeDrawableStates(drawableState, STATE_PRESSED);
        }
        return drawableState;
    }

    /** Returns the icon for this view. */
    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP

        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            int left = getPaddingLeft();
            int right = getWidth() - getPaddingRight();
            int top = getPaddingTop();
            int bottom = getPaddingTop() + getWidth() + (int) (getTextSize());

            if (!(event.getX() >= left && event.getX() <= right && event.getY() >= top
                    && event.getY() <= bottom))
                return false;
        }

        boolean result = super.onTouchEvent(event);

        // Check for a stylus button press, if it occurs cancel any long press checks.
        if (mStylusEventHelper.onMotionEvent(event)) {
            mLongPressHelper.cancelLongPress();
            result = true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // If we're in a stylus button press, don't check for long press.
                if (!mStylusEventHelper.inStylusButtonPressed()) {
                    mLongPressHelper.postCheckForLongPress();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLongPressHelper.cancelLongPress();

                //Remove Dark
                if(mIcon instanceof FastBitmapDrawable){
                    ((FastBitmapDrawable)mIcon).setNormalIcon();
                }
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

    public void setStayPressed(boolean stayPressed) {
        mStayPressed = stayPressed;
        refreshDrawableState();
    }

    @Override
    public void onLauncherResume() {
        // Reset the pressed state of icon that was locked in the press state while activity
        // was launching
        setStayPressed(false);
    }

    void clearPressedBackground() {
        setPressed(false);
        setStayPressed(false);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Unlike touch events, keypress event propagate pressed state change immediately,
        // without waiting for onClickHandler to execute. Disable pressed state changes here
        // to avoid flickering.
        mIgnorePressedStateChange = true;
        boolean result = super.onKeyUp(keyCode, event);
        mIgnorePressedStateChange = false;
        refreshDrawableState();
        return result;
    }

    @SuppressWarnings("wrongcall")
    protected void drawWithoutBadge(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
        invalidate();
    }

    public void toggleChecked() {
        mIsChecked = !mIsChecked;
        invalidate();
    }

    public boolean isChecked(){
        return mIsChecked;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean drawCheck = shouldDrawCheck() && !mIsDragging;
        boolean drawPlus = mIconFromQuickAccess;

        if(drawCheck || drawPlus) {
            //move the canvas
            canvas.translate(getScrollX(), getScrollY());

            //Calculate mark icon size
            int markIconSize = (int) (mIcon.getIntrinsicWidth() * MARK_ICON_SCALE_FROM_PARENT);

            //Calculate drawing icon area
            int entireWidth = getWidth();
            int iconWidth = mIcon.getIntrinsicWidth();
            int markIconLeft = iconWidth + (entireWidth - iconWidth)/2 - markIconSize;
            int markIconTop = getPaddingTop();

            final Drawable d;
            if(drawCheck){
                if(mIconFromFolderDialog)
                    markIconLeft = (entireWidth - iconWidth)/2;

                //Get icon
                if(mIsChecked) {
                    d = getResources().getDrawable(R.drawable.ic_icon_checked, null);
                    d.setAlpha((int)(mCheckIconAlpha*255));
                }
                else {
                    d = getResources().getDrawable(R.drawable.ic_icon_unchecked, null);
                    d.setAlpha((int)(mCheckIconAlpha*255));
                    if(mIconFromFolderDialog && mIconFilter != null) {
                        d.setColorFilter(mIconFilter);
                    }
                }
            }
            else{
                //Get icon
                if(mQuickAccessIconType) {
                    d = getResources().getDrawable(R.drawable.ic_icon_plus, null);
                }
                else {
                    d = getResources().getDrawable(R.drawable.ic_icon_minus, null);
                }
            }

            //Now draw
            d.setBounds(markIconLeft, markIconTop,
                    markIconLeft + markIconSize, markIconTop + markIconSize);
            d.draw(canvas);
        }
        else {
            drawBadgeIfNecessary(canvas);
        }

        //If icon is form quick access, then draw the app name at bottom
        if(mIconFromQuickAccess) {
            int bottom = (int) (getPaddingTop() + getCompoundPaddingTop() + getTextSize() + 60);

            Rect bounds = new Rect();
            mQAPaint.getTextBounds(mAppName, 0, mAppName.length(), bounds);
            int height = bounds.height();
            int width = bounds.width();

            //set padding 10
            canvas.drawText(mAppName, (getWidth() - width)*0.5f, bottom - getResources().getDimension(R.dimen.quick_access_app_text_size), mQAPaint);
        }
    }

    public void setDragging(boolean dragging) {
        mIsDragging = dragging;
        invalidate();
    }

    /**
     * Draws the icon badge in the top right corner of the icon bounds.
     * @param canvas The canvas to draw to.
     */
    protected void drawBadgeIfNecessary(Canvas canvas) {
        if (!mForceHideBadge && (hasBadge() || mBadgeScale > 0)) {
            getIconBounds(mTempIconBounds);
            mTempSpaceForBadgeOffset.set((getWidth() - mIconSize) / 2, getPaddingTop());
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.translate(scrollX, scrollY);
            mBadgeRenderer.draw(canvas, mBadgeColor, mTempIconBounds, mBadgeScale,
                    mTempSpaceForBadgeOffset,
                    mBadgeInfo == null ? -1 : mBadgeInfo.getNotificationCount());
            canvas.translate(-scrollX, -scrollY);
        }
    }

    public void forceHideBadge(boolean forceHideBadge) {
        if (mForceHideBadge == forceHideBadge) {
            return;
        }
        mForceHideBadge = forceHideBadge;

        if (forceHideBadge) {
            invalidate();
        } else if (hasBadge()) {
            ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, 0, 1).start();
        }
    }

    public boolean isOnHotseat() {
        ItemInfo itemInfo = (ItemInfo) getTag();
        return itemInfo.isContainerHotSeat();
    }

    public boolean isInFolder() {
        if(getParent() instanceof ShortcutAndWidgetContainer){
            return ((ShortcutAndWidgetContainer)getParent()).isFolder();
        }
        return false;
    }

    public Folder getParentFolder() {
        if(isInFolder())
            return (Folder)(getParent().getParent().getParent().getParent().getParent());
        return null;
    }

    private boolean hasBadge() {
        return mBadgeInfo != null;
    }

    public void getIconBounds(Rect outBounds) {
        int top = getPaddingTop();
        int left = (getWidth() - mIconSize) / 2;
        int right = left + mIconSize;
        int bottom = top + mIconSize;
        outBounds.set(left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCenterVertically) {
            Paint.FontMetrics fm = getPaint().getFontMetrics();
            int cellHeightPx = mIconSize + getCompoundDrawablePadding() +
                    (int) Math.ceil(fm.bottom - fm.top);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setPadding(getPaddingLeft(), (height - cellHeightPx) / 2, getPaddingRight(),
                    getPaddingBottom());
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onColorChange(@NotNull ResolveInfo resolveInfo) {
        setTextColor(resolveInfo.getColor());
    }

    @Override
    public void setTextColor(int color) {
        mTextColor = color;
        super.setTextColor(getModifiedColor());
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        mTextColor = colors.getDefaultColor();
        if (Float.compare(mTextAlpha, 1) == 0) {
            super.setTextColor(colors);
        } else {
            super.setTextColor(getModifiedColor());
        }
    }

    public boolean shouldTextBeVisible() {
        Object tag = getParent() instanceof FolderIcon ? ((View) getParent()).getTag() : getTag();
        ItemInfo info = tag instanceof ItemInfo ? (ItemInfo) tag : null;
        if (info != null && info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            return !Utilities.getLawnchairPrefs(getContext()).getHideDockLabels();
        }
        return true;
    }

    public void setTextVisibility(boolean visible) {
        setTextAlpha(visible ? 1 : 0);
    }

    private void setTextAlpha(float alpha) {
        mTextAlpha = alpha;
        super.setTextColor(getModifiedColor());
    }

    private int getModifiedColor() {
        if (mTextAlpha == 0) {
            // Special case to prevent text shadows in high contrast mode
            return Color.TRANSPARENT;
        }
        return ColorUtils.setAlphaComponent(
                mTextColor, Math.round(Color.alpha(mTextColor) * mTextAlpha));
    }

    /**
     * Creates an animator to fade the text in or out.
     * @param fadeIn Whether the text should fade in or fade out.
     */
    public ObjectAnimator createTextAlphaAnimator(boolean fadeIn) {
        float toAlpha = shouldTextBeVisible() && fadeIn ? 1 : 0;
        return ObjectAnimator.ofFloat(this, TEXT_ALPHA_PROPERTY, toAlpha);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    public void applyPromiseState(boolean promiseStateChanged) {
        if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();
            final boolean isPromise = info.hasPromiseIconUi();
            final int progressLevel = isPromise ?
                    ((info.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE) ?
                            info.getInstallProgress() : 0)) : 100;

            PreloadIconDrawable preloadDrawable = applyProgressLevel(progressLevel);
            if (preloadDrawable != null && promiseStateChanged) {
                preloadDrawable.maybePerformFinishedAnimation();
            }
        }
    }

    public PreloadIconDrawable applyProgressLevel(int progressLevel) {
        if (getTag() instanceof ItemInfoWithIcon) {
            ItemInfoWithIcon info = (ItemInfoWithIcon) getTag();
            if (progressLevel >= 100) {
                setContentDescription(info.contentDescription != null
                        ? info.contentDescription : "");
            } else if (progressLevel > 0) {
                setContentDescription(getContext()
                        .getString(R.string.app_downloading_title, info.title,
                                NumberFormat.getPercentInstance().format(progressLevel * 0.01)));
            } else {
                setContentDescription(getContext()
                        .getString(R.string.app_waiting_download_title, info.title));
            }
            if (mIcon != null) {
                final PreloadIconDrawable preloadDrawable;
                if (mIcon instanceof PreloadIconDrawable) {
                    preloadDrawable = (PreloadIconDrawable) mIcon;
                    preloadDrawable.setLevel(progressLevel);
                } else {
                    preloadDrawable = DrawableFactory.get(getContext())
                            .newPendingIcon(info, getContext());
                    preloadDrawable.setLevel(progressLevel);
                    setIcon(preloadDrawable);
                }
                return preloadDrawable;
            }
        }
        return null;
    }

    public void applyBadgeState(ItemInfo itemInfo, boolean animate) {
        if (mIcon instanceof FastBitmapDrawable) {
            boolean wasBadged = mBadgeInfo != null;
            mBadgeInfo = mActivity.getBadgeInfoForItem(itemInfo);
            boolean isBadged = mBadgeInfo != null;
            float newBadgeScale = isBadged ? 1f : 0;
            mBadgeRenderer = mActivity.getDeviceProfile().mBadgeRenderer;
            if (wasBadged || isBadged) {
                // Animate when a badge is first added or when it is removed.
                if (animate && (wasBadged ^ isBadged) && isShown()) {
                    ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, newBadgeScale).start();
                } else {
                    mBadgeScale = newBadgeScale;
                    invalidate();
                }
            }
            if (itemInfo.contentDescription != null) {
                if (hasBadge()) {
                    int count = mBadgeInfo.getNotificationCount();
                    setContentDescription(getContext().getResources().getQuantityString(
                            R.plurals.badged_app_label, count, itemInfo.contentDescription, count));
                } else {
                    setContentDescription(itemInfo.contentDescription);
                }
            }
        }
    }

    /**
     * Sets the icon for this view based on the layout direction.
     */
    public void setIcon(Drawable icon) {
        if (mIsIconVisible) {
            applyCompoundDrawables(icon);
        }
        mIcon = icon;
    }

    public void clearIcon() {
        mIcon = null;
        setCompoundDrawables(null, null, null, null);
    }

    public void setIconVisible(boolean visible) {
        mIsIconVisible = visible;
        Drawable icon = visible ? mIcon : new ColorDrawable(Color.TRANSPARENT);
        applyCompoundDrawables(icon);
    }

    protected void applyCompoundDrawables(Drawable icon) {
        if (icon == null) return;

        // If we had already set an icon before, disable relayout as the icon size is the
        // same as before.
        mDisableRelayout = mIcon != null;

        icon.setBounds(0, 0, mIconSize, mIconSize);
        if (mLayoutHorizontal) {
            setCompoundDrawablesRelative(icon, null, null, null);
        } else {
            setCompoundDrawables(null, icon, null, null);
        }
        mDisableRelayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mDisableRelayout) {
            super.requestLayout();
        }
    }

    /**
     * Applies the item info if it is same as what the view is pointing to currently.
     */
    @Override
    public void reapplyItemInfo(ItemInfoWithIcon info) {
        if (getTag() == info) {
            mIconLoadRequest = null;
            mDisableRelayout = true;

            // Optimization: Starting in N, pre-uploads the bitmap to RenderThread.
            info.iconBitmap.prepareToDraw();

            if (info instanceof AppInfo) {
                applyFromApplicationInfo((AppInfo) info);
            } else if (info instanceof ShortcutInfo) {
                applyFromShortcutInfo((ShortcutInfo) info);
                mActivity.invalidateParent(info);
            } else if (info instanceof PackageItemInfo) {
                applyFromPackageItemInfo((PackageItemInfo) info);
            }

            mDisableRelayout = false;
        }
    }

    /**
     * Verifies that the current icon is high-res otherwise posts a request to load the icon.
     */
    public void verifyHighRes() {
        verifyHighRes(BubbleTextView.this);
    }

    public void verifyHighRes(ItemInfoUpdateReceiver callback) {
        if (mIconLoadRequest != null) {
            mIconLoadRequest.cancel();
            mIconLoadRequest = null;
        }
        if (getTag() instanceof ItemInfoWithIcon) {
            ItemInfoWithIcon info = (ItemInfoWithIcon) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance(getContext()).getIconCache()
                        .updateIconInBackground(callback, info);
            }
        }
    }

    @Keep
    public void setCheckIconAlpha(float alpha) {
        mCheckIconAlpha = alpha;
        invalidate();
    }

    private boolean shouldDrawCheck() {
        if(getParent() instanceof ShortcutAndWidgetContainer) {
            return ((ShortcutAndWidgetContainer)getParent()).shouldDrawCheck();
        }

        return mIconFromFolderDialog;
    }

    public int getIconSize() {
        return mIconSize;
    }

    public void setIconSize(int iconSize) {
        mIconSize = iconSize;
        setIcon(mIcon);
    }

    public int getRank() {
        if(getTag() instanceof ItemInfo) {
            return ((ItemInfo)getTag()).rank;
        }
        return -1;
    }

    public void setIconFromFolderDialog(boolean isFrom) {
        mIconFromFolderDialog = isFrom;
        if(isFrom)
            mIconFilter = new PorterDuffColorFilter(Color.parseColor("#FFBBBBBB"), PorterDuff.Mode.SRC_ATOP);
    }

    public void setIconFromAssistApps(boolean isFrom) {
        mIconFromAssistApps = isFrom;
        if(isFrom)
            mIconFilter = new PorterDuffColorFilter(Color.parseColor("#FFCCCCCC"), PorterDuff.Mode.SRC_ATOP);
    }

    //icon type true: draw +, false: draw -
    public void setIconFromQuickAccess(boolean isFrom, boolean iconType) {
        mIconFromQuickAccess = isFrom;
        mQuickAccessIconType = iconType;

        Resources resources = getResources();
        mQAPaint.setTextSize(resources.getDimension(R.dimen.quick_access_app_text_size));
        if(iconType) {
            mQAPaint.setColor(resources.getColor(R.color.quick_access_app_text_color_primary, null));
        }
        else{
            mQAPaint.setColor(resources.getColor(R.color.quick_access_app_text_color_secondary, null));
        }
    }

    public void setQuickAccessAppName(String appName) {
        mAppName = appName;
    }

    public boolean isIconFromFolderDialog() {
        return mIconFromFolderDialog;
    }

    public boolean isIconFromAssistApps() {
        return mIconFromAssistApps;
    }

    public boolean isIconFromQuickAccess() {
        return mIconFromQuickAccess;
    }

    public boolean getQuickAccessIconType() {
        return mQuickAccessIconType;
    }

    protected boolean isTextHidden() {
        return mHideText;
    }

    public int getBadgeColor() {
        return mBadgeColor;
    }
}
