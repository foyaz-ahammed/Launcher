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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.FolderPagedView;
import com.android.launcher3.graphics.DragPreviewProvider;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GroupIconManager {
    //Folder getting in animation
    private static final int FOLDER_GET_IN_ANIMATION_DELAY = 100;

    //Launcher
    private Launcher mLauncher;
    private DragController mDragController;

    //Dragging main view
    private BubbleTextView mMainView;
    private ItemInfo mMainViewItemInfo;
    private int mMainViewPageNo;
    private boolean mMainViewIsInFolder = false;
    private Folder mMainViewFolder;
    private int mRunningAnimatorCount = 0;

    //List of selected app icons
    private List<GroupIconCell> mGroupAppIconCells = new ArrayList<GroupIconCell>();

    //Top translate positions (0,0)
    float mScaledCellWidth;
    float mScaledCellHeight;
    float mScale;
    float mFolderScale;

    public GroupIconManager(Launcher launcher){
        mLauncher = launcher;
    }

    public void addItemFromView(BubbleTextView view){
        mGroupAppIconCells.add(new GroupIconCell(view));
    }

    public void setMainView(BubbleTextView mainView){
        mMainView = mainView;

        if(mainView != null) {
            mMainViewItemInfo = (ItemInfo) mainView.getTag();

            if (mMainView.isInFolder()) {
                //Get the folder information
                mMainViewIsInFolder = true;

                //Getting parents until folder ( mainView - shortcutsAndWidgets - cellLayout - pagedView - linearLayout - folder)
                mMainViewFolder = (Folder) (mMainView.getParent().getParent().getParent()
                        .getParent().getParent());
                mMainViewPageNo = ((ViewGroup) mMainView.getParent().getParent().getParent())
                        .indexOfChild((View) mMainView.getParent().getParent());
            } else {
                mMainViewIsInFolder = false;
                mMainViewFolder = null;
            }
        }
    }

    public void getAllCheckedViews(){
        clear();

        //Check unnecessary cases, and return
        if(!mLauncher.drawCheck())
            return;
        if(mMainView == null)
            return;
        if(!mMainView.isChecked())
            return;

        //Get all checked views
        Workspace workspace = mLauncher.getWorkspace();
        for (int i = 0; i < workspace.getChildCount(); i++) {
            CellLayout cellLayout = (CellLayout) workspace.getChildAt(i);
            ShortcutAndWidgetContainer shortcutAndWidgetContainer = cellLayout
                    .getShortcutsAndWidgets();
            for (int j = 0; j < shortcutAndWidgetContainer.getChildCount(); j++) {
                if (shortcutAndWidgetContainer.getChildAt(j) instanceof BubbleTextView &&
                        shortcutAndWidgetContainer.getChildAt(j) != mMainView) {
                    BubbleTextView childView = (BubbleTextView) shortcutAndWidgetContainer
                            .getChildAt(j);
                    if (childView.isChecked()) {
                        addItemFromView(childView);
                    }
                }

                else if(shortcutAndWidgetContainer.getChildAt(j) instanceof FolderIcon){
                    FolderIcon folderIcon = (FolderIcon)shortcutAndWidgetContainer.getChildAt(j);
                    FolderPagedView folderPagedView = folderIcon.getFolder().getFolderPagedView();
                    int folderScreenId = (int) ((ItemInfo)(folderIcon.getTag())).screenId;

                    //Loop and find checked icons
                    for(int k = 0; k < folderPagedView.getPageCount(); k ++){
                        CellLayout folderCellLayout = folderPagedView.getPageAt(k);
                        ShortcutAndWidgetContainer folderContainer = folderCellLayout.getShortcutsAndWidgets();

                        for (int l = 0; l < folderContainer.getChildCount(); l ++){
                            BubbleTextView folderChildView = (BubbleTextView) folderContainer.getChildAt(l);
                            if (folderChildView.isChecked() && folderChildView != mMainView) {
                                mGroupAppIconCells.add(new GroupIconCell(folderIcon, folderChildView,
                                         folderScreenId, k));
                            }
                        }
                    }
                }
            }
        }
    }

    public void hideAndMakeInitialDragViews(int mainDragX, int mainDragY, float scale, float folderScale){
        if(mMainViewIsInFolder){
            mScaledCellWidth = mLauncher.getDeviceProfile().folderCellWidthPx * folderScale;
            mScaledCellHeight = mLauncher.getDeviceProfile().folderCellHeightPx * folderScale;
        }
        else{
            mScaledCellWidth = mLauncher.getDeviceProfile().getCellSize().x * scale;
            mScaledCellHeight = mLauncher.getDeviceProfile().getCellSize().y * scale;
        }
        mScale = scale;
        mFolderScale = folderScale;

        //Set running animation status
        mRunningAnimatorCount = 0;

        for (int i = 0; i < mGroupAppIconCells.size(); i ++){
            GroupIconCell iconCell = mGroupAppIconCells.get(i);

            //Make drag view, and add it to drag layer
            iconCell.makeGroupIconDragView(mLauncher, mMainViewIsInFolder? folderScale : scale);

            if(iconCell.mGroupIconDragView.getParent() == null)
                mLauncher.getDragLayer().addView(iconCell.mGroupIconDragView);

            iconCell.mGroupIconDragView.show();

            //Remove the cell view from parent(folder or workspace)
            if(!iconCell.isFolder()) {
                ViewGroup container = (ViewGroup) iconCell.mCellView.getParent();
                container.removeView(iconCell.mCellView);
            }
            else {
                if(iconCell.getFolder() == mMainViewFolder) {
                    mMainViewFolder.getFolderPagedView().removeItem(iconCell.mCellView);
                }
                else{
                    ShortcutInfo shortcutInfo = (ShortcutInfo) iconCell.mCellView.getTag();
                    iconCell.mFolder.getInfo().removeInside(shortcutInfo, true);
                }
            }

            AnimatorListener animatorListener = new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mRunningAnimatorCount --;
                    if(mRunningAnimatorCount == 0){
                        mDragController.mGroupIconMergeAnimation = false;
                        if(iconCell.isFolder()){
                            mMainViewFolder.setFirstStepAnimationEnded(true);
                        }
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            };

            if(!mMainViewIsInFolder) {
                //Play animation
                mRunningAnimatorCount ++;
                playAnimationGetIn(iconCell.mGroupIconDragView, mainDragX, mainDragY)
                        .addListener(animatorListener);
            }
            else{
                if(iconCell.isFolder()){
                    //In this case, animation is running, increase running animator count
                    mRunningAnimatorCount ++;
                    playAnimationGetIn(iconCell.mGroupIconDragView, mainDragX, mainDragY)
                            .addListener(animatorListener);
                    mMainViewFolder.setFirstStepAnimationEnded(false);
                }
                else{
                    //In this case, do not play animation
                    moveDragViewToTargetPosition(iconCell.mGroupIconDragView, mainDragX, mainDragY);
                }
            }
        }
    }

    public void addAllToExistingFolder(CellLayout targetLayout, int[] targetCells){
        int startDelay = 0;
        for (int i = 0; i < mGroupAppIconCells.size(); i ++){
            GroupIconCell iconCell = mGroupAppIconCells.get(i);
            ItemInfo itemInfo = (ItemInfo) iconCell.mCellView.getTag();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLauncher.getWorkspace().addToExistingFolder(iconCell.mGroupIconDragView, itemInfo, targetLayout, targetCells);
                }
            }, startDelay);
            startDelay += FOLDER_GET_IN_ANIMATION_DELAY;
        }
    }

    public void moveAllToOriginalPositions(){
        // This means there is no enough space to fill group icons

        //First send to workspace
        for (int i = 0; i < mGroupAppIconCells.size(); i ++){
            GroupIconCell iconCell = mGroupAppIconCells.get(i);

            if(!iconCell.isFolder()){
                ItemInfo item = (ItemInfo) iconCell.mCellView.getTag();

                //Add to screen
                CellLayout cellLayout = mLauncher.getWorkspace().getScreenWithId(item.screenId);
                mLauncher.getWorkspace()
                        .addInScreen(iconCell.mCellView, Favorites.CONTAINER_DESKTOP,
                                item.screenId, item.cellX, item.cellY, 1, 1);
                cellLayout.onDropChild(iconCell.mCellView);
                iconCell.mCellView.setChecked(false);

                //Hide the dragview
                iconCell.mGroupIconDragView.setVisibility(View.INVISIBLE);
            }
        }

        //-----------Then send to folders
        //Get all Folders
        List<Folder> folders = new ArrayList<>();
        for (int i = 0; i < mGroupAppIconCells.size(); i ++) {
            GroupIconCell iconCell = mGroupAppIconCells.get(i);
            if(iconCell.isFolder() && iconCell.getFolder() != mMainViewFolder){
                folders.add(iconCell.getFolder());
            }
        }

        //Send to folders
        for (int i = 0; i < folders.size(); i ++) {
            Folder folder = folders.get(i);
            if(folder != null)
                folder.onDropWithFailing();
        }
    }

    public void createOrMoveShortcutsToTargetPositions(List<Point> resultPositionList, long screenId){
        if(resultPositionList.size() == 0) {
            return;
        }

        CellLayout cellLayout = mLauncher.getWorkspace().getScreenWithId(screenId);
        for (int i = 0; i < resultPositionList.size(); i ++){
            Point point = resultPositionList.get(i);
            GroupIconCell iconCell = mGroupAppIconCells.get(i);

            //This case, drop external
            if(iconCell.isFolder()) {
                //First remove from folder
                Folder folder = iconCell.getFolder();
                if(folder.getItemCount() <= 1 && !folder.isDestroyed()){
                    folder.replaceFolderWithFinalItem();
                }

                ShortcutInfo info = (ShortcutInfo) iconCell.mCellView.getTag();
                info.container = Favorites.CONTAINER_DESKTOP;

                View view = mLauncher.createShortcut(cellLayout, info);

                mLauncher.getModelWriter().addOrMoveItemInDatabase(info, Favorites.CONTAINER_DESKTOP, screenId,
                        point.x, point.y);

                mLauncher.getWorkspace().addInScreen(view, Favorites.CONTAINER_DESKTOP, screenId, point.x, point.y,
                        info.spanX, info.spanY);
                cellLayout.onDropChild(view);

                cellLayout.getShortcutsAndWidgets().measureChild(view);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams)view.getLayoutParams();
                int childLeft = lp.x;
                int childTop = lp.y;
                view.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);
                iconCell.mGroupIconDragView.setScale(mScale);

                //Play animation
                playAnimationGetOut(iconCell.mGroupIconDragView, view, point.x, point.y, false, null);
            }

            //This case, drop internal
            else {
                ItemInfo item = (ItemInfo) iconCell.mCellView.getTag();
                mLauncher.getModelWriter().modifyItemInDatabase(item, Favorites.CONTAINER_DESKTOP, screenId, point.x, point.y, item.spanX, item.spanY);

                //Add to screen
                mLauncher.getWorkspace().addInScreen(iconCell.mCellView, Favorites.CONTAINER_DESKTOP, screenId, point.x, point.y, 1, 1);
                cellLayout.onDropChild(iconCell.mCellView);
                cellLayout.setUseTempCoords(false);
                cellLayout.getShortcutsAndWidgets().measureChild(iconCell.mCellView);

                CellLayout.LayoutParams lp = (CellLayout.LayoutParams)iconCell.mCellView.getLayoutParams();
                int childLeft = lp.x;
                int childTop = lp.y;
                iconCell.mCellView.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);
                iconCell.mGroupIconDragView.setScale(mScale);

                //Play animation
                playAnimationGetOut(iconCell.mGroupIconDragView, iconCell.mCellView, point.x, point.y, false, null);
            }
        }
    }

    public void getDraggingIconsFromSourceFolder(Folder folder, List<BubbleTextView> viewList){
        for (int i = 0; i < mGroupAppIconCells.size(); i ++){
            GroupIconCell iconCell = mGroupAppIconCells.get(i);
            if(iconCell.isFolder() && iconCell.getFolder() == folder){
                viewList.add(iconCell.getCellView());
            }
        }
    }

    public void removeAllDragViews(){
        if(mGroupAppIconCells.isEmpty())
            return;
        for (int i = 0; i < mGroupAppIconCells.size(); i ++){
            GroupIconCell iconCell = mGroupAppIconCells.get(i);

            if(iconCell.mGroupIconDragView.getParent() != null){
                mLauncher.getDragLayer().removeView(iconCell.mGroupIconDragView);
            }
        }
    }

    public void dragMoveWithoutAnimation(LinkedList<Point> points){
        for (int i = 0; i < mGroupAppIconCells.size(); i ++){
            int index = Math.min(i, points.size() - 1);
            Point point = points.get(index);

            GroupIconCell iconCell = mGroupAppIconCells.get(i);
            iconCell.mGroupIconDragView.clearAnimation();

            iconCell.mGroupIconDragView.setTranslationX(point.x);
            iconCell.mGroupIconDragView.setTranslationY(point.y);
        }
    }

    public void dragMoveWithAnimation(float newTranslationX, float newTranslationY){
        for (int i = 0; i < mGroupAppIconCells.size(); i ++){
            GroupIconCell iconCell = mGroupAppIconCells.get(i);

            //If old animator exists, stop them
            if(iconCell.mGroupIconDragView.getTag(R.string.group_drag_translate_animator_x) != null) {
                Animator oldAnimator1 = (Animator) iconCell.mGroupIconDragView
                        .getTag(R.string.group_drag_translate_animator_x);
                oldAnimator1.cancel();

                Animator oldAnimator2 = (Animator) iconCell.mGroupIconDragView
                        .getTag(R.string.group_drag_translate_animator_y);
                oldAnimator2.cancel();
            }

            //Translate X animation
            Animator animator1 = ObjectAnimator.ofFloat(iconCell.mGroupIconDragView, "translationX", newTranslationX);
            iconCell.mGroupIconDragView.setTag(R.string.group_drag_translate_animator_x, animator1);
            animator1.setDuration(100);
            animator1.start();

            //Translate Y animation
            Animator animator2 = ObjectAnimator.ofFloat(iconCell.mGroupIconDragView, "translationY", newTranslationY);
            iconCell.mGroupIconDragView.setTag(R.string.group_drag_translate_animator_y, animator2);
            animator2.setDuration(100);
            animator2.start();
        }
    }

    public void moveDragViewToTargetPosition(View view, float destTranslationX, float destTranslationY){
        view.setTranslationX(destTranslationX);
        view.setTranslationY(destTranslationY);
    }

    public Animator playAnimationGetIn(View view, float destTranslationX, float destTranslationY){
        //Get translate position source
        float translationX_src = view.getTranslationX();
        float translationY_src = view.getTranslationY();

        //Define animation, and calculate translation position
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float)animation.getAnimatedValue();
                float translationX = translationX_src + (destTranslationX - translationX_src) * value;
                float translationY = translationY_src + (destTranslationY - translationY_src) * value;
                view.setTranslationX(translationX);
                view.setTranslationY(translationY);
            }
        });
        animator.setDuration(200);
        animator.start();

        mDragController.addToRunningAnimatorList(animator);
        return animator;
    }

    public void playAnimationGetOut(View dragView, View targetView, int targetCellX, int targetCellY, boolean toNextPage, Folder mainFolder){
        //Get translate position source and target
        float translationX_src = dragView.getTranslationX();
        float translationY_src = dragView.getTranslationY();
        final float translationX_dest;
        final float translationY_dest;

        if(toNextPage){
            translationX_dest = Math.min(translationX_src + targetView.getWidth() * (3 - targetCellX),
                    mainFolder.getLeft() + mainFolder.getWidth() * mainFolder.getScaleX());
            translationY_dest = translationY_src;
        }
        else{
            int[] tempPos = new int[2];
            calculateBitmapDragPosition(tempPos, targetView);

            if(targetView instanceof BubbleTextView && !((BubbleTextView)targetView).isInFolder()){
                //remove padding
                translationX_dest = tempPos[0];
                translationY_dest = tempPos[1];
            }
            else{
                translationX_dest = tempPos[0];
                translationY_dest = tempPos[1];
            }
        }

        //Define animation, and calculate translation position
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float)animation.getAnimatedValue();
                float translationX = translationX_src + (translationX_dest - translationX_src) * value;
                float translationY = translationY_src + (translationY_dest - translationY_src) * value;
                dragView.setTranslationX(translationX);
                dragView.setTranslationY(translationY);
            }
        });

        animator.addListener(new AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animation) {
                targetView.setVisibility(View.INVISIBLE);
                if(targetView instanceof BubbleTextView)
                    ((BubbleTextView)targetView).setChecked(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                targetView.setVisibility(View.VISIBLE);
                dragView.setVisibility(View.INVISIBLE);
                if(dragView.getParent() != null)
                    ((ViewGroup)dragView.getParent()).removeView(dragView);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        animator.setDuration(200);
        animator.start();
    }

    public static Bitmap calculateBitmapDragPosition(int[] pos, View view){
        DragPreviewProvider provider = new DragPreviewProvider(view);
        Bitmap bitmap = provider.createDragBitmap();
        float scale = provider.getScaleAndPosition(bitmap, pos);
        Rect dragRect = new Rect();
        ((BubbleTextView) view).getIconBounds(dragRect);
        pos[1] = Math.round(pos[1] + dragRect.top*scale);
        return bitmap;
    }

    public float getScale(){
        return mScale;
    }

    public float getFolderScale(){
        return mFolderScale;
    }

    public boolean isMainViewInFolder(){
        return mMainViewIsInFolder;
    }

    public BubbleTextView getMainView(){
        return mMainView;
    }

    public GroupIconCell getGroupIconCell(int index){
        return mGroupAppIconCells.get(index);
    }

    public int getGroupIconCount(){
        return mGroupAppIconCells.size();
    }

    public void clear(){
        removeAllDragViews();
        mGroupAppIconCells.clear();
    }

    public void setDragController(DragController dragController){
        mDragController = dragController;
    }
}
