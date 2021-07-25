package com.android.launcher3.assistant;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;

/**
 * 개별적인 Assist 항목들을 위한 기초 클라스
 */
public class AssistItemContainer extends LinearLayout implements AssistPopupMenu.MenuSelectListener {
    Launcher mLauncher;

    int defaultIndex; // 이 항목의 본래 index
    int currentIndex; // 이 항목의 현재 index
    boolean isPinned = false; // pin 상태

    public AssistItemContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
    }

    /**
     * 현재 위치 설정
     * @param index 설정할 위치
     */
    public void setCurrentIndex(int index) {
        currentIndex = index;
    }

    /**
     * pin 상태 얻기
     * @return pin true 이면 pin 이 된 상태이고 false 이면 아님
     */
    public boolean getPinStatus() {
        return isPinned;
    }

    /**
     * 본래위치값 얻기
     * @return 본래위치값
     */
    public int getDefaultIndex() {
        return defaultIndex;
    }

    /**
     * 항목의 현재 위치 얻기
     * @return 현재 위치
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Pin 단추를 눌렀을때의 처리
     */
    @Override
    public void onSelectPinOnTop() {
        LinearLayout mainLayout = mLauncher.getDesktop().getAssistViewsContainer().mAssistMainRecyclerViewAdapter.mMainLayout;
        AssistMainRecyclerViewAdapter assistMainRecyclerViewAdapter = mLauncher.getDesktop().getAssistViewsContainer().mAssistMainRecyclerViewAdapter;
        if (!isPinned) {
            for (int i = 3; i < currentIndex; i ++ ) {
                ((AssistItemContainer) (mainLayout.getChildAt(i))).setCurrentIndex(i + 1);
            }
            mLauncher.getDesktop().getAssistViewsContainer().mAssistMainRecyclerViewAdapter.mMainLayout.removeView(this);
            mLauncher.getDesktop().getAssistViewsContainer().mAssistMainRecyclerViewAdapter.mMainLayout.addView(this, 3);
            currentIndex = 3;
            isPinned = true;

            // 항목들의 순서를 자료기지에 갱신
            mLauncher.getModelWriter().updateAssistantSectionOrder(
                    assistMainRecyclerViewAdapter.mEstimationView.currentIndex, assistMainRecyclerViewAdapter.mEstimationView.isPinned ? 1 : 0,
                    assistMainRecyclerViewAdapter.mCalendarView.currentIndex, assistMainRecyclerViewAdapter.mCalendarView.isPinned ? 1 : 0,
                    assistMainRecyclerViewAdapter.mClockView.currentIndex, assistMainRecyclerViewAdapter.mClockView.isPinned ? 1 : 0,
                    assistMainRecyclerViewAdapter.mFavoriteContacts.currentIndex, assistMainRecyclerViewAdapter.mFavoriteContacts.isPinned ? 1 : 0);
        }
    }

    /**
     * Unpin 단추를 눌렀을때의 처리
     */
    @Override
    public void onSelectUnpin() {
        LinearLayout mainLayout = mLauncher.getDesktop().getAssistViewsContainer().mAssistMainRecyclerViewAdapter.mMainLayout;
        AssistMainRecyclerViewAdapter assistMainRecyclerViewAdapter = mLauncher.getDesktop().getAssistViewsContainer().mAssistMainRecyclerViewAdapter;
        if (isPinned) {
            int i;
            for (i = currentIndex + 1; i < mainLayout.getChildCount(); i ++) {
                AssistItemContainer assistItemContainer = (AssistItemContainer) (mainLayout.getChildAt(i));
                if (!assistItemContainer.getPinStatus()) {
                    if (defaultIndex < assistItemContainer.getDefaultIndex()) {
                        mainLayout.removeView(this);
                        mainLayout.addView(this, assistItemContainer.getCurrentIndex() - 1);
                        currentIndex = assistItemContainer.getCurrentIndex() - 1;
                        break;
                    } else assistItemContainer.setCurrentIndex(i - 1);
                } else assistItemContainer.setCurrentIndex(i - 1);
            }
            if (i == mainLayout.getChildCount()) {
                mainLayout.removeView(this);
                mainLayout.addView(this, mainLayout.getChildCount());
                currentIndex = mainLayout.getChildCount() - 1;
            }
            isPinned = false;

            // 항목들의 순서를 자료기지에 갱신
            mLauncher.getModelWriter().updateAssistantSectionOrder(
                    assistMainRecyclerViewAdapter.mEstimationView.currentIndex, assistMainRecyclerViewAdapter.mEstimationView.isPinned ? 1 : 0,
                    assistMainRecyclerViewAdapter.mCalendarView.currentIndex, assistMainRecyclerViewAdapter.mCalendarView.isPinned ? 1 : 0,
                    assistMainRecyclerViewAdapter.mClockView.currentIndex, assistMainRecyclerViewAdapter.mClockView.isPinned ? 1 : 0,
                    assistMainRecyclerViewAdapter.mFavoriteContacts.currentIndex, assistMainRecyclerViewAdapter.mFavoriteContacts.isPinned ? 1 : 0);
        }
    }

    @Override
    public void onSelectSettings() {

    }
}
