package com.android.launcher3.assistant;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.android.launcher3.R;

/**
 * 대화창을 위한 layout
 */
public class AssistMenuLayout extends LinearLayout {

    // layout 너비와 높이
    int mLayoutWidth = 0;
    int mLayoutHeight = 0;
    float mBgRadius; // 끝 모서리 아로진정도
    Path mClippedPath = new Path();
    RectF mViewRect = new RectF();

    public AssistMenuLayout(Context context) {
        this(context, null);
    }

    public AssistMenuLayout(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistMenuLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mBgRadius = context.getResources().getDimension(R.dimen.assist_bg_round_rect_radius);
    }

    public void setWidth(int width){
        mLayoutWidth = width;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Calculate widthspec size, and set it to measure
        int widthSpec = MeasureSpec.makeMeasureSpec(mLayoutWidth, MeasureSpec.EXACTLY);
        super.onMeasure(widthSpec, heightMeasureSpec);

        //Set view path, and invalidate
        mLayoutHeight = getMeasuredHeight();
        mViewRect.set(0, 0, mLayoutWidth, mLayoutHeight);
        invalidate();
    }

    @Override
    public void dispatchDraw(Canvas canvas){
        mClippedPath.addRoundRect(mViewRect, mBgRadius, mBgRadius, Path.Direction.CCW);
        canvas.clipPath(mClippedPath);
        super.dispatchDraw(canvas);
    }
}
