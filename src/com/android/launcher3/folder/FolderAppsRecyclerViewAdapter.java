/*
 *     Copyright (C) 2020 Lawnchair Team.
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.android.launcher3.folder;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.folder.FolderAppsRecyclerViewAdapter.ViewHolder;
import java.util.ArrayList;
import java.util.List;

/**
 * `등록부에 항목추가`대화창의 App목록 RecyclerView에 설정하는 Adapter
 */
public class FolderAppsRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {
    static final int APPS_PER_ROW = 4;

    Launcher mLauncher;
    ExtendedFolder mExtendedFolder;
    Folder mFolder;

    private final LayoutInflater mLayoutInflater;
    private final GridLayoutManager mGridLayoutMgr;

    List<ShortcutInfo> mShortcutInfoList;   //등록부에 속해있는 Shortcut들의 ShortcutInfo목록
    List<AppInfo> mAppInfoList;             //전체 App들의 AppInfo목록

    int mCheckedIconCount;      //선택된 항목개수
    boolean[] mCheckStatus;     //선택상태

    public FolderAppsRecyclerViewAdapter(Launcher launcher, ExtendedFolder extendedFolder, Folder folder, AlphabeticalAppsList appsList){
        mLauncher = launcher;
        mExtendedFolder = extendedFolder;
        mFolder = folder;

        mLayoutInflater = LayoutInflater.from(launcher);

        GridSpanSizer gridSizer = new GridSpanSizer();
        mGridLayoutMgr = new AppsGridLayoutManager(launcher);
        mGridLayoutMgr.setSpanSizeLookup(gridSizer);

        mCheckedIconCount = folder.getIconCount();

        //ShortcutInfo 목록을 얻는다.
        mShortcutInfoList = folder.getShortCutInformationList();

        //ShortcutInfo목록으로부터 AppInfo목록을 얻어서 먼저 추가한다. (먼저 현시되여야 하기때문에 앞에 추가해준다.)
        mAppInfoList = new ArrayList<>();
        int i;
        List<AppInfo> tempList = new ArrayList<>(appsList.getApps());
        for (i = 0; i < mShortcutInfoList.size(); i ++){
            ShortcutInfo shortcutInfo = mShortcutInfoList.get(i);
            for (int j = 0; j < tempList.size(); j ++){
                AppInfo appInfo = tempList.get(j);

                if(sameInformation(shortcutInfo, appInfo)){
                    mAppInfoList.add(appInfo);
                    tempList.remove(j);
                    break;
                }
            }
        }

        //그다음 남은 AppInfo목록을 추가한다.
        mAppInfoList.addAll(tempList);

        //선택상태를 설정한다.
        mCheckStatus = new boolean[appsList.getApps().size()];
        for (i = 0; i < mShortcutInfoList.size(); i ++) {   //등록부에 있던 Shortcut들은 선택된것으로
            mCheckStatus[i] = true;
        }

        for (i = mShortcutInfoList.size(); i < mAppInfoList.size(); i ++) { //나머지는 선택안된것으로 설정한다.
            mCheckStatus[i] = false;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                R.layout.folder_icon_to_add, parent, false);
        icon.getLayoutParams().height = mLauncher.getDeviceProfile().folderCellHeightPx;
        return new ViewHolder(icon);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo info = mAppInfoList.get(position);
        BubbleTextView icon = (BubbleTextView) holder.itemView;
        icon.reset();
        icon.setIconFromFolderDialog(true);
        icon.applyFromApplicationInfo(info);
        icon.setChecked(containsInfo(info));

        icon.setOnClickListener(v -> {
            if(v instanceof BubbleTextView){
                BubbleTextView view = (BubbleTextView)v;
                view.toggleChecked();

                if(view.isChecked()) mCheckedIconCount ++;
                else mCheckedIconCount --;

                mExtendedFolder.updateFolderHeaderText(mCheckedIconCount);
                mCheckStatus[position] = view.isChecked();
            }
        });
    }

    public List<AppInfo> getAllCheckedIcons(){
        List<AppInfo> result = new ArrayList<>();
        for (int i = 0; i < mAppInfoList.size(); i ++){
            if(mCheckStatus[i]){
                result.add(mAppInfoList.get(i));
            }
        }
        return  result;
    }

    public boolean containsInfo(AppInfo info){
        for (int i = 0; i < mShortcutInfoList.size(); i ++){
            ShortcutInfo shortcutInfo = mShortcutInfoList.get(i);
            if(sameInformation(shortcutInfo, info)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return mAppInfoList.size();
    }

    /**
     * LayoutManager 를 돌려준다.
     */
    public GridLayoutManager getLayoutManager() {
        return mGridLayoutMgr;
    }

    public static boolean sameInformation(ShortcutInfo shortcutInfo, AppInfo appInfo){
        return shortcutInfo.getPackageName().equals(appInfo.getPackageName());
    }

    /**
     * RecyclerView에 설정할 GridLayoutManager클라스
     */
    public class AppsGridLayoutManager extends GridLayoutManager {

        public AppsGridLayoutManager(Context context) {
            super(context, APPS_PER_ROW, GridLayoutManager.VERTICAL, false);
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);

            // Ensure that we only report the number apps for accessibility not including other
            // adapter views
            final AccessibilityRecordCompat record = AccessibilityEventCompat
                    .asRecord(event);
            record.setItemCount(mAppInfoList.size());
            record.setFromIndex(Math.max(0,
                    record.getFromIndex() - getRowsNotForAccessibility(record.getFromIndex())));
            record.setToIndex(Math.max(0,
                    record.getToIndex() - getRowsNotForAccessibility(record.getToIndex())));
        }

        @Override
        public int getRowCountForAccessibility(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return super.getRowCountForAccessibility(recycler, state) -
                    getRowsNotForAccessibility(mAppInfoList.size() - 1);
        }

        @Override
        public void onInitializeAccessibilityNodeInfoForItem(RecyclerView.Recycler recycler,
                RecyclerView.State state, View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);

            ViewGroup.LayoutParams lp = host.getLayoutParams();
            AccessibilityNodeInfoCompat.CollectionItemInfoCompat cic = info.getCollectionItemInfo();
            if (!(lp instanceof LayoutParams) || (cic == null)) {
                return;
            }
            LayoutParams glp = (LayoutParams) lp;
            info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                    cic.getRowIndex() - getRowsNotForAccessibility(glp.getViewAdapterPosition()),
                    cic.getRowSpan(),
                    cic.getColumnIndex(),
                    cic.getColumnSpan(),
                    cic.isHeading(),
                    cic.isSelected()));
        }

        /**
         * Returns the number of rows before {@param adapterPosition}, including this position which
         * should not be counted towards the collection info.
         */
        private int getRowsNotForAccessibility(int adapterPosition) {
            adapterPosition = Math.max(adapterPosition, mAppInfoList.size() - 1);

            return adapterPosition + 1;
        }
    }

    /**
     * GridLayoutManager 클라스에 적용할 Span개수를 설정하기 위한 SpanSizeLookup계승클라스
     */
    public static class GridSpanSizer extends GridLayoutManager.SpanSizeLookup {

        public GridSpanSizer() {
            super();
            setSpanIndexCacheEnabled(true);
        }

        @Override
        public int getSpanSize(int position) {
            //Span 크기는 1로 돌려준다.
            return 1;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }
}
