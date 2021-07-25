package com.android.launcher3.assistant.search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.assistant.search.SearchHistoryAdapter.ViewHolder;

/**
 * 검색기록목록을 위한 Adapter
 */
public class SearchHistoryAdapter extends RecyclerView.Adapter<ViewHolder> {
    LayoutInflater mInflater;
    SearchLayout mSearchLayout;

    public SearchHistoryAdapter(Context context, SearchLayout searchLayout) {
        mInflater = LayoutInflater.from(context);
        mSearchLayout = searchLayout;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.search_item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mHistoryItem.setText(mSearchLayout.mHistoryList.get(position));
    }

    @Override
    public int getItemCount() {
        return mSearchLayout.mHistoryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mHistoryItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mHistoryItem = itemView.findViewById(R.id.history_item);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mSearchLayout.mSearchInputBox.setText(mSearchLayout.mHistoryList.get(getAdapterPosition()));
            mSearchLayout.mSearchInputBox.setSelection(mSearchLayout.mSearchInputBox.getText().length());
        }
    }
}
