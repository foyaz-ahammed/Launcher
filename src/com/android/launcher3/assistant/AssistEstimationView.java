package com.android.launcher3.assistant;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

/**
 * Memory 와 걸음수부분을 포함하는 부분
 */
public class AssistEstimationView extends AssistItemContainer {

    public AssistEstimationView(Context context) {
        this(context, null);
    }

    public AssistEstimationView(Context context,
                                @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistEstimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        defaultIndex = 3;
        currentIndex = 3;
        isPinned = true;

        // 자료기지에서 순서와 pin 상태 얻기
        Cursor cursor = mLauncher.getModelWriter().getAssistantEstimationSectionOrder();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                currentIndex = cursor.getInt(0);
                isPinned = cursor.getInt(1) != 0;
            }
            cursor.close();
        }
    }

}
