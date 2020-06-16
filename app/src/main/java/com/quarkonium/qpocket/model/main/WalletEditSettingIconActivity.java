package com.quarkonium.qpocket.model.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.util.WalletIconUtils;
import com.quarkonium.qpocket.view.CircleImageView;
import com.quarkonium.qpocket.R;

import java.util.ArrayList;
import java.util.List;

//修改头像
public class WalletEditSettingIconActivity extends BaseActivity {

    private class WalletIconAdapter extends BaseQuickAdapter<Integer, BaseViewHolder> {

        WalletIconAdapter(int layoutId, List<Integer> datas) {
            super(layoutId, datas);
        }

        @Override
        public void convert(BaseViewHolder holder, Integer resId) {
            CircleImageView imageView = holder.getView(R.id.wallet_icon);
            imageView.setBorderWidth((int) UiUtils.dpToPixel(3));
            imageView.setImageResource(resId);
            imageView.setBorderOverlay(true);
            imageView.setOnClickListener((view) -> onChooseIcon(resId));

            if (WalletIconUtils.getResourcesUri(getApplicationContext(), resId).equals(mIconPath)) {
                imageView.setBorderColor(getResources().getColor(R.color.text_title));
            } else {
                imageView.setBorderColor(getResources().getColor(R.color.text_hint));
            }
        }

        private void onChooseIcon(int id) {
            String path = WalletIconUtils.getResourcesUri(getApplicationContext(), id);
            Intent intent = getIntent();
            intent.putExtra(Constant.KEY_WALLET_ICON_PATH, path);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private String mIconPath;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_edit_setting_icon;
    }

    @Override
    public int getActivityTitle() {
        return R.string.wallet_edit_icon;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mIconPath = getIntent().getStringExtra(Constant.KEY_WALLET_ICON_PATH);

        mTopBarView.setTitle(R.string.wallet_edit_icon);

        RecyclerView recyclerView = findViewById(R.id.change_icon_layout);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        List<Integer> list = new ArrayList<>();
        for (int id : Constant.WALLET_ICON_IDS) {
            list.add(id);
        }
        WalletIconAdapter adapter = new WalletIconAdapter(R.layout.holder_recycler_icon_item, list);
        recyclerView.setAdapter(adapter);
    }
}
