package com.android.launcher3.assistant;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Assist page 의 항목들을 위한 Adapter
 */
public class AssistMainRecyclerViewAdapter extends RecyclerView.Adapter<AssistMainRecyclerViewAdapter.ViewHolder> {
    LayoutInflater mInflater;
    AssistViewsContainer mAssistViewsContainer;
    AssistShortcutAppsView mShortcutAppsView;
    LinearLayout mSearchBox;
    AssistFavoriteShortcut mCalendarView;
    AssistFavoriteShortcut mClockView;
    public AssistFavoriteContacts mFavoriteContacts;
    AssistEstimationView mEstimationView;
    AssistMemoryStatus mMemoryView;
    public AssistStep mStep;
    LinearLayout mMainLayout;
    Launcher mLauncher;

    public AssistMainRecyclerViewAdapter(Launcher launcher, AssistViewsContainer assistViewsContainer, Context context) {
        mLauncher = launcher;
        mInflater = LayoutInflater.from(context);
        mAssistViewsContainer = assistViewsContainer;
    }

    @NonNull
    @Override
    public AssistMainRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.assist_main_layout, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssistMainRecyclerViewAdapter.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mMainLayout = itemView.findViewById(R.id.assist_main_view);
            mSearchBox = itemView.findViewById(R.id.assist_search_box);
            mShortcutAppsView = itemView.findViewById(R.id.shortcuts_and_apps);
            mCalendarView = itemView.findViewById(R.id.assist_calendar);
            mClockView = itemView.findViewById(R.id.assist_clock);
            mEstimationView = itemView.findViewById(R.id.assist_memory_and_step);
            mMemoryView = itemView.findViewById(R.id.assist_memory);
            mStep = itemView.findViewById(R.id.assist_step);
            mFavoriteContacts = itemView.findViewById(R.id.assist_favorite_contacts);
            mSearchBox.setOnClickListener(this);

            mShortcutAppsView.setRootView(mAssistViewsContainer);

            DeviceProfile grid = mLauncher.getDeviceProfile();
            Rect padding = grid.workspacePadding;
            mMainLayout.setPadding(padding.left, padding.top, padding.right, padding.top);

            rearrangeViews();
        }

        /**
         * 부분별 순서를 자료기지에서 얻고 재정렬
         */
        private void rearrangeViews() {
            int originLayoutViewCount = mMainLayout.getChildCount();
            List<Integer> indexList = new ArrayList<>();
            List<String> sectionList = new ArrayList<>();

            Cursor cursor = mLauncher.getModelWriter().getAssistantSectionOrder();
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    indexList.add(cursor.getInt(0));
                    sectionList.add("estimation");
                    indexList.add(cursor.getInt(2));
                    sectionList.add("calendar");
                    indexList.add(cursor.getInt(4));
                    sectionList.add("clock");
                    indexList.add(cursor.getInt(6));
                    sectionList.add("contacts");
                }

                for (int i = 3; i < originLayoutViewCount; i ++)
                    mMainLayout.removeViewAt(3);
                for (int i = 3; i < originLayoutViewCount; i ++) {
                    for (int j = 0; j < indexList.size(); j ++) {
                        if (indexList.get(j) == i) {
                            if (sectionList.get(j).equals("estimation")) mMainLayout.addView(mEstimationView);
                            else if (sectionList.get(j).equals("calendar")) mMainLayout.addView(mCalendarView);
                            else if (sectionList.get(j).equals("clock")) mMainLayout.addView(mClockView);
                            else if (sectionList.get(j).equals("contacts")) mMainLayout.addView(mFavoriteContacts);
                        }
                    }
                }
                cursor.close();
            }
        }

        @Override
        public void onClick(View view) {
            if (view == mSearchBox) {
                mAssistViewsContainer.openSearchView();
            }
        }
    }
}
