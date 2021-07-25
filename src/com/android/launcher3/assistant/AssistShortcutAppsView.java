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
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import java.util.ArrayList;
import java.util.List;

public class AssistShortcutAppsView extends AssistItemContainer implements AllAppsStore.OnUpdateListener,
        View.OnClickListener, AssistPopupMenu.MenuSelectListener {

    private static final String DEFAULT_MESSAGE = "messag";
    private static final String DEFAULT_CONTACTS = "contacts";
    private static final String DEFAULT_CAMERA = "camera";
    private static final String DEFAULT_CALENDAR = "calendar";
    private static final String DEFAULT_CLOCK = "clock";

    private static final String DEFAULT_MESSAGE_PACKAGE = "";
    private static final String DEFAULT_CONTACTS_PACKAGE = "";
    private static final String DEFAULT_CAMERA_PACKAGE = "";
    private static final String DEFAULT_CALENDAR_PACKAGE = "com.android.krcalendar";
    private static final String DEFAULT_CLOCK_PACKAGE = "";

    private static final String DEFAULT_CALENDAR_NEW_EVENT = "new_event";
    private static final String DEFAULT_CLOCK_NEW_ALARM = "alarm_create";

    private List<ShortcutInfo> mAssistantDefaultShortcuts = new ArrayList<>();
    private List<SimpleAppInfo> mAssistantDefaultApps = new ArrayList<>();

    Launcher mLauncher;
    DeepShortcutManager mDeepShortcutManager;

    AssistViewsContainer mRootView;

    RecyclerView mAppListRecyclerView;
    AssistRecyclerViewAdapter mAppListRecyclerViewAdapter;

    RecyclerView mShortcutListRecyclerView;
    AssistRecyclerViewAdapter mShortcutListRecyclerViewAdapter;

    //All apps, and shortcuts
    private List<AppInfo> mApps = new ArrayList<>();
    private List<ShortcutInfo> mShortcuts = new ArrayList<>();

    ImageButton mMenuButton;

    public AssistShortcutAppsView(
            @NonNull Context context) {
        this(context, null);
    }

    public AssistShortcutAppsView(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistShortcutAppsView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

        mLauncher = Launcher.getLauncher(context);
        mDeepShortcutManager = DeepShortcutManager.getInstance(mLauncher);

        setDefaultApps();
    }

    public void setRootView(AssistViewsContainer view){
        mRootView = view;
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mMenuButton = findViewById(R.id.menu_button);
        mMenuButton.setOnClickListener(this);

        //Set adapter for app list recycler view
        mAppListRecyclerView = findViewById(R.id.assist_app_list);
        mAppListRecyclerViewAdapter = new AssistRecyclerViewAdapter(mLauncher, AssistRecyclerViewAdapter.ADAPTER_TYPE_APP);

        LinearLayoutManager appsViewLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mAppListRecyclerView.setLayoutManager(appsViewLayoutManager);
        mAppListRecyclerView.setAdapter(mAppListRecyclerViewAdapter);

        //Set adapter for shortcut list recycler view
        mShortcutListRecyclerView = findViewById(R.id.assist_shortcut_list);
        mShortcutListRecyclerViewAdapter = new AssistRecyclerViewAdapter(mLauncher, AssistRecyclerViewAdapter.ADAPTER_TYPE_SHORTCUT);

        LinearLayoutManager shortcutsViewLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mShortcutListRecyclerView.setLayoutManager(shortcutsViewLayoutManager);
        mShortcutListRecyclerView.setAdapter(mShortcutListRecyclerViewAdapter);
    }

    public void setDefaultApps(){
        //Set 5 Apps
        //Currently use Message, Contacts, Camera, Calendar, Clock

        mAssistantDefaultApps.clear();
        mAssistantDefaultApps.add(new SimpleAppInfo(DEFAULT_MESSAGE, DEFAULT_MESSAGE_PACKAGE));
        mAssistantDefaultApps.add(new SimpleAppInfo(DEFAULT_CONTACTS, DEFAULT_CONTACTS_PACKAGE));
        mAssistantDefaultApps.add(new SimpleAppInfo(DEFAULT_CAMERA, DEFAULT_CAMERA_PACKAGE));
        mAssistantDefaultApps.add(new SimpleAppInfo(DEFAULT_CALENDAR, DEFAULT_CALENDAR_PACKAGE));
        mAssistantDefaultApps.add(new SimpleAppInfo(DEFAULT_CLOCK, DEFAULT_CLOCK_PACKAGE));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLauncher.getAppsView().getAppsStore().addUpdateListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLauncher.getAppsView().getAppsStore().removeUpdateListener(this);
    }

    @Override
    public void onAppsUpdated() {
        mApps.clear();
        AllAppsStore appsStore = mLauncher.getAppsView().getAppsStore();
        mApps.addAll(appsStore.getApps());
        getAllShortcuts();

        mAppListRecyclerViewAdapter.setViewWidth(mShortcutListRecyclerView.getWidth());

        //Get apps to display, and apply to recyclerview
        List<AppInfo> filteredAppList = getFilteredApps();
        mAppListRecyclerViewAdapter.updateAppData(filteredAppList);
        mAppListRecyclerViewAdapter.notifyDataSetChanged();

        mShortcutListRecyclerViewAdapter.setViewWidth(mShortcutListRecyclerView.getWidth());

        //Get filtered shortcuts
        updateShortcutsData();
    }

    public void updateShortcutsData(){
        List<ShortcutInfo> filteredShortcutList = getFilteredShortcuts();
        mShortcutListRecyclerViewAdapter.updateShortcutData(filteredShortcutList);
        mShortcutListRecyclerViewAdapter.notifyDataSetChanged();
    }

    public List<ShortcutInfo> getFilteredShortcuts(){
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

        //If no shortcut in database, show default shortcuts
        if(shortcutInfoList.size() == 0 && mAssistantDefaultShortcuts.size() > 0) {
            mLauncher.getModelWriter().updateQuickAccessShortcuts(mAssistantDefaultShortcuts);
            return getFilteredShortcuts();
        }

        return shortcutInfoList;
    }

    ShortcutInfo findShortcut(String shortcutId, String packageName){
        for (int i = 0; i < mShortcuts.size(); i ++){
            ShortcutInfo info = mShortcuts.get(i);

            if(info.getPackageName().equals(packageName) && info.getDeepShortcutId().equals(shortcutId))
                return info;
        }

        return null;
    }

    List<ShortcutInfo> getShortcuts(AppInfo info){
        List<ShortcutInfo> resultShortcuts = new ArrayList<>();

        for (int i = 0; i < mShortcuts.size(); i ++){
            ShortcutInfo shortcutInfo = mShortcuts.get(i);
            if(shortcutInfo.getPackageName() .equals(info.getPackageName())) {
                resultShortcuts.add(shortcutInfo);
            }
        }

        return resultShortcuts;
    }

    public void getAllShortcuts(){

        //Get first app's user
        UserHandle user = null;
        if (mApps.size() > 0) {
            user = mApps.get(0).user;
        }

        if (user != null) {
            mShortcuts.clear();

            List<ShortcutInfoCompat> shortcutInfoCompats = mDeepShortcutManager.getAllShortcuts(
                    ShortcutQuery.FLAG_MATCH_MANIFEST | ShortcutQuery.FLAG_MATCH_DYNAMIC, user);
            if (shortcutInfoCompats != null && shortcutInfoCompats.size() > 0) {

                for (int i = 0; i < shortcutInfoCompats.size(); i++) {
                    ShortcutInfoCompat shortcutInfoCompat = shortcutInfoCompats.get(i);
                    final ShortcutInfo si = new ShortcutInfo(shortcutInfoCompat, mLauncher);

                    LauncherIcons li = LauncherIcons.obtain(mLauncher);
                    li.createShortcutIcon(shortcutInfoCompat, false /* badged */).applyTo(si);
                    li.recycle();

                    mShortcuts.add(si);
                }
            }
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private List<AppInfo> getFilteredApps(){
        List<AppInfo> systemApps = new ArrayList<>();
        List<AppInfo> filteredApps = new ArrayList<>();

        PackageManager pm = mLauncher.getPackageManager();
        for (int i = 0; i < mApps.size(); i ++){
            AppInfo appInfo = mApps.get(i);
            boolean isSystemApp = false;
            try {
                isSystemApp = (pm.getApplicationInfo(appInfo.getPackageName(), 0).flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if(isSystemApp){
                    systemApps.add(appInfo);
                }

            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < mAssistantDefaultApps.size(); i ++){
            SimpleAppInfo simpleAppInfo = mAssistantDefaultApps.get(i);
            String containingWord = simpleAppInfo.containingWord;
            String exactPackageName = simpleAppInfo.exactPackageName;

            for (int j = 0; j < mApps.size(); j ++){
                String pkgName = mApps.get(j).getPackageName();

                boolean found = false;

                if(!exactPackageName.equals(""))
                    found = pkgName.equals(exactPackageName);
                else
                    found = pkgName.contains(containingWord);

                if(found){
                    AppInfo appInfo = mApps.get(j);
                    filteredApps.add(appInfo);

                    boolean isCalendar = containingWord.equals(DEFAULT_CALENDAR);
                    boolean isClock = containingWord.equals(DEFAULT_CLOCK);
                    boolean isContacts = containingWord.equals(DEFAULT_CONTACTS);

                    //Get assistant apps(calendar and clock)
                    if(isCalendar || isClock || isContacts) {
                        ShortcutInfo shortcutInfo = null;

                        List<ShortcutInfo> appShortcuts = getShortcuts(appInfo);
                        for (int k = 0; k < appShortcuts.size(); k ++){
                            ShortcutInfo info = appShortcuts.get(k);
                            String shortcutId = info.getDeepShortcutId();

                            //For calendar
                            if(isCalendar){
                                if(shortcutId.toLowerCase().contains(DEFAULT_CALENDAR_NEW_EVENT))
                                    shortcutInfo = info;

                                break;
                            }

                            //For clock
                            else {
                                if(shortcutId.toLowerCase().contains(DEFAULT_CLOCK_NEW_ALARM))
                                    shortcutInfo = info;
                                break;
                            }
                        }

                        //If matching shortcut not found, and the app has only one shortcut, then use it instead
                        if(shortcutInfo == null && appShortcuts.size() == 1){
                            shortcutInfo = appShortcuts.get(0);
                        }

                        if(isCalendar)
                            mRootView.setCalendarData(appInfo, shortcutInfo);
                        else if (isClock)
                            mRootView.setClockData(appInfo, shortcutInfo);
                        else if (isContacts)
                            mRootView.setContactsData(appInfo, shortcutInfo);

                        //Add to default shortcut list
                        if(shortcutInfo != null)
                            mAssistantDefaultShortcuts.add(shortcutInfo);
                    }
                    break;
                }
            }
        }

        return filteredApps;
    }

    @Override
    public void onClick(View v) {
        if(v == mMenuButton){
            //Show popup menu
            AssistPopupMenu popupMenu = new AssistPopupMenu(getContext(), mMenuButton,
                    false, false, true, getPaddingLeft(), getPaddingRight());

            popupMenu.addMenuSelectListener(this);
            popupMenu.show();
        }
    }

    public List<AppInfo> getEntireApps(){
        return mApps;
    }

    public List<ShortcutInfo> getEntireShortcuts(){
        return mShortcuts;
    }

    @Override
    public void onSelectSettings(){
        //Currently show the expanded view on this function
        AssistViewsContainer mAssistViewsContainer = mLauncher.getDesktop().getAssistViewsContainer();
        mAssistViewsContainer.openExpandedShortcutsView();
    }

    public static class SimpleAppInfo{
        String containingWord;
        String exactPackageName;

        SimpleAppInfo(String word, String pkg){
            containingWord = word;
            exactPackageName = pkg;
        }
    }
}
