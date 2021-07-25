package com.android.launcher3.assistant.search.searchItems;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.AttributeSet;
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
import com.android.launcher3.assistant.search.searchItems.SearchSmsContainer.SearchSmsInfo;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 개별적인 Sms 검색항목
 */
public class SearchItemSms extends SearchItemView {
    Launcher mLauncher;

    //화상을 표시할 view
    ImageView mItemImg;

    //제목을 표시할 view
    TextView mItemTitle;

    //짧은 내용을 표시할 view
    TextView mItemShortContent;

    //수정된 시간을 표시할 view
    TextView mModifiedDate;

    // 개별적인 항목정보를 담고있는 object
    SearchSmsInfo mInfo;

    public SharedPreferences sharedPrefs;

    public SearchItemSms(Context context) {
        this(context, null);
    }

    public SearchItemSms(Context context,
                          @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemSms(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        //Get image, and title views
        mItemImg = findViewById(R.id.item_img);
        mItemTitle = findViewById(R.id.item_title);
        mItemShortContent = findViewById(R.id.item_short_content);
        mModifiedDate = findViewById(R.id.item_modified_date);
    }

    /**
     * 개별적인 Sms 항목 현시
     * @param info Sms 항목정보
     * @param query 검색문자렬
     * @param searchItemContainer 검색된 항목들을 담고있는 Container
     */
    @Override
    public void applyFromItemInfo(SearchItemInfo info, String query, SearchItemContainer searchItemContainer) {
        if(!(info instanceof SearchSmsInfo)){
            return;
        }
        mSearchItemContainer = searchItemContainer;

        SearchSmsInfo searchSmsInfo = (SearchSmsInfo) info;

        //화상 및 제목 설정
        String highlightedString = convertToHighlightedHtmlString(
                searchSmsInfo.title, query);
        mItemTitle.setText(Html.fromHtml(highlightedString, FROM_HTML_MODE_LEGACY));
        mItemTitle.setMaxLines(1);

        //Sms 내용 설정
        String highlightedContent = convertToHighlightedHtmlString(
                searchSmsInfo.shortContent, query);
        mItemShortContent.setText(Html.fromHtml(highlightedContent, FROM_HTML_MODE_LEGACY));
        mItemShortContent.setMaxLines(1);

        //아이콘화상 설정
        if(searchSmsInfo.icon != null)
            mItemImg.setImageDrawable(searchSmsInfo.icon);

        Date sendDate;

        try {
            sendDate = new Date(Long.parseLong(searchSmsInfo.modifiedDate));
        } catch (Exception e) {
            sendDate = new Date(0);
        }

        String dateString;

        if (sharedPrefs.getBoolean("hour_format", false)) {
            dateString = DateFormat.getDateInstance(DateFormat.SHORT, Locale.KOREA).format(sendDate);
        } else {
            dateString = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(sendDate);
        }

        if (sharedPrefs.getBoolean("hour_format", false)) {
            dateString += " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.KOREA).format(sendDate);
        } else {
            dateString += " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(sendDate);
        }

        mModifiedDate.setText(dateString);

        //항목정보 보관
        mInfo = searchSmsInfo;
    }

    @Override
    public void onClick(View v) {
        //Click self view
        if(v == this){
            //입력건반 숨기기
            InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
            boolean isKeyboardUp = imm.isAcceptingText();

            if (isKeyboardUp)
            {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            onClickApp();
        }
    }

    /**
     * Sms 관련 App 열기
     */
    public void onClickApp(){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SENDTO);
        sendIntent.setData(Uri.parse("smsto:" + mInfo.address));
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mLauncher.startActivity(sendIntent);
    }
}
