package com.quarkonium.qpocket.model.wallet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWWalletDao;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.view.BannerIndicator;
import com.quarkonium.qpocket.view.ScrollGallery;
import com.quarkonium.qpocket.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class HomeActivity extends BaseActivity {

    private static class HomeAdapter extends PagerAdapter {

        private WeakReference<Activity> mActivity;
        private ArrayList<Integer> mData = new ArrayList<>();
        private String[] mTitles;
        private String[] mMessages;

        private boolean isFromManager;

        HomeAdapter(Activity activity, boolean isFromManager) {
            mActivity = new WeakReference<>(activity);
            this.isFromManager = isFromManager;

            if (!isFromManager) {
                mData.add(R.drawable.home_create_logo1);
                mData.add(R.drawable.home_create_logo3);
                mData.add(R.drawable.home_create_logo2);
            } else {
                mData.add(R.drawable.home_create_logo_manager);
            }

            mTitles = activity.getResources().getStringArray(R.array.create_wallet_titles);
            mMessages = activity.getResources().getStringArray(R.array.create_wallet_messages);
        }

        @Override
        public int getCount() {
            return mData.size() > 1 ? Integer.MAX_VALUE : 1;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, final int position) {
            int p = calIndex(position);
            Activity activity = mActivity.get();
            if (activity != null) {
                View itemView = LayoutInflater.from(activity).inflate(R.layout.home_bananer_item, container, false);
                ImageView imageView = itemView.findViewById(R.id.icon);
                int resId = mData.get(p);
                Glide.with(activity)
                        .asBitmap()
                        .load(resId)
                        .into(imageView);

                TextView title = itemView.findViewById(R.id.create_title);
                title.setText(mTitles[p]);

                TextView message = itemView.findViewById(R.id.create_message);
                message.setText(mMessages[p]);

                if (isFromManager) {
                    title.setVisibility(View.INVISIBLE);
                    message.setVisibility(View.INVISIBLE);
                }

                container.addView(itemView);
                return itemView;
            }
            return container;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        int calIndex(int position) {
            int size = mData.size();
            if (size == 0) {
                return 0;
            }
            return position % size;
        }
    }

    protected void initStatusBar() {
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window win = getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
            // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
            win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            // 部分机型的statusbar会有半透明的黑色背景
            win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_home;
    }

    @Override
    public int getActivityTitle() {
        return R.string.home;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        boolean isFromManager = getIntent().getBooleanExtra(Constant.FROM_WALLET_MANAGER, false);

        View back = findViewById(R.id.re_take);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(v -> finish());

        View mCreateWallet = findViewById(R.id.new_account_action);
        mCreateWallet.setOnClickListener((v) -> createWallet());
        View mExportWallet = findViewById(R.id.import_account_action);
        mExportWallet.setOnClickListener((v) -> exportWallet());

        BannerIndicator bannerIndicator = findViewById(R.id.banner_indicator);
        if (isFromManager) {
            bannerIndicator.setVisibility(View.GONE);
        } else {
            bannerIndicator.setCount(3);
        }

        ScrollGallery viewPager = findViewById(R.id.create_home_view_page);
        viewPager.setAdapter(new HomeAdapter(this, isFromManager));
        if (!isFromManager) {
            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int i, float v, int i2) {

                }

                @Override
                public void onPageSelected(int i) {
                    bannerIndicator.setCurrentItem(i % 3);
                }

                @Override
                public void onPageScrollStateChanged(int i) {

                }
            });
            viewPager.setAutoScroll(true);
            viewPager.setCurrentItem(Integer.MAX_VALUE / 2);
        }
    }

    private boolean hasWallet() {
        QWWalletDao dao = new QWWalletDao(getApplication());
        List<QWWallet> list = dao.queryAll();
        return list != null && !list.isEmpty();
    }

    //创建钱包
    private void createWallet() {
        Intent intent = new Intent(this, CreateWalletActivity.class);
        startActivity(intent);
    }

    //导入钱包
    private void exportWallet() {
        Intent intent = new Intent(this, ImportWalletActivity.class);
        startActivity(intent);
    }
}
