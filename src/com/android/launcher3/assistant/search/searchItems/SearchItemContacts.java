package com.android.launcher3.assistant.search.searchItems;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.search.SearchItemContainer;
import com.android.launcher3.assistant.search.SearchItemContainer.SearchItemInfo;
import com.android.launcher3.assistant.search.SearchItemView;
import com.android.launcher3.assistant.search.SearchPhonesPopup;
import com.android.launcher3.assistant.search.searchItems.SearchContactsContainer.SearchContactsInfo;

import java.util.ArrayList;

import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static android.text.Html.FROM_HTML_MODE_LEGACY;

/**
 * 개별적인 주소검색항목
 */
public class SearchItemContacts extends SearchItemView {
    Launcher mLauncher;

    // 주소화상을 표시하는 view
    ImageView mItemAvatar;

    // 주소이름을 표시하는 view
    TextView mItemName;

    // Dialer 아이콘을 표시하는 view
    ImageView mItemDialer;

    // Message 아이콘을 표시하는 view
    ImageView mItemMessage;

    // 주소정보를 포함하는 object
    SearchContactsInfo mInfo;

    int POPUP_TYPE_CALL = 1;
    int POPUP_TYPE_SEND_MESSAGE = 2;

    ArrayList<SearchContactsContainer.PhonePair> mPhoneList = new ArrayList<>();

    public SearchItemContacts(Context context) {
        this(context, null);
    }

    public SearchItemContacts(Context context,
                              @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemContacts(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mItemAvatar = findViewById(R.id.item_icon);
        mItemName = findViewById(R.id.item_name);
        mItemDialer = findViewById(R.id.item_dialer);
        mItemDialer.setOnClickListener(this);
        mItemDialer.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mSearchItemContainer.pushDownAnim(0.95f, mSearchItemContainer.durationPush);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (mSearchItemContainer.rect != null && !mSearchItemContainer.isOutside && !mSearchItemContainer.rect.contains(getLeft() + (int) motionEvent.getX(), getTop() + (int) motionEvent.getY())) {
                        mSearchItemContainer.isOutside = true;
                    }
                    mSearchItemContainer.pushDownAnim(1.0f, mSearchItemContainer.durationRelease);
                } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    mSearchItemContainer.pushDownAnim(1.0f, mSearchItemContainer.durationRelease);
                }
                return false;
            }
        });

        mItemMessage = findViewById(R.id.item_message);
        mItemMessage.setOnClickListener(this);
        mItemMessage.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mSearchItemContainer.pushDownAnim(0.95f, mSearchItemContainer.durationPush);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (mSearchItemContainer.rect != null && !mSearchItemContainer.isOutside && !mSearchItemContainer.rect.contains(getLeft() + (int) motionEvent.getX(), getTop() + (int) motionEvent.getY())) {
                        mSearchItemContainer.isOutside = true;
                    }
                    mSearchItemContainer.pushDownAnim(1.0f, mSearchItemContainer.durationRelease);
                } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    mSearchItemContainer.pushDownAnim(1.0f, mSearchItemContainer.durationRelease);
                }
                return false;
            }
        });
    }

    /**
     * 개별적인 주소항목 현시
     * @param info 주소항목정보
     * @param query 검색문자렬
     * @param searchItemContainer 검색된 항목들을 담고있는 Container
     */
    @Override
    public void applyFromItemInfo(SearchItemInfo info, String query, SearchItemContainer searchItemContainer) {
        if(!(info instanceof SearchContactsInfo)){
            return;
        }

        mSearchItemContainer = searchItemContainer;
        SearchContactsInfo searchContactsInfo = (SearchContactsInfo) info;

        // 제목 설정
        String highlightedString = convertToHighlightedHtmlString(searchContactsInfo.name, query);
        mItemName.setText(Html.fromHtml(highlightedString, FROM_HTML_MODE_LEGACY));

        // 아이콘 설정
        if(searchContactsInfo.avatar != null) {
            mItemAvatar.setImageDrawable(searchContactsInfo.avatar);
            mItemAvatar.setBackground(new ShapeDrawable(new OvalShape()));
            mItemAvatar.setClipToOutline(true);
        } else {
            mItemAvatar.setImageResource(R.drawable.ic_person_light_large);
            mItemAvatar.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.search_sms_default_icon));
        }

        mPhoneList = searchContactsInfo.phoneArray;

        // 주소정보 보관
        mInfo = searchContactsInfo;
    }

    @Override
    public void onClick(View v) {
        //Click self view
        if(v == this){
            ContactsContract.QuickContact.showQuickContact(getContext(), this, mInfo.lookupUri, ContactsContract.QuickContact.MODE_LARGE, null);

            // 입력건반 숨기기
            InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
            boolean isKeyboardUp = imm.isAcceptingText();

            if (isKeyboardUp)
            {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } else if (v == mItemDialer) {
            if (mPhoneList.size() == 1) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + mPhoneList.get(0).phoneNumber));
                if (checkSelfPermission(getContext(),
                        android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mLauncher.startActivity(callIntent);
            } else if (mPhoneList.size() > 1) {
                SearchPhonesPopup phonesPopup = new SearchPhonesPopup(getContext(), mPhoneList,
                        getResources().getString(R.string.search_phone_popup_title_call), POPUP_TYPE_CALL);
                phonesPopup.setGravity(3);
                phonesPopup.show();
            }

        } else if (v == mItemMessage) {
            if (mPhoneList.size() == 1) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SENDTO);
                sendIntent.setData(Uri.parse("smsto:" + mPhoneList.get(0).phoneNumber));
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mLauncher.startActivity(sendIntent);
            } else if (mPhoneList.size() > 1) {
                SearchPhonesPopup phonesPopup = new SearchPhonesPopup(getContext(), mPhoneList,
                        getResources().getString(R.string.search_phone_popup_title_send_message), POPUP_TYPE_SEND_MESSAGE);
                phonesPopup.setGravity(3);
                phonesPopup.show();
            }
        }
    }
}
