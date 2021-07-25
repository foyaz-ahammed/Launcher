/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

import static com.android.launcher3.anim.Interpolators.ACCEL_2;
import static com.android.launcher3.states.RotationHelper.REQUEST_NONE;

import android.graphics.Rect;
import android.view.animation.Interpolator;

import ch.deletescape.lawnchair.states.HomeState;
import ch.deletescape.lawnchair.states.OptionsState;
import com.android.launcher3.states.SpringLoadedState;
import com.android.launcher3.uioverrides.AllAppsState;
import com.android.launcher3.uioverrides.FastOverviewState;
import com.android.launcher3.uioverrides.OverviewState;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.userevent.nano.LauncherLogProto.ContainerType;

import java.util.Arrays;


/**
 * 현재 Launcher의 상태를 나타내는 클라스
 */
public class LauncherState {
    /**
     * Set of elements indicating various workspace elements which change visibility across states
     * Note that workspace is not included here as in that case, we animate individual pages
     */
    public static final int NONE = 0;
    public static final int HOTSEAT_ICONS = 1;
    public static final int ALL_APPS_HEADER = 1 << 1;
    public static final int ALL_APPS_HEADER_EXTRA = 1 << 2; // e.g. app predictions
    public static final int ALL_APPS_CONTENT = 1 << 3;
    public static final int VERTICAL_SWIPE_INDICATOR = 1 << 4;
    public static final int OPTIONS_VIEW = 1 << 5;

    //기발상수들
    protected static final int FLAG_MULTI_PAGE = 1;
    protected static final int FLAG_DISABLE_ACCESSIBILITY = 1 << 1;
    protected static final int FLAG_DISABLE_RESTORE = 1 << 2;
    protected static final int FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED = 1 << 3;
    protected static final int FLAG_DISABLE_PAGE_CLIPPING = 1 << 4;
    protected static final int FLAG_PAGE_BACKGROUNDS = 1 << 5;
    protected static final int FLAG_DISABLE_INTERACTION = 1 << 6;
    protected static final int FLAG_OVERVIEW_UI = 1 << 7;
    protected static final int FLAG_HIDE_BACK_BUTTON = 1 << 8;
    protected static final int FLAG_HAS_SYS_UI_SCRIM = 1 << 9;

    protected static final PageAlphaProvider DEFAULT_ALPHA_PROVIDER =
            new PageAlphaProvider(ACCEL_2) {
                @Override
                public float getPageAlpha(int pageIndex) {
                    return 1;
                }
            };

    /**
     * Launcher state 배렬
     * 아래의 6가지 Launcher state들을 포함하고 있다.
     */
    private static final LauncherState[] sAllStates = new LauncherState[6];

    /**
     * 개별적인 Launcher state들
     */
    public static final LauncherState NORMAL = new HomeState(0, ContainerType.WORKSPACE, 0,
            FLAG_DISABLE_RESTORE | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED | FLAG_HIDE_BACK_BUTTON |
                    FLAG_HAS_SYS_UI_SCRIM);
    public static final LauncherState SPRING_LOADED = new SpringLoadedState(1);
    public static final LauncherState OVERVIEW = new OverviewState(2);
    public static final LauncherState FAST_OVERVIEW = new FastOverviewState(3);
    public static final LauncherState ALL_APPS = new AllAppsState(4);
    public static final LauncherState OPTIONS = new OptionsState(5);

    protected static final Rect sTempRect = new Rect();

    public final int ordinal;

    /**
     * Used for containerType in {@link com.android.launcher3.logging.UserEventDispatcher}
     */
    public final int containerType;

    /**
     * True if the state can be persisted across activity restarts.
     */
    public final boolean disableRestore;

    /**
     * True if workspace has multiple pages visible.
     */
    public final boolean hasMultipleVisiblePages;

    /**
     * Accessibility flag for workspace and its pages.
     * @see android.view.View#setImportantForAccessibility(int)
     */
    public final int workspaceAccessibilityFlag;

    /**
     * Properties related to state transition animation
     *
     * @see WorkspaceStateTransitionAnimation
     */
    public final boolean hasWorkspacePageBackground;

    public final int transitionDuration;

    /**
     * True if the state allows workspace icons to be dragged.
     */
    public final boolean workspaceIconsCanBeDragged;

    /**
     * True if the workspace pages should not be clipped relative to the workspace bounds
     * for this state.
     */
    public final boolean disablePageClipping;

    /**
     * True if launcher can not be directly interacted in this state;
     */
    public final boolean disableInteraction;

    /**
     * True if the state has overview panel visible.
     */
    public final boolean overviewUi;

    /**
     * True if the back button should be hidden when in this state (assuming no floating views are
     * open, launcher has window focus, etc).
     */
    public final boolean hideBackButton;

    public final boolean hasSysUiScrim;

    public LauncherState(int id, int containerType, int transitionDuration, int flags) {
        this.containerType = containerType;
        this.transitionDuration = transitionDuration;

        this.hasWorkspacePageBackground = (flags & FLAG_PAGE_BACKGROUNDS) != 0;
        this.hasMultipleVisiblePages = (flags & FLAG_MULTI_PAGE) != 0;
        this.workspaceAccessibilityFlag = (flags & FLAG_DISABLE_ACCESSIBILITY) != 0
                ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : IMPORTANT_FOR_ACCESSIBILITY_AUTO;
        this.disableRestore = (flags & FLAG_DISABLE_RESTORE) != 0;
        this.workspaceIconsCanBeDragged = (flags & FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED) != 0;
        this.disablePageClipping = (flags & FLAG_DISABLE_PAGE_CLIPPING) != 0;
        this.disableInteraction = (flags & FLAG_DISABLE_INTERACTION) != 0;
        this.overviewUi = (flags & FLAG_OVERVIEW_UI) != 0;
        this.hideBackButton = (flags & FLAG_HIDE_BACK_BUTTON) != 0;
        this.hasSysUiScrim = (flags & FLAG_HAS_SYS_UI_SCRIM) != 0;

        this.ordinal = id;
        sAllStates[id] = this;
    }

    public static LauncherState[] values() {
        return Arrays.copyOf(sAllStates, sAllStates.length);
    }

    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        return new float[] {1, 0, 0};
    }

    /**
     * Returns 2 floats designating how to transition overview:
     *   scale for the current and adjacent pages
     *   translationY factor where 0 is top aligned and 0.5 is centered vertically
     */
    public float[] getOverviewScaleAndTranslationYFactor(Launcher launcher) {
        return new float[] {1.1f, 0f};
    }

    public void onStateEnabled(Launcher launcher) {
        dispatchWindowStateChanged(launcher);
    }

    public void onStateDisabled(Launcher launcher) { }

    public int getVisibleElements(Launcher launcher) {
        if (launcher.getDeviceProfile().isVerticalBarLayout()) {
            return HOTSEAT_ICONS | VERTICAL_SWIPE_INDICATOR;
        }
        return HOTSEAT_ICONS | VERTICAL_SWIPE_INDICATOR;
    }

    /**
     * Fraction shift in the vertical translation UI and related properties
     *
     * @see com.android.launcher3.allapps.AllAppsTransitionController
     */
    public float getVerticalProgress(Launcher launcher) {
        return 1f;
    }

    public float getScrimProgress(Launcher launcher) {
        return getVerticalProgress(launcher);
    }

    public float getWorkspaceScrimAlpha(Launcher launcher) {
        return 0;
    }

    public float getWorkspaceBlurAlpha(Launcher launcher) {
        return 0;
    }

    public String getDescription(Launcher launcher) {
        return launcher.getWorkspace().getCurrentPageDescription();
    }

    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        if (this != NORMAL || !launcher.getDeviceProfile().shouldFadeAdjacentWorkspaceScreens()) {
            return DEFAULT_ALPHA_PROVIDER;
        }
        final int centerPage = launcher.getWorkspace().getNextPage();
        return new PageAlphaProvider(ACCEL_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return  pageIndex != centerPage ? 0 : 1f;
            }
        };
    }

    public LauncherState getHistoryForState(LauncherState previousState) {
        // No history is supported
        return NORMAL;
    }

    /**
     * Called when the start transition ends and the user settles on this particular state.
     */
    public void onStateTransitionEnd(Launcher launcher) {
        if (this == NORMAL) {
            UiFactory.resetOverview(launcher);
        }
        if (this == NORMAL) {
            // Clear any rotation locks when going to normal state
            launcher.getRotationHelper().setCurrentStateRequest(REQUEST_NONE);
        }
    }

    protected static void dispatchWindowStateChanged(Launcher launcher) {
        launcher.getWindow().getDecorView().sendAccessibilityEvent(TYPE_WINDOW_STATE_CHANGED);
    }

    public static abstract class PageAlphaProvider {

        public final Interpolator interpolator;

        public PageAlphaProvider(Interpolator interpolator) {
            this.interpolator = interpolator;
        }

        public abstract float getPageAlpha(int pageIndex);
    }
}