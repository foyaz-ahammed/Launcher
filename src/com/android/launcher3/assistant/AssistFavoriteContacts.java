package com.android.launcher3.assistant;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.launcher3.AppInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.assistant.search.searchItems.SearchContactsContainer;
import com.android.launcher3.assistant.search.searchItems.SearchContactsContainer.SearchContactsInfo;
import com.android.launcher3.touch.ItemClickHandler;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * 즐겨찾는 주소들을 현시하는 부분
 */
public class AssistFavoriteContacts extends AssistItemContainer implements OnClickListener {

    // App 정보와 Shortcut 정보
    AppInfo mAppInfo = null;
    ShortcutInfo mShortcutInfo = null;
    View mStartTitle;
    ImageButton mMenuButton;
    ImageView mAppIcon;
    RecyclerView mFavoritesList;
    AssistFavoriteContactsRecyclerViewAdapter mFavoriteContactsAdapter;

    public static final int FAVORITES_PER_ROW = 5;

    List<SearchContactsInfo> mData = new ArrayList<>();

    private final ContentObserver contentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            getFavoriteContacts();
            mFavoriteContactsAdapter.notifyDataSetChanged();
        }
    };

    public AssistFavoriteContacts(Context context) {
        this(context, null);
    }

    public AssistFavoriteContacts(Context context,
                                  @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistFavoriteContacts(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        defaultIndex = 6;
        currentIndex = 6;
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mStartTitle = findViewById(R.id.dialog_start_title);
        mStartTitle.setOnClickListener(this);
        mMenuButton = findViewById(R.id.menu_button);
        mMenuButton.setOnClickListener(this);
        mAppIcon = findViewById(R.id.app_icon);
        mFavoritesList = findViewById(R.id.favorites_list);

        getFavoriteContacts();
        mFavoriteContactsAdapter = new AssistFavoriteContactsRecyclerViewAdapter(mLauncher, getContext(), this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mLauncher, FAVORITES_PER_ROW,
                GridLayoutManager.VERTICAL, false);
        mFavoritesList.setLayoutManager(gridLayoutManager);
        mFavoritesList.setAdapter(mFavoriteContactsAdapter);

        // 이 부분에 해당하는 순서와 pin 상태를 자료기지에서부터 얻기
        Cursor cursor = mLauncher.getModelWriter().getAssistantContactsSectionOrder();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                currentIndex = cursor.getInt(0);
                isPinned = cursor.getInt(1) != 0;
            }
            cursor.close();
        }
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        if (checkSelfPermission(getContext(),
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ;
        }
        getContext().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
    }

    @Override
    public void onDetachedFromWindow(){
        getContext().getContentResolver().unregisterContentObserver(contentObserver);
        super.onDetachedFromWindow();
    }

    /**
     * 즐겨찾는 주소들을 얻기
     */
    public void getFavoriteContacts() {
        mData = new ArrayList<>();
        if (checkSelfPermission(getContext(),
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ;
        }
        Cursor cursor = getContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                null, "starred=?", new String[] {"1"}, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getCount(); i ++) {
                SearchContactsInfo contact = new SearchContactsInfo();
                Long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor phones = getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);

                while (phones.moveToNext()) {
                    SearchContactsContainer.PhonePair phonePair = new SearchContactsContainer.PhonePair();
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
                    contact.phoneArray.add(phonePair);
                }

                Bitmap profileImage = null;
                try {
                    profileImage = getContactPhoto(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                contact.name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (profileImage != null)
                    contact.avatar = new BitmapDrawable(getResources(), profileImage);
                mData.add(contact);

                cursor.moveToNext();
            }
            cursor.close();
        }
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

    public void setAppAndShortcut(AppInfo appInfo, ShortcutInfo shortcutInfo){
        mAppInfo = appInfo;
        mShortcutInfo = shortcutInfo;

        Drawable appIcon = new BitmapDrawable(null, appInfo.iconBitmap);
        mAppIcon.setImageDrawable(appIcon);
    }

    @Override
    public void onClick(View v) {
        //Open app
        if(v == mStartTitle){
            if(mAppInfo == null){
                Toast.makeText(mLauncher, R.string.app_not_installed, Toast.LENGTH_SHORT).show();
            }
            else{
                ItemClickHandler.startAppShortcutOrInfoActivity(null, mAppInfo, mLauncher);
            }
        }
        else if(v == mMenuButton){
            //Show popup menu
            AssistPopupMenu popupMenu = new AssistPopupMenu(getContext(), mMenuButton,
                    !isPinned, isPinned, false, getPaddingLeft(), getPaddingRight());
            popupMenu.addMenuSelectListener(this);
            popupMenu.show();
        }
    }
}
