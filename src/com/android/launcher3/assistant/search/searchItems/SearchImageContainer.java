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
 * 화상 검색부분
 */
public class SearchImageContainer extends SearchItemContainer {

    Launcher mLauncher;

    public SearchImageContainer(Context context) {
        this(context, null);
    }

    public SearchImageContainer(Context context,
                                @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchImageContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    /**
     * 화상 검색
     */
    @Override
    public void doSearch() {
        List<SearchItemInfo> resultList = new ArrayList<>();
        resultList = getImages(mQuery);
        updateSearchResult(resultList);
    }

    /**
     * 주어진 검색문자렬에 관한 화상목록 얻기
     * @param query 검색문자렬
     * @return 화상목록
     */
    public List<SearchItemInfo> getImages(String query) {
        List<SearchItemInfo> list = new ArrayList<>();
        Uri allImagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContext().getContentResolver().query(
                allImagesUri, null, MediaStore.Images.Media.DISPLAY_NAME + " LIKE ?", new String[]{"%" + query + "%"}, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i ++) {
                SearchImageInfo imageInfo = new SearchImageInfo();
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                String dataPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                imageInfo.title = name;
                imageInfo.dataPath = dataPath;
                list.add(imageInfo);

                cursor.moveToNext();
            }
        }
        return list;
    }

    /**
     * 개별적인 화상 항목을 표시할 layout id 얻기
     * @return layout id
     */
    @Override
    public int getLayoutResource(){
        return R.layout.search_item_image;
    }

    /**
     * 개별적인 화상 검색항목을 위한 static class
     */
    public static class SearchImageInfo extends SearchItemInfo {
        String dataPath;
    }
}
