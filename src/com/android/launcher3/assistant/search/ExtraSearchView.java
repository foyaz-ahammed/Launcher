package com.android.launcher3.assistant.search;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 검색문자렬이 비였을때 현시할 View
 */
public class ExtraSearchView extends LinearLayout implements ExtraViewsListener {

    List<ExtraViewsListener> mListeners = new ArrayList<>();

    public ExtraSearchView(Context context) {
        this(context, null);
    }

    public ExtraSearchView(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExtraSearchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        for (int i = 0; i < getChildCount(); i ++){
            if(getChildAt(i) instanceof ExtraViewsListener){
                mListeners.add((ExtraViewsListener)getChildAt(i));
            }
        }
    }

    @Override
    public void onOpen() {
        //Notify children that they are opened
        for (int i = 0; i < mListeners.size(); i ++){
            final ExtraViewsListener listener = mListeners.get(i);

            post(new Runnable() {
                @Override
                public void run() {
                    listener.onOpen();
                }
            });
        }
    }
}
