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

/**
 * 전화번호목록을 표시하는 Recyclerview 를 위한 Adapter
 */
public class PhoneItemRecyclerViewAdapter extends RecyclerView.Adapter<PhoneItemRecyclerViewAdapter.ViewHolder> {

    private static final int VIEW_TYPE_SEARCH_RESULT_ITEM = 0;
    private static final int VIEW_TYPE_DIVIDER = 1;

    Launcher mLauncher;
    LayoutInflater mLayoutInflater;
    ArrayList<?> mPhoneList; // 전화번호목록
    int mPopupType; // 대화창형태 (call 혹은 message)

    PhoneItemRecyclerViewAdapter(Launcher launcher, ArrayList<?> phoneList, int popupType){
        mLauncher = launcher;
        mLayoutInflater = LayoutInflater.from(launcher);
        mPhoneList = phoneList;
        mPopupType = popupType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_DIVIDER) {
            LinearLayout view = (LinearLayout) mLayoutInflater.inflate(R.layout.assist_phone_item_divider, parent, false);
            return new ViewHolder(view);
        }

        if(viewType == VIEW_TYPE_SEARCH_RESULT_ITEM) {
            SearchItemView view = (SearchItemView)mLayoutInflater.inflate(R.layout.search_item_phone, parent, false);
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
        SearchItemInfo info = (SearchItemInfo) mPhoneList.get(index);

        SearchItemView view = (SearchItemView) holder.itemView;
        view.applyFromItemInfo(info, mPopupType == 1 ? "call" : "message", null);
        view.setOnClickListener(view);

    }

    @Override
    public int getItemViewType(int position) {
        // SEARCH_RESULT_ITEM 은 짝수번째 위치를 의미하는것이고, DIVIDER 는 홀수번째를 의미함.
        if(position % 2 == 1)
            return VIEW_TYPE_SEARCH_RESULT_ITEM;
        return VIEW_TYPE_DIVIDER;
    }

    @Override
    public int getItemCount() {
        // 구분선개수와 항목개수를 합하여 Recyclerview 항목개수 얻기
        if(mPhoneList.isEmpty())
            return 0;
        return 2*mPhoneList.size();
    }

    public final static class ViewHolder extends RecyclerView.ViewHolder{

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
