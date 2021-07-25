package com.android.launcher3.assistant.search.searchItems;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.search.SearchItemContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * 비데오검색부분
 */
public class SearchVideoContainer extends SearchItemContainer {

    Launcher mLauncher;

    public SearchVideoContainer(Context context) {
        this(context, null);
    }

    public SearchVideoContainer(Context context,
                                @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchVideoContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    /**
     * 비데오 검색
     */
    @Override
    public void doSearch() {
        List<SearchItemInfo> resultList = new ArrayList<>();
        resultList = getVideos(mQuery);
        updateSearchResult(resultList);
    }

    /**
     * 주어진 검색문자렬에 관한 비데오목록 얻기
     * @param query 검색문자렬
     * @return 비데오목록
     */
    public List<SearchItemInfo> getVideos(String query) {
        List<SearchItemInfo> list = new ArrayList<>();
        Uri allVideosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContext().getContentResolver().query(
                allVideosUri, null, MediaStore.Video.Media.DISPLAY_NAME + " LIKE ?", new String[]{"%" + query + "%"}, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i ++) {
                SearchVideoInfo videoInfo = new SearchVideoInfo();
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                String dataPath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID));

                videoInfo.title = name;
                videoInfo.dataPath = dataPath;
                videoInfo.id = id;
                list.add(videoInfo);

                cursor.moveToNext();
            }
        }
        return list;
    }

    /**
     * 개별적인 비데오 항목을 표시할 layout id 얻기
     * @return layout id
     */
    @Override
    public int getLayoutResource(){
        return R.layout.search_item_video;
    }

    /**
     * 개별적인 비데오 검색항목을 위한 static class
     */
    public static class SearchVideoInfo extends SearchItemInfo {
        String dataPath;
        long id;
    }
}
