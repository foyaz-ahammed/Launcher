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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.allapps.AllAppsRecyclerView;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import java.util.List;
import java.util.Objects;

/**
 * 등록부에 항목추가 대화창
 * 등록부({@link Folder}) 에서 추가단추를 눌렀을때 펼쳐지는 대화창
 */
public class ExtendedFolder extends Dialog implements View.OnClickListener{
    private static final float MAIN_FOLDER_VIEW_PERCENT = 0.6f;

    private final Launcher mLauncher;
    private final Folder mFolder;   //대화창이 펼쳐지기전의 등록부 View

    //App목록
    private final AlphabeticalAppsList mApps;

    //RecyclerView 와 Adapter
    private RecyclerView mAppsRecyclerView;
    private final FolderAppsRecyclerViewAdapter mAdapter;

    //확인, 취소단추
    private Button mOkButton;
    private Button mCancelButton;

    //항목개수에 해당한 TextView
    private TextView mFolderHeader;

    public ExtendedFolder(Context context, Folder folder) {
        super(context);

        mLauncher = Launcher.getLauncher(context);
        mFolder = folder;

        AllAppsRecyclerView appListView = mLauncher.getAppsListView();

        mApps = appListView.getApps();
        mAdapter = new FolderAppsRecyclerViewAdapter(mLauncher, this, mFolder, mApps);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_item_to_folder);

        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        setCanceledOnTouchOutside(true);

        //자식 View 들 얻기
        TextView folderNameView = findViewById(R.id.folder_name);
        mCancelButton = findViewById(R.id.btn_cancel);
        mOkButton = findViewById(R.id.btn_ok);
        mFolderHeader = findViewById(R.id.folder_header);
        mAppsRecyclerView = findViewById(R.id.folder_main_apps);

        //RecyclerView 설정
        mAppsRecyclerView.setLayoutManager(mAdapter.getLayoutManager());
        mAppsRecyclerView.setAdapter(mAdapter);

        //항목개수 Label갱신
        String folderName = mFolder.getFolderName();
        folderNameView.setText(folderName);

        updateFolderHeaderText(mFolder.getIconCount());
        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
    }

    @Override
    public void onAttachedToWindow(){
        super.onAttachedToWindow();

        //RecyclerView의 높이를 계산하여 설정한다.
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mAppsRecyclerView.getLayoutParams();
        lp.height = (int)(mLauncher.getResources().getDisplayMetrics().heightPixels * MAIN_FOLDER_VIEW_PERCENT);
        mAppsRecyclerView.setLayoutParams(lp);

        synchronized(mAdapter){
            // notify() is being called here when the thread and
            // synchronized block does not own the lock on the object.
            mAdapter.notify();
        }
    }

    /**
     * Header Label수정
     * 실례: 등록부1에 추가(3/20)
     *
     * @param itemCount 선택된 항목개수
     */
    public void updateFolderHeaderText(int itemCount) {
        String textHeader = mLauncher.getResources().getString(
                R.string.folder_add_dialog_title,
                mFolder.getFolderName(), itemCount,
                mApps.getAdapterItems().size());
        mFolderHeader.setText(textHeader);
    }

    /**
     * 선택변경사항을 보관한다.
     */
    private void saveAll(){
        //선택한 App 항목들을 Folder에 반영한다.
        mFolder.removeAddIcon();
        mFolder.mContent.removeAllItems();
        mFolder.mInfo.contents.clear();
        List<AppInfo> checkedIconList = mAdapter.getAllCheckedIcons();
        for (int i = 0; i < checkedIconList.size(); i ++){
            AppInfo appInfo = checkedIconList.get(i);
            ShortcutInfo si = appInfo.makeShortcut();

            //추가
            mFolder.addAppIcon(si, i);
            mFolder.mInfo.contents.add(si);
        }

        //정렬
        mFolder.mItemsInvalidated = true;
        mFolder.rearrangeChildren(-1);
        mFolder.getFolderIcon().updatePreviewItems(false);

        //`추가`단추를 삽입한다.
        mFolder.insertAddIcon();
        mFolder.mItemsInvalidated = true;
    }

    @Override
    public void onClick(View v) {
        if (v == mCancelButton) {   //취소단추를 눌렀을때
            dismiss();
        }
        else if (v == mOkButton) {  //확인단추를 눌렀을때
            saveAll();
            dismiss();
        }
    }
}
