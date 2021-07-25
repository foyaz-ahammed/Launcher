package com.android.launcher3.assistant.search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * 검색항목들을 위한 Adapter
 */
public class SearchRecyclerViewAdapter extends RecyclerView.Adapter<SearchRecyclerViewAdapter.ViewHolder> {
    LayoutInflater mInflater;
    SearchLayout mSearchLayout;
    public LinearLayout mSearchResultLayout;
    LinearLayout mEmptySearchResultView;

    // 검색 항목들을 담고 있는 container 목록
    public List<SearchItemContainer> mSearchItemContainerList = new ArrayList<>();

    public SearchRecyclerViewAdapter(Context context, SearchLayout searchLayout) {
        mInflater = LayoutInflater.from(context);
        mSearchLayout = searchLayout;
    }

    @NonNull
    @Override
    public SearchRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.search_result_layout, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchRecyclerViewAdapter.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mEmptySearchResultView = itemView.findViewById(R.id.search_no_result);
            mSearchResultLayout = itemView.findViewById(R.id.search_result_container);
            for (int i = 0; i < mSearchResultLayout.getChildCount(); i ++){
                if(mSearchResultLayout.getChildAt(i) instanceof SearchItemContainer){
                    SearchItemContainer child = (SearchItemContainer) mSearchResultLayout.getChildAt(i);
                    child.setSearchLayout(this);
                    mSearchItemContainerList.add(child);
                }
            }

            mSearchLayout.updateVisiblePart(true);
        }

        // 모든 검색항목의 검색이 끝났는지 확인
        public void searchResultLoaded(){
            // 모든 검색항목의 검색이 끝나지 않았으면 탈퇴
            for (SearchItemContainer child: mSearchItemContainerList){
                if(!child.loadFinished()) {
                    return;
                }
            }

            boolean noResult = true;
            for (SearchItemContainer child: mSearchItemContainerList){
                if(!child.noResult()) {
                    noResult = false;
                    break;
                }
            }

            if(noResult){
                mEmptySearchResultView.setVisibility(VISIBLE);
            }
            else{
                mEmptySearchResultView.setVisibility(GONE);
            }
        }

        @Override
        public void onClick(View view) {

        }
    }
}
