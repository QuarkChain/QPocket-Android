package com.quarkonium.qpocket.model.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CoinsActivity extends BaseActivity implements OnItemClickListener {

    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, CoinsActivity.class);
        activity.startActivity(intent);
    }

    private static class CoinsBean {
        String name;
        String key;
    }

    private class SettingsCoinsAdapter extends BaseQuickAdapter<CoinsBean, BaseViewHolder> {

        SettingsCoinsAdapter(int layoutResId, @Nullable List<CoinsBean> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder holder, CoinsBean item) {
            holder.setText(R.id.name, item.name);

            String key = item.key;
            View view = holder.getView(R.id.right_img);
            if (key.equals(mCurrentKey)) {
                view.setAlpha(1);
            } else {
                view.setAlpha(0);
            }

            if (holder.getAdapterPosition() == getItemCount() - 1) {
                holder.getView(R.id.line).setVisibility(View.GONE);
            } else {
                holder.getView(R.id.line).setVisibility(View.VISIBLE);
            }
        }
    }

    private SettingsCoinsAdapter mAdapter;
    private String mCurrentKey;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_settings_language;
    }

    @Override
    public int getActivityTitle() {
        return R.string.settings_coin;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.settings_coin);

        mAdapter = new SettingsCoinsAdapter(R.layout.holder_recycler_settings_languages_item, new ArrayList<>());
        mAdapter.setOnItemClickListener(this);
        RecyclerView recyclerView = findViewById(R.id.languages_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(mAdapter);

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentKey = SharedPreferencesUtils.getCurrentMarketCoin(getApplicationContext());

        String[] keys = Constant.MARKET_PRICE_COINS;
        String[] names = getResources().getStringArray(R.array.setings_coin_all_country);
        ArrayList<CoinsBean> models = new ArrayList<>();
        int size = keys.length;
        for (int i = 0; i < size; i++) {
            CoinsBean bean = new CoinsBean();
            bean.name = names[i];
            bean.key = keys[i];
            models.add(bean);
        }
        mAdapter.setNewInstance(models);
    }

    @Override
    public void onItemClick(@NotNull BaseQuickAdapter adapter, @NotNull View view, int position) {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }

        List<CoinsBean> models = mAdapter.getData();
        CoinsBean item = models.get(position);
        mCurrentKey = item.key;
        adapter.notifyDataSetChanged();
        SharedPreferencesUtils.setCurrentMarketCoin(getApplicationContext(), mCurrentKey);
//        UmengStatistics.changeLanguageClickCount(getApplicationContext(), mCurrentKey, QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
    }
}
