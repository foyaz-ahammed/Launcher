package com.android.launcher3.assistant.search;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 모든 검색 항목들을 포함하는 view 들을 위한 기초클라스
 * 이 클라스를 계승한 클라스를 창조하면 #getLayoutResource() 함수를 override 하여야 한다.
 */

public class SearchItemContainer extends LinearLayout implements View.OnClickListener, SearchLayout.QueryChangListener {

    private static final int SHOW_RESULT_COUNT_ON_LESS = 3;
    private static final int SHOW_RESULT_MAX_COUNT = 10;
    private static final int EXPAND_BUTTON_ANIMATOR_DURATION = 150;
    public static final long DEFAULT_PUSH_DURATION = 50;
    public static final long DEFAULT_RELEASE_DURATION = 125;
    public static final AccelerateDecelerateInterpolator DEFAULT_INTERPOLATOR
            = new AccelerateDecelerateInterpolator();

    //Launcher root activity
    Launcher mLauncher;

    // 검색 layout
    SearchRecyclerViewAdapter.ViewHolder mSearchLayout;

    // 제목 label 과 검색항목제목
    String mTitle;
    TextView mSearchItemTitle;

    // 확대/축소 상태 표시
    boolean mExpanded = false;
    ImageButton mExpandButton;
    LinearLayout mSearchItemTitleContainer;

    // 확대단추 회전 animator
    ValueAnimator mExpandButtonAnimator;
    float mRecyclerViewStartHeight;
    float mRecyclerViewEndHeight;

    // 검색결과를 현시할 recyclerview
    RecyclerView mRecyclerView;

    // 검색결과를 위한 adapter
    SearchItemRecyclerViewAdapter mAdapter;

    protected String mQuery;
    // 현시할 검색결과정보목록
    List<SearchItemInfo> mItemInfoList = new ArrayList<>();

    private WeakReference<View> weakView;
    public boolean isOutside;
    public Rect rect;
    public long durationPush = DEFAULT_PUSH_DURATION;
    public long durationRelease = DEFAULT_RELEASE_DURATION;
    public AccelerateDecelerateInterpolator interpolatorPush = DEFAULT_INTERPOLATOR;
    public AnimatorSet scaleAnimSet;

    // 검색이 끝나면 true, 아니면 false
    private boolean mLoadFinished = true;

    public SearchItemContainer(Context context) {
        this(context, null);
    }

    public SearchItemContainer(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchItemContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //launcher 얻기
        mLauncher = Launcher.getLauncher(context);

        @SuppressLint("Recycle") TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SearchItemContainer);

        //제목 얻기
        mTitle = typedArray.getString(R.styleable.SearchItemContainer_searchItemTitle);

        LayoutInflater.from(context).inflate(R.layout.assist_search_item_view, this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        // 제목표시를 위한 view 를 얻고 제목설정
        mSearchItemTitle = findViewById(R.id.search_item_title);
        mSearchItemTitle.setText(mTitle);

        // 확대단추 얻고 click listener 설정
        mExpandButton = findViewById(R.id.expand_button);
        mSearchItemTitleContainer = findViewById(R.id.search_item_title_container);
        mSearchItemTitleContainer.setOnClickListener(this);
        mSearchItemTitleContainer.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    pushDownAnim(0.95f, durationPush);
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (rect != null && !isOutside && !rect.contains(getLeft() + (int) motionEvent.getX(), getTop() + (int) motionEvent.getY())) {
                        isOutside = true;
                    }
                    pushDownAnim(1.0f, durationRelease);
                } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    pushDownAnim(1.0f, durationRelease);
                }
                return false;
            }
        });

        // RecyclerView 얻기
        mRecyclerView = findViewById(R.id.search_item_list);

        // RecyclerView 에 layout manager 설정
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        // Adapter 생성
        mAdapter = new SearchItemRecyclerViewAdapter(mLauncher, this, getLayoutResource(), showClickViewEffect());
        mRecyclerView.setAdapter(mAdapter);

        // 확대단추상태 설정
        updateExpandButtonVisibility();

        PushDownAnim.setPushDownAnimTo(this).setScale( PushDownAnim.MODE_SCALE,
                0.95f  );

        rect = new Rect(getLeft(), getTop(), getRight(), getBottom() );
    }

    /**
     * 내리펼침 Animation
     * @param scale 확대정도
     * @param duration 내리펼침 지속시간
     */
    public void pushDownAnim(float scale, long duration) {
        isOutside = false;
        animate().cancel();
        if( scaleAnimSet != null ){
            scaleAnimSet.cancel();
        }

        ObjectAnimator scaleX = ObjectAnimator.ofFloat( this, "scaleX", scale );
        ObjectAnimator scaleY = ObjectAnimator.ofFloat( this, "scaleY", scale );
        scaleX.setInterpolator( interpolatorPush );
        scaleX.setDuration(duration);
        scaleY.setInterpolator( interpolatorPush );
        scaleY.setDuration( duration );

        scaleAnimSet = new AnimatorSet();
        scaleAnimSet
                .play( scaleX )
                .with( scaleY );
        scaleX.addListener( new AnimatorListenerAdapter(){
            @Override
            public void onAnimationStart( Animator animation ){
                super.onAnimationStart( animation );
            }

            @Override
            public void onAnimationEnd( Animator animation ){
                super.onAnimationEnd( animation );
            }
        } );
        scaleX.addUpdateListener( new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate( ValueAnimator valueAnimator ){
                View p = (View) getParent();
                if( p != null ) p.invalidate();
            }
        } );
        scaleAnimSet.start();
    }

    /**
     * 현시할 목록 얻기
     * @param showMore true 이면 10개현시, false 이면 3개로 제한
     * @return
     */
    public List<SearchItemInfo> getDisplayingInfoList(boolean showMore){
        final int showingItemCount;
        if(showMore)
            showingItemCount = Math.min(mItemInfoList.size(), SHOW_RESULT_MAX_COUNT);
        else
            showingItemCount = Math.min(mItemInfoList.size(), SHOW_RESULT_COUNT_ON_LESS);

        List<SearchItemInfo> resultList = new ArrayList<>();
        for (int i = 0; i < showingItemCount; i ++){
            SearchItemInfo info = mItemInfoList.get(i);
            resultList.add(info);
        }

        return resultList;
    }

    //검색문자렬 보관, 검색
    @Override
    public void onQueryChanged(String query) {
        mQuery = query;
        mLoadFinished = false;

        // 검색을 위한 thread 실행
        post(new Runnable() {
            @Override
            public void run() {
                doSearch();
            }
        });
    }

    // 검색을 위해 이 함수를 override 하여야 한다.
    public void doSearch(){
        //Do nothing
    }

    public void updateSearchResult(List<SearchItemInfo> list){

        // 검색결과 보관
        mItemInfoList = list;

        // 검색이 끝나면 부모 layout 에 알림
        mLoadFinished = true;
        if(mSearchLayout != null)
            mSearchLayout.searchResultLoaded();

        // 검색결과가 비였는지 검사하고 비였으면 view 를 숨기기
        if(list.isEmpty()){
            setVisibility(GONE);

            // 이 경우 recyclerview 를 갱신할 필요가 없으므로 탈퇴
            return;
        }
        else{
            setVisibility(VISIBLE);
        }

        // 확대단추 현시 및 숨기기
        updateExpandButtonVisibility();

        // 검색결과 현시
        List<SearchItemInfo> resultList = getDisplayingInfoList(mExpanded);
        mAdapter.updateItemInfoList(resultList, mQuery);

        // Recyclerview 의 높이 갱신
        updateRecyclerViewHeight(resultList.size());
    }

    /**
     * RecyclerView 의 높이 갱신
     * @param itemCount RecyclerView 가 포함하고 있는 항목개수
     */
    public void updateRecyclerViewHeight(int itemCount){
        float expectedHeight = getExpectedRecyclerViewHeight(itemCount);
        mRecyclerView.getLayoutParams().height = (int) expectedHeight;
    }

    /**
     * 항목개수에 관하여 RecyclerView 의 예상되는 높이 얻기
     * @param itemCount 항목개수
     * @return 높이
     */
    public float getExpectedRecyclerViewHeight(int itemCount){
        if(itemCount == 0)
            return 0;

        // 짝수번째는 검색결과항목이고 홀수번재는 구분선이다.
        // 총높이 계산
        float searchItemHeight = mLauncher.getResources().getDimension(R.dimen.search_item_height);
        float searchDividerHeight = mLauncher.getResources().getDimension(R.dimen.search_item_divider_height);
        return Utilities.getFloorInteger(itemCount * searchItemHeight + (itemCount - 1) * searchDividerHeight);
    }

    @Override
    public void onClick(View v){
        //Clicked expand button
        if(v == mSearchItemTitleContainer){
            InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
            boolean isKeyboardUp = imm.isAcceptingText();

            if (isKeyboardUp) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            showExpand();
        }
    }

    /**
     * 확대단추 현시 및 숨기기
     */
    public void updateExpandButtonVisibility(){
        //Update visibility of expanded button
        if(mItemInfoList.size() > SHOW_RESULT_COUNT_ON_LESS)
            mExpandButton.setVisibility(VISIBLE);
        else
            mExpandButton.setVisibility(INVISIBLE);
    }

    /**
     * 확대 및 축소상태 반영
     */
    public void showExpand(){
        //확대상태 전환
        mExpanded = !mExpanded;

        Runnable endAction = null;
        List<SearchItemInfo> resultList = getDisplayingInfoList(mExpanded);
        if(mExpanded)
            mAdapter.updateItemInfoList(resultList, mQuery);
        else
            endAction = new Runnable() {
                @Override
                public void run() {
                    mAdapter.updateItemInfoList(resultList, mQuery);
                }
            };

        // Recyclerview 의 높이 갱신
        mRecyclerViewStartHeight = mRecyclerView.getHeight();
        mRecyclerViewEndHeight = getExpectedRecyclerViewHeight(resultList.size());
        playExpandButtonAnimator(endAction);
    }

    /**
     * 확대 및 축소 Animation 상태갱신
     * @param progress
     */
    public void setExpandAnimatorProgress(float progress){
        //Rotate max to 180 degree
        float degree = 180 * (mExpanded? progress : 1 - progress);
        float height = mRecyclerViewStartHeight + (mRecyclerViewEndHeight - mRecyclerViewStartHeight) * progress;
        mExpandButton.setRotation(degree);
        mRecyclerView.getLayoutParams().height = (int) height;
        mRecyclerView.requestLayout();
    }

    /**
     * 확대 및 축소단추 Animation 상태갱신
     * @param runnable
     */
    public void playExpandButtonAnimator(Runnable runnable){
        //Play rotating animation of the button
        if(mExpandButtonAnimator != null){
            if(mExpandButtonAnimator.isRunning()) {
                mExpandButtonAnimator.pause();
                mExpandButtonAnimator.removeAllListeners();
            }
            mExpandButtonAnimator = null;
        }

        mExpandButtonAnimator = ValueAnimator.ofFloat(0, 1);
        mExpandButtonAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float)animation.getAnimatedValue();
                setExpandAnimatorProgress(value);
            }
        });
        mExpandButtonAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(runnable != null)
                    runnable.run();
            }
        });

        mExpandButtonAnimator.setDuration(EXPAND_BUTTON_ANIMATOR_DURATION);
        mExpandButtonAnimator.start();
    }

    /*-- Override this method --*/
    /**
     * layout resource id 를 반환하는 함수 (례: R.layout.item_layout)
     * @return
     */
    protected int getLayoutResource(){
        // 이것은 아무런 의미도 없다. 그저 불필요한 값을 반환한것뿐이다.
        return 0;
    }

    /**
     * 검색결과의 빈상태 확인
     * @return true 이면 빈상태이고, false 이면 결과가 있음
     */
    public boolean noResult(){
        return mItemInfoList.isEmpty();
    }

    /**
     * 검색이 끝났는지 확인
     * @return true 이면 검색이 끝난것이고 false 이면 검색이 끝나지 않음
     */
    public boolean loadFinished(){
        return mLoadFinished;
    }

    /**
     * 검색 layout 설정
     * @param searchLayout 설정할 검색 layout
     */
    public void setSearchLayout(SearchRecyclerViewAdapter.ViewHolder searchLayout){
        mSearchLayout = searchLayout;
    }

    /*-- Override this method if needed --*/
    protected boolean showClickViewEffect(){
        //Return true by default
        return true;
    }

    /**
     * 개별적인 검색항목 object 를 위한 기초클라스
     */
    public abstract static class SearchItemInfo{
        public Drawable icon;
        public String title;
    }
}
