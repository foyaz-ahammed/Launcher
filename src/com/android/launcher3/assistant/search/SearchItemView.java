package com.android.launcher3.assistant.search;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.android.launcher3.R;
import com.android.launcher3.assistant.search.SearchItemContainer.SearchItemInfo;

/**
 * 개별적인 검색항목들을 위한 기초클라스
 */
public abstract class SearchItemView extends LinearLayout implements View.OnClickListener{

    //강조표시할 색
    public int mHighlightColor;
    public SearchItemContainer mSearchItemContainer;

    public SearchItemView(Context context) {
        this(context, null);
    }

    public SearchItemView(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHighlightColor = getContext().getColor(R.color.search_keyword_highlight_color);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (mSearchItemContainer != null) {
                    if (action == MotionEvent.ACTION_DOWN) {
                        mSearchItemContainer.pushDownAnim(0.95f, mSearchItemContainer.durationPush);
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        if (mSearchItemContainer.rect != null && !mSearchItemContainer.isOutside && !mSearchItemContainer.rect.contains(getLeft() + (int) motionEvent.getX(), getTop() + (int) motionEvent.getY())) {
                            mSearchItemContainer.isOutside = true;
                        }
                        mSearchItemContainer.pushDownAnim(1.0f, mSearchItemContainer.durationRelease);
                    } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                        mSearchItemContainer.pushDownAnim(1.0f, mSearchItemContainer.durationRelease);
                    }
                }
                return false;
            }
        });
    }

    /**
     * 주어진 string 을 강조표시된 html 형의 string 으로 변환하는 함수
     * @param oldString 변환할 string
     * @param highlight 강조표시하여야 할 string
     * @return 강조표시된 html 형의 string
     */
    public String convertToHighlightedHtmlString(String oldString, String highlight){
        String regexInput = "(?i)(" + highlight + ")";

        // 강조표시할 색상을 integer 에서 string 으로 변환
        String hexString = Integer.toHexString(mHighlightColor);
        String color = "#" + hexString.substring(2);
        String replaceString = "<font color='"+ color + "'>$1</font>";

        // 변환된 string 반환
        return oldString.replaceAll(regexInput, replaceString);
    }

    public abstract void applyFromItemInfo(SearchItemInfo info, String query, SearchItemContainer searchItemContainer);
}
