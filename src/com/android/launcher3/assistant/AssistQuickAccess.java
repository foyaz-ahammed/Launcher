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

package com.android.launcher3.assistant;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import java.util.ArrayList;
import java.util.List;

public class AssistQuickAccess extends LinearLayout implements
        View.OnKeyListener,
        View.OnClickListener{

    public static final int SHORTCUTS_PER_ROW = 5;
    public static final int SHORTCUTS_MIN_COUNT = 1;

    Launcher mLauncher;
    AssistViewsContainer mRootView;
    ImageView mBackButton;

    private RecyclerView mAllShortcutsRecyclerView;
    private AssistAllShortcutsRecyclerViewAdapter mAllShortcutsAdapter;

    //Have shortcuts that will be showing
    private AssistDisplayShortcutsView mDisplayShortcuts;

    private View mTopHeader;

    //Parent view of all shortcuts recyclerview
    LinearLayout mShortcutViewsContainer;

    DeepShortcutManager mDeepShortcutManager;
    List<ShortcutInfo> mShortcutList = new ArrayList<>();
    List<AppInfo> mAppList = new ArrayList<>();

    //Have heights of status bar and navigation bar
    Rect mInsets;

    //Common padding of each views
    int mCommonPadding = 0;

    public AssistQuickAccess(Context context) {
        this(context, null);
    }

    public AssistQuickAccess(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistQuickAccess(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = Launcher.getLauncher(context);
        mDeepShortcutManager = DeepShortcutManager.getInstance(mLauncher);
    }

    public void setInsets(Rect insets){
        mInsets = insets;

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mShortcutViewsContainer.getLayoutParams();
        lp.bottomMargin = mInsets.bottom;
    }

    public void setRootView(AssistViewsContainer rootView){
        mRootView = rootView;
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mBackButton = findViewById(R.id.back_button);
        mBackButton.setOnClickListener(this);

        mAllShortcutsRecyclerView = findViewById(R.id.all_shortcuts);
        mAllShortcutsAdapter = new AssistAllShortcutsRecyclerViewAdapter(mLauncher, this);

        GridLayoutManager layoutManager = new GridLayoutManager(mLauncher, SHORTCUTS_PER_ROW, GridLayoutManager.VERTICAL, false);
        mAllShortcutsRecyclerView.setLayoutManager(layoutManager);
        mAllShortcutsRecyclerView.setAdapter(mAllShortcutsAdapter);

        mDisplayShortcuts = findViewById(R.id.display_shortcuts);
        mDisplayShortcuts.setQuickAccessView(this);

        //Set size of display shortcuts view
        int oneShortcutHeight = mLauncher.getDeviceProfile().folderCellHeightPx + 40;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)mDisplayShortcuts.getLayoutParams();
        lp.height = oneShortcutHeight;

        mShortcutViewsContainer = findViewById(R.id.shortcut_views_container);
        mTopHeader = findViewById(R.id.topHeader);

        //Add key listener
        setFocusableInTouchMode(true);
        requestFocus();
        setOnKeyListener(this);
    }

    public void setAdapterData(List<ShortcutInfo> shortcutsList, List<AppInfo> appList){
        mShortcutList = shortcutsList;
        mAppList = appList;
        updateData();
    }

    public void updateData(){

        //Load shortcuts from database
        List<ShortcutInfo> displayingShortcuts = loadDisplayingShortcuts();

        //Then send data to display shortcuts view, and all shortcuts view
        if(mShortcutList != null) {
            mDisplayShortcuts.setAppInfoList(mAppList);
            mDisplayShortcuts.setShortcutInfoList(displayingShortcuts);

            mAllShortcutsAdapter.setShortcutInfoList(mShortcutList);
            mAllShortcutsAdapter.setApps(mAppList);
            mAllShortcutsAdapter.setDisplayingInfoList(displayingShortcuts);
            mAllShortcutsAdapter.notifyDataSetChanged();
        }
    }

    public List<ShortcutInfo> loadDisplayingShortcuts(){
        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();
        //Load from database
        Cursor cursor = mLauncher.getModelWriter().getQuickAccessShortcuts();
        if(cursor != null){
            while (cursor.moveToNext()){
                String shortcutId = cursor.getString(0);
                String packageName = cursor.getString(1);

                //Then get shortcut with id and package name
                ShortcutInfo info = findShortcut(shortcutId, packageName);
                if(info != null)
                    shortcutInfoList.add(info);
            }
        }

        return shortcutInfoList;
    }

    ShortcutInfo findShortcut(String shortcutId, String packageName){
        for (int i = 0; i < mShortcutList.size(); i ++){
            ShortcutInfo info = mShortcutList.get(i);

            if(info.getPackageName().equals(packageName) && info.getDeepShortcutId().equals(shortcutId))
                return info;
        }

        return null;
    }

    public void addItemToDisplayShortcutView(ShortcutInfo info){
        mDisplayShortcuts.addItem(info);
    }

    public void addItemToAllShortcutView(ShortcutInfo info){
        mAllShortcutsAdapter.addItem(info);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            //Save the result to database
            List<ShortcutInfo> shortcutInfoList = mDisplayShortcuts.getShortcutInfoList();
            mLauncher.getModelWriter().updateQuickAccessShortcuts(shortcutInfoList);

            mRootView.closeExpandedShortcutsView();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if(v == mBackButton){
            //Save the result to database
            List<ShortcutInfo> shortcutInfoList = mDisplayShortcuts.getShortcutInfoList();
            mLauncher.getModelWriter().updateQuickAccessShortcuts(shortcutInfoList);

            mRootView.closeExpandedShortcutsView();
        }
    }

    public void setCommonPadding(int padding){
        mCommonPadding = padding;

        //Set padding to children
        mTopHeader.setPadding(mCommonPadding, mTopHeader.getPaddingTop(),
                mTopHeader.getPaddingRight(), mTopHeader.getPaddingBottom());
    }

    public AssistDisplayShortcutsView getDisplayShortcutsView(){
        return mDisplayShortcuts;
    }
}
