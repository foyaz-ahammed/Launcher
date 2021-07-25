package com.android.launcher3.assistant;

import static ch.deletescape.lawnchair.views.LawnchairBackgroundView.ALPHA_INDEX_STATE;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import ch.deletescape.lawnchair.LawnchairLauncher;
import ch.deletescape.lawnchair.util.InvertedMultiValueAlpha.InvertedAlphaProperty;
import ch.deletescape.lawnchair.views.LawnchairBackgroundView;
import com.android.launcher3.AppInfo;
import com.android.launcher3.Desktop;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.assistant.search.SearchLayout;
import com.android.launcher3.graphics.WorkspaceAndHotseatScrim;
import com.chauthai.overscroll.RecyclerViewBouncy;

/**
 * Assist 항목들을 포함하는 layout
 */
public class AssistViewsContainer extends RelativeLayout implements Insettable {

    Launcher mLauncher;
    Desktop mDesktop;

    private final Rect mInsets = new Rect();

    //Assistant views
    RecyclerViewBouncy mMainScrollView;
    public AssistMainRecyclerViewAdapter mAssistMainRecyclerViewAdapter;
    AssistQuickAccess mQuickAccessView;

    public int width;

    //검색 view
    SearchLayout mSearchView = null;

    public AssistViewsContainer(Context context) {
        this(context, null);
    }

    public AssistViewsContainer(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistViewsContainer(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mMainScrollView = findViewById(R.id.assist_main_scrollView);
        mAssistMainRecyclerViewAdapter = new AssistMainRecyclerViewAdapter(mLauncher, this, getContext());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mMainScrollView.setLayoutManager(linearLayoutManager);
        mMainScrollView.setAdapter(mAssistMainRecyclerViewAdapter);
    }

    /**
     * Shortcut 확장페지 현시
     */
    public void openExpandedShortcutsView(){
        mAssistMainRecyclerViewAdapter.mMainLayout.setVisibility(GONE);

        //Create view inflating layout resource
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        mQuickAccessView = (AssistQuickAccess) layoutInflater.inflate(R.layout.assist_shortcut_expanded, this, false);

        //값설정
        mQuickAccessView.setRootView(this);
        mQuickAccessView.setInsets(mInsets);
        mQuickAccessView.setAdapterData(mAssistMainRecyclerViewAdapter.mShortcutAppsView.getEntireShortcuts(), mAssistMainRecyclerViewAdapter.mShortcutAppsView.getEntireApps());

        //QuickAccessView 추가
        addView(mQuickAccessView);

        mQuickAccessView.setPadding(mQuickAccessView.getPaddingLeft(), mInsets.top + mQuickAccessView.getPaddingTop(),
                mQuickAccessView.getPaddingRight(), mQuickAccessView.getPaddingBottom());

        mDesktop.setInterceptTouchAllowed(false);
    }

    /**
     * Shortcut 확장페지 닫기
     */
    public void closeExpandedShortcutsView(){
        // 모든 상태 복귀
        mAssistMainRecyclerViewAdapter.mMainLayout.setVisibility(VISIBLE);
        removeView(mQuickAccessView);
        mQuickAccessView = null;
        mDesktop.setInterceptTouchAllowed(true);

        mAssistMainRecyclerViewAdapter.mShortcutAppsView.updateShortcutsData();
    }

    /**
     * 검색칸을 눌렀을때 검색창 현시
     */
    public void openSearchView(){
        mAssistMainRecyclerViewAdapter.mMainLayout.setVisibility(GONE);

        // blur 효과 설정
        applyBlur(true);

        //Create view inflating layout resource
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        mSearchView = (SearchLayout) layoutInflater.inflate(R.layout.assist_search_layout, this, false);
        mSearchView.setRootView(this);
        mSearchView.setInsets(mInsets);

        // SearchView 추가
        addView(mSearchView);
        mSearchView.setPadding(mSearchView.getPaddingLeft(), mInsets.top + mSearchView.getPaddingTop(),
                mSearchView.getPaddingRight(), mSearchView.getPaddingBottom());

        mDesktop.setInterceptTouchAllowed(false);
    }

    /**
     * Blur 효과 적용
     * @param searchViewOpening 검색창현시상태이면 true 아니면 false
     */
    public void applyBlur(boolean searchViewOpening){
        WorkspaceAndHotseatScrim scrim = mLauncher.getDragLayer().getScrim();
        LawnchairBackgroundView background = LawnchairLauncher.getLauncher(mLauncher).getBackground();
        InvertedAlphaProperty blurProperty = background.getBlurAlphas().getProperty(ALPHA_INDEX_STATE);
        if(searchViewOpening) {
            scrim.setScrimProgress(0.35f);
            blurProperty.setValue(1);
        }
        else{
            scrim.setScrimProgress(0);
            blurProperty.setValue(0);
        }
    }

    /**
     * 뒤로가기 단추를 눌렀을때 검색창 닫기
     */
    public void closeSearchView(){
        // 모든 상태 복귀
        mAssistMainRecyclerViewAdapter.mMainLayout.setVisibility(VISIBLE);
        removeView(mSearchView);
        mSearchView = null;
        mDesktop.setInterceptTouchAllowed(true);

        //Release blur
        applyBlur(false);
    }

    /**
     * Calendar 정보 설정
     * @param appInfo app 정보
     * @param shortcutInfo shortcut 정보
     */
    public void setCalendarData(AppInfo appInfo, ShortcutInfo shortcutInfo){
        mAssistMainRecyclerViewAdapter.mCalendarView.setAppAndShortcut(appInfo, shortcutInfo);
    }

    /**
     * Clock 정보 설정
     * @param appInfo app 정보
     * @param shortcutInfo shortcut 정보
     */
    public void setClockData(AppInfo appInfo, ShortcutInfo shortcutInfo){
        mAssistMainRecyclerViewAdapter.mClockView.setAppAndShortcut(appInfo, shortcutInfo);
    }

    /**
     * Contacts 정보 설정
     * @param appInfo app 정보
     * @param shortcutInfo shortcut 정보
     */
    public void setContactsData(AppInfo appInfo, ShortcutInfo shortcutInfo) {
        mAssistMainRecyclerViewAdapter.mFavoriteContacts.setAppAndShortcut(appInfo, shortcutInfo);
    }

    /**
     * Desktop 설정
     * @param desktop
     */
    public void setDesktop(Desktop desktop){
        mDesktop = desktop;
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
        mMainScrollView.setPadding(0, mInsets.top, 0, mInsets.bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
    }

    /**
     * 검색창 현시상태 얻기
     * @return 현시되였으면 true 아니면 false
     */
    public boolean isSearchViewOpen(){
        return mSearchView != null;
    }

    /**
     * 검색창 layout 얻기
     * @return 검색창 layout
     */
    public SearchLayout getSearchLayout() {
        return mSearchView;
    }
}
