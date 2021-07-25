package com.android.launcher3.assistant;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.tu.circlelibrary.CirclePercentBar;

/**
 * Assist page 에 보여주는 걸음수부분
 */
public class AssistStep extends AssistItemContainer implements View.OnClickListener {
    int steps = 0; // 오늘의 걸음수

    CirclePercentBar mProgress;
    TextView mSteps;

    public AssistStep(Context context) {
        this(context, null);
    }

    public AssistStep(Context context,
                              @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistStep(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mSteps = findViewById(R.id.steps_number);
        mProgress = findViewById(R.id.circle_bar);

        mSteps.setText(String.valueOf(steps));
        mProgress.setPercentData((float) steps / 100, new DecelerateInterpolator());

        setOnClickListener(this);
    }

    /**
     * 걸음수자료 갱신함수
     * @param data 갱신할 걸음수
     */
    public void updateSteps(int data) {
        steps = data;
        onFinishInflate();
    }

    @Override
    public void onClick(View view) {
        Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage("org.secuso.privacyfriendlyactivity");
        if (launchIntent != null)
            getContext().startActivity(launchIntent);
    }
}
