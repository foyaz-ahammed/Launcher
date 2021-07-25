package com.android.launcher3.assistant;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.android.launcher3.R;
import java.util.Date;

/**
 * Assist 페지 맨 우에 표시할 view
 */
public class AssistTopHeader extends AssistItemContainer {
    TextView mMainTitle;
    TextView mSubTitle;

    public AssistTopHeader(Context context) {
        this(context, null);
    }

    public AssistTopHeader(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistTopHeader(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mMainTitle = findViewById(R.id.main_title);
        mSubTitle = findViewById(R.id.sub_title);

        setMainAndSubtitle();
    }

    /**
     * 제목표시
     */
    public void setMainAndSubtitle(){
        Date date = new Date();
        long todayMillis = date.getTime();

        String subTitleString = DateUtils.formatDateRange(getContext(), todayMillis, todayMillis, DateUtils.FORMAT_SHOW_DATE);
        mSubTitle.setText(subTitleString);

        String mainTitleString;
        mainTitleString = getResources().getString(R.string.today_string) + ", " +
                DateUtils.formatDateRange(getContext(), todayMillis, todayMillis, DateUtils.FORMAT_SHOW_WEEKDAY);
        mMainTitle.setText(mainTitleString);
    }
}
