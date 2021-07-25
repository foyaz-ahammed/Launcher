package com.android.launcher3.assistant.search.searchItems;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
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
import com.android.launcher3.assistant.search.searchItems.SearchAudioContainer.SearchAudioInfo;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

/**
 * 개별적인 Audio 검색항목
 */
public class SearchItemAudios extends SearchItemView {
    Launcher mLauncher;

    //Audio Cover 화상을 표시할 view
    ImageView mItemImg;

    //이름을 표시할 view
    TextView mItemTitle;

    // 검색된 Audio 정보
    SearchAudioInfo mSearchAudioInfo;

    public SearchItemAudios(Context context) {
        this(context, null);
    }

    public SearchItemAudios(Context context,
                            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemAudios(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        // 화상 및 이름 view 들을 얻기
        mItemImg = findViewById(R.id.item_img);
        mItemTitle = findViewById(R.id.item_title);
    }

    @Override
    public void applyFromItemInfo(SearchItemInfo info, String query, SearchItemContainer searchItemContainer) {
        if(!(info instanceof SearchAudioInfo)){
            return;
        }

        mSearchItemContainer = searchItemContainer;
        SearchAudioInfo searchAudioInfo = (SearchAudioInfo) info;

        // 이름설정
        String highlightedString = convertToHighlightedHtmlString(searchAudioInfo.title, query);
        mItemTitle.setText(Html.fromHtml(highlightedString, FROM_HTML_MODE_LEGACY));

        mSearchAudioInfo = searchAudioInfo;
    }

    @Override
    public void onClick(View v) {
        //Click self view
        if(v == this){
            // 입력건반 숨기기
            InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
            boolean isKeyboardUp = imm.isAcceptingText();

            if (isKeyboardUp)
            {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }

        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mSearchAudioInfo.id);

        // Audio 를 실행시킬 app 열기
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "audio/*");
        mLauncher.startActivity(intent);
    }
}
