package com.android.launcher3.assistant.search.searchItems;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.assistant.search.SearchItemContainer;
import java.util.ArrayList;
import java.util.List;

/**
 * App 검색부분
 */
public class SearchAppsContainer extends SearchItemContainer {

    Launcher mLauncher;

    public SearchAppsContainer(Context context) {
        this(context, null);
    }

    public SearchAppsContainer(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchAppsContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    /**
     * 주어진 문자렬에 관한 app 검색
     */
    @Override
    public void doSearch() {
        AllAppsStore appsStore = mLauncher.getAppsView().getAppsStore();

        List<SearchItemInfo> resultList = new ArrayList<>();
        for (AppInfo appInfo : appsStore.getApps()) {
            // app title 에서 검색
            String appTitle = appInfo.title.toString();
            if(appTitle.toLowerCase().contains(mQuery.toLowerCase())) {
                SearchAppsInfo info = new SearchAppsInfo();

                // app 아이콘과 title 설정
                info.title = appTitle;
                info.icon = new BitmapDrawable(getResources(), appInfo.iconBitmap);

                // appInfo 추가
                info.appInfo = appInfo;

                // 결과목록에 추가
                resultList.add(info);
            }
        }

        updateSearchResult(resultList);
    }

    /**
     * 개별적인 app 항목의 layout id 얻기
     * @return layout id
     */
    @Override
    public int getLayoutResource(){
        return R.layout.search_item_app;
    }

    // 개별적인 App 검색항목을 위한 static class
    public static class SearchAppsInfo extends SearchItemInfo{
        AppInfo appInfo;
    }
}
