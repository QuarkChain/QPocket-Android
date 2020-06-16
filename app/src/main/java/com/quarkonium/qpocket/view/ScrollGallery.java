package com.quarkonium.qpocket.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Transformation;

import com.quarkonium.qpocket.view.listener.FixedSpeedScroller;

import java.lang.reflect.Field;


/**
 * 支持每项Item不同的滚动时间,
 * 支持监听长按
 */
public class ScrollGallery extends ViewPager {

    private static final int AUTO_SCROLL_TIME = 2500;
    private boolean isAutoScroll = false;
    private int mAutoScrollTime = AUTO_SCROLL_TIME;
    private static final int HANDLER_MSG = 100;
    private FixedSpeedScroller mScroller = null;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == HANDLER_MSG) {
                scrollRight();
                sendEmptyMessageDelayed(HANDLER_MSG, mAutoScrollTime);
            }
        }
    };

    public ScrollGallery(Context context) {
        this(context, null);
    }

    public ScrollGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSoundEffectsEnabled(false);
        setFadingEdgeLength(0);
        controlViewPagerSpeed();
    }


    private void controlViewPagerSpeed() {
        try {
            Field field;
            try {
                field = ViewPager.class.getDeclaredField("mScroller");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            field.setAccessible(true);

            mScroller = new FixedSpeedScroller(
                    this.getContext(),
                    new AccelerateInterpolator());
            mScroller.setmDuration(450); // 2000ms
            field.set(this, mScroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        return false;
    }

    protected void scrollLeft() {
        onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
//        if (getAdapter() != null && getAdapter() instanceof BannerAdapter) {
//            BannerAdapter cycleAdapter = (BannerAdapter) getAdapter();
//            int positionNow = getCurrentItem();
//            int time = cycleAdapter.getAutoScrollTime(positionNow - 1);
//            if (time > 0) {
//                mAutoScrollTime = time;
//            }
//        }
    }

    protected void scrollRight() {
        onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
        int positionNow = getCurrentItem();
        setCurrentItem(positionNow + 1);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (isAutoScroll) {
            stopAutoScroll();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (isAutoScroll) {
            if (hasWindowFocus) {
                startAutoScroll();
            } else {
                stopAutoScroll();
            }
        }
    }

    private void startAutoScroll() {
        handler.removeMessages(HANDLER_MSG);
        handler.sendEmptyMessageDelayed(HANDLER_MSG, mAutoScrollTime);
    }

    private void stopAutoScroll() {
        handler.removeMessages(HANDLER_MSG);
    }

    public void setAutoScroll(boolean autoScroll) {
        if (autoScroll != isAutoScroll) {
            isAutoScroll = autoScroll;
            if (isAutoScroll) {
                setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN: {
                                stopAutoScroll();
                                break;
                            }
                            case MotionEvent.ACTION_CANCEL: {
                                startAutoScroll();
                                break;
                            }
                            case MotionEvent.ACTION_UP: {
                                startAutoScroll();
                                break;
                            }

                            default: {
                                stopAutoScroll();
                                break;
                            }
                        }
                        return false;
                    }
                });
                startAutoScroll();
            } else {
                stopAutoScroll();
            }
        }
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        super.setAdapter(adapter);
//        if (adapter instanceof BannerAdapter) {
//            BannerAdapter cycleAdapter = (BannerAdapter) adapter;
//            int time = cycleAdapter.getAutoScrollTime(0);
//            if (time > 0) {
//                mAutoScrollTime = time;
//            }
//        }
    }
//
//    private boolean mIsInLongPress;

//    public void onLongPress(MotionEvent e) {
//        mIsInLongPress = true;
//        if (mLongPressListener != null) {
//            mLongPressListener.onLongPress();
//        }
//    }

    public interface onLongPressListener {
        void onLongPress();

        void onLongPressUp();
    }
}
