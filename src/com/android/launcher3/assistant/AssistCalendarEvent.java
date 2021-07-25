package com.android.launcher3.assistant;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.net.Uri;
import android.provider.CalendarContract.Events;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.AssistantCalendar.SimpleCalendarEvent;
import java.util.Calendar;
import java.util.Date;

/**
 * 개별적인 Calendar Event 현시를 위한 layout
 */
public class AssistCalendarEvent extends LinearLayout implements View.OnClickListener{
    //Event 정보
    int mId;
    String mTitle;
    boolean mAllDay;
    Date mStartTime;
    Date mEndTime;
    long mStartTimeMillis;
    long mEndTimeMillis;

    Launcher mLauncher;

    //Event timing 형태(ongoing, upcoming, or further)
    int mEventTimingState;

    TextView mStartTimeLabel;
    TextView mEndTimeLabel;
    TextView mAllDayLabel;
    TextView mEventTitleLabel;
    TextView mEventDescriptionLabel;

    //Drawing paint, path
    Paint mPaint = new Paint();
    Path mClipPath = new Path();

    // 배경 색상;
    int mEntireBackgroundColor = 0;
    int mLeftSideBackgroundColor = 0;
    float mLeftSideBgSize = 0;
    float mBgRadius = 0;

    // 설정색상들을 위한 3개의 상태
    private static final int ONGOING_EVENT = 0;
    private static final int UPCOMING_EVENT = 1;
    private static final int FURTHER_EVENT = 2;

    // 1분 밀리초
    private static final long ONE_MINUTE = 60000;

    // upcoming 사건의 제한시간(15 분)
    private static final long UPCOMING_LIMIT = ONE_MINUTE * 15;

    public AssistCalendarEvent(Context context) {
        this(context, null);
    }

    public AssistCalendarEvent(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AssistCalendarEvent(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = Launcher.getLauncher(context);

        mLeftSideBgSize = context.getResources().getDimension(R.dimen.calendar_event_left_bg_size);
        mBgRadius = context.getResources().getDimension(R.dimen.calendar_event_bg_radius);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mStartTimeLabel = findViewById(R.id.event_start_time);
        mEndTimeLabel = findViewById(R.id.event_end_time);
        mAllDayLabel = findViewById(R.id.event_all_day);
        mEventTitleLabel = findViewById(R.id.event_title);
        mEventDescriptionLabel = findViewById(R.id.event_description);
    }

    /**
     * Calendar event 정보표시
     * @param event 표시할 event
     */
    public void applyFrom(SimpleCalendarEvent event){
        mId = event.id;
        mTitle = event.title;
        mStartTime = new Date(event.startTime);
        mEndTime = new Date(event.endTime);
        mAllDay = event.allDay;
        mStartTimeMillis = event.startTime;
        mEndTimeMillis = event.endTime;

        updateVisibilityForTimeLabels();

        mEventTimingState = getEventTimingState();

        updateLabels();
        updateColors();

        invalidate();
    }

    @Override
    public void dispatchDraw(Canvas canvas){
        mClipPath.reset();
        mClipPath.addRoundRect(0, 0, getWidth(), getHeight(), mBgRadius, mBgRadius, Path.Direction.CCW);
        canvas.clipPath(mClipPath);

        mPaint.setStyle(Style.FILL);

        // 전체배경 그리기
        mPaint.setColor(mEntireBackgroundColor);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);

        //Draw the left side background
        mPaint.setColor(mLeftSideBackgroundColor);
        canvas.drawRect(0, 0, mLeftSideBgSize, getHeight(), mPaint);

        super.dispatchDraw(canvas);
    }

    /**
     * TextView 들의 text 갱신
     */
    @SuppressLint("DefaultLocale")
    public void updateLabels(){
        Resources resource = getResources();

        // 제목 설정
        mEventTitleLabel.setText(mTitle);

        final String description;

        if(mAllDay){
            String allDayText = resource.getString(R.string.all_day);
            mAllDayLabel.setText(allDayText);
            description = DateFormat.format("MM/dd/yyyy", mStartTime).toString();
        }
        else{
            // 분과 시의 표시를 위해 두개의 수자문자 리용
            final String startTime = String.format("%1$02d:%2$02d", mStartTime.getHours(), mStartTime.getMinutes());
            final String endTime = String.format("%1$02d:%2$02d", mEndTime.getHours(), mEndTime.getMinutes());

            // TextView 에 text 설정
            mStartTimeLabel.setText(startTime);
            mEndTimeLabel.setText(endTime);

            if(mEventTimingState == ONGOING_EVENT)
                description = resource.getString(R.string.on_going);

            else if(mEventTimingState == UPCOMING_EVENT){
                // 남은 분을 표시
                Calendar calendar = Calendar.getInstance();
                long currentTimeMillis = calendar.getTimeInMillis();
                long millisDiff = mStartTimeMillis - currentTimeMillis;

                int minutes = (int) (millisDiff / ONE_MINUTE);

                // 현재의 float 값보다 항상 큰 int 형 설정
                if(millisDiff % ONE_MINUTE > 0)
                    minutes += 1;

                description = resource.getString(R.string.upcoming_events_time_remaining, minutes);
            }
            else {
                description = "";
            }
        }

        mEventDescriptionLabel.setText(description);
    }

    // 배경과 본문색상 갱신
    public void updateColors(){
        int descriptionTextColor = 0;
        Resources resource = getResources();

        switch (mEventTimingState){
            case ONGOING_EVENT:
                descriptionTextColor = resource.getColor(R.color.ongoing_label_text_color, null);
                mEntireBackgroundColor = resource.getColor(R.color.ongoing_background, null);
                mLeftSideBackgroundColor = resource.getColor(R.color.ongoing_left_side_background, null);
                break;

            case UPCOMING_EVENT:
                descriptionTextColor = resource.getColor(R.color.upcoming_label_text_color, null);
                mEntireBackgroundColor = resource.getColor(R.color.upcoming_background, null);
                mLeftSideBackgroundColor = resource.getColor(R.color.upcoming_left_side_background, null);
                break;

            case FURTHER_EVENT:
                descriptionTextColor = resource.getColor(R.color.further_label_text_color, null);
                mEntireBackgroundColor = resource.getColor(R.color.further_background, null);
                mLeftSideBackgroundColor = resource.getColor(R.color.further_left_side_background, null);
                break;
        }

        mEventDescriptionLabel.setTextColor(descriptionTextColor);
    }

    /**
     * Event timing 형태 얻기
     */
    private int getEventTimingState(){
        // 현재 시간의 밀리초 얻기
        Calendar calendar = Calendar.getInstance();
        long currentTimeMillis = calendar.getTimeInMillis();

        // event 가 하루종일 event 이면, further 상태 반환
        if(mAllDay)
            return FURTHER_EVENT;

        // 현재 시간이 시작시간보다 후이고 마감시간이전이면 ongoing 상태 반환
        if(currentTimeMillis >= mStartTimeMillis && currentTimeMillis <= mEndTimeMillis)
            return ONGOING_EVENT;

        // 현재시간과 시작시간과의 차이가 15분보다 작으면 upcoming 상태 반환
        if(mStartTimeMillis - currentTimeMillis <= UPCOMING_LIMIT)
            return UPCOMING_EVENT;

        // 다른 경우에는 further 상태 반환
        return FURTHER_EVENT;
    }

    // 하루종일 event 이면 시작 및 마감시간을 숨기고 그렇지 않으면 하루종일 label 을 숨기기
    public void updateVisibilityForTimeLabels(){
        if(mAllDay){
            mStartTimeLabel.setVisibility(GONE);
            mEndTimeLabel.setVisibility(GONE);
            mAllDayLabel.setVisibility(VISIBLE);
        }
        else{
            mStartTimeLabel.setVisibility(VISIBLE);
            mEndTimeLabel.setVisibility(VISIBLE);
            mAllDayLabel.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        //Clicked the self view

        if(v == this){
            // Event 를 현시하는 Calendar app 의 activity 실행
            Intent intent = new Intent(Intent.ACTION_VIEW);

            //Add parameters
            Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, mId);
            intent.setData(eventUri);
            intent.setClassName(AssistantCalendar.CALENDAR_APP_PACKAGE, AssistantCalendar.CALENDAR_VIEW_EVENT_CLASS);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartTimeMillis);
            intent.putExtra(EXTRA_EVENT_END_TIME, mEndTimeMillis);
            mLauncher.startActivity(intent);
        }
    }
}
