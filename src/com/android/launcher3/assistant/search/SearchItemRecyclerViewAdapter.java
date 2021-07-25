package com.android.launcher3.assistant.search;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.search.SearchItemContainer.SearchItemInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 개별적인 검색항목의 RecyclerView 를 위한 Adapter
 */
public class SearchItemRecyclerViewAdapter extends RecyclerView.Adapter<SearchItemRecyclerViewAdapter.ViewHolder> {

    private static final int VIEW_TYPE_SEARCH_RESULT_ITEM = 0;
    private static final int VIEW_TYPE_DIVIDER = 1;

    Launcher mLauncher;
    SearchItemContainer mSearchItemContainer;
    LayoutInflater mLayoutInflater;
    int mLayoutResource; // 개별적인 항목을 표시할 layout resource id
    boolean mShowClickingEffect; // 누름효과설정여부

    // 표시할 결과목록
    List<SearchItemInfo> mItemInfoList = new ArrayList<>();

    // 강조표시할 검색문자렬
    String mQuery;

    SearchItemRecyclerViewAdapter(Launcher launcher, SearchItemContainer searchItemContainer, int layoutResource, boolean showClickingEffect){
        mLauncher = launcher;
        mSearchItemContainer = searchItemContainer;
        mLayoutInflater = LayoutInflater.from(launcher);
        mLayoutResource = layoutResource;
        mShowClickingEffect = showClickingEffect;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_DIVIDER) {
            LinearLayout view = (LinearLayout) mLayoutInflater.inflate(R.layout.assist_search_item_divider, parent, false);
            return new ViewHolder(view);
        }

        if(viewType == VIEW_TYPE_SEARCH_RESULT_ITEM) {
            SearchItemView view = (SearchItemView)mLayoutInflater.inflate(mLayoutResource, parent, false);
            return new ViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if(viewType == VIEW_TYPE_DIVIDER)
            return;

        // 목록에서 개별 항목의 index 를 얻고 그 index 에 관하여 항목 정보 얻기
        int index = position/2;
        SearchItemInfo info = mItemInfoList.get(index);

        // 개별적인 항목정보를 view 에 반영
        SearchItemView view = (SearchItemView) holder.itemView;
        view.applyFromItemInfo(info, mQuery, mSearchItemContainer);

        // 클릭효과가 필요하지 않으면 간단한 transparent background 적용
        if(!mShowClickingEffect){
            view.setBackground(null);
        }

        if(mShowClickingEffect){
            view.setOnClickListener(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // SEARCH_RESULT_ITEM 은 짝수번째 위치를 의미하는것이고, DIVIDER 는 홀수번째를 의미함.
        if(position % 2 == 0)
            return VIEW_TYPE_SEARCH_RESULT_ITEM;
        return VIEW_TYPE_DIVIDER;
    }

    @Override
    public int getItemCount() {
        // 구분선개수와 항목개수를 합하여 Recyclerview 항목개수 얻기
        if(mItemInfoList.isEmpty())
            return 0;
        return 2*mItemInfoList.size() - 1;
    }

    /**
     * 목록갱신
     * @param list 갱신할 새로운 목록
     * @param query 검색문자렬
     */
    public void updateItemInfoList(List<SearchItemInfo> list, String query){
        mItemInfoList = list;
        mQuery = query;
        notifyDataSetChanged();
    }

    /**
     * 개별적인 항목을 표시할 viewHolder
     */
    public final static class ViewHolder extends RecyclerView.ViewHolder{

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
