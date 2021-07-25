package com.android.launcher3.assistant.search.searchItems;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.search.SearchItemContainer;
import java.util.ArrayList;
import java.util.List;

/**
 * Sms 검색부분
 */
public class SearchSmsContainer extends SearchItemContainer {

    Launcher mLauncher;

    public SearchSmsContainer(Context context) {
        this(context, null);
    }

    public SearchSmsContainer(Context context,
                               @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchSmsContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    /**
     * Sms 검색
     */
    @Override
    public void doSearch() {
        List<SearchItemInfo> resultList = new ArrayList<>();
        ArrayList<String[]> messages = new ArrayList<String[]>();
        messages = fillMessages(mQuery);
        for (String[] message : messages) {
            SearchSmsInfo info = new SearchSmsInfo();
            info.title = message[6] != null ? message[6] : message[0];
            info.address = message[0];
            info.shortContent = message[1];
            info.modifiedDate = message[2];
            resultList.add(info);
        }
        updateSearchResult(resultList);
    }

    /**
     * 주어진 검색문자렬에 관한 Sms 목록 얻기
     * @param query 검색문자렬
     * @return Sms 목록
     */
    public ArrayList<String[]> fillMessages(String text) {
        ArrayList<String[]> messages = new ArrayList<String[]>();

        final String searchKeyword = "%" + mQuery + "%";
        Cursor cursor = getContext().getContentResolver().query(Telephony.Sms.CONTENT_URI, null,
                Telephony.Sms.BODY + " || " + Telephony.Sms.ADDRESS + " LIKE ? ", new String[] {searchKeyword}, "date DESC");

        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] data = new String[7];
                data[0] = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                data[1] = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                data[2] = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                data[3] = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                data[4] = "false";
                data[5] = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                data[6] = getContactName(getContext(), data[0]);
                messages.add(data);

                cursor.moveToNext();
            }
        }
        cursor.close();

        return messages;
    }

    /**
     * 주어진 전화번호에 관한 주소정보를 얻는 함수
     * @param context The application context
     * @param phoneNumber 전화번호
     * @return 주소이름
     */
    public String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }

    /**
     * 개별적인 Sms 항목을 표시할 layout id 얻기
     * @return layout id
     */
    @Override
    public int getLayoutResource(){
        return R.layout.search_item_sms;
    }

    /**
     * 개별적인 Sms 검색항목을 위한 static class
     */
    public static class SearchSmsInfo extends SearchItemInfo{
        public Drawable icon;
        public String title;
        public String address;
        public String shortContent;
        public String modifiedDate;
    }
}
