package com.android.launcher3.assistant.search.searchItems;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

import android.content.Context;
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
import com.android.launcher3.assistant.search.searchItems.SearchAppsContainer.SearchAppsInfo;
import com.android.launcher3.touch.ItemClickHandler;

/**
 * 개별적인 App 검색항목
 */
public class SearchItemApps extends SearchItemView {
    Launcher mLauncher;

    //App icon 을 표시할 view
    ImageView mItemIcon;

    //App 이름을 표시할 view
    TextView mItemTitle;

    // App 정보를 담고 있는 object
    SearchAppsInfo mInfo;

    public SearchItemApps(Context context) {
        this(context, null);
    }

    public SearchItemApps(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemApps(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        // 아이콘 및 이름을 표시할 view 들을 얻기
        mItemIcon = findViewById(R.id.item_icon);
        mItemTitle = findViewById(R.id.item_title);
    }

    @Override
    public void applyFromItemInfo(SearchItemInfo info, String query, SearchItemContainer searchItemContainer) {
        if(!(info instanceof SearchAppsInfo)){
            return;
        }

        mSearchItemContainer = searchItemContainer;
        SearchAppsInfo searchAppsInfo = (SearchAppsInfo)info;

        // 이름 설정
        String highlightedString = convertToHighlightedHtmlString(searchAppsInfo.title, query);
        mItemTitle.setText(Html.fromHtml(highlightedString, FROM_HTML_MODE_LEGACY));

        // 아이콘 설정
        if(searchAppsInfo.icon != null)
            mItemIcon.setImageDrawable(searchAppsInfo.icon);

        // App 정보 보관
        mInfo = searchAppsInfo;
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
            onClickApp();
        }
    }

    /**
     * 해당 App 열기
     */
    public void onClickApp(){
        if(mInfo != null && mInfo.appInfo != null){
            ItemClickHandler.startAppShortcutOrInfoActivity(null, mInfo.appInfo, mLauncher);
        }
    }
}
