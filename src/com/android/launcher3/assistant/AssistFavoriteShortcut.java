package com.android.launcher3.assistant;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.android.launcher3.AppInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.touch.ItemClickHandler;

/**
 * Assist 페지에 Clock 를 표시하기 위한 layout
 */
public class AssistFavoriteShortcut extends AssistItemContainer implements OnClickListener, AssistPopupMenu.MenuSelectListener {

    //App 정보와 Shortcut 정보
    AppInfo mAppInfo = null;
    ShortcutInfo mShortcutInfo = null;
    View mStartTitle;
    View mEndTitle;
    ImageButton mMenuButton;
    ImageView mAppIcon;

    public AssistFavoriteShortcut(Context context) {
        this(context, null);
    }

    public AssistFavoriteShortcut(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistFavoriteShortcut(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        defaultIndex = 5;
        currentIndex = 5;
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mStartTitle = findViewById(R.id.dialog_start_title);
        mStartTitle.setOnClickListener(this);
        mEndTitle = findViewById(R.id.dialog_end_title);
        mEndTitle.setOnClickListener(this);
        mMenuButton = findViewById(R.id.menu_button);
        mMenuButton.setOnClickListener(this);
        mAppIcon = findViewById(R.id.app_icon);

        // 자료기지에서 이 부분의 순서와 pin 상태를 얻기
        Cursor cursor = mLauncher.getModelWriter().getAssistantClockSectionOrder();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                currentIndex = cursor.getInt(0);
                isPinned = cursor.getInt(1) != 0;
            }
            cursor.close();
        }
    }

    /**
     * App 과 Shortcut 설정
     * @param appInfo 설정할 App 정보
     * @param shortcutInfo 설정할 shortcut 정보
     */
    public void setAppAndShortcut(AppInfo appInfo, ShortcutInfo shortcutInfo){
        mAppInfo = appInfo;
        mShortcutInfo = shortcutInfo;

        Drawable appIcon = new BitmapDrawable(null, appInfo.iconBitmap);
        mAppIcon.setImageDrawable(appIcon);
    }

    @Override
    public void onClick(View v) {
        //Open app
        if(v == mStartTitle){
            if(mAppInfo == null){
                Toast.makeText(mLauncher, R.string.app_not_installed, Toast.LENGTH_SHORT).show();
            }
            else{
                ItemClickHandler.startAppShortcutOrInfoActivity(null, mAppInfo, mLauncher);
            }
        }
        else if(v == mEndTitle) {
            if(mAppInfo == null){
                Toast.makeText(mLauncher, R.string.app_not_installed, Toast.LENGTH_SHORT).show();
            }
            else if(mShortcutInfo == null){
                Toast.makeText(mLauncher, R.string.shortcut_not_exist, Toast.LENGTH_SHORT).show();
            }
            else{
                ItemClickHandler.onClickAppShortcut(null, mShortcutInfo, mLauncher);
            }
        }
        else if(v == mMenuButton){
            //Show popup menu
            AssistPopupMenu popupMenu = new AssistPopupMenu(getContext(), mMenuButton,
                    !isPinned, isPinned,false, getPaddingLeft(), getPaddingRight());
            popupMenu.addMenuSelectListener(this);
            popupMenu.show();
        }
    }
}
