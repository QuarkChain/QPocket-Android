package com.quarkonium.qpocket.base;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quarkonium.qpocket.util.AppLanguageUtils;
import com.quarkonium.qpocket.util.ConstantLanguages;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.quarkonium.qpocket.view.TopBarView;
import com.tendcloud.tenddata.TCAgent;


public abstract class BaseActivity extends AppCompatActivity {

    protected abstract int getLayoutResource();

    public abstract int getActivityTitle();

    protected abstract void onInitialization(Bundle bundle);

    private String mTitle;
    protected TopBarView mTopBarView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initStatusBar();
        super.onCreate(savedInstanceState);
        if (getLayoutResource() != 0) {
            setContentView(getLayoutResource());
        }

        View view = findViewById(R.id.top_layout);
        if (view instanceof TopBarView) {
            mTopBarView = (TopBarView) view;
        }
        this.onInitialization(savedInstanceState);
        if (mTopBarView != null) {
            mTopBarView.setBackClickListener(v -> finish());
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        String language = SharedPreferencesUtils.getCurrentLanguages(newBase);
        if (ConstantLanguages.AUTO.equals(language)) {
            AppLanguageUtils.applyChange(newBase);
            super.attachBaseContext(newBase);
            return;
        }
        super.attachBaseContext(AppLanguageUtils.attachBaseContext(newBase, language));
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 初始化状态栏相关，
     * PS: 设置全屏需要在调用super.onCreate(arg0);之前设置setIsFullScreen(true);否则在Android 6.0下非全屏的activity会出错;
     * SDK19：可以设置状态栏透明，但是半透明的SYSTEM_BAR_BACKGROUNDS会不好看；
     * SDK21：可以设置状态栏颜色，并且可以清除SYSTEM_BAR_BACKGROUNDS，但是不能设置状态栏字体颜色（默认的白色字体在浅色背景下看不清楚）；
     * SDK23：可以设置状态栏为浅色（SYSTEM_UI_FLAG_LIGHT_STATUS_BAR），字体就回反转为黑色。
     * 为兼容目前效果，仅在SDK23才显示沉浸式。
     */
    protected void initStatusBar() {
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window win = getWindow();
            win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
            win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //设置状态栏颜色
            win.setStatusBarColor(Color.WHITE);
//            //设置导航栏颜色
//            win.setNavigationBarColor(Color.BLACK);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(mTitle)) {
            TCAgent.onPageStart(this, mTitle);
            UmengStatistics.onPageStart(mTitle);
        } else {
            String title = getTitleString();
            if (!TextUtils.isEmpty(title)) {
                TCAgent.onPageStart(this, title);
                UmengStatistics.onPageStart(title);
            }
        }

        UmengStatistics.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!TextUtils.isEmpty(mTitle)) {
            TCAgent.onPageEnd(this, mTitle);
            UmengStatistics.onPageEnd(mTitle);
        } else {
            String title = getTitleString();
            if (!TextUtils.isEmpty(title)) {
                TCAgent.onPageEnd(this, title);
                UmengStatistics.onPageEnd(title);
            }
        }

        UmengStatistics.onPause(this);
    }

    private String getTitleString() {
        return getActivityTitle() == 0 ? "" : getResources().getString(getActivityTitle());
    }

    protected void setTitleString(String titleString) {
        mTitle = titleString;
    }
}
