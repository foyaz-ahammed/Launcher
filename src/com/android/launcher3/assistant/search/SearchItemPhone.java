package com.android.launcher3.assistant.search;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.search.SearchItemContainer.SearchItemInfo;
import com.android.launcher3.assistant.search.searchItems.SearchContactsContainer.PhonePair;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * 개별적인 전화번호항목
 */
public class SearchItemPhone extends SearchItemView {
    Launcher mLauncher;
    TextView mPhoneNumber; // 전화번호
    TextView mPhoneType; // 전화번호형태 (home, work, ...)

    PhonePair mPhonePair;
    String mPopupType; // popup 형태, call 혹은 message

    public SearchItemPhone(Context context) {
        this(context, null);
    }

    public SearchItemPhone(Context context,
                           @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemPhone(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mPhoneNumber = findViewById(R.id.item_phone_number);
        mPhoneType = findViewById(R.id.item_phone_type);

    }

    /**
     * 개별적인 전화번호 항목 현시
     * @param info 전화번호정보
     * @param popupType popup 형태, call 혹은 message
     * @param searchItemContainer 항목들을 포함하는 container
     */
    @Override
    public void applyFromItemInfo(SearchItemInfo info, String popupType, SearchItemContainer searchItemContainer) {
        if(!(info instanceof PhonePair)){
            return;
        }

        PhonePair phoneInfo = (PhonePair) info;
        mPhoneNumber.setText(phoneInfo.phoneNumber);
        mPhoneType.setText(phoneInfo.type);

        mPhonePair = (PhonePair) info;
        mPopupType = popupType;
    }

    @Override
    public void onClick(View v) {
        //Click self view
        if(v == this){
            onClickApp();
        }
    }

    /**
     * popup 형태에 따라 해당 app 열기
     */
    public void onClickApp(){
        if (mPopupType.equals("call")) {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + mPhonePair.phoneNumber));
            if (checkSelfPermission(getContext(),
                    android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mLauncher.startActivity(callIntent);
        } else {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SENDTO);
            sendIntent.setData(Uri.parse("smsto:" + mPhonePair.phoneNumber));
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mLauncher.startActivity(sendIntent);
        }
    }
}
