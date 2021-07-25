/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.graphics.Bitmap;

/**
 * 아이콘을 가진 ItemInfo클라스
 * @see ItemInfo
 */
public abstract class ItemInfoWithIcon extends ItemInfo {

    /**
     * 아이콘화상
     */
    public Bitmap iconBitmap;

    /**
     * 아이콘색갈
     */
    public int iconColor;

    /**
     * 화질이 낮은 아이콘을 리용하겠는가?
     */
    public boolean usingLowResIcon;

    //아이콘이 비능동되는 여러가지 조건들
    /**
     * 기재가 SafeModel방식으로 기동한것으로 하여 아이콘이 비능동되는것
     */
    public static final int FLAG_DISABLED_SAFEMODE = 1;

    /**
     * 원본 App이 비유효(파괴되거나 설치해제됨)되였을때 아이콘이 비능동되는것
     */
    public static final int FLAG_DISABLED_NOT_AVAILABLE = 1 << 1;

    /**
     * App이 정지됨으로 해서 아이콘이 비능동되는것 (Google Play지원을 받고있는 app들에 한한것임)
     */
    public static final int FLAG_DISABLED_SUSPENDED = 1 << 2;

    /**
     * 사용자가 Quiet Mode에 있으므로 해서 아이콘이 비능동되는것
     */
    public static final int FLAG_DISABLED_QUIET_USER = 1 << 3;

    /**
     * Indicates that the icon is disabled as the publisher has disabled the actual shortcut.
     */
    public static final int FLAG_DISABLED_BY_PUBLISHER = 1 << 4;

    /**
     * 현재의 사용자에 lock가 걸긴것으로 하여 아이콘이 비능동되는것
     */
    public static final int FLAG_DISABLED_LOCKED_USER = 1 << 5;

    /**
     * Sesame결합이 되지 않은것으로 하여 아이콘이 비능동되는것
     */
    public static final int FLAG_DISABLED_BY_SESAME = 1 << 10;

    /**
     * 아이콘이 비능동되는 모든 경우를 다 포함한 FLAG_MASK
     */
    public static final int FLAG_DISABLED_MASK = FLAG_DISABLED_SAFEMODE |
            FLAG_DISABLED_NOT_AVAILABLE | FLAG_DISABLED_SUSPENDED |
            FLAG_DISABLED_QUIET_USER | FLAG_DISABLED_BY_PUBLISHER |
            FLAG_DISABLED_LOCKED_USER | FLAG_DISABLED_BY_SESAME;

    /**
     * 체계 App
     */
    public static final int FLAG_SYSTEM_YES = 1 << 6;

    /**
     * 체계 App이 아닌 다른 App
     */
    public static final int FLAG_SYSTEM_NO = 1 << 7;

    public static final int FLAG_SYSTEM_MASK = FLAG_SYSTEM_YES | FLAG_SYSTEM_NO;

    /**
     * Flag indicating that the icon is badged.
     */
    public static final int FLAG_ICON_BADGED = 1 << 9;

    /**
     * 실시간적으로 계산되는 Item의 상태
     * 새로운 정보가 창조될때마다 갱신이 된다.
     */
    public int runtimeStatusFlags = 0;

    protected ItemInfoWithIcon() { }

    protected ItemInfoWithIcon(ItemInfoWithIcon info) {
        super(info);
        iconBitmap = info.iconBitmap;
        iconColor = info.iconColor;
        usingLowResIcon = info.usingLowResIcon;
        runtimeStatusFlags = info.runtimeStatusFlags;
    }

    /**
     * 항목이 비능동인가?
     * 비능동조건중 어느 하나라도 부합되면 비능동된다.
     */
    @Override
    public boolean isDisabled() {
        return (runtimeStatusFlags & FLAG_DISABLED_MASK) != 0;
    }
}
