package com.android.launcher3.assistant.search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * 최대높이를 설정하는 RecyclerView
 */
public class MaxHeightRecyclerView extends RecyclerView {
    private int mMaxHeight = 700; // RecyclerView 의 최대높이

    public MaxHeightRecyclerView(@NonNull Context context) {
        super(context);
    }

    public MaxHeightRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxHeightRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        heightSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        super.onMeasure(widthSpec, heightSpec);
    }

    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
        requestLayout();
    }
}
