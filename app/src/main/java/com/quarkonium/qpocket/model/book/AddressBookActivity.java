package com.quarkonium.qpocket.model.book;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.address.table.QWAddressBook;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.model.book.viewmodel.AddressBookViewModel;
import com.quarkonium.qpocket.model.book.viewmodel.AddressBookViewModelFactory;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class AddressBookActivity extends BaseActivity implements OnItemClickListener {


    private class AddressBookAdapter extends BaseQuickAdapter<QWAddressBook, BaseViewHolder> {

        AddressBookAdapter(int layoutResId, @Nullable List<QWAddressBook> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder holder, QWAddressBook item) {
            ImageView imageView = holder.getView(R.id.address_book_icon);
            Glide.with(imageView)
                    .asBitmap()
                    .load(item.getIcon())
                    .into(imageView);

            holder.setText(R.id.address_book_name, item.getName());

            String address = item.getAddress();
            String value = getType(item.getCoinType()) + QWWalletUtils.parseAddressTo8Show(Keys.toChecksumHDAddress(address));
            holder.setText(R.id.address_book_address, value);


            View goWeb = holder.getView(R.id.address_book_copy);
            goWeb.setOnClickListener(v -> onCopy(address));

            View edit = holder.getView(R.id.address_book_edit);
            edit.setTag(item);
            edit.setOnClickListener(this::onEdit);
        }

        private String getType(int accountType) {
            switch (accountType) {
                case Constant.ACCOUNT_TYPE_TRX:
                    return "TRX: ";
                case Constant.ACCOUNT_TYPE_QKC:
                    return "QKC: ";
            }
            return "ETH: ";
        }

        private void onCopy(String address) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                String label = getString(R.string.wallet_copy_address_label);
                // 创建普通字符型ClipData
                ClipData mClipData = ClipData.newPlainText(label, address);
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);

                MyToast.showSingleToastShort(AddressBookActivity.this, R.string.copy_success);
            }
        }

        private void onEdit(View view) {
            QWAddressBook item = (QWAddressBook) view.getTag();
            AddressBookCreateActivity.startActivity(AddressBookActivity.this, item);
        }
    }


    private static final String ACCOUNT_TYPE = "account_type";
    private static final String SETTING_ACCOUNT_TYPE = "setting_account_type";
    public static final String ACCOUNT_ADDRESS = "account_address";

    public static void startActivity(Activity context, int accountType) {
        Intent intent = new Intent(context, AddressBookActivity.class);
        intent.putExtra(ACCOUNT_TYPE, accountType);
        context.startActivityForResult(intent, Constant.REQUEST_CODE_ADDRESS_BOOK);
    }

    public static void startSettingActivity(Activity context, int accountType) {
        Intent intent = new Intent(context, AddressBookActivity.class);
        intent.putExtra(SETTING_ACCOUNT_TYPE, accountType);
        context.startActivity(intent);
    }

    private View mEmptyView;

    private AddressBookAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private int mAccountType = -1;
    private int mSettingAccountType = Constant.ACCOUNT_TYPE_ETH;

    @Inject
    AddressBookViewModelFactory mAddressViewModelFactory;
    private AddressBookViewModel mAddressViewModel;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_address_book_layout;
    }

    @Override
    public int getActivityTitle() {
        return R.string.address_book_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

        mTopBarView.setTitle(R.string.address_book_title);
        mTopBarView.setRightImage(R.drawable.add_address);
        mTopBarView.setRightImageClickListener(v -> onAddBookClick());

        mEmptyView = findViewById(R.id.address_book_empty_layout);
        mRecyclerView = findViewById(R.id.address_book_rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mAdapter = new AddressBookAdapter(R.layout.address_book_item_layout, new ArrayList<>());
        mRecyclerView.setAdapter(mAdapter);

        findViewById(R.id.add_book_action).setOnClickListener(v -> onAddBookClick());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mAccountType = getIntent().getIntExtra(ACCOUNT_TYPE, -1);
        mSettingAccountType = getIntent().getIntExtra(SETTING_ACCOUNT_TYPE, Constant.ACCOUNT_TYPE_ETH);

        mAddressViewModel = new ViewModelProvider(this, mAddressViewModelFactory)
                .get(AddressBookViewModel.class);
        mAddressViewModel.addressBook().observe(this, this::addressBookSuccess);
        mAddressViewModel.feach(mAccountType);

        if (mAccountType != -1) {
            mAdapter.setOnItemClickListener(this);
        }
    }

    private void addressBookSuccess(List<QWAddressBook> list) {
        if (list == null || list.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);

            mAdapter.setNewInstance(list);
        }
    }

    private void onAddBookClick() {
        if (mAccountType != -1) {
            AddressBookCreateActivity.startActivity(this, mAccountType);
        } else {
            AddressBookCreateActivity.startSettingActivity(this, mSettingAccountType);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_ADDRESS_BOOK && resultCode == Activity.RESULT_OK) {
            mAddressViewModel.feach(mAccountType);
        }
    }

    @Override
    public void onItemClick(@NotNull BaseQuickAdapter adapter, @NotNull View view, int position) {
        QWAddressBook book = mAdapter.getData().get(position);
        Intent intent = getIntent();
        intent.putExtra(ACCOUNT_ADDRESS, book.getAddress());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
