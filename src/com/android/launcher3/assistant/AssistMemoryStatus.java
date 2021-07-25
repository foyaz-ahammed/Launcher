package com.android.launcher3.assistant;

import static android.content.Context.ACTIVITY_SERVICE;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.android.launcher3.R;

/**
 * Assist page 에 Memory 상태표시를 위한 layout
 */
public class AssistMemoryStatus extends AssistItemContainer implements View.OnClickListener {

    int mLayoutWidth = 0; // layout 너비

    TextView mMemorySizeLabel;

    private static final long GIGA_TO_MEGA = 1024;
    private static final long ONE_MEGABYTE = 0x100000L;
    private static final int MEMORY_CHECK_DELAY = 500;
    Runnable mMemoryCheckThread; // Memory 검사를 위한 runnable
    float mTotalSizeGB = 0; // GB 단위의 Memory 총 크기
    float mAvailableSizeGB = 0; // GB 단위의 사용가능한 Memory 크기
    float mAvailableSizeGBOnAnimation = 0; // GB 단위의 Animation 상태에서 보여주는 Memory 크기
    long mTotalSizeMB = 0; // MB 단위의 Memory 총 크기
    long mAvailableSizeMB = 0; // MB 단위의 사용가능한 Memory 크기
    float mAnimateValue = 0; // Animation 상태에서의 사용가능한 Memory 크기

    float mBgRadius;
    Path mClippedPath = new Path();
    RectF mViewRect = new RectF();
    Paint mPaint = new Paint();

    Animator mFlowAnimator = null;

    public AssistMemoryStatus(Context context) {
        this(context, null);
    }

    public AssistMemoryStatus(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistMemoryStatus(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPaint.setColor(mLauncher.getColor(R.color.used_memory_fill_color));
        mBgRadius = context.getResources().getDimension(R.dimen.assist_bg_round_rect_radius);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mMemorySizeLabel = findViewById(R.id.memory_size);
        // 0.5초마다 메모리상태 검사
        mMemoryCheckThread = new Runnable() {
            @Override
            public void run() {

                MemoryInfo mi = new MemoryInfo();
                ActivityManager activityManager = (ActivityManager) mLauncher.getSystemService(ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);

                long totalMegs = mi.totalMem/ONE_MEGABYTE;
                long availableMegs = mi.availMem/ONE_MEGABYTE;

                boolean changed = mTotalSizeMB != totalMegs || mAvailableSizeMB != availableMegs;
                if(changed){
                    updateMemory(totalMegs, availableMegs);
                }

                postDelayed(mMemoryCheckThread, MEMORY_CHECK_DELAY);
            }
        };
        post(mMemoryCheckThread);

        setOnClickListener(this);
    }

    @Override
    public void dispatchDraw(Canvas canvas){
//        mClippedPath.addRoundRect(mViewRect, mBgRadius, mBgRadius, Path.Direction.CCW);
//        canvas.clipPath(mClippedPath);
//
//        //Now fill area for memory used size with blue color
//        if(mTotalSizeGB != 0 && mAvailableSizeGB != 0){
//            int width = getWidth();
//            int height = getHeight();
//
//            canvas.drawRect(0, mAnimateValue * height, width, height, mPaint);
//        }
        super.dispatchDraw(canvas);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        //Calculate widthspec size, and set it to measure
//        int widthSpec = MeasureSpec.makeMeasureSpec(mLayoutWidth, MeasureSpec.EXACTLY);
//        super.onMeasure(widthSpec, heightMeasureSpec);
//
//        //Set view path, and invalidate
//        mLayoutHeight = getMeasuredHeight();
//        mLayoutHeight = getMeasuredHeight();
//        mViewRect.set(0, 0, mLayoutWidth, mLayoutHeight);
//        invalidate();
//    }

    /**
     * Memory 상태 갱신
     * @param totalSize Memory 총 크기
     * @param availableSize 사용가능한 크기
     */
    public void updateMemory(long totalSize, long availableSize){
        mTotalSizeMB = totalSize;
        mAvailableSizeMB = availableSize;

        float totalSizeGigaByte = Math.round(mTotalSizeMB*10f/GIGA_TO_MEGA)/10f;
        float availableSizeGigaByte = Math.round(mAvailableSizeMB*10f/GIGA_TO_MEGA)/10f;

        if(mTotalSizeGB != totalSizeGigaByte || mAvailableSizeGB != availableSizeGigaByte) {
            mTotalSizeGB = totalSizeGigaByte;
            mAvailableSizeGB = availableSizeGigaByte;

            String memoryText = String.format("%sGB/%sGB", mAvailableSizeGB, mTotalSizeGB);
            mMemorySizeLabel.setText(memoryText);
        }

        mAnimateValue = mAvailableSizeMB * 1f / mTotalSizeMB;
        invalidate();
    }

    public void setWidth(int width){
        mLayoutWidth = width;
    }

    @Override
    public void onClick(View v) {
        // 이 layout 을 클릭하였을때 animation 재생
        if(v == this) {
            if(mFlowAnimator != null && mFlowAnimator.isRunning()){
                mFlowAnimator.pause();
                mFlowAnimator = null;
            }

            mAvailableSizeGBOnAnimation = 0;

            ValueAnimator animator = ValueAnimator.ofFloat(1, mAvailableSizeMB *1f / mTotalSizeMB);
            animator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mAnimateValue = (float)animation.getAnimatedValue();
                    float value = Math.round(animation.getAnimatedFraction() * mAvailableSizeGB * 10f)/10f;

                    if(value != mAvailableSizeGBOnAnimation) {
                        mAvailableSizeGBOnAnimation = value;
                        String memoryText = String.format("%sGB/%sGB", mAvailableSizeGBOnAnimation, mTotalSizeGB);
                        mMemorySizeLabel.setText(memoryText);
                    }

                    invalidate();
                }
            });

            animator.start();
            mFlowAnimator = animator;
        }
    }
}
