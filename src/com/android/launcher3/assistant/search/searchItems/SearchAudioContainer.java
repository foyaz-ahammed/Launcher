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
 * Audio 검색부분
 */
public class SearchAudioContainer extends SearchItemContainer {

    Launcher mLauncher;

    public SearchAudioContainer(Context context) {
        this(context, null);
    }

    public SearchAudioContainer(Context context,
                                @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchAudioContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    /**
     * 주어진 문자렬에 관한 audio 검색
     */
    @Override
    public void doSearch() {
        List<SearchItemInfo> resultList = new ArrayList<>();
        resultList = getAudios(mQuery);
        updateSearchResult(resultList);
    }

    /**
     * 주어진 검색문자렬에 준하여 Audio 얻기
     * @param query 검색문자렬
     * @return 검색문자렬을 포함하는 Audio 목록
     */
    public List<SearchItemInfo> getAudios(String query) {
        List<SearchItemInfo> list = new ArrayList<>();
        Uri allAudiosUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContext().getContentResolver().query(
                allAudiosUri, null, MediaStore.Audio.Media.DISPLAY_NAME + " LIKE ?", new String[]{"%" + query + "%"}, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i ++) {
                SearchAudioInfo audioInfo = new SearchAudioInfo();
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                String dataPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

                audioInfo.title = name;
                audioInfo.dataPath = dataPath;
                audioInfo.id = id;
                list.add(audioInfo);

                cursor.moveToNext();
            }
        }
        return list;
    }

    /**
     * 개별적인 Audio 항목을 표시할 layout id 얻기
     * @return layout id
     */
    @Override
    public int getLayoutResource(){
        return R.layout.search_item_audio;
    }

    /**
     * 개별적인 Audio 검색항목을 위한 static class
     */
    public static class SearchAudioInfo extends SearchItemInfo {
        String dataPath;
        long id;
    }
}
