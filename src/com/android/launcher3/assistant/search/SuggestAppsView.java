package com.android.launcher3.assistant.search;

import android.content.Context;
import android.graphics.Color;

import android.util.AttributeSet;

import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.touch.ItemClickHandler;
import com.google.android.apps.nexuslauncher.allapps.PredictionsFloatingHeader;
import java.util.ArrayList;
import java.util.List;

/**
 * 추천 app 들을 표시하기 위한 view
 */
public class SuggestAppsView extends LinearLayout implements ExtraViewsListener {
    private static final int SUGGESTION_APPS_SHOW_COUNT = 4;

    Launcher mLauncher;

    // 추천 app 목록
    List<AppInfo> mSuggestionAppList = new ArrayList<>();

    public SuggestAppsView(Context context) {
        this(context, null);
    }

    public SuggestAppsView(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SuggestAppsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    public void onOpen() {
        // 추천 app 목록 얻기
        if(mLauncher.getAppsView() == null)
            return;

        List<ItemInfoWithIcon> itemInfoList = ((PredictionsFloatingHeader)mLauncher.getAppsView().getFloatingHeaderView()).getPredictionRowView().getPredictedApps();
        if(itemInfoList == null)
            return;

        mSuggestionAppList.clear();
        for (int i = 0; i < itemInfoList.size(); i ++){
            if(itemInfoList.get(i) instanceof AppInfo){
                AppInfo appInfo = (AppInfo)itemInfoList.get(i);
                mSuggestionAppList.add(appInfo);

                // 최대 4개의 app 추가
                if(mSuggestionAppList.size() == SUGGESTION_APPS_SHOW_COUNT)
                    break;
            }
        }

        applySuggestionApps();
    }

    /**
     * 추천 app 들을 적용
     */
    public void applySuggestionApps(){
        removeAllViews();

        DeviceProfile dp = Launcher.getLauncher(getContext()).getDeviceProfile();
        int appCount = mSuggestionAppList.size();
        while (getChildCount() < appCount){

            BubbleTextView childView = (BubbleTextView) mLauncher.getLayoutInflater().inflate(
                    R.layout.all_apps_icon, this, false);
            childView.setOnClickListener(ItemClickHandler.INSTANCE);
            childView.setTextColor(Color.WHITE);
            LayoutParams layoutParams = (LayoutParams) childView.getLayoutParams();
            layoutParams.height = dp.allAppsCellHeightPx;
            layoutParams.width = 0;
            layoutParams.weight = 1.0f;
            addView(childView);
        }

        for (int i = 0; i < appCount; i ++){
            BubbleTextView childView = (BubbleTextView) getChildAt(i);
            AppInfo appInfo = mSuggestionAppList.get(i);
            childView.applyFromApplicationInfo(appInfo);
        }
    }
}
