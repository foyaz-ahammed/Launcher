package com.android.launcher3.assistant;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import java.util.Objects;

/**
 * Assist 페지의 개별적인 항목들의 설정을 위한 대화창
 */
public class AssistPopupMenu extends Dialog implements View.OnClickListener{

    Launcher mLauncher;

    boolean mVisiblePinOnTop; // Pin 보임상태
    boolean mVisibleUnpin; // Unpin 보임상태
    boolean mVisibleSettings; // Settings 보임상태

    int mParentPaddingLeft;
    int mParentPaddingRight;

    Button mButtonPinOnTop;
    Button mButtonUnpin;
    Button mButtonSettings;

    public AssistMenuLayout mLayout;
    public ImageButton mCallerView;

    MenuSelectListener mListener;

    public AssistPopupMenu(@NonNull Context context, ImageButton callerView, boolean visiblePinOnTop, boolean visibleUnpin, boolean visibleSettings, int paddingLeft, int paddingRight) {
        super(context, R.style.Theme_SmallDim);

        mLauncher = Launcher.getLauncher(context);

        mVisiblePinOnTop = visiblePinOnTop;
        mVisibleUnpin = visibleUnpin;
        mVisibleSettings = visibleSettings;
        mParentPaddingLeft = paddingLeft;
        mParentPaddingRight = paddingRight;
        mCallerView = callerView;
    }

    /**
     * Menu select listener 추가
     * @param listener
     */
    public void addMenuSelectListener(MenuSelectListener listener){
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);

        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.assist_common_menu);
        mLayout = findViewById(R.id.menu_layout);
        mButtonPinOnTop = findViewById(R.id.assist_menu_pin_on_top);
        mButtonUnpin = findViewById(R.id.assist_menu_unpin);
        mButtonSettings = findViewById(R.id.assist_menu_settings);

        if(!mVisiblePinOnTop){
            mButtonPinOnTop.setVisibility(View.GONE);
        }
        if (!mVisibleUnpin) {
            mButtonUnpin.setVisibility(View.GONE);
        }
        if(!mVisibleSettings){
            mButtonSettings.setVisibility(View.GONE);
        }

        mButtonPinOnTop.setOnClickListener(this);
        mButtonUnpin.setOnClickListener(this);
        mButtonSettings.setOnClickListener(this);

        Context context = getContext();

        WindowManager.LayoutParams wmlp = getWindow().getAttributes();
        int entireWidth = context.getResources().getDisplayMetrics().widthPixels;
        int width = (entireWidth - mParentPaddingLeft - mParentPaddingRight)/2;

        int []pos = new int[2];
        Utilities.getDescendantCoordRelativeToAncestor(mCallerView, mLauncher.getDesktop().getAssistViewsContainer(),
                pos, true);

        // 표시할 위치 설정
        wmlp.gravity = Gravity.TOP | Gravity.START;
        wmlp.x = entireWidth - width - mParentPaddingRight;
        wmlp.y = pos[1];

        mLayout.setWidth(width);
    }

    @Override
    public void onClick(View v) {
        if(v == mButtonPinOnTop){
            if(mListener != null){
                mListener.onSelectPinOnTop();
            }
            dismiss();
        }
        else if (v == mButtonUnpin) {
            if (mListener != null) {
                mListener.onSelectUnpin();
            }
            dismiss();
        }
        else if(v == mButtonSettings){
            if(mListener != null){
                mListener.onSelectSettings();
            }
            dismiss();
        }
    }

    public interface MenuSelectListener {
        public void onSelectPinOnTop();
        public void onSelectUnpin();
        public void onSelectSettings();
    }
}
