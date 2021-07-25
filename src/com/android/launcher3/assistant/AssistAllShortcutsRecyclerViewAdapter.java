package com.android.launcher3.assistant;

import static com.android.launcher3.assistant.AssistQuickAccess.SHORTCUTS_PER_ROW;

import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.assistant.AssistAllShortcutsRecyclerViewAdapter.ViewHolder;
import java.util.Arrays;
import java.util.List;

/**
 * 모든 Shortcut 들의 현시를 위한 Adapter
 */
public class AssistAllShortcutsRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

    Launcher mLauncher;
    AssistQuickAccess mQuickAccess;
    LayoutInflater mLayoutInflater;

    List<ShortcutInfo> mShortcutInfoList;
    List<AppInfo> mApps;
    boolean [] mItemsShowFlag;

    public AssistAllShortcutsRecyclerViewAdapter(Launcher launcher, AssistQuickAccess quickAccess){
        mLauncher = launcher;
        mLayoutInflater = LayoutInflater.from(mLauncher);

        mQuickAccess = quickAccess;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                R.layout.folder_icon_to_add, parent, false);

        icon.setClickable(true);
        icon.setIconFromQuickAccess(true, true);

        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams) icon.getLayoutParams();

        // 높이 증가
        lp.height = mLauncher.getDeviceProfile().folderCellHeightPx + 20;
        lp.topMargin = 20;

        return new ViewHolder(icon);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int index = getIndexInList(position);
        ShortcutInfo info = mShortcutInfoList.get(index);
        BubbleTextView icon = (BubbleTextView) holder.itemView;
        icon.reset();
        icon.applyFromShortcutInfo(info);
        icon.setQuickAccessAppName(getAppName(info));

        icon.setOnClickListener(v -> {
            removeItem(position);
        });
    }

    /**
     * 주어진 shortcut 의 해당 app 이름 얻기
     * @param shortcutInfo shortcut 정보
     * @return
     */
    public String getAppName(ShortcutInfo shortcutInfo){
        for (int i = 0; i < mApps.size(); i ++){
            AppInfo appInfo = mApps.get(i);
            if(appInfo.getPackageName().equals(shortcutInfo.getPackageName()))
                return appInfo.title.toString();
        }
        return "";
    }

    /**
     * shortcut 항목 추가
     * @param info 추가할 shortcut 항목
     */
    public void addItem(ShortcutInfo info){
        // 주어진 shortcut 정보로부터 index 얻기
        int index = -1;
        for (int i = 0; i < mShortcutInfoList.size(); i ++){
            ShortcutInfo shortcutInfo = mShortcutInfoList.get(i);
            if(shortcutInfo.getDeepShortcutId().equals(info.getDeepShortcutId()) &&
                    shortcutInfo.getPackageName().equals(info.getPackageName())){
                index = i;
                break;
            }
        }

        if(index != -1){
            mItemsShowFlag[index] = true;
            notifyDataSetChanged();
        }
    }

    /**
     * 위치에 해당하는 shortcut 제거
     * @param position 제거할 shortcut 의 위치
     */
    public void removeItem(int position){
        // 추가한 항목의 개수가 최대수이면 제거하지 않고 toast 현시
        if(mQuickAccess.getDisplayShortcutsView().getDisplayingItemCount() >= SHORTCUTS_PER_ROW){
            Toast toast = Toast.makeText(mLauncher,
                    mLauncher.getResources().getString(R.string.shortcut_count_max_warn),
                    Toast.LENGTH_SHORT);

            toast.show();

            return;
        }

        // 위치와 일치하는 항목의 flag 를 false 로 설정
        int index = getIndexInList(position);
        mItemsShowFlag[index] = false;

        notifyDataSetChanged();

        // shortcut 를 추가하여 shortcut 표시
        ShortcutInfo info = mShortcutInfoList.get(index);
        mQuickAccess.addItemToDisplayShortcutView(info);
    }

    @Override
    public int getItemCount() {
        //표시항목의 크기를 반환

        int size = 0;
        for (boolean b : mItemsShowFlag) {
            if (b)
                size++;
        }

        return size;
    }

    /**
     * ShortcutInfoList 에서 주어진 position 에 관한 index 얻기
     * @param position 위치
     * @return index
     */
    public int getIndexInList(int position){
        int index = 0;
        for (int i = 0; i < mShortcutInfoList.size(); i ++){
            if(mItemsShowFlag[i]){
                if(index == position){
                    return i;
                }
                index ++;
            }
        }
        return -1;
    }

    /**
     * Shortcut 목록 설정
     * @param list 설정할 shortcut 목록
     */
    public void setShortcutInfoList(List<ShortcutInfo> list){
        mShortcutInfoList = list;

        // 항목들의 표시상태 flag 초기화
        mItemsShowFlag = new boolean[list.size()];
        Arrays.fill(mItemsShowFlag, true);
    }

    /**
     * App 목록 설정
     * @param list 설정할 app 목록
     */
    public void setApps(List<AppInfo> list){
        mApps = list;
    }

    //This shortcuts should not be shown
    public void setDisplayingInfoList(List<ShortcutInfo> list){
        if(list == null || list.size() == 0)
            return;

        for (int i = 0; i < list.size(); i ++){
            ShortcutInfo info = list.get(i);

            for (int j = 0; j < mShortcutInfoList.size(); j ++){
                ShortcutInfo shortcutInfo = mShortcutInfoList.get(j);

                //Find from the list, and set showing flag false
                if(info.getDeepShortcutId().equals(shortcutInfo.getDeepShortcutId()) &&
                        info.getPackageName().equals(shortcutInfo.getPackageName())){
                    //Found one, so do not show it
                    mItemsShowFlag[i] = false;
                    break;
                }
            }
        }
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
