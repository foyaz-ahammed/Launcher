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
import android.content.ContentValues;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;

import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.util.ContentWriter;

/**
 * Launcher에서 한개 항목의 정보를 나타내는 클라스이다.
 * 항목으로서는 App, Shortcut, Folder, Widget들이 될수 있다.
 *
 * @see ItemInfoWithIcon
 * @see AppInfo
 * @see ShortcutInfo
 */
public class ItemInfo {
    public static final int NO_ID = -1;
    public long id = NO_ID; //자료기지에서 리용하는 id

    /**
     * Item 형식
     * 아래와 같은 Item Type 들중의 하나로 될수 있다.
     * {@link LauncherSettings.Favorites#ITEM_TYPE_APPLICATION}         //App
     * {@link LauncherSettings.Favorites#ITEM_TYPE_SHORTCUT}            //Shortcut
     * {@link LauncherSettings.Favorites#ITEM_TYPE_DEEP_SHORTCUT}       //Deep Shortcut
     * {@link LauncherSettings.Favorites#ITEM_TYPE_FOLDER}              //Folder
     * {@link LauncherSettings.Favorites#ITEM_TYPE_APPWIDGET}           //AppWidget
     * {@link LauncherSettings.Favorites#ITEM_TYPE_CUSTOM_APPWIDGET}    //CustomAppWidget
     */
    public int itemType;

    /**
     * Item을 포함하고 있는 Container
     * 아래와 같은 Container 들중의 하나로 될수 있다.
     *
     * {@link LauncherSettings.Favorites#CONTAINER_DESKTOP}     //Desktop
     * {@link LauncherSettings.Favorites#CONTAINER_HOTSEAT}     //Hotseat
     */
    public long container = NO_ID;

    /**
     * Item을 포함하는 화면의 식별자
     */
    public long screenId = -1;

    /**
     * 화면에서 차지하고 있는 X좌표
     */
    public int cellX = -1;

    /**
     * 화면에서 차지하고 있는 Y좌표
     */
    public int cellY = -1;

    /**
     * 수평의 cell 개수
     */
    public int spanX = 1;

    /**
     * 수직의 cell 개수
     */
    public int spanY = 1;

    /**
     * {@link #spanX}가 취할수 있는 최소값
     */
    public int minSpanX = 1;

    /**
     * {@link #spanY}가 취할수 있는 최소값
     */
    public int minSpanY = 1;

    /**
     * Item의 번호 (매 Item들은 어떠한 목록에 위치 순서대로 들어가있다.)
     */
    public int rank = 0;

    /**
     * Item 이름 (App/Shortcut/Folder 이름이 될수 있다.)
     */
    public CharSequence title;

    /**
     * Item의 ContentDescription
     */
    public CharSequence contentDescription;

    public UserHandle user;

    public ItemInfo() {
        user = Process.myUserHandle();
    }

    ItemInfo(ItemInfo info) {
        copyFrom(info);
    }

    /**
     * 객체내용 복사
     * @param info 다른 ItemInfo객체
     */
    public void copyFrom(ItemInfo info) {
        //성원변수들을 그대로 복사한다.
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        rank = info.rank;
        screenId = info.screenId;
        itemType = info.itemType;
        container = info.container;
        user = info.user;
        contentDescription = info.contentDescription;
    }

    public Intent getIntent() {
        return null;
    }

    public ComponentName getTargetComponent() {
        Intent intent = getIntent();
        if (intent != null) {
            return intent.getComponent();
        } else {
            return null;
        }
    }

    /**
     * 자료기지보관을 위한 ContentWrite객체에 필요한 정보들을 복사한다.
     * @param writer ContentWriter
     */
    public void writeToValues(ContentWriter writer) {
        writer.put(LauncherSettings.Favorites.ITEM_TYPE, itemType)
                .put(LauncherSettings.Favorites.CONTAINER, container)
                .put(LauncherSettings.Favorites.SCREEN, screenId)
                .put(LauncherSettings.Favorites.CELLX, cellX)
                .put(LauncherSettings.Favorites.CELLY, cellY)
                .put(LauncherSettings.Favorites.SPANX, spanX)
                .put(LauncherSettings.Favorites.SPANY, spanY)
                .put(LauncherSettings.Favorites.RANK, rank);
    }

    /**
     * ContentValues객체에서 필요한 정보들을 복사한다.
     * @param values ContentValues
     */
    public void readFromValues(ContentValues values) {
        itemType = values.getAsInteger(LauncherSettings.Favorites.ITEM_TYPE);
        container = values.getAsLong(LauncherSettings.Favorites.CONTAINER);
        screenId = values.getAsLong(LauncherSettings.Favorites.SCREEN);
        cellX = values.getAsInteger(LauncherSettings.Favorites.CELLX);
        cellY = values.getAsInteger(LauncherSettings.Favorites.CELLY);
        spanX = values.getAsInteger(LauncherSettings.Favorites.SPANX);
        spanY = values.getAsInteger(LauncherSettings.Favorites.SPANY);
        rank = values.getAsInteger(LauncherSettings.Favorites.RANK);
    }

    /**
     * ContentWriter객체에 Item정보들을 보관한다.
     * @param writer ContentWriter
     */
    public void onAddToDatabase(ContentWriter writer) {
        if (screenId == Workspace.EXTRA_EMPTY_SCREEN_ID) {
            // We should never persist an item on the extra empty screen.
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }

        writeToValues(writer);
        writer.put(LauncherSettings.Favorites.PROFILE_ID, user);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "(" + dumpProperties() + ")";
    }

    protected String dumpProperties() {
        return "id=" + id
                + " type=" + LauncherSettings.Favorites.itemTypeToString(itemType)
                + " container=" + LauncherSettings.Favorites.containerToString((int)container)
                + " screen=" + screenId
                + " cell(" + cellX + "," + cellY + ")"
                + " span(" + spanX + "," + spanY + ")"
                + " minSpan(" + minSpanX + "," + minSpanY + ")"
                + " rank=" + rank
                + " user=" + user
                + " title=" + title;
    }

    /**
     * @return 항목이 비능동되였는가를 돌려준다.
     */
    public boolean isDisabled() {
        return false;
    }

    /**
     * @return Hotseat령역의 항목인가를 돌려준다
     */
    public boolean isContainerHotSeat(){
        return container == Favorites.CONTAINER_HOTSEAT;
    }

    /**
     * @return Desktop령역의 항목인가를 돌려준다.
     */
    public boolean isContainerDesktop(){
        return container == Favorites.CONTAINER_DESKTOP;
    }
}
