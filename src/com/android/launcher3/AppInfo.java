/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageManagerHelper;

/**
 * App Drawer에 표시되는 App들에서 리용되는 ItemInfo 클라스
 */
public class AppInfo extends ItemInfoWithIcon {

    //App을 기동시킬때 리용되는 Intent
    public Intent intent;

    public ComponentName componentName;

    public CharSequence originalTitle;

    public AppInfo() {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
    }

    @Override
    public Intent getIntent() {
        return intent;
    }

    /**
     * Must not hold the Context.
     */
    public AppInfo(Context context, LauncherActivityInfo info, UserHandle user) {
        this(info, user, UserManagerCompat.getInstance(context).isQuietModeEnabled(user));
    }

    public AppInfo(LauncherActivityInfo info, UserHandle user, boolean quietModeEnabled) {
        this.componentName = info.getComponentName();
        this.container = ItemInfo.NO_ID;
        this.user = user;
        intent = makeLaunchIntent(info);

        if (quietModeEnabled) {
            runtimeStatusFlags |= FLAG_DISABLED_QUIET_USER;
        }
        updateRuntimeFlagsForActivityTarget(this, info);
    }

    public AppInfo(AppInfo info) {
        super(info);
        componentName = info.componentName;
        originalTitle = Utilities.trim(info.originalTitle);
        title = Utilities.trim(info.title);
        intent = new Intent(info.intent);
    }

    /**
     * App의 package명을 돌려준다.
     */
    public String getPackageName(){
        return componentName.getPackageName();
    }

    @Override
    protected String dumpProperties() {
        return super.dumpProperties() + " componentName=" + componentName;
    }

    /**
     * ShortcutInfo 객체를 만들어서 돌려준다.
     * @return ShortcutInfo
     */
    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }

    public ComponentKey toComponentKey() {
        return new ComponentKey(componentName, user);
    }

    public static Intent makeLaunchIntent(LauncherActivityInfo info) {
        return makeLaunchIntent(info.getComponentName());
    }

    /**
     * 실행할수 있는 Intent 생성
     *
     * @param cn ComponentName
     * @return 생성된 intent를 돌려준다.
     */
    public static Intent makeLaunchIntent(ComponentName cn) {
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(cn)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }

    /**
     * runtimeStatusFlags갱신
     * @param info ItemInfoWithIcon
     * @param lai LauncherActivityInfo
     */
    public static void updateRuntimeFlagsForActivityTarget(
            ItemInfoWithIcon info, LauncherActivityInfo lai) {
        ApplicationInfo appInfo = lai.getApplicationInfo();
        if (PackageManagerHelper.isAppSuspended(appInfo)) {
            info.runtimeStatusFlags |= FLAG_DISABLED_SUSPENDED;
        }
        info.runtimeStatusFlags |= (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                ? FLAG_SYSTEM_NO : FLAG_SYSTEM_YES;
    }
}
