package com.quarkonium.qpocket.model.main.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.quarkonium.qpocket.R;

public class SpinnerPopWindow {
    public interface SpinnerOnItemClickListener {
        void onItemClick(int position, Object o);
    }

    private static final int DEFAULT_ELEVATION = 16;

    private PopupWindow mPopWindow;
    private ListView mListView;
    private int mSelectPosition;
    private BaseAdapter mAdapter;

    private SpinnerOnItemClickListener mOnItemClickListener;

    public SpinnerPopWindow(Context context) {
        mPopWindow = new PopupWindow(context);
        mPopWindow.setOutsideTouchable(true);
        mPopWindow.setFocusable(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPopWindow.setElevation(DEFAULT_ELEVATION);
            mPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_drawable));
        } else {
            mPopWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.drop_down_shadow));
        }


        mListView = new ListView(context);
        mListView.setDivider(null);
        mListView.setBackgroundColor(Color.WHITE);
        mListView.setItemsCanFocus(true);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setHorizontalScrollBarEnabled(false);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(position, mAdapter != null ? mAdapter.getItem(position) : null);
                }
                mSelectPosition = position;
                mPopWindow.dismiss();
            }
        });
        mPopWindow.setContentView(mListView);
    }

    public void setAdapter(BaseAdapter adapter) {
        mAdapter = adapter;
        mListView.setAdapter(adapter);
    }

    public void setOnItemClickListener(SpinnerOnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public void show(View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(view.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(getPopUpHeight(view), View.MeasureSpec.AT_MOST);
        mListView.measure(widthSpec, heightSpec);
        mPopWindow.setWidth(mListView.getMeasuredWidth());
        mPopWindow.setHeight(mListView.getMeasuredHeight());
        mPopWindow.showAsDropDown(view, 0, 10);
    }

    public void showWidth(View view, int width) {
        int heightSpec = View.MeasureSpec.makeMeasureSpec(getPopUpHeight(view), View.MeasureSpec.AT_MOST);
        mListView.measure(width, heightSpec);
        mPopWindow.setWidth(mListView.getMeasuredWidth());
        mPopWindow.setHeight(mListView.getMeasuredHeight());
        mPopWindow.showAsDropDown(view, 0, 10);
    }

    public void dismiss() {
        mPopWindow.dismiss();
    }

    private int getPopUpHeight(View view) {
        return Math.max(verticalSpaceBelow(view), verticalSpaceAbove(view));
    }

    private int verticalSpaceAbove(View view) {
        return getParentVerticalOffset(view);
    }

    private int verticalSpaceBelow(View view) {
        int displayHeight = view.getContext().getResources().getDisplayMetrics().heightPixels;
        return displayHeight - getParentVerticalOffset(view) - view.getMeasuredHeight();
    }

    private int getParentVerticalOffset(View view) {
        int[] locationOnScreen = new int[2];
        view.getLocationOnScreen(locationOnScreen);
        return locationOnScreen[1];
    }

    public int getSelectPosition() {
        return mSelectPosition;
    }
}
