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

package com.android.launcher3.groupselect;

import static com.android.launcher3.groupselect.GroupIconManager.calculateBitmapDragPosition;

import android.graphics.Bitmap;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;

public class GroupIconCell {
    //Source view
    BubbleTextView mCellView;

    //Drag view
    GroupIconView mGroupIconDragView;

    int mCellX;
    int mCellY;
    long mScreenId;

    //Case folder
    boolean mIsFolder = false;
    FolderIcon mFolderIcon;
    Folder mFolder;
    int mPageNumber;
    int mFolderCellX;
    int mFolderCellY;

    public GroupIconCell(BubbleTextView view){
        mCellView = view;

        ItemInfo info = (ItemInfo)view.getTag();
        mCellX = info.cellX;
        mCellY = info.cellY;
        mScreenId = info.screenId;
    }

    //Create icon cell for app icon inside folder
    public GroupIconCell(FolderIcon folderIcon, BubbleTextView view, int folderScreenId, int pageNumber){
        mIsFolder = true;
        mFolderIcon = folderIcon;
        mFolder = folderIcon.getFolder();
        mCellView = view;
        mPageNumber = folderScreenId;
        mScreenId = pageNumber;

        ItemInfo folderInfo = (ItemInfo)folderIcon.getTag();
        mFolderCellX = folderInfo.cellX;
        mFolderCellY = folderInfo.cellY;

        ItemInfo cellInfo = (ItemInfo)view.getTag();
        mCellX = cellInfo.cellX;
        mCellY = cellInfo.cellY;
    }

    public void makeGroupIconDragView(Launcher launcher, float scale){
        int []tempXY = new int[2];
        Bitmap bitmap = calculateBitmapDragPosition(tempXY, mCellView);

        mGroupIconDragView = new GroupIconView(launcher, bitmap, tempXY, scale);
    }

    public BubbleTextView getCellView(){
        return mCellView;
    }

    public GroupIconView getGroupIconDragView(){
        return mGroupIconDragView;
    }

    public Folder getFolder() {
        return mFolder;
    }

    public boolean isFolder(){
        return mIsFolder;
    }
}
