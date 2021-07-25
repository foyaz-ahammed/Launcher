package com.android.launcher3.assistant.search.searchItems;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.SettingsUtility.search.AppSearchResult;
import com.android.launcher3.assistant.SettingsUtility.search.SearchResult;
import com.android.launcher3.assistant.search.SearchItemContainer;
import com.android.launcher3.assistant.search.SearchItemContainer.SearchItemInfo;
import com.android.launcher3.assistant.search.SearchItemView;
import com.android.launcher3.assistant.search.searchItems.SearchSettingsContainer.SearchSettingsInfo;
import java.util.List;

/**
 * 개별적인 설정검색항목
 */
public class SearchItemSettings extends SearchItemView {
    static final int REQUEST_CODE_NO_OP = 0;
    Launcher mLauncher;

    //아이콘을 표시할 view
    ImageView mItemIcon;

    //제목을 표시할 view
    TextView mItemTitle;

    //개별적인 설정항목정보를 담고 있는 object
    SearchSettingsInfo mInfo;

    public SearchItemSettings(Context context) {
        this(context, null);
    }

    public SearchItemSettings(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemSettings(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        // 아이콘 및 제목 view 들을 얻기
        mItemIcon = findViewById(R.id.item_icon);
        mItemTitle = findViewById(R.id.item_title);
    }

    /**
     * 개별적인 설정 항목 현시
     * @param info 설정항목정보
     * @param query 검색문자렬
     * @param searchItemContainer 검색된 항목들을 담고있는 Container
     */
    @Override
    public void applyFromItemInfo(SearchItemInfo info, String query, SearchItemContainer searchItemContainer) {
        if(!(info instanceof SearchSettingsInfo)){
            return;
        }
        mSearchItemContainer = searchItemContainer;

        SearchSettingsInfo searchAppsInfo = (SearchSettingsInfo)info;

        //제목설정
        String highlightedString = convertToHighlightedHtmlString(searchAppsInfo.title, query);
        mItemTitle.setText(Html.fromHtml(highlightedString, FROM_HTML_MODE_LEGACY));

        // 아이콘화상 설정
        if(searchAppsInfo.icon != null)
            mItemIcon.setImageDrawable(searchAppsInfo.icon);

        // 항목정보 보관
        mInfo = searchAppsInfo;
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
            onClickSearchItem();
        }
    }

    /**
     * 설정항목관련 Activity 현시
     */
    public void onClickSearchItem(){
        SearchResult result = mInfo.searchResult;

        final Intent intent = result.payload.getIntent();
        // Use app user id to support work profile use case.
        if (result instanceof AppSearchResult) {
            mLauncher.startActivity(intent);
        } else {
            final PackageManager pm = mLauncher.getPackageManager();
            final List<ResolveInfo> info = pm.queryIntentActivities(intent, 0 /* flags */);
            if (info != null && !info.isEmpty()) {
                mLauncher.startActivityForResult(intent, REQUEST_CODE_NO_OP);
            }
        }
    }
}
