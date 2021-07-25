package com.android.launcher3.assistant;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.AssistantCalendar.SimpleCalendarEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Calendar event 들을 현시하기 위한 Adapter
 */
public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.ViewHolder> {
    Launcher mLauncher;
    LayoutInflater mLayoutInflater;

    //Calendar event 목록
    List<SimpleCalendarEvent> mUpcomingEvents = new ArrayList<>();

    public CalendarEventAdapter(Launcher launcher){
        mLauncher = launcher;
        mLayoutInflater = LayoutInflater.from(launcher);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AssistCalendarEvent childView = (AssistCalendarEvent)mLayoutInflater.inflate(
                R.layout.assist_calendar_event, parent, false);
        return new ViewHolder(childView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AssistCalendarEvent childView = (AssistCalendarEvent) holder.itemView;
        childView.applyFrom(mUpcomingEvents.get(position));
        childView.setOnClickListener(childView);
    }

    @Override
    public int getItemCount() {
        return mUpcomingEvents.size();
    }

    /**
     * Upcoming event 목록 설정
     * @param eventList 설정할 Upcoming event 목록
     */
    public void setUpcomingEvents(List<SimpleCalendarEvent> eventList){
        mUpcomingEvents = eventList;
    }

    /**
     * 개별적인 event 들을 위한 viewHolder
     */
    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
