package com.android.launcher3.assistant;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.assistant.AssistRecyclerViewAdapter.ViewHolder;
import com.android.launcher3.touch.ItemClickHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Assist shortcut 항목들의 현시를 위한 Adapter
 */
public class AssistRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

    public static final int ASSIST_DISPLAY_APP_COUNT = 5;
    public static final int ADAPTER_TYPE_APP = 100;
    public static final int ADAPTER_TYPE_SHORTCUT = 101;

    Launcher mLauncher;

    LayoutInflater mLayoutInflater;
    List<AppInfo> mFilteredApps = new ArrayList<>();
    List<ShortcutInfo> mFilteredShortcuts = new ArrayList<>();

    int mViewWidth = 0;
    int mAdapterType;

    AssistRecyclerViewAdapter(Launcher launcher, int adapterType){
        mLauncher = launcher;
        mLayoutInflater = LayoutInflater.from(mLauncher);
        mAdapterType = adapterType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                R.layout.folder_icon_to_add, parent, false);

        icon.setClickable(true);
        icon.setIconFromAssistApps(true);
        icon.getLayoutParams().height = mLauncher.getDeviceProfile().cellHeightPx;
        icon.getLayoutParams().width = mViewWidth / ASSIST_DISPLAY_APP_COUNT;
        return new ViewHolder(icon);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(mAdapterType == ADAPTER_TYPE_APP) {
            AppInfo info = mFilteredApps.get(position);
            BubbleTextView icon = (BubbleTextView) holder.itemView;
            icon.reset();
            icon.applyFromApplicationInfo(info);

            icon.setOnClickListener(v -> {
                ItemClickHandler.startAppShortcutOrInfoActivity(v, info, mLauncher);
            });
        }
        else if(mAdapterType == ADAPTER_TYPE_SHORTCUT) {
            ShortcutInfo info = mFilteredShortcuts.get(position);
            BubbleTextView icon = (BubbleTextView) holder.itemView;
            icon.reset();
            icon.applyFromShortcutInfo(info);

            icon.setOnClickListener(v -> {
                ItemClickHandler.onClickAppShortcut(v, info, mLauncher);
            });
        }
    }

    @Override
    public int getItemCount() {
        if(mAdapterType == ADAPTER_TYPE_APP)
            return mFilteredApps.size();

        else if (mAdapterType == ADAPTER_TYPE_SHORTCUT)
            return mFilteredShortcuts.size();

        return 0;
    }

    /**
     * view 너비 설정
     * @param width 새로 설정할 너비
     */
    public void setViewWidth(int width){
        mViewWidth = width;
    }

    /**
     * App 목록갱신
     * @param appInfoList 새로 갱신할 app 목록
     */
    public void updateAppData(List<AppInfo> appInfoList){
        mFilteredApps = appInfoList;
    }

    /**
     * Shortcut 자료 갱신
     * @param shortcutInfoList 새로 갱신할 shortcut 목록
     */
    public void updateShortcutData(List<ShortcutInfo> shortcutInfoList){
        mFilteredShortcuts = shortcutInfoList;
    }

    /**
     * 개별적인 아이콘들을 위한 viewHolder
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View v) {
            super(v);
        }
    }
}
