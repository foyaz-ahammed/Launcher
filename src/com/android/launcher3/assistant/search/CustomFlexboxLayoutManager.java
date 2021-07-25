package com.android.launcher3.assistant.search;

import android.content.Context;

import com.google.android.flexbox.FlexboxLayoutManager;

/**
 * Scroll 상태를 설정하는 layout manager
 */
public class CustomFlexboxLayoutManager extends FlexboxLayoutManager {
    private boolean isScrollEnabled = true; // Scroll 가능상태

    public CustomFlexboxLayoutManager(Context context) {
        super(context);
    }

    public void setScrollEnabled(boolean isEnabled) {
        this.isScrollEnabled = isEnabled;
    }

    @Override
    public boolean canScrollVertically() {
        return isScrollEnabled && super.canScrollVertically();
    }
}
