package com.android.launcher3.assistant.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.assistant.AssistViewsContainer;
import com.chauthai.overscroll.RecyclerViewBouncy;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.JustifyContent;

import java.util.ArrayList;
import java.util.List;

/**
 * 검색창
 */
public class SearchLayout extends LinearLayout implements View.OnClickListener {
    Launcher mLauncher;

    AssistViewsContainer mRootView;

    // 검색결과를 위한 RecyclerView
    RecyclerViewBouncy mRecyclerView;

    // 검색문자렬이 비였을때 현시할 view
    ExtraSearchView mExtraView;
    MaxHeightRecyclerView mHistory;
    SearchHistoryAdapter mSearchHistoryAdapter;

    // 검색입력칸
    EditText mSearchInputBox;

    SearchRecyclerViewAdapter mSearchRecyclerViewAdapter;

    TextView mSearchClear;
    RelativeLayout mSearchArea;

    // 상태띠 와 Navigation bar 높이
    Rect mInsets;

    List<String> mHistoryList = new ArrayList(); // 검색기록목록

    boolean isQueryChanged = false; // 검색문자렬 변경상태

    private final ContentObserver historyObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            getSearchHistoryFromDB();
            mSearchHistoryAdapter.notifyDataSetChanged();
        }
    };

    public SearchLayout(Context context) {
        this(context, null);
    }

    public SearchLayout(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mRecyclerView = findViewById(R.id.search_result_container_scrollview);
        mSearchRecyclerViewAdapter = new SearchRecyclerViewAdapter(getContext(), this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mSearchRecyclerViewAdapter);

        // 입력칸 view 를 얻고 change Listener 추가
        mSearchInputBox = findViewById(R.id.search_keyword_input);
        mSearchInputBox.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                isQueryChanged = true;
                mRecyclerView.scrollToPosition(0);
                queryChanged();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        });

        mExtraView = findViewById(R.id.extra_displaying_view);
        mSearchArea = findViewById(R.id.search_history_area);
        mSearchClear = findViewById(R.id.search_history_clear);
        mSearchClear.setOnClickListener(this);

        mHistory = findViewById(R.id.search_history);
        mHistory.setMaxHeight(380);

        // 자료기지에서 검색기록 얻기
        getSearchHistoryFromDB();
        if (mHistoryList.size() == 0) mSearchArea.setVisibility(View.GONE);

        mSearchHistoryAdapter = new SearchHistoryAdapter(getContext(), this);
        CustomFlexboxLayoutManager layoutManager = new CustomFlexboxLayoutManager(getContext());
        layoutManager.setScrollEnabled(false);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.FLEX_START);
        mHistory.setLayoutManager(layoutManager);
        mHistory.setAdapter(mSearchHistoryAdapter);

        // 검색입력칸에 초점설정
        mSearchInputBox.requestFocus();
        InputMethodManager imm =  (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null && event.getAction() == MotionEvent.ACTION_MOVE) {
                    InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
                    boolean isKeyboardUp = imm.isAcceptingText();

                    if (isKeyboardUp)
                    {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    if (!mSearchInputBox.getText().toString().isEmpty() && isQueryChanged) {
                        if (mSearchArea.getVisibility() == View.GONE)
                            mSearchArea.setVisibility(View.VISIBLE);
                        isQueryChanged = false;
                        mLauncher.getModelWriter().addSearchHistoryItem(mSearchInputBox.getText().toString());

                        getSearchHistoryFromDB();
                        mSearchHistoryAdapter.notifyDataSetChanged();
                    }
                }
                return false;
            }
        });

        // 처음에 ExtraView 표시
        mExtraView.onOpen();
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();
        getContext().getContentResolver().registerContentObserver(LauncherSettings.SearchHistory.CONTENT_URI, true, historyObserver);
    }

    @Override
    public void onDetachedFromWindow(){
        getContext().getContentResolver().unregisterContentObserver(historyObserver);
        super.onDetachedFromWindow();
    }

    /**
     * 자료기지에서 검색기록얻기
     */
    public void getSearchHistoryFromDB() {
        mHistoryList = new ArrayList<>();
        Cursor cursor = mLauncher.getModelWriter().getSearchHistory();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                mHistoryList.add(cursor.getString(cursor.getColumnIndex(LauncherSettings.SearchHistory.SEARCH_QUERY)));
            }
            cursor.close();
        }
    }

    /**
     * 검색문자렬이 비였는지 확인
     * @return 비였으면 true, 아니면 false
     */
    public boolean isEmptyQuery() {
        return mSearchInputBox.getText().toString().equals("");
    }

    /**
     * 검색문자렬 빈것으로 설정
     */
    public void setQueryEmpty() {
        mSearchInputBox.setText("");
    }

    /**
     * 검색문자렬이 변경되였을때의 처리
     */
    public void queryChanged(){
        String query = mSearchInputBox.getText().toString();

        updateVisiblePart(query.isEmpty());
        if(query.isEmpty()){
            mExtraView.onOpen();
            return;
        }

        // 검색문자렬이 변경되였음을 개별적인 검색항목들을 포함하는 container 들에 알림
        for (int i = 0; i < mSearchRecyclerViewAdapter.mSearchItemContainerList.size(); i ++){
            SearchItemContainer child = mSearchRecyclerViewAdapter.mSearchItemContainerList.get(i);
            child.onQueryChanged(query);
        }
    }

    /**
     * 검색문자렬이 비였으면 ExtraView 를 현시하고 그렇지 않으면 검색결과창 현시
     * @param queryEmpty 검색문자렬이 비였으면 true 아니면 false
     */
    public void updateVisiblePart(boolean queryEmpty){
        // extra view 현시
        if(queryEmpty){
            mRecyclerView.setVisibility(GONE);
            mExtraView.setVisibility(VISIBLE);
        }

        // 검색결과창 현시
        else{
            mRecyclerView.setVisibility(VISIBLE);
            mExtraView.setVisibility(GONE);
        }
    }

    public void setInsets(Rect insets){
        mInsets = insets;

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mRecyclerView.getLayoutParams();
        lp.bottomMargin = mInsets.bottom;
    }

    /**
     * Root view 설정
     * @param parent 설정할 root view
     */
    public void setRootView(AssistViewsContainer parent){
        mRootView = parent;
    }

    @Override
    public void onClick(View view) {
        if (view == mSearchClear) {

            ClearHistoryPopup clearHistoryPopup = new ClearHistoryPopup(getContext(), this);
            clearHistoryPopup.setGravity(3);
            clearHistoryPopup.setCanceledOnTouchOutside(false);
            clearHistoryPopup.show();
        }
    }

    /**
     * 검색기록 초기화
     */
    void clearHistory() {
        mLauncher.getModelWriter().clearSearchHistory();
        mHistoryList.clear();
        mSearchHistoryAdapter.notifyDataSetChanged();
        mSearchArea.setVisibility(View.GONE);
    }

    public interface QueryChangListener{
        public void onQueryChanged(String query);
    }
}
