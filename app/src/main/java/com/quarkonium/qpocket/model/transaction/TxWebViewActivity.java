package com.quarkonium.qpocket.model.transaction;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.AgentWebSettingsImpl;
import com.just.agentweb.DefaultWebClient;
import com.just.agentweb.IAgentWebSettings;
import com.just.agentweb.MiddlewareWebChromeBase;
import com.just.agentweb.MiddlewareWebClientBase;
import com.just.agentweb.PermissionInterceptor;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.view.TopBarView2;

import org.conscrypt.OpenSSLProvider;

import java.security.Security;

import io.reactivex.disposables.Disposable;

public class TxWebViewActivity extends BaseActivity {

    protected PermissionInterceptor mPermissionInterceptor = new PermissionInterceptor() {

        /**
         * PermissionInterceptor 能达到 url1 允许授权， url2 拒绝授权的效果。
         * @return true 该Url对应页面请求权限进行拦截 ，false 表示不拦截。
         */
        @Override
        public boolean intercept(String url, String[] permissions, String action) {
            return false;
        }
    };


    public static void startActivity(Activity activity, String address, String hash) {
        if (TextUtils.isEmpty(address)) {
            return;
        }
        String url = "";
        if (WalletUtils.isValidAddress(address)) {
            if (Constant.sETHNetworkId == Constant.ETH_PUBLIC_PATH_ROPSTEN_INDEX) {
                url = Constant.TX_ETH_ROPSTEN_HASH_PATH + hash;
            } else if (Constant.sETHNetworkId == Constant.ETH_PUBLIC_PATH_KOVAN_INDEX) {
                url = Constant.TX_ETH_KOVAN_HASH_PATH + hash;
            } else if (Constant.sETHNetworkId == Constant.ETH_PUBLIC_PATH_RINKBY_INDEX) {
                url = Constant.TX_ETH_RINKEBY_HASH_PATH + hash;
            } else {
                url = Constant.TX_ETH_HASH_PATH + hash;
            }
        } else if (QWWalletUtils.isQKCValidAddress(address)) {
            if (Constant.sNetworkId.intValue() == Constant.QKC_PUBLIC_DEVNET_INDEX) {
                url = Constant.TX_QKC_HASH_DEVNET_PATH + hash;
            } else {
                url = Constant.TX_QKC_HASH_PATH + hash;
            }
        } else if (TronWalletClient.isTronAddressValid(address)) {
            url = Constant.TX_TRX_HASH_PATH + hash;
        }
        Intent intent = new Intent(activity, TxWebViewActivity.class);
        intent.putExtra(HASH_URL, url);
        activity.startActivity(intent);
    }

    public static void startActivity(Activity activity, String url) {
        Intent intent = new Intent(activity, TxWebViewActivity.class);
        intent.putExtra(HASH_URL, url);
        intent.putExtra(ONLY_SHOW_TEXT, true);
        activity.startActivity(intent);
    }

    public static void startActivityAirdrop(Activity activity, String url) {
        Intent intent = new Intent(activity, TxWebViewActivity.class);
        intent.putExtra(HASH_URL, url);
        intent.putExtra(ONLY_SHOW_TEXT, true);
        intent.putExtra(AIRDROPS, true);
        activity.startActivity(intent);
    }

    public static void startActivity(Context context, String url) {
        Intent intent = new Intent(context, TxWebViewActivity.class);
        intent.putExtra(HASH_URL, url);
        intent.putExtra(ONLY_SHOW_TEXT, true);
        context.startActivity(intent);
    }

    private static final String HASH_URL = "hash_url";
    private static final String ONLY_SHOW_TEXT = "only_show_text";
    private static final String AIRDROPS = "airdrops";

    private TextView mTitleView;
    private PopupWindow mMenuPopWindow;
    private AgentWeb mAgentWeb;

    private Disposable mDisposable;

    private boolean isOnlyShowText;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_dapp_webview;
    }

    @Override
    public int getActivityTitle() {
        return R.string.web_view_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        TopBarView2 mTopBarView = findViewById(R.id.top_layout);

        mTopBarView.setHomeClickListener(v -> finish());
        mTopBarView.getBackView().setOnClickListener(v -> {
            // true表示AgentWeb处理了该事件
            if (mAgentWeb == null || !mAgentWeb.back()) {
                TxWebViewActivity.this.finish();
            }
        });
        mTopBarView.setRightImageClickListener(v -> {
            // 刷新
            if (mAgentWeb != null) {
                mAgentWeb.getUrlLoader().reload(); // 刷新
            }
        });
        mTopBarView.setRight2ImageClickListener(this::openMenu);

        mTitleView = mTopBarView.getTitleView();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isOnlyShowText = getIntent().getBooleanExtra(ONLY_SHOW_TEXT, false);
        buildAgentWeb();
    }

    @Override
    protected void onPause() {
        if (mAgentWeb != null) {
            mAgentWeb.getWebLifeCycle().onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mAgentWeb != null) {
            mAgentWeb.getWebLifeCycle().onResume();
        }
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAgentWeb != null && mAgentWeb.handleKeyEvent(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void buildAgentWeb() {
        String path = getIntent().getStringExtra(HASH_URL);
        //修复android5.0以下访问网络失败
        if (Build.VERSION.SDK_INT < 21 && Security.getProvider("Okhttp") == null) {
            Security.insertProviderAt(new OpenSSLProvider("Okhttp"), 1);
        }

        //创建AgentWeb ，注意创建AgentWeb的时候应该使用加入SonicWebViewClient中间件
        View errorView = LayoutInflater.from(this).inflate(R.layout.web_error_page, null, false);
        TextView textView = errorView.findViewById(R.id.web_error_title);
        textView.setText(String.format(getString(R.string.web_error_title), ""));
        mAgentWeb = AgentWeb.with(this)//
                .setAgentWebParent(findViewById(R.id.web_view_layout), -1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))//传入AgentWeb的父控件。
                .useDefaultIndicator(getResources().getColor(R.color.text_title), 3)//设置进度条颜色与高度，-1为默认值，高度为2，单位为dp。
                .setAgentWebWebSettings(AgentWebSettingsImpl.getInstance())//设置 IAgentWebSettings。
                .setWebViewClient(new WebViewClient())//WebViewClient ， 与 WebView 使用一致 ，但是请勿获取WebView调用setWebViewClient(xx)方法了,会覆盖AgentWeb DefaultWebClient,同时相应的中间件也会失效。
                .setWebChromeClient(new WebChromeClient()) //WebChromeClient
                .setPermissionInterceptor(mPermissionInterceptor) //权限拦截 2.0.0 加入。
                .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK) //严格模式 Android 4.2.2 以下会放弃注入对象 ，使用AgentWebView没影响。
//                .setAgentWebUIController(new UIController(getActivity())) //自定义UI  AgentWeb3.0.0 加入。
                .setMainFrameErrorView(errorView) //参数1是错误显示的布局，参数2点击刷新控件ID -1表示点击整个布局都刷新， AgentWeb 3.0.0 加入。
                .useMiddlewareWebChrome(new MiddlewareWebChromeBase() {
                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        mTitleView.setText(title);
                    }
                }) //设置WebChromeClient中间件，支持多个WebChromeClient，AgentWeb 3.0.0 加入。
                .useMiddlewareWebClient(new MiddlewareWebClientBase() {
                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                        if (TxWebViewActivity.this.isFinishing()) {
                            return;
                        }
                        if (error.getPrimaryError() == SslError.SSL_IDMISMATCH && ToolUtils.checkSslErrorForIDMISMATCH(error)) {
                            handler.proceed();
                            return;
                        }
                        String message = view.getContext().getResources().getString(R.string.ssl_error_message);
                        message = String.format(message, view.getUrl());
                        final QuarkSDKDialog dialog = new QuarkSDKDialog(TxWebViewActivity.this);
                        dialog.setTitle(R.string.ssl_error_title);
                        dialog.setMessage(message);
                        dialog.setNegativeBtn(R.string.ssl_error_cancel, v -> {
                            // Android默认的处理方式
                            handler.cancel();
                            dialog.dismiss();
                        });
                        dialog.setPositiveBtn(R.string.ssl_error_ok, v -> {
                            // 接受所有网站的证书
                            handler.proceed();
                            dialog.dismiss();
                        });
                        dialog.show();
                    }

                    @Override
                    public void onLoadResource(WebView view, String url) {
                        super.onLoadResource(view, url);
                        if (isOnlyShowText) {
                            view.loadUrl("javascript:(function() { " +
                                    "document.body.removeChild(document.getElementsByClassName(\"bottom-down\")[0]); " +
                                    "})()");
                        }
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        if (isOnlyShowText) {
                            view.loadUrl("javascript:(function() { " +
                                    "document.body.removeChild(document.getElementsByClassName(\"bottom-down\")[0]); " +
                                    "})()");
                        }
                    }
                }) //设置WebViewClient中间件，支持多个WebViewClient， AgentWeb 3.0.0 加入。
                .setOpenOtherPageWays(DefaultWebClient.OpenOtherPageWays.DISALLOW)//打开其他页面时，弹窗质询用户前往其他应用 AgentWeb 3.0.0 加入。
                .interceptUnkownUrl() //拦截找不到相关页面的Url AgentWeb 3.0.0 加入。
                .createAgentWeb()
                .ready()
                .go(path);//创建AgentWeb。

        IAgentWebSettings settings = mAgentWeb.getAgentWebSettings();
        //自适应屏幕
        settings.getWebSettings().setUseWideViewPort(true);//设置webview推荐使用的窗口，使html界面自适应屏幕
        settings.getWebSettings().setLoadWithOverviewMode(true);//缩放至屏幕的大小
        //缓存模式
        settings.getWebSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        // AgentWeb 没有把WebView的功能全面覆盖 ，所以某些设置 AgentWeb 没有提供 ， 请从WebView方面入手设置。
        mAgentWeb.getWebCreator().getWebView().setOverScrollMode(WebView.OVER_SCROLL_NEVER);
    }


    private void openMenu(View view) {
        if (mMenuPopWindow != null) {
            if (mMenuPopWindow.isShowing()) {
                mMenuPopWindow.dismiss();
            }
            mMenuPopWindow = null;
        }

        View contentView = View.inflate(this, R.layout.store_menu_layout, null);
        mMenuPopWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        //********分享地址********
        View replace = contentView.findViewById(R.id.menu_share);
        replace.setOnClickListener(v -> {
            mMenuPopWindow.dismiss();
            share();
        });

        //***********拷贝地址***********
        View saveFilter = contentView.findViewById(R.id.menu_copy);
        saveFilter.setOnClickListener(v -> {
            mMenuPopWindow.dismiss();
            copy();
        });

        //***********在浏览器中打开***********
        View WatermarkEditMenu = contentView.findViewById(R.id.menu_open);
        WatermarkEditMenu.setOnClickListener(v -> {
            mMenuPopWindow.dismiss();
            openLink();
        });

        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mMenuPopWindow.setAnimationStyle(R.style.PopMoveAnimStyle);
        mMenuPopWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mMenuPopWindow.setTouchable(true);
        mMenuPopWindow.setTouchInterceptor((View v, MotionEvent event) -> false);

        int popupWidth = contentView.getMeasuredWidth();
        int width = (int) (getResources().getDisplayMetrics().widthPixels - UiUtils.dpToPixel(10)) - popupWidth;
        int height = (int) (getStatusBarHeight() + getResources().getDimension(R.dimen.appbar_top_height) + UiUtils.dpToPixel(5));
        mMenuPopWindow.showAtLocation(view, Gravity.NO_GRAVITY, width, height);
    }

    /**
     * 分享
     */
    public void share() {
        if (mAgentWeb != null) {
            String extraText = mAgentWeb.getWebCreator().getWebView().getUrl();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.menu_share_url));
            intent.putExtra(Intent.EXTRA_TEXT, extraText);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, getString(R.string.menu_share_url)));
        }
    }

    /**
     * 实现文本复制功能
     */
    public void copy() {
        if (mAgentWeb != null) {
            String content = mAgentWeb.getWebCreator().getWebView().getUrl();
            // 得到剪贴板管理器
            ClipboardManager cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cmb != null) {
                String label = getString(R.string.main_menu_tag_dapp);
                // 创建普通字符型ClipData
                ClipData mClipData = ClipData.newPlainText(label, content);
                // 将ClipData内容放到系统剪贴板里。
                cmb.setPrimaryClip(mClipData);

                MyToast.showSingleToastShort(this, R.string.copy_success);
            }
        }
    }

    /**
     * 使用浏览器打开链接
     */
    public void openLink() {
        if (mAgentWeb != null) {
            String content = mAgentWeb.getWebCreator().getWebView().getUrl();
            if (!TextUtils.isEmpty(content) && content.startsWith("http")) {
                Uri issuesUrl = Uri.parse(content);
                Intent intent = new Intent(Intent.ACTION_VIEW, issuesUrl);
                startActivity(intent);
            }
        }
    }


    @Override
    protected void onDestroy() {
        if (mDisposable != null && mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
        mDisposable = null;
        super.onDestroy();
    }
}
