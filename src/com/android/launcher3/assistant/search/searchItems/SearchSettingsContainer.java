package com.android.launcher3.assistant.search.searchItems;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.assistant.SettingsUtility.instrumentation.MetricsFeatureProvider;
import com.android.launcher3.assistant.SettingsUtility.overlay.FeatureFactory;
import com.android.launcher3.assistant.SettingsUtility.search.SearchFeatureProvider;
import com.android.launcher3.assistant.SettingsUtility.search.SearchResult;
import com.android.launcher3.assistant.SettingsUtility.search.indexing.IndexingCallback;
import com.android.launcher3.assistant.search.SearchItemContainer;
import com.android.settings.intelligence.nano.SettingsIntelligenceLogProto.SettingsIntelligenceEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 설정검색부분
 */
public class SearchSettingsContainer extends SearchItemContainer implements
        LoaderManager.LoaderCallbacks<List<? extends SearchResult>>, IndexingCallback {

    private static final String DEFAULT_SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String READ_SEARCH_INDEXABLES = "android.permission.READ_SEARCH_INDEXABLES";

    Launcher mLauncher;

    //설정 아이콘
    Bitmap mDefaultIcon;

    //Variables for search
    private MetricsFeatureProvider mMetricsFeatureProvider;
    SearchFeatureProvider mSearchFeatureProvider;

    public SearchSettingsContainer(Context context) {
        this(context, null);
    }

    public SearchSettingsContainer(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);

        getDefaultIcon();
    }

    public SearchSettingsContainer(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);

        // 권한허용 확인
        if(Utilities.hasPermission(mLauncher, READ_SEARCH_INDEXABLES)){
            mSearchFeatureProvider = FeatureFactory.get(context).searchFeatureProvider();
            mMetricsFeatureProvider = FeatureFactory.get(context).metricsFeatureProvider(context);

            mSearchFeatureProvider.initFeedbackButton();
            mSearchFeatureProvider.updateIndexAsync(mLauncher, this);
        }
    }

    /**
     * 설정 검색
     */
    @Override
    public void doSearch() {
        //권한허용 확인
        if(!Utilities.hasPermission(mLauncher, READ_SEARCH_INDEXABLES)) {
            updateSearchResult(new ArrayList<>());
            return;
        }

        final boolean isEmptyQuery = TextUtils.isEmpty(mQuery);

        //Indexing 이 끝나지 않은 경우 검색문자룔을 등록하고 검색하지 않는다.
        if (!mSearchFeatureProvider.isIndexingComplete(mLauncher)) {
            return;
        }

        if (!isEmptyQuery) {
            mMetricsFeatureProvider.logEvent(SettingsIntelligenceEvent.EventType.PERFORM_SEARCH);
            restartLoaders();
        }

        if(mDefaultIcon == null)
            getDefaultIcon();
    }

    /**
     * 개별적인 설정 항목을 표시할 layout id 얻기
     * @return layout id
     */
    @Override
    public int getLayoutResource(){
        return R.layout.search_item_settings;
    }

    /**
     * Indexing 이 끝났을때 호출되는 callback
     */
    @Override
    public void onIndexingFinished() {
        final LoaderManager loaderManager = mLauncher.getLoaderManager();
        loaderManager.initLoader(SearchLoaderId.SEARCH_RESULT, null /* args */,
                this /* callback */);

        doSearch();
    }

    /**
     * 검색관련 Loader 생성
     */
    @Override
    public Loader<List<? extends SearchResult>> onCreateLoader(int id, Bundle args) {
        if (id == SearchLoaderId.SEARCH_RESULT) {
            return mSearchFeatureProvider.getSearchResultLoader(mLauncher, mQuery);
        }
        return null;
    }

    /**
     * 검색이 끝났을때 호출되는 callback
     */
    @Override
    public void onLoadFinished(Loader<List<? extends SearchResult>> loader,
            List<? extends SearchResult> data) {

        List<SearchItemInfo> resultList = new ArrayList<>();

        // SearchResult object 를 SearchSettingsInfo object 로 변환
        for (SearchResult searchResult: data){
            SearchSettingsInfo info = new SearchSettingsInfo();
            info.title = searchResult.title.toString();
            if(searchResult.icon != null)
                info.icon = searchResult.icon;
            else
                info.icon = new BitmapDrawable(getResources(), mDefaultIcon);

            info.searchResult = searchResult;
            resultList.add(info);
        }

        updateSearchResult(resultList);
    }

    @Override
    public void onLoaderReset(Loader<List<? extends SearchResult>> loader) {

    }

    /**
     * Loader 재실행
     */
    public void restartLoaders(){
        final LoaderManager loaderManager = mLauncher.getLoaderManager();
        loaderManager.restartLoader(
                SearchLoaderId.SEARCH_RESULT, null /* args */, this /* callback */);
    }

    /**
     * 설정 표준 아이콘 얻기
     */
    public void getDefaultIcon(){
        AllAppsStore appsStore = mLauncher.getAppsView().getAppsStore();
        if(appsStore == null)
            return;
        if(appsStore.getApps() == null || appsStore.getApps().isEmpty())
            return;

        for (AppInfo appInfo: appsStore.getApps()){
            if(appInfo.getPackageName() .equals(DEFAULT_SETTINGS_PACKAGE_NAME)){
                mDefaultIcon = appInfo.iconBitmap;
                break;
            }
        }
    }

    public static final class SearchLoaderId {
        // Search Query IDs
        public static final int SEARCH_RESULT = 1;

        // Saved Query IDs
        public static final int SAVE_QUERY_TASK = 2;
        public static final int REMOVE_QUERY_TASK = 3;
        public static final int SAVED_QUERIES = 4;
    }

    /**
     * 개별적인 설정 검색항목을 위한 static class
     */
    public static class SearchSettingsInfo extends SearchItemInfo{
        SearchResult searchResult;
    }
}
