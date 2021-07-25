package com.android.launcher3.assistant;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.android.launcher3.R;

/**
 * Assist 페지에 간단한 note 를 표시하기 위한 layout
 */
public class AssistBriefNote extends LinearLayout implements View.OnClickListener {
    ImageButton mMenuButton;
    View mDialogTitle;

    public AssistBriefNote(Context context) {
        this(context, null);
    }

    public AssistBriefNote(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistBriefNote(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mMenuButton = findViewById(R.id.menu_button);
        mMenuButton.setOnClickListener(this);
        setOnClickListener(this);

        mDialogTitle = findViewById(R.id.dialog_start_title);
        mDialogTitle.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v == mMenuButton){
            // popup 창 현시
            AssistPopupMenu popupMenu = new AssistPopupMenu(getContext(), mMenuButton,
                    true, false,false, getPaddingLeft(), getPaddingRight());

            popupMenu.show();
        }
        else if(v == this || v == mDialogTitle){
            Toast toast = Toast.makeText(getContext(),
                    "Entering brief note",
                    Toast.LENGTH_SHORT);

            toast.show();
        }
    }
}

