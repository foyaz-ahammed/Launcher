package com.android.launcher3.assistant.search.searchItems;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
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
import com.android.launcher3.assistant.search.searchItems.SearchImageContainer.SearchImageInfo;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

/**
 * 개별적인 화상검색항목
 */
public class SearchItemImages extends SearchItemView {
    Launcher mLauncher;

    //화상을 표시할 view
    ImageView mItemImg;

    //제목을 표시할 view
    TextView mItemTitle;

    // 화상정보
    SearchImageInfo mSearchImageInfo;

    public SearchItemImages(Context context) {
        this(context, null);
    }

    public SearchItemImages(Context context,
                            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemImages(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        // 화상 및 제목 view 들을 얻기
        mItemImg = findViewById(R.id.item_img);
        mItemTitle = findViewById(R.id.item_title);
    }

    /**
     * 개별적인 화상 항목 현시
     * @param info 화상항목정보
     * @param query 검색문자렬
     * @param searchItemContainer 검색된 항목들을 담고있는 Container
     */
    @Override
    public void applyFromItemInfo(SearchItemInfo info, String query, SearchItemContainer searchItemContainer) {
        if(!(info instanceof SearchImageInfo)){
            return;
        }

        mSearchItemContainer = searchItemContainer;
        SearchImageInfo searchImageInfo = (SearchImageInfo) info;

        // 화상 및 제목설정
        String highlightedString = convertToHighlightedHtmlString(searchImageInfo.title, query);
        mItemTitle.setText(Html.fromHtml(highlightedString, FROM_HTML_MODE_LEGACY));

        Glide.with(getContext())
                .load(searchImageInfo.dataPath)
                .apply(new RequestOptions().centerCrop())
                .into(mItemImg);
        mItemImg.setBackground(new ShapeDrawable(new OvalShape()));
        mItemImg.setClipToOutline(true);

        mSearchImageInfo = searchImageInfo;
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
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(mSearchImageInfo.dataPath), "image/*");
        mLauncher.startActivity(intent);
    }
}
