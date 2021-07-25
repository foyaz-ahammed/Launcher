/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static com.android.launcher3.anim.Interpolators.ACCEL;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.util.SparseArray;

import com.android.launcher3.graphics.BitmapInfo;
import org.jetbrains.annotations.NotNull;

/**
 * App이나 Shortcut의 아이콘을 그리는데 리용되는 Drawable클라스이다.
 *
 * @see BubbleTextView
 */
public class FastBitmapDrawable extends Drawable {

    //아이콘을 눌렀을때 scale값 (아이콘을 누를때는 약간 축소된다.)
    public static final float PRESSED_SCALE = 0.9f;

    private static final float DISABLED_DESATURATION = 1f;
    private static final float DISABLED_BRIGHTNESS = 0.5f;

    public static final int CLICK_FEEDBACK_DURATION = 200;

    // Since we don't need 256^2 values for combinations of both the brightness and saturation, we
    // reduce the value space to a smaller value V, which reduces the number of cached
    // ColorMatrixColorFilters that we need to keep to V^2
    private static final int REDUCED_FILTER_VALUE_SPACE = 48;

    // A cache of ColorFilters for optimizing brightness and saturation animations
    private static final SparseArray<ColorFilter> sCachedFilter = new SparseArray<>();

    // Temporary matrices used for calculation
    private static final ColorMatrix sTempBrightnessMatrix = new ColorMatrix();
    private static final ColorMatrix sTempFilterMatrix = new ColorMatrix();

    protected final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    protected Bitmap mBitmap;
    protected final int mIconColor;

    //아이콘이 눌리운 상태인가?
    private boolean mIsPressed;

    //아이콘이 비능동되였는가?
    private boolean mIsDisabled;

    //Drawable 의 확대/축소 animation을 위한 속성객체
    private static final Property<FastBitmapDrawable, Float> SCALE
            = new Property<FastBitmapDrawable, Float>(Float.TYPE, "scale") {
        @Override
        public Float get(FastBitmapDrawable fastBitmapDrawable) {
            if(fastBitmapDrawable.mParentTextView != null) {
                return fastBitmapDrawable.mParentTextView.getScaleX();
            } else {
                return fastBitmapDrawable.mScale;
            }
        }

        @Override
        public void set(FastBitmapDrawable fastBitmapDrawable, Float value) {
            //mParentTextView 가 설정되여있으면 그것의 scale 값을 조절하고
            //그렇지 않으면 그리기에서 Canvas의 scale값을 조절한다.
            if(fastBitmapDrawable.mParentTextView != null) {
                fastBitmapDrawable.mParentTextView.setScaleX(value);
                fastBitmapDrawable.mParentTextView.setScaleY(value);
            } else {
                fastBitmapDrawable.mScale = value;
                fastBitmapDrawable.invalidateSelf();
            }
        }
    };
    private ObjectAnimator mScaleAnimator;
    private float mScale = 1;

    // The saturation and brightness are values that are mapped to REDUCED_FILTER_VALUE_SPACE and
    // as a result, can be used to compose the key for the cached ColorMatrixColorFilters
    private int mDesaturation = 0;
    private int mBrightness = 0;
    private int mAlpha = 255;
    private int mPrevUpdateKey = Integer.MAX_VALUE;

    private BubbleTextView mParentTextView = null;

    //Dark Mask color(default gray)
    private int mDarkMaskColor = Color.GRAY;

    public FastBitmapDrawable(Bitmap b) {
        this(b, Color.TRANSPARENT);
    }
    public FastBitmapDrawable(BitmapInfo info) {
        this(info.icon, info.color);
    }
    public FastBitmapDrawable(ItemInfoWithIcon info) {
        this(info.iconBitmap, info.iconColor);
    }
    protected FastBitmapDrawable(Bitmap b, int iconColor) {
        mBitmap = b;
        mIconColor = iconColor;
        setFilterBitmap(true);
    }

    public void setParentTextView(BubbleTextView view){
        mParentTextView = view;
        mDarkMaskColor = view.getContext().getColor(R.color.dark_icon_color_mask);
    }

    @Override
    public final void draw(Canvas canvas) {
        if (mScaleAnimator != null) {
            int count = canvas.save();
            Rect bounds = getBounds();
            canvas.scale(mScale, mScale, bounds.exactCenterX(), bounds.exactCenterY());
            drawInternal(canvas, bounds);
            canvas.restoreToCount(count);
        } else {
            drawInternal(canvas, getBounds());
        }
    }

    /**
     * 아이콘그리기를 진행한다.
     * @param canvas Canvas
     * @param bounds 그리기령역
     */
    protected void drawInternal(Canvas canvas, Rect bounds) {
        canvas.drawBitmap(mBitmap, null, bounds, mPaint);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // No op
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setFilterBitmap(boolean filterBitmap) {
        mPaint.setFilterBitmap(filterBitmap);
        mPaint.setAntiAlias(filterBitmap);
    }

    public int getAlpha() {
        return mAlpha;
    }

    public float getAnimatedScale() {
        return mScaleAnimator == null ? 1 : mScale;
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public int getMinimumWidth() {
        return getBounds().width();
    }

    @Override
    public int getMinimumHeight() {
        return getBounds().height();
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public ColorFilter getColorFilter() {
        return mPaint.getColorFilter();
    }

    /**
     * Drawable의 상태변화 처리
     * @param state int[]
     */
    @Override
    protected boolean onStateChange(int[] state) {
        //눌리운 상태인가를 먼저 검사한다.
        boolean isPressed = false;
        for (int s : state) {
            if (s == android.R.attr.state_pressed) {
                isPressed = true;
                break;
            }
        }

        if (mIsPressed != isPressed) {
            mIsPressed = isPressed;

            //이미 진행하던 Animation은 중지시킨다.
            if (mScaleAnimator != null) {
                mScaleAnimator.cancel();
                mScaleAnimator = null;
            }

            if (mIsPressed) {   //눌리웠을때
                if(mParentTextView != null){
                    //TextView가 Assist 의 Apps & Shortcut 나 Quick Access, 등록부대화창에서 리용되는것이라면
                    //Animation을 진행할 필요가 없다.
                    //나머지 경우들에 Animation을 진행한다.
                    if(!mParentTextView.isIconFromFolderDialog() &&
                            !mParentTextView.isIconFromAssistApps() &&
                            !mParentTextView.isIconFromQuickAccess()) {
                        //축소 animation을 위한 animator를 창조하고 시작한다.
                        mScaleAnimator = ObjectAnimator.ofFloat(this, SCALE, PRESSED_SCALE);
                        mScaleAnimator.setDuration(CLICK_FEEDBACK_DURATION);
                        mScaleAnimator.setInterpolator(ACCEL);
                        mScaleAnimator.start();
                    }

                    //필요에 따라 아이콘이 눌리울때 어두운 Mask색을 그려준다.
                    if(!(mParentTextView.isIconFromQuickAccess() && !mParentTextView.getQuickAccessIconType())) {
                        setDarkIcon();
                    }
                }
            } else {    //눌리우지 않았을때
                mScale = 1f;

                //축소된 아이콘을 본래되로 복귀한다.
                if(mParentTextView != null){
                    mParentTextView.setScaleX(mScale);
                    mParentTextView.setScaleY(mScale);

                    //어두운 Mask색을 없앤다.
                    setNormalIcon();
                }
            }
            return true;
        }
        return false;
    }

    private void invalidateDesaturationAndBrightness() {
        setDesaturation(mIsDisabled ? DISABLED_DESATURATION : 0);
        setBrightness(mIsDisabled ? DISABLED_BRIGHTNESS : 0);
    }

    /**
     * 아이콘 능동/비능동 설정
     * @param isDisabled true: 능동, false: 비능동
     */
    public void setIsDisabled(boolean isDisabled) {
        if (mIsDisabled != isDisabled) {
            mIsDisabled = isDisabled;
            invalidateDesaturationAndBrightness();
        }
    }

    /**
     * Sets the saturation of this icon, 0 [full color] -> 1 [desaturated]
     */
    private void setDesaturation(float desaturation) {
        int newDesaturation = (int) Math.floor(desaturation * REDUCED_FILTER_VALUE_SPACE);
        if (mDesaturation != newDesaturation) {
            mDesaturation = newDesaturation;
            updateFilter();
        }
    }

    /**
     * 어두운 Mask색을 그려준다. (ColorFilter 설정)
     */
    public void setDarkIcon(){
        ColorFilter filter = new PorterDuffColorFilter(mDarkMaskColor, PorterDuff.Mode.MULTIPLY);
        mPaint.setColorFilter(filter);
        invalidateSelf();
    }

    /**
     * Mask색을 없앤다.
     */
    public void setNormalIcon(){
        mPaint.setColorFilter(null);
        invalidateSelf();
    }

    public float getDesaturation() {
        return (float) mDesaturation / REDUCED_FILTER_VALUE_SPACE;
    }

    /**
     * Sets the brightness of this icon, 0 [no add. brightness] -> 1 [2bright2furious]
     */
    private void setBrightness(float brightness) {
        int newBrightness = (int) Math.floor(brightness * REDUCED_FILTER_VALUE_SPACE);
        if (mBrightness != newBrightness) {
            mBrightness = newBrightness;
            updateFilter();
        }
    }

    private float getBrightness() {
        return (float) mBrightness / REDUCED_FILTER_VALUE_SPACE;
    }

    /**
     * Updates the paint to reflect the current brightness and saturation.
     */
    protected void updateFilter() {
        boolean usePorterDuffFilter = false;
        int key = -1;
        if (mDesaturation > 0) {
            key = (mDesaturation << 16) | mBrightness;
        } else if (mBrightness > 0) {
            // Compose a key with a fully saturated icon if we are just animating brightness
            key = (1 << 16) | mBrightness;

            // We found that in L, ColorFilters cause drawing artifacts with shadows baked into
            // icons, so just use a PorterDuff filter when we aren't animating saturation
            usePorterDuffFilter = true;
        }

        // Debounce multiple updates on the same frame
        if (key == mPrevUpdateKey) {
            return;
        }
        mPrevUpdateKey = key;

        if (key != -1) {
            ColorFilter filter = sCachedFilter.get(key);
            if (filter == null) {
                float brightnessF = getBrightness();
                int brightnessI = (int) (255 * brightnessF);
                if (usePorterDuffFilter) {
                    filter = new PorterDuffColorFilter(Color.argb(brightnessI, 255, 255, 255),
                            PorterDuff.Mode.SRC_ATOP);
                } else {
                    float saturationF = 1f - getDesaturation();
                    sTempFilterMatrix.setSaturation(saturationF);
                    if (mBrightness > 0) {
                        // Brightness: C-new = C-old*(1-amount) + amount
                        float scale = 1f - brightnessF;
                        float[] mat = sTempBrightnessMatrix.getArray();
                        mat[0] = scale;
                        mat[6] = scale;
                        mat[12] = scale;
                        mat[4] = brightnessI;
                        mat[9] = brightnessI;
                        mat[14] = brightnessI;
                        sTempFilterMatrix.preConcat(sTempBrightnessMatrix);
                    }
                    filter = new ColorMatrixColorFilter(sTempFilterMatrix);
                }
                sCachedFilter.append(key, filter);
            }
        }

        invalidateSelf();
    }

    @Override
    public ConstantState getConstantState() {
        return new MyConstantState(mBitmap, mIconColor);
    }

    protected static class MyConstantState extends ConstantState {
        protected final Bitmap mBitmap;
        protected final int mIconColor;

        public MyConstantState(Bitmap bitmap, int color) {
            mBitmap = bitmap;
            mIconColor = color;
        }

        @NotNull
        @Override
        public Drawable newDrawable() {
            return new FastBitmapDrawable(mBitmap, mIconColor);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }
    }
}
