package com.quarkonium.qpocket.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.quarkonium.qpocket.util.UiUtils;

public class PasswordLevelView extends View {
    // 当前密级强度
    private Level mCurLevel;
    // 默认情况下的密级颜色
    private int defaultColor = Color.argb(255, 220, 220, 220);
    private int mBoard;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public enum Level {
        DANGER("风险", Color.parseColor("#ffff3233"), 0),
        LOW("弱", Color.parseColor("#fff6c049"), 1),
        MID("中", Color.parseColor("#ff00a7dd"), 2),
        STRONG("强", Color.parseColor("#ff00cd8a"), 3);


        String mStrLevel;
        int mLevelResColor;
        int mIndex;

        Level(String levelText, int levelResColor, int index) {
            mStrLevel = levelText;
            mLevelResColor = levelResColor;
            mIndex = index;
        }
    }

    public PasswordLevelView(Context context) {
        this(context, null);
    }

    public PasswordLevelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordLevelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mBoard = (int) UiUtils.dpToPixel(1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //计算密级色块区域的宽高
        float eachLevelWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int levelNum = Level.values().length;
        float eachLevelHeight = (getHeight() - getPaddingTop() - getPaddingBottom()) / levelNum;

        int startIndexOfDefaultColor = mCurLevel == null ? 0 : mCurLevel.mIndex + 1;
        float startRectBottom = getHeight() - getPaddingBottom();

        // 画密级色块
        for (int i = 0; i < levelNum; i++) {
            if (i >= startIndexOfDefaultColor) {
                mPaint.setColor(defaultColor);
            } else {
                mPaint.setColor(mCurLevel.mLevelResColor);
            }

            canvas.drawRect(
                    getPaddingLeft(),
                    startRectBottom - eachLevelHeight,
                    getPaddingLeft() + eachLevelWidth,
                    startRectBottom,
                    mPaint);
            startRectBottom -= eachLevelHeight + mBoard;
        }
    }

    /**
     * 显示level对应等级的色块
     *
     * @param level 密码密级
     */
    public void showLevel(Level level) {
        mCurLevel = level;
        invalidate();
    }

    public Level getLevel() {
        return mCurLevel;
    }
}
