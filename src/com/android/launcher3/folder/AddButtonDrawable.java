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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.R;
import com.android.launcher3.anim.Interpolators;

/**
 * 등록부의 추가단추에 그려지는 아이콘
 */
public class AddButtonDrawable extends Drawable {
    int mWidth;     //너비
    int mHeight;    //높이
    int mIconSize;  //`+`아이콘의 크기
    RectF mDrawingRect; //둥근4각형 령역

    //추가단추
    private AddButtonBubbleTextView mParentTextView = null;

    //길게 눌렀을때 덧 그려주는 Mask 색
    private int mDarkMaskColor = Color.GRAY;

    //단추가 눌리운 상태인가, 아닌가?
    private boolean mIsPressed = false;

    //단추를 누르거나 놓을때 진행되는 animation
    private ObjectAnimator mScaleAnimator;

    Context mContext;
    Paint mPaint = new Paint();

    public AddButtonDrawable(Context context, int width, int height, int padding){
        mContext = context;

        //아이콘그리기령역, 크기들을 얻는다.
        mWidth = width;
        mHeight = height;
        mDrawingRect = new RectF(padding, padding, width - padding, height - padding);
        mIconSize = mWidth / 2;

        //Paint에 배경색을 설정한다.
        int bgColor = context.getColor(R.color.add_button_background_color);
        mPaint.setColor(bgColor);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        //둥근4각형을 그린다.
        canvas.drawRoundRect(mDrawingRect, 20, 20, mPaint);

        //`+`단추를 그린다.
        Drawable drawable = mContext.getDrawable(R.drawable.ic_add_to_folder);
        int start = (mWidth - mIconSize) / 2;
        if(drawable != null) {
            drawable.setBounds(start, start, start + mIconSize, start + mIconSize);
            drawable.draw(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean isPressed = false;
        for (int s : state) {
            if (s == android.R.attr.state_pressed) {
                isPressed = true;
                break;
            }
        }

        if(mIsPressed != isPressed){
            mIsPressed = isPressed;

            if (mScaleAnimator != null) {
                mScaleAnimator.cancel();
                mScaleAnimator = null;
            }

            if (mIsPressed) {
                if(mParentTextView != null){
                    //If the text view is from folder dialog, do not play scale animation
                    // Animate when going to pressed state

                    mScaleAnimator = ObjectAnimator.ofFloat(this, "scale", FastBitmapDrawable.PRESSED_SCALE);
                    mScaleAnimator.setDuration(FastBitmapDrawable.CLICK_FEEDBACK_DURATION);
                    mScaleAnimator.setInterpolator(Interpolators.ACCEL);
                    mScaleAnimator.start();

                    setDarkIcon();
                }
            } else {

                //Scale to original
                if(mParentTextView != null){
                    mScaleAnimator = ObjectAnimator.ofFloat(this, "scale", 1);
                    mScaleAnimator.setDuration(FastBitmapDrawable.CLICK_FEEDBACK_DURATION);
                    mScaleAnimator.setInterpolator(Interpolators.ACCEL);
                    mScaleAnimator.start();

                    setNormalIcon();
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Paint 에 어두운 Mask 색을 입힌다. (단추를 누르고 있는동안 아이콘이 어두워진다.)
     */
    public void setDarkIcon(){
        ColorFilter filter = new PorterDuffColorFilter(mDarkMaskColor, PorterDuff.Mode.MULTIPLY);
        mPaint.setColorFilter(filter);
        invalidateSelf();
    }

    /**
     * 어두운 Mask색을 없앤다.
     */
    public void setNormalIcon(){
        mPaint.setColorFilter(null);
        invalidateSelf();
    }

    /**
     * Animation속성함수("scale")
     * @return scaleX값을 돌려준다.
     */
    @Keep
    public float getScale(){
        if(mParentTextView != null)
            return mParentTextView.getScaleX();

        return 1;
    }

    /**
     * Animation속성함수("scale")
     * 단추를 비률만큼 확대/축소시킨다.
     *
     * @param scale 확대/축소 비률
     */
    @Keep
    public void setScale(float scale){
        if(mParentTextView != null){
            mParentTextView.setScaleX(scale);
            mParentTextView.setScaleY(scale);
        }
    }

    /**
     * 단추를 설정한다.
     * @param view 단추
     */
    public void setParentTextView(AddButtonBubbleTextView view){
        mParentTextView = view;
        mDarkMaskColor = view.getContext().getColor(R.color.dark_icon_color_mask_add);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * State변화를 감지하는가?
     * @return true를 돌려줌
     */
    @Override
    public boolean isStateful() {
        return true;
    }
}
