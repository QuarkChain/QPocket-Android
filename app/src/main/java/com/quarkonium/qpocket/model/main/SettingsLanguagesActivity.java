package com.quarkonium.qpocket.model.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.util.AppLanguageUtils;
import com.quarkonium.qpocket.util.ConstantLanguages;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.SharedPreferencesUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.MainApplication;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SettingsLanguagesActivity extends BaseActivity implements OnItemClickListener {

    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, SettingsLanguagesActivity.class);
        activity.startActivityForResult(intent, Constant.REQUEST_CODE_CHANGE_LANGUAGE);
    }

    private class SettingsLanguagesAdapter extends BaseQuickAdapter<HashMap<String, String>, BaseViewHolder> {

        SettingsLanguagesAdapter(int layoutResId, @Nullable List<HashMap<String, String>> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder holder, HashMap<String, String> item) {
            String name = item.get("name");
            holder.setText(R.id.name, name);

            String key = item.get("key");
            View view = holder.getView(R.id.right_img);
            if (TextUtils.equals(key, mCurrentKey)) {
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

    private final static String DEFAULT_LANGUAGES_KEY = "language_key";

    private SettingsLanguagesAdapter mAdapter;
    private String mCurrentKey;
    private String mDefaultKey;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_settings_language;
    }

    @Override
    public int getActivityTitle() {
        return R.string.settings_languages_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

        mTopBarView.setTitle(R.string.settings_languages_title);
        mTopBarView.setRightText(R.string.settings_languages_save);
        mTopBarView.setRightTextClickListener(this::onSave);

        mAdapter = new SettingsLanguagesAdapter(R.layout.holder_recycler_settings_languages_item, new ArrayList<>());
        mAdapter.setOnItemClickListener(this);

        RecyclerView recyclerView = findViewById(R.id.languages_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(mAdapter);

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentKey = SharedPreferencesUtils.getCurrentLanguages(getApplicationContext());
        mDefaultKey = getIntent().getStringExtra(DEFAULT_LANGUAGES_KEY);
        if (TextUtils.isEmpty(mDefaultKey)) {
            mDefaultKey = mCurrentKey;
        }

        String[] names = getResources().getStringArray(R.array.settings_languages);
        String[] keys = ConstantLanguages.LANGUAGES;
        ArrayList<HashMap<String, String>> models = new ArrayList<>();
        int size = names.length;
        for (int i = 0; i < size; i++) {
            HashMap<String, String> model = new HashMap<>();
            model.put("name", names[i]);
            model.put("key", keys[i]);
            models.add(model);
        }
        mAdapter.setNewInstance(models);
    }

    @Override
    public void onItemClick(@NotNull BaseQuickAdapter adapter, @NotNull View view, int position) {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }

        List<HashMap<String, String>> models = mAdapter.getData();
        HashMap<String, String> item = models.get(position);
        mCurrentKey = item.get("key");
        adapter.notifyDataSetChanged();

        UmengStatistics.changeLanguageClickCount(getApplicationContext(), mCurrentKey, QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
    }

    //保存
    public void onSave(View view) {
        String key = SharedPreferencesUtils.getCurrentLanguages(getApplicationContext());
        if (mCurrentKey.equals(key)) {
            return;
        }
        onChangeAppLanguage(mCurrentKey);

        UmengStatistics.topBarLanguageSaveClickCount(getApplicationContext(), QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
    }

    private void onChangeAppLanguage(String newLanguage) {
        SharedPreferencesUtils.setCurrentLanguages(getApplicationContext(), newLanguage);

        //切换applaction语言
        AppLanguageUtils.applyChange(MainApplication.getContext());

        //重启activity 解决部分手机闪烁
        Intent intent = new Intent(this, SettingsLanguagesActivity.class);
        intent.putExtra(DEFAULT_LANGUAGES_KEY, mDefaultKey);
        startActivity(intent);
        overridePendingTransition(0, 0);

        new WebView(this).destroy();
        super.finish();
    }

    @Override
    public void finish() {
        if (!TextUtils.equals(mDefaultKey, mCurrentKey)) {
            Constant.sIsChangeLanguage = true;
        }
        super.finish();
    }
}
