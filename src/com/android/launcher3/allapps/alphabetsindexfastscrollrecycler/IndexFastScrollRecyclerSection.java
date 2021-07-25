package com.android.launcher3.allapps.alphabetsindexfastscrollrecycler;

/**
 * Created by MyInnos on 31-01-2017.
 */

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SectionIndexer;
import androidx.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsRecyclerView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 해당 index 로 Scrolling 을 위한 class
 */
public class IndexFastScrollRecyclerSection extends RecyclerView.AdapterDataObserver {

    private float mIndexbarWidth;
    private float mIndexbarMargin;
    private float mPreviewPadding;
    private float mDensity;
    private float mScaledDensity;
    private int mListViewWidth;
    private int mListViewHeight;
    private int mCurrentSection = -1;
    private boolean mIsIndexing = false;
    private AllAppsRecyclerView mRecyclerView = null;
    private SectionIndexer mIndexer = null;
    public String[] mSections = null;
    private RectF mIndexbarRect;

    private int setIndexTextSize;
    private float setIndexbarWidth;
    private float setIndexbarMargin;
    private int setPreviewPadding;
    private boolean previewVisibility = true;
    private int setIndexBarCornerRadius;
    private Typeface setTypeface = null;
    private Boolean setIndexBarVisibility = true;
    private Boolean setSetIndexBarHighLightTextVisibility = true;
    private Boolean setIndexBarStrokeVisibility = true;
    public int mIndexBarStrokeWidth;
    private @ColorInt
    int mIndexBarStrokeColor;
    private @ColorInt
    int indexbarBackgroudColor;
    private @ColorInt
    int indexbarTextColor;
    private @ColorInt
    int indexbarHighLightTextColor;

    private int setPreviewTextSize;
    private @ColorInt
    int previewBackgroundColor;
    private @ColorInt
    int previewTextColor;
    private int previewBackgroudAlpha;
    private int preViewRightMargin;
    private int indexbarBackgroudAlpha;

    private int indexPaintPaintColor = Color.WHITE;
    AttributeSet attrs;

    private int animTranslate = 0;

    public IndexFastScrollRecyclerSection(Context context, RecyclerView recyclerView) {

        setIndexTextSize = 12/*recyclerView.setIndexTextSize*/;
        setIndexbarWidth = 15/*recyclerView.mIndexbarWidth*/;
        setIndexbarMargin = 5/*recyclerView.mIndexbarMargin*/;
        setPreviewPadding = 5/*recyclerView.mPreviewPadding*/;
        setPreviewTextSize = 50/*recyclerView.mPreviewTextSize*/;
        previewBackgroundColor = Color.WHITE/*recyclerView.mPreviewBackgroudColor*/;
        previewTextColor = Color.BLACK/*recyclerView.mPreviewTextColor*/;
        preViewRightMargin = 90;
        previewBackgroudAlpha = convertTransparentValueToBackgroundAlpha(1f/*recyclerView.mPreviewTransparentValue*/);

        mIndexBarStrokeColor = Color.BLACK/*recyclerView.mSetIndexBarStrokeColor*/;
        mIndexBarStrokeWidth = 2/*recyclerView.mIndexBarStrokeWidth*/;

        setIndexBarCornerRadius = 5/*recyclerView.mIndexBarCornerRadius*/;
        indexbarBackgroudColor = context.getResources().getColor(R.color.all_apps_background_color)/*recyclerView.mIndexbarBackgroudColor*/;
        indexbarTextColor = Color.argb(150, 255, 255, 255)/*recyclerView.mIndexbarTextColor*/;
        indexbarHighLightTextColor = Color.BLACK/*recyclerView.indexbarHighLightTextColor*/;

        indexbarBackgroudAlpha = convertTransparentValueToBackgroundAlpha(0.3f/*recyclerView.mIndexBarTransparentValue*/);

        mDensity = context.getResources().getDisplayMetrics().density;
        mScaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        mRecyclerView = (AllAppsRecyclerView) recyclerView;
        setAdapter(mRecyclerView.getAdapter());

        mIndexbarWidth = setIndexbarWidth * mDensity;
        mIndexbarMargin = setIndexbarMargin * mDensity;
        mPreviewPadding = setPreviewPadding * mDensity;
    }

    public boolean isIndexing(){
        return mIsIndexing;
    }

    /**
     * 그리기함수
     */
    public void draw(Canvas canvas) {
        Log.w("IndexFast-draw", "curselection: " + mCurrentSection);
        canvas.translate(0, animTranslate);
        if (setIndexBarVisibility) {
            Paint indexbarPaint = new Paint();

            indexbarPaint.setColor(indexbarBackgroudColor);
            indexbarPaint.setAlpha(indexbarBackgroudAlpha);
            indexbarPaint.setAntiAlias(true);
//            canvas.drawRoundRect(mIndexbarRect, setIndexBarCornerRadius * mDensity, setIndexBarCornerRadius * mDensity, indexbarPaint);

            if (setIndexBarStrokeVisibility) {
//                indexbarPaint.setStyle(Paint.Style.STROKE);
//                indexbarPaint.setColor(mIndexBarStrokeColor);
//                indexbarPaint.setStrokeWidth(mIndexBarStrokeWidth); // set stroke width
//                canvas.drawRoundRect(mIndexbarRect, setIndexBarCornerRadius * mDensity, setIndexBarCornerRadius * mDensity, indexbarPaint);
            }

            if (mSections != null && mSections.length > 0) {
                // Preview is shown when mCurrentSection is set
                if (previewVisibility && mCurrentSection >= 0 && mSections[mCurrentSection] != ""
                        && mIsIndexing && mRecyclerView.foundLetterStarting(mSections[mCurrentSection])) {
                    Paint previewPaint = new Paint();
                    previewPaint.setColor(previewBackgroundColor);
                    previewPaint.setAlpha(previewBackgroudAlpha);
                    previewPaint.setAntiAlias(true);
                    previewPaint.setShadowLayer(3, 0, 0, Color.argb(64, 0, 0, 0));

                    Paint previewTextPaint = new Paint();
                    previewTextPaint.setColor(previewTextColor);
                    previewTextPaint.setAntiAlias(true);
                    previewTextPaint.setTextSize(setPreviewTextSize * mScaledDensity);
                    previewTextPaint.setTypeface(setTypeface);

                    float previewTextWidth = previewTextPaint.measureText(mSections[mCurrentSection]);
                    float previewSize = 2 * mPreviewPadding + previewTextPaint.descent() - previewTextPaint.ascent();
                    previewSize = Math.max(previewSize, previewTextWidth + 2 * mPreviewPadding);
                    RectF previewRect = new RectF(mListViewWidth - previewSize - preViewRightMargin
                            , (mListViewHeight - previewSize) / 2
                            , mListViewWidth - preViewRightMargin
                            , (mListViewHeight - previewSize) / 2 + previewSize);

                    canvas.drawRoundRect(previewRect, 5 * mDensity, 5 * mDensity, previewPaint);
                    canvas.drawText(mSections[mCurrentSection], previewRect.left + (previewSize - previewTextWidth) / 2 - 1
                            , previewRect.top + (previewSize - (previewTextPaint.descent() - previewTextPaint.ascent())) / 2 - previewTextPaint.ascent(), previewTextPaint);
                    fade(300);
                }

                Paint indexPaint = new Paint();
                indexPaint.setColor(indexbarTextColor);
                indexPaint.setAntiAlias(true);
                indexPaint.setTextSize(setIndexTextSize * mScaledDensity);
                indexPaint.setTypeface(setTypeface);

                float sectionHeight = (mIndexbarRect.height() - 2 * mIndexbarMargin) / mSections.length;
                float paddingTop = (sectionHeight - (indexPaint.descent() - indexPaint.ascent())) / 2;
                for (int i = 0; i < mSections.length; i++) {
                    if (setSetIndexBarHighLightTextVisibility) {
                        if (mCurrentSection > -1 && i == mCurrentSection) {
                            indexPaint.setTypeface(Typeface.create(setTypeface, Typeface.BOLD));
                            indexPaint.setTextSize((setIndexTextSize + 3) * mScaledDensity);
                        } else {
                            indexPaint.setTypeface(setTypeface);
                            indexPaint.setTextSize(setIndexTextSize * mScaledDensity);
                        }
                        indexPaint.setColor(indexbarTextColor);

                        float paddingLeft = (mIndexbarWidth - indexPaint.measureText(mSections[i])) / 2;
                        if(mCurrentSection > -1 && i == mCurrentSection){
                            float padding = 2;
                            float size = mIndexbarRect.right - mIndexbarRect.left + padding*2;
                            RectF rectF = new RectF(
                                    mIndexbarRect.left - padding , mIndexbarRect.top + mIndexbarMargin + sectionHeight * i + paddingTop,
                                    mIndexbarRect.right + padding, mIndexbarRect.top + mIndexbarMargin + sectionHeight * i + paddingTop + size
                            );

                            canvas.drawRoundRect(rectF, 5, 5, indexbarPaint);
                        }
                        canvas.drawText(mSections[i], mIndexbarRect.left + paddingLeft
                                , mIndexbarRect.top + mIndexbarMargin + sectionHeight * i + paddingTop - indexPaint.ascent(), indexPaint);
                    } else {
                        float paddingLeft = (mIndexbarWidth - indexPaint.measureText(mSections[i])) / 2;
                        canvas.drawText(mSections[i], mIndexbarRect.left + paddingLeft
                                , mIndexbarRect.top + mIndexbarMargin + sectionHeight * i + paddingTop - indexPaint.ascent(), indexPaint);
                    }

                }
            }
        }
    }

    Handler handler = new Handler();
    Runnable check = new Runnable() {
        @Override
        public void run() {
            if(mRecyclerView.mFastScrollHelper.isSmoothScrolling()){
                handler.postDelayed(check, 100);
            }
            else {
                mIsIndexing = false;
                mRecyclerView.onFastScrollCompleted();
                ((View) (mRecyclerView.getParent().getParent())).invalidate();
            }
        }
    };

    /**
     * 다치기 사건 처리
     */
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // ACTION_DOWN 사건이 index bar 령역에서 발생하였으면 indexing 시작
                if (contains(ev.getX(), ev.getY())) {

                    // 이것은 Motion Event 가 index bar 에서부터 시작되였다는것을 나타낸다
                    mIsIndexing = true;
                    // down 한 위치가 어느 section 에 있는지 확인하고 그 section 으로 목록을 이동
                    mCurrentSection = getSectionByPoint(ev.getY());
                    scrollToPosition();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsIndexing) {
                    // down 한 위치가 어느 section 에 있는지 확인하고 그 section 으로 목록을 이동
                    mCurrentSection = getSectionByPoint(ev.getY());
                    scrollToPosition();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsIndexing) {
                    handler.post(check);
                }
                break;
        }
        return false;
    }

    /**
     * Alphabet sidebar 의 지정된 위치로 Scroll 하는 함수
     */
    private void scrollToPosition() {
        String firstLetter = mSections[mCurrentSection];
        if (Locale.getDefault().toLanguageTag().startsWith("ko")) {
            if ((firstLetter.charAt(0) == 'Z' && mSections[mCurrentSection - 1].charAt(0) == 'ㅎ') ||
                    (firstLetter.charAt(0) == 'ㄱ' && mSections[mCurrentSection + 1].charAt(0) == 'A') ||
            (firstLetter.charAt(0) == '#' && mSections[mCurrentSection + 2].charAt(0) == 'A'))
            {
                changeAlphabet(firstLetter.charAt(0));
                if (firstLetter.charAt(0) == 'Z') mCurrentSection = mSections.length - 1;
            }
        }
        mRecyclerView.scrollToLetter(firstLetter);
    }

    /**
     * Alphabet sidebar 부분 변경함수
     * @param firstLetter 영어 혹은 조선어 자모의 첫글자
     */
    public void changeAlphabet(char firstLetter) {
        String str;
        int mAnimateHeight;
        if (firstLetter <= 'Z' && firstLetter >= 'A') {
            str = "#ㄱABCDEFGHIJKLMNOPQRSTUVWXYZ";
            mAnimateHeight = -mListViewHeight;
        } else {
            str = "#ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎZ";
            mAnimateHeight = mListViewHeight;
        }
        ArrayList<String> alphabetFull = new ArrayList<>();
        for (int i = 0; i < str.length(); i++) {
            alphabetFull.add(String.valueOf(str.charAt(i)));
        }
        mSections = (String[]) alphabetFull.toArray(new String[0]);

        // 조선어로부터 영어로 혹은 영어로부터 조선어로 변경할때의 animation
        ValueAnimator mValueAnimator = ValueAnimator.ofInt(mAnimateHeight, 0);
        mValueAnimator.setDuration(200);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                animTranslate = (int) valueAnimator.getAnimatedValue();
                ((View) (mRecyclerView.getParent().getParent())).invalidate();
            }
        });
        mValueAnimator.start();
    }

    public void onSizeChanged(int w, int h, int topMargin, int bottomMargin) {
        mListViewWidth = w;
        mListViewHeight = h;
        mIndexbarRect = new RectF(w - mIndexbarMargin - mIndexbarWidth + 5
                , mIndexbarMargin + topMargin
                , w - mIndexbarMargin + 5
                , h - mIndexbarMargin - bottomMargin - 100);
    }

    /**
     * Adapter Observer 등록 및 SectionIndexer 설정
     * @param adapter Observer 를 등록할 adapter
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (adapter instanceof SectionIndexer) {
            adapter.registerAdapterDataObserver(this);
            mIndexer = (SectionIndexer) adapter;
            mSections = (String[]) mIndexer.getSections();
        }
    }

    @Override
    public void onChanged() {
        super.onChanged();
        updateSections();
    }

    /**
     * Section 갱신
     */
    public void updateSections() {
        mSections = (String[]) mIndexer.getSections();
    }

    /**
     * 주어진 x, y 좌표가 indexbar 령역에 속하는지 확인
     * @param x x좌표
     * @param y y좌표
     * @return 속하면 true, 아니면 false
     */
    public boolean contains(float x, float y) {
        // 주어진 point 가 오른쪽 margin 을 포함하는 indexbar 령역에 있는지 확인
        return (x >= mIndexbarRect.left && y >= mIndexbarRect.top && y <= mIndexbarRect.top + mIndexbarRect.height());
    }

    /**
     * 주어진 point 가 indexbar 령역에 속하는지 확인
     * @param ev point 값을 가지고 있는 MotionEvent 값
     * @return 속하면 true, 아니면 false
     */
    public boolean contains(MotionEvent ev) {
        // 주어진 point 가 오른쪽 margin 을 포함하는 indexbar 령역에 있는지 확인
        float x = ev.getX();
        float y = ev.getY();
        if(ev.getAction() == MotionEvent.ACTION_DOWN)
            return (x >= mIndexbarRect.left && y >= mIndexbarRect.top && y <= mIndexbarRect.top + mIndexbarRect.height());
        return mIsIndexing;
    }

    /**
     * 주어진 y좌표 에 기초하여 현재 section 위치 얻기
     * @param y y좌표
     * @return section 값
     */
    private int getSectionByPoint(float y) {
        if (mSections == null || mSections.length == 0)
            return 0;
        if (y < mIndexbarRect.top + mIndexbarMargin)
            return 0;
        if (y >= mIndexbarRect.top + mIndexbarRect.height() - mIndexbarMargin)
            return mSections.length - 1;
        return (int) ((y - mIndexbarRect.top - mIndexbarMargin) / ((mIndexbarRect.height() - 2 * mIndexbarMargin) / mSections.length));
    }

    /**
     * 주어진 문자에 기초하여 현재 section 위치 얻기
     * @param c 탐색할 문자
     * @return section 값
     */
    private int getSelectionByLetter(char c){
        for (int i = 0; i < mSections.length; i ++){
            if(mSections[i].charAt(0) == c){
                return i;
            }
        }
        return -1;
    }

    /**
     * 주어진 문자에 기초하여 현재의 section 설정 함수
     * @param c 기초로 되는 문자
     */
    public void setSelectionByLetter(char c){
        int selection = getSelectionByLetter(c);
        mCurrentSection = selection;

        ((View)(mRecyclerView.getParent().getParent())).invalidate();
    }

    private Runnable mLastFadeRunnable = null;

    private void fade(long delay) {
        if (mRecyclerView != null) {
            if (mLastFadeRunnable != null) {
                mRecyclerView.removeCallbacks(mLastFadeRunnable);
            }
            mLastFadeRunnable = new Runnable() {
                @Override
                public void run() {
                    ((View)mRecyclerView.getParent()).invalidate();
                }
            };
            mRecyclerView.postDelayed(mLastFadeRunnable, delay);
        }
    }

    private int convertTransparentValueToBackgroundAlpha(float value) {
        return (int) (255 * value);
    }

    /**
     * @param value int to set the text size of the index bar
     */
    public void setIndexTextSize(int value) {
        setIndexTextSize = value;
    }

    /**
     * @param value float to set the width of the index bar
     */
    public void setIndexbarWidth(float value) {
        mIndexbarWidth = value;
    }

    /**
     * @param value float to set the margin of the index bar
     */
    public void setIndexbarMargin(float value) {
        mIndexbarMargin = value;
    }

    /**
     * @param value int to set preview padding
     */
    public void setPreviewPadding(int value) {
        setPreviewPadding = value;
    }

    /**
     * @param value int to set the radius of the index bar
     */
    public void setIndexBarCornerRadius(int value) {
        setIndexBarCornerRadius = value;
    }

    /**
     * @param value float to set the transparency of the color for index bar
     */
    public void setIndexBarTransparentValue(float value) {
        indexbarBackgroudAlpha = convertTransparentValueToBackgroundAlpha(value);
    }

    /**
     * @param typeface Typeface to set the typeface of the preview & the index bar
     */
    public void setTypeface(Typeface typeface) {
        setTypeface = typeface;
    }

    /**
     * @param shown boolean to show or hide the index bar
     */
    public void setIndexBarVisibility(boolean shown) {
        setIndexBarVisibility = shown;
    }


    /**
     * @param shown boolean to show or hide the index bar
     */
    public void setIndexBarStrokeVisibility(boolean shown) {
        setIndexBarStrokeVisibility = shown;
    }

    /**
     * @param shown boolean to show or hide the preview box
     */
    public void setPreviewVisibility(boolean shown) {
        previewVisibility = shown;
    }

    /**
     * @param value int to set the text size of the preview box
     */
    public void setIndexBarStrokeWidth(int value) {
        mIndexBarStrokeWidth = value;
    }


    /**
     * @param value int to set the text size of the preview box
     */
    public void setPreviewTextSize(int value) {
        setPreviewTextSize = value;
    }

    /**
     * @param color The color for the preview box
     */
    public void setPreviewColor(@ColorInt int color) {
        previewBackgroundColor = color;
    }

    /**
     * @param color The text color for the preview box
     */
    public void setPreviewTextColor(@ColorInt int color) {
        previewTextColor = color;
    }

    /**
     * @param value float to set the transparency value of the preview box
     */
    public void setPreviewTransparentValue(float value) {
        previewBackgroudAlpha = convertTransparentValueToBackgroundAlpha(value);
    }

    /**
     * @param color The color for the scroll track
     */
    public void setIndexBarColor(@ColorInt int color) {
        indexbarBackgroudColor = color;
    }

    /**
     * @param color The text color for the index bar
     */
    public void setIndexBarTextColor(@ColorInt int color) {
        indexbarTextColor = color;
    }

    /**
     * @param color The text color for the index bar
     */
    public void setIndexBarStrokeColor(@ColorInt int color) {
        mIndexBarStrokeColor = color;
    }


    /**
     * @param color The text color for the index bar
     */
    public void setIndexbarHighLightTextColor(@ColorInt int color) {
        indexbarHighLightTextColor = color;
    }

    /**
     * @param shown boolean to show or hide the index bar
     */
    public void setIndexBarHighLightTextVisibility(boolean shown) {
        setSetIndexBarHighLightTextVisibility = shown;
    }

}