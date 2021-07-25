package com.android.launcher3.assistant.search.searchItems;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Directory;
import android.support.annotation.NonNull;
import android.app.LoaderManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.net.Uri.Builder;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.SearchSnippets;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.ContactsUtilities.ContactsCompat;
import com.android.launcher3.assistant.ContactsUtilities.FavoritesAndContactsLoader;
import com.android.launcher3.assistant.search.SearchItemContainer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 주소 검색부분
 */
public class SearchContactsContainer extends SearchItemContainer
        implements LoaderManager.LoaderCallbacks<Cursor> {

    Launcher mLauncher;

    // 자료기지에서 얻을 column 마당들
    private static final String[] FILTER_PROJECTION_PRIMARY = new String[] {
            Contacts._ID,                           // 0
            Contacts.DISPLAY_NAME_PRIMARY,          // 1
            Contacts.CONTACT_PRESENCE,              // 2
            Contacts.CONTACT_STATUS,                // 3
            Contacts.PHOTO_ID,                      // 4
            Contacts.PHOTO_THUMBNAIL_URI,           // 5
            Contacts.LOOKUP_KEY,                    // 6
            Contacts.PHONETIC_NAME,                 // 7
            Contacts.STARRED,                       // 8
            SearchSnippets.SNIPPET,                 // 9
    };

    private static final String DIRECTORY_ID_ARG_KEY = "directoryId";

    public SearchContactsContainer(Context context) {
        this(context, null);
    }

    public SearchContactsContainer(Context context,
                                   @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchContactsContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    /**
     * 주어진 문자렬에 관한 주소검색
     */
    @Override
    public void doSearch() {
        final boolean isEmptyQuery = TextUtils.isEmpty(mQuery);
        if (!isEmptyQuery) {
            restartLoaders();
        }
    }

    /**
     * LoaderManager 재실행
     */
    public void restartLoaders(){
        final LoaderManager loaderManager = mLauncher.getLoaderManager();
        Bundle args = new Bundle();
        args.putLong(DIRECTORY_ID_ARG_KEY, 0);
        loaderManager.restartLoader(0, args, this);
    }

    /**
     * 개별적인 주소항목을 표시할 layout id 얻기
     * @return layout id
     */
    @Override
    public int getLayoutResource(){
        return R.layout.search_item_contact;
    }

    /**
     * Loader 구성
     * @param loader
     * @param directoryId
     */
    public void configureLoader(CursorLoader loader, long directoryId) {
        String sortOrder = null;
        String query = mQuery.trim();
        final Builder builder = ContactsCompat.getContentUri().buildUpon();
        appendSearchParameters(builder, query, directoryId);
        loader.setUri(builder.build());
        loader.setProjection(FILTER_PROJECTION_PRIMARY);
        sortOrder = Contacts.SORT_KEY_PRIMARY + ", " + Contacts.SORT_KEY_PRIMARY;
        loader.setSortOrder(sortOrder);
    }

    /**
     * Builder 에 검색파라메터들 추가
     */
    private void appendSearchParameters(Builder builder, String query, long directoryId) {
        builder.appendPath(query); // Builder will encode the query
        builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                String.valueOf(directoryId));
//        if (directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE) {
//            builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
//                    String.valueOf(getDirectoryResultLimit(getDirectoryById(directoryId))));
//        }
        builder.appendQueryParameter(ContactsContract.SearchSnippets.DEFERRED_SNIPPETING_KEY, "1");
    }

    /**
     * 주소 Uri 에 해당하는 화상 얻기
     * @param thumbnailUri 주소 Uri
     * @return 주소에 해당하는 Bitmap 화상
     */
    private Bitmap getContactPhoto(String thumbnailUri) throws Exception {
        InputStream stream = null;
        try {
            stream = getContext().getContentResolver().openInputStream(Uri.parse(thumbnailUri));
            Bitmap  bm = BitmapFactory.decodeResourceStream(getResources(), null, stream, null, null);
            if (bm != null)
                Log.d("Contacts photo", String.format("Bitmap: %d x %d", bm.getWidth(), bm.getHeight()));
            return bm;
        } finally {
            if (stream != null)
                stream.close();
        }
    }

    /**
     * Loader 생성
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @android.support.annotation.Nullable Bundle args) {
        CursorLoader loader = new FavoritesAndContactsLoader(getContext());
        long directoryId = args != null && args.containsKey(DIRECTORY_ID_ARG_KEY)
                ? args.getLong(DIRECTORY_ID_ARG_KEY) : Directory.DEFAULT;
        configureLoader(loader, directoryId);
        return loader;
    }

    /**
     * 해당 검색이 끝났을때 호출되는 callback
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        List<SearchItemInfo> resultList = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i ++) {
                SearchContactsInfo info = new SearchContactsInfo();
                String contactId = cursor.getString(cursor.getColumnIndex(Contacts._ID));

                Long contactIdLong = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
                String contactLookupKey = cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY));
                Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactIdLong, contactLookupKey);
                info.lookupUri = lookupUri;

                Cursor phones = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                while (phones.moveToNext()) {
                    PhonePair phonePair = new PhonePair();
                    phonePair.phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    int type = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    switch (type) {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            phonePair.type = getResources().getString(R.string.type_home);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            phonePair.type =  getResources().getString(R.string.type_mobile);
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                            phonePair.type = getResources().getString(R.string.type_work);
                            break;
                        default:
                            phonePair.type = getResources().getString(R.string.type_other);
                    }
                    info.phoneArray.add(phonePair);
                }

                String name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
                Bitmap profileImage = null;
                try {
                    profileImage = getContactPhoto(cursor.getString(cursor.getColumnIndexOrThrow(Contacts.PHOTO_THUMBNAIL_URI)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                info.name = name;
                if (profileImage != null)
                    info.avatar = new BitmapDrawable(getResources(), profileImage);
                resultList.add(info);
                cursor.moveToNext();
            }
        }
        updateSearchResult(resultList);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    public static class PhonePair extends SearchItemInfo {
        public String type;
        public String phoneNumber;
    }

    /**
     * 개별적인 주소 검색항목을 위한 static class
     */
    public static class SearchContactsInfo extends SearchItemInfo {
        public Drawable avatar;
        public String name;
        public ArrayList<PhonePair> phoneArray = new ArrayList<>();
        public Uri lookupUri;
    }
}
