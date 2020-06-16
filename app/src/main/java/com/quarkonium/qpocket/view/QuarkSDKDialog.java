package com.quarkonium.qpocket.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.quarkonium.qpocket.R;

public class QuarkSDKDialog extends Dialog {

    private Drawable mPositiveBtnDrawable;
    private Drawable mNegativeBtnDrawable;
    private String mPositiveBtnText;
    private String mNegativeBtnText;
    private String mTitle;
    private CharSequence mMessage;

    private int mWidth;
    private View.OnClickListener mPositiveBtnClickListener;
    private View.OnClickListener mNegativeBtnClickListener;
    private boolean mCanceledOnTouchOutside = false;

    private View mCheckView;
    private boolean mShowCheck;
    private int mCheckStrId = -1;

    private CharSequence mInfoText;

    public QuarkSDKDialog(Context context) {
        super(context, R.style.CompositeSDKFullScreenDialog);
    }

    public QuarkSDKDialog(Context context, boolean canceledOnTouchOutside) {
        super(context, R.style.CompositeSDKFullScreenDialog);
        mCanceledOnTouchOutside = canceledOnTouchOutside;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_common_layout);
        setCanceledOnTouchOutside(mCanceledOnTouchOutside);

        setOnKeyListener((DialogInterface dialog, int keyCode, KeyEvent event) -> {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                        return true;
                    }
                    return true;
                }
        );

        TextView positiveBtn = findViewById(R.id.positive_btn);
        if (null != mPositiveBtnDrawable) {
            positiveBtn.setBackgroundDrawable(mPositiveBtnDrawable);
        }

        if (null != mPositiveBtnText) {
            positiveBtn.setText(mPositiveBtnText);
        } else {
            positiveBtn.setVisibility(View.GONE);
        }

        if (null != mPositiveBtnClickListener) {
            positiveBtn.setOnClickListener(mPositiveBtnClickListener);
        }

        TextView negativeBtn = findViewById(R.id.negative_btn);
        if (null != mNegativeBtnDrawable) {
            negativeBtn.setBackgroundDrawable(mNegativeBtnDrawable);
        }

        if (null != mNegativeBtnText) {
            negativeBtn.setText(mNegativeBtnText);
        } else {
            negativeBtn.setVisibility(View.GONE);
        }

        if (null != mNegativeBtnClickListener) {
            negativeBtn.setOnClickListener(mNegativeBtnClickListener);
        }

        TextView title = findViewById(R.id.dialog_title);
        if (!TextUtils.isEmpty(mTitle)) {
            title.setVisibility(View.VISIBLE);
            title.setText(mTitle);
        } else {
            title.setVisibility(View.GONE);
        }

        TextView message = findViewById(R.id.dialog_tv);
        if (!TextUtils.isEmpty(mMessage)) {
            if (title.getVisibility() != View.VISIBLE) {
                message.setTextColor(title.getCurrentTextColor());
            }
            message.setVisibility(View.VISIBLE);
            message.setText(mMessage);
            message.setMovementMethod(ScrollingMovementMethod.getInstance());
        } else {
            message.setVisibility(View.GONE);
        }

        TextView info = findViewById(R.id.dialog_info);
        if (!TextUtils.isEmpty(mInfoText)) {
            Paint paint = info.getPaint();
            Rect rect = new Rect();
            paint.getTextBounds("海报", 0, 1, rect);
            float height = rect.height() * 4.5f;
            info.setMaxHeight(Math.round(height));

            info.setText(mInfoText);
            info.setMovementMethod(ScrollingMovementMethod.getInstance());
            info.setVisibility(View.VISIBLE);
        } else {
            info.setVisibility(View.GONE);
        }

        mCheckView = findViewById(R.id.check);
        if (mShowCheck) {
            if (mCheckStrId != -1) {
                ((TextView) findViewById(R.id.check_text)).setText(mCheckStrId);
            }
            findViewById(R.id.check_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.check_text).setOnClickListener(v -> mCheckView.performClick());
            mCheckView.setOnClickListener(v -> {
                if (mCheckView.isSelected()) {
                    mCheckView.setSelected(false);
                } else {
                    mCheckView.setSelected(true);
                }
            });
        }

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.8);
        getWindow().setAttributes(attributes);
    }

    public void setPositiveBtn(String text, View.OnClickListener listener) {
        setPositiveBtn(null,
                text,
                listener);
    }

    public void setPositiveBtn(int text, View.OnClickListener listener) {
        setPositiveBtn(null,
                text,
                listener);
    }

    public void setPositiveBtn(int drawable, int text, View.OnClickListener listener) {
        Resources resources = getContext().getResources();
        setPositiveBtn(drawable == 0 ? null : resources.getDrawable(drawable),
                text == 0 ? "" : resources.getString(text),
                listener);
    }

    public void setPositiveBtn(Drawable drawable, int text, View.OnClickListener listener) {
        Resources resources = getContext().getResources();
        setPositiveBtn(drawable,
                text == 0 ? "" : resources.getString(text),
                listener);
    }

    public void setPositiveBtn(int drawable, String text, View.OnClickListener listener) {
        Resources resources = getContext().getResources();
        setPositiveBtn(drawable == 0 ? null : resources.getDrawable(drawable),
                text,
                listener);
    }

    public void setPositiveBtn(Drawable drawable, String text, View.OnClickListener listener) {
        mPositiveBtnDrawable = drawable;
        mPositiveBtnText = text;
        mPositiveBtnClickListener = listener;
    }

    public void setNegativeBtn(String text, View.OnClickListener listener) {
        setNegativeBtn(null,
                text,
                listener);
    }

    public void setNegativeBtn(int text, View.OnClickListener listener) {
        setNegativeBtn(null,
                text,
                listener);
    }

    public void setNegativeBtn(int drawable, int text, View.OnClickListener listener) {
        Resources resources = getContext().getResources();
        setNegativeBtn(drawable == 0 ? null : resources.getDrawable(drawable),
                text == 0 ? "" : resources.getString(text),
                listener);
    }

    public void setNegativeBtn(Drawable drawable, int text, View.OnClickListener listener) {
        Resources resources = getContext().getResources();
        setNegativeBtn(drawable,
                text == 0 ? "" : resources.getString(text),
                listener);
    }

    public void setNegativeBtn(int drawable, String text, View.OnClickListener listener) {
        Resources resources = getContext().getResources();
        setNegativeBtn(drawable == 0 ? null : resources.getDrawable(drawable),
                text,
                listener);
    }

    public void setNegativeBtn(Drawable drawable, String text, View.OnClickListener listener) {
        mNegativeBtnDrawable = drawable;
        mNegativeBtnText = text;
        mNegativeBtnClickListener = listener;
    }

    public void setTitle(int text) {
        setTitle(getContext().getResources().getString(text));
    }

    public void setTitle(String text) {
        mTitle = text;
    }

    public void setMessage(int text) {
        setMessage(getContext().getString(text));
    }

    public void setMessage(CharSequence text) {
        mMessage = text;
    }

    public void setInfo(int text) {
        setInfo(getContext().getString(text));
    }

    public void setInfo(CharSequence text) {
        mInfoText = text;
    }

    public void showCheckBox(boolean isShow) {
        mShowCheck = isShow;
    }

    public void setCheckStrId(int id) {
        mCheckStrId = id;
    }

    public boolean isChecked() {
        return mCheckView != null && mCheckView.isSelected();
    }

    public static class Builder {
        private Drawable mPositiveBtnDrawable;
        private Drawable mNegativeBtnDrawable;
        private String mPositiveBtnText;
        private String mNegativeBtnText;
        private String mTitle;
        private String mMessage;
        private View.OnClickListener mPositiveBtnClickListener;
        private View.OnClickListener mNegativeBtnClickListener;
        private Context mContext;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setPositiveBtn(int text, View.OnClickListener listener) {
            return setPositiveBtn(null,
                    text,
                    listener);
        }

        public Builder setPositiveBtn(int drawable, int text, View.OnClickListener listener) {
            Resources resources = getContext().getResources();
            return setPositiveBtn(resources.getDrawable(drawable),
                    resources.getString(text),
                    listener);
        }

        public Builder setPositiveBtn(Drawable drawable, int text, View.OnClickListener listener) {
            Resources resources = getContext().getResources();
            return setPositiveBtn(drawable,
                    resources.getString(text),
                    listener);
        }

        public Builder setPositiveBtn(int drawable, String text, View.OnClickListener listener) {
            Resources resources = getContext().getResources();
            return setPositiveBtn(resources.getDrawable(drawable),
                    text,
                    listener);
        }

        public Builder setPositiveBtn(Drawable drawable, String text, View.OnClickListener listener) {
            mPositiveBtnDrawable = drawable;
            mPositiveBtnText = text;
            mPositiveBtnClickListener = listener;
            return this;
        }

        public Builder setNegativeBtn(int text, View.OnClickListener listener) {
            return setNegativeBtn(null,
                    text,
                    listener);
        }

        public Builder setNegativeBtn(int drawable, int text, View.OnClickListener listener) {
            Resources resources = getContext().getResources();
            return setNegativeBtn(resources.getDrawable(drawable),
                    resources.getString(text),
                    listener);
        }

        public Builder setNegativeBtn(Drawable drawable, int text, View.OnClickListener listener) {
            Resources resources = getContext().getResources();
            return setNegativeBtn(drawable,
                    resources.getString(text),
                    listener);
        }

        public Builder setNegativeBtn(int drawable, String text, View.OnClickListener listener) {
            Resources resources = getContext().getResources();
            return setNegativeBtn(resources.getDrawable(drawable),
                    text,
                    listener);
        }

        public Builder setNegativeBtn(Drawable drawable, String text, View.OnClickListener listener) {
            mNegativeBtnDrawable = drawable;
            mNegativeBtnText = text;
            mNegativeBtnClickListener = listener;
            return this;
        }

        public Builder setTitle(int text) {
            return setTitle(getContext().getResources().getString(text));
        }

        public Builder setTitle(String text) {
            mTitle = text;
            return this;
        }

        public Builder setMessage(int text) {
            return setMessage(getContext().getString(text));
        }

        public Builder setMessage(String text) {
            mMessage = text;
            return this;
        }

        public QuarkSDKDialog create() {
            QuarkSDKDialog dialog = new QuarkSDKDialog(mContext);
            dialog.setTitle(mTitle);
            dialog.setMessage(mMessage);
            dialog.setPositiveBtn(mPositiveBtnDrawable, mPositiveBtnText, mPositiveBtnClickListener);
            dialog.setNegativeBtn(mNegativeBtnDrawable, mNegativeBtnText, mNegativeBtnClickListener);
            return dialog;
        }

        private Context getContext() {
            return mContext;
        }
    }

    @Override
    public void show() {
        super.show();

        if (mWidth > 0) {
            Window window = getWindow();
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.width = mWidth;
            window.setAttributes(windowParams);
        }
    }

    public void setWidth(int width) {
        mWidth = width;
    }
}
