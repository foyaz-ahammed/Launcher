package com.android.launcher3.assistant.search;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.AssistMenuLayout;

import java.util.Objects;

/**
 * 검색기록삭제를 위한 대화창
 */
public class ClearHistoryPopup extends Dialog implements View.OnClickListener {
    Launcher mLauncher;
    SearchLayout mSearchLayout;
    AssistMenuLayout mClearConfirm;
    TextView mCancel;
    TextView mClear;

    int GRAVITY_TOP = 1;
    int GRAVITY_CENTER = 2;
    int GRAVITY_BOTTOM = 3;

    public ClearHistoryPopup(@NonNull Context context, SearchLayout searchLayout) {
        super(context);
        mLauncher = Launcher.getLauncher(context);
        mSearchLayout = searchLayout;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.search_history_clear_confirm);
        mClearConfirm = findViewById(R.id.search_history_clear_confirm);
        mCancel = findViewById(R.id.cancel);
        mClear = findViewById(R.id.clear);
        mCancel.setOnClickListener(this);
        mClear.setOnClickListener(this);

        int entireWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mClearConfirm.setWidth((int) (entireWidth * 0.90));
    }

    /**
     * 대화창의 Gravity 설정
     * @param gravityType Gravity 형태
     */
    public void setGravity(int gravityType) {
        Window window = getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        if (gravityType == GRAVITY_CENTER)
            wlp.gravity = Gravity.CENTER;
        else if (gravityType == GRAVITY_BOTTOM)
            wlp.gravity = Gravity.BOTTOM;
        window.setAttributes(wlp);
    }

    @Override
    public void onClick(View view) {
        if (view == mCancel) {
            dismiss();
        }
        else if (view == mClear) {
            mSearchLayout.clearHistory();
            dismiss();
        }
    }
}
