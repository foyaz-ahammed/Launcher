package com.android.launcher3.assistant;

import static android.Manifest.permission.READ_CALENDAR;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.android.launcher3.Launcher.CalendarEventListener;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Assist 페지에 Calendar 를 표시하기 위한 layout
 */
public class AssistantCalendar extends AssistFavoriteShortcut implements CalendarEventListener {
    public static final String CALENDAR_APP_PACKAGE = "com.android.krcalendar";
    public static final String CALENDAR_VIEW_EVENT_CLASS = "com.android.calendar.EventInfoActivity";

    RecyclerView mEventListRecyclerView;
    CalendarEventAdapter mCalendarEventAdapter;

    //Calendar events
    List<SimpleCalendarEvent> mUpcomingEvents = new ArrayList<>();

    public AssistantCalendar(Context context) {
        this(context, null);
    }

    public AssistantCalendar(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistantCalendar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mCalendarEventAdapter = new CalendarEventAdapter(mLauncher);

        defaultIndex = 4;
        currentIndex = 4;

        mLauncher.setCalendarEventListener(this);
        this.onDetachedFromWindow();
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mEventListRecyclerView = findViewById(R.id.upcoming_event_list);

        // scroll 을 비능동으로 만든다.
        mEventListRecyclerView.setNestedScrollingEnabled(false);

        // Layout manager 설정
        LinearLayoutManager layoutManager = new LinearLayoutManager(mLauncher, GridLayoutManager.VERTICAL, false);
        mEventListRecyclerView.setLayoutManager(layoutManager);
        mEventListRecyclerView.setAdapter(mCalendarEventAdapter);

        // 자료기지에서 이 부분의 순서와 pin 상태를 얻기
        Cursor cursor = mLauncher.getModelWriter().getAssistantCalendarSectionOrder();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                currentIndex = cursor.getInt(0);
                isPinned = cursor.getInt(1) != 0;
            }
            cursor.close();
        }

        getUpcomingEvents();
    }

    /**
     * Calendar event 들을 얻기
     */
    public void getUpcomingEvents(){
        if (ActivityCompat.checkSelfPermission(mLauncher, READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mUpcomingEvents.clear();

        // Julian 번허로 오늘 얻기
        Calendar calendar = Calendar.getInstance();
        int todayJulian = Utilities.getJulianDay(calendar);

        // 현재시간의 millisecond 얻기
        long currentTimeMillis = calendar.getTimeInMillis();

        Uri.Builder builder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
        //add two ids, 1:start, 2:end
        ContentUris.appendId(builder, todayJulian);
        ContentUris.appendId(builder, todayJulian);

        Cursor cursor = null;

        cursor = mLauncher.getContentResolver().query(builder.build(), new String[]{
                CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY
        }, null, null, null);

        if(cursor == null){
            return;
        }

        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i ++){
            int id = cursor.getInt(0);
            String title = cursor.getString(1);
            long startTime = cursor.getLong(2);
            long endTime = cursor.getLong(3);
            boolean allDay = cursor.getInt(4) != 0;

            // 끝내기시간이 현재시간보다 이전이면 건너띄기
            if(endTime >= currentTimeMillis) {
                SimpleCalendarEvent event = new SimpleCalendarEvent(id, title, startTime, endTime,
                        allDay);
                mUpcomingEvents.add(event);
            }
            cursor.moveToNext();
        }

        cursor.close();

        /*-- Then update recyclerview --*/
        float oneCalendarEventItemHeight = getResources().getDimension(R.dimen.assist_calendar_event_item_height) +
                getResources().getDimension(R.dimen.calendar_event_margin);
        mEventListRecyclerView.getLayoutParams().height = (int) (oneCalendarEventItemHeight * mUpcomingEvents.size());

        mCalendarEventAdapter.setUpcomingEvents(mUpcomingEvents);
        mCalendarEventAdapter.notifyDataSetChanged();

    }

    @Override
    public void onEventChange() {
        getUpcomingEvents();
    }

    /**
     * Calendar event 를 위한 class
     */
    public static class SimpleCalendarEvent {
        int id;
        String title;
        long startTime;
        long endTime;
        boolean allDay;

        public SimpleCalendarEvent(int nId, String nTitle, long nStartTime, long nEndTime, boolean nAllDay){
            id = nId;
            title = nTitle;
            startTime = nStartTime;
            endTime = nEndTime;
            allDay = nAllDay;
        }
    }
}
