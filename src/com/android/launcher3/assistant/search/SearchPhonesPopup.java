package com.android.launcher3.assistant.search;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.AssistMenuLayout;

import java.util.ArrayList;
import java.util.Objects;

/**
 * 전화번호목록 현시를 위한 대화창
 */
public class SearchPhonesPopup extends Dialog {
    Launcher mLauncher;
    AssistMenuLayout mPhoneMenu;
    MaxHeightRecyclerView mRecyclerView;
    PhoneItemRecyclerViewAdapter mPhoneItemRecyclerViewAdapter;
    TextView mTitle;
    String mTitleTxt;
    int mPopupType;

    ArrayList<?> mPhoneList; // 전화번호목록

    int GRAVITY_TOP = 1;
    int GRAVITY_CENTER = 2;
    int GRAVITY_BOTTOM = 3;

    public SearchPhonesPopup(@NonNull Context context, ArrayList<?> phoneList, String title, int popupType) {
        super(context);
        mLauncher = Launcher.getLauncher(context);
        mPhoneList = phoneList;
        mTitleTxt = title;
        mPopupType = popupType;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.search_phones_menu);
        mPhoneMenu = findViewById(R.id.phone_menu_layout);
        mRecyclerView = findViewById(R.id.search_phone_list);
        mTitle = findViewById(R.id.phones_title);
        mTitle.setText(mTitleTxt);

        mPhoneItemRecyclerViewAdapter = new PhoneItemRecyclerViewAdapter(mLauncher, mPhoneList, mPopupType);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mPhoneItemRecyclerViewAdapter);

        int entireWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mPhoneMenu.setWidth((int) (entireWidth * 0.90));
    }

    /**
     * Gravity 설정
     * @param gravityType 설정할 Gravity 형태
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
}
