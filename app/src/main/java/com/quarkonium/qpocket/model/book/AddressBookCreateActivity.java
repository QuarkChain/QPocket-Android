package com.quarkonium.qpocket.model.book;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.address.dao.QWAddressBookDao;
import com.quarkonium.qpocket.api.db.address.table.QWAddressBook;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.WalletUtils;
import com.quarkonium.qpocket.model.main.CaptureActivity;
import com.quarkonium.qpocket.model.permission.PermissionHelper;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.ToolUtils;
import com.quarkonium.qpocket.util.WalletIconUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.QuarkSDKDialog;
import com.quarkonium.qpocket.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.disposables.Disposable;

public class AddressBookCreateActivity extends BaseActivity {
    private class MyTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            check();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private static final String ACCOUNT_TYPE = "account_type";
    private static final String SETTING_ACCOUNT_TYPE = "setting_account_type";
    public static final String ADDRESS_BOOK = "address_book";

    public static void startActivity(Activity context, QWAddressBook book) {
        Intent intent = new Intent(context, AddressBookCreateActivity.class);
        intent.putExtra(ADDRESS_BOOK, book);
        context.startActivityForResult(intent, Constant.REQUEST_CODE_ADDRESS_BOOK);
    }

    public static void startActivity(Activity context, int accountType) {
        Intent intent = new Intent(context, AddressBookCreateActivity.class);
        intent.putExtra(ACCOUNT_TYPE, accountType);
        context.startActivityForResult(intent, Constant.REQUEST_CODE_ADDRESS_BOOK);
    }

    public static void startSettingActivity(Activity context, int accountType) {
        Intent intent = new Intent(context, AddressBookCreateActivity.class);
        intent.putExtra(SETTING_ACCOUNT_TYPE, accountType);
        context.startActivityForResult(intent, Constant.REQUEST_CODE_ADDRESS_BOOK);
    }

    private TextView mNetworkView;
    private EditText mNameView;
    private EditText mAddressView;

    private View mDeleteView;
    private TextView mActionView;

    private int mSettingAccountType;
    private int mAccountType;
    private QWAddressBook mBook;

    private PopupWindow mMenuPopWindow;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_address_book_create_layout;
    }

    @Override
    public int getActivityTitle() {
        return R.string.address_book_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {

        mTopBarView.setTitle(R.string.address_book_add);
        mTopBarView.setRightText(R.string.delete);
        mDeleteView = mTopBarView.getRightTextView();
        mDeleteView.setVisibility(View.GONE);

        mNetworkView = findViewById(R.id.address_book_network);
        mNameView = findViewById(R.id.address_book_name);
        mNameView.addTextChangedListener(new MyTextWatcher());
        mAddressView = findViewById(R.id.address_book_address);
        mAddressView.addTextChangedListener(new MyTextWatcher());

        mActionView = findViewById(R.id.add_book_action);
        mActionView.setEnabled(false);
        mActionView.setOnClickListener(v -> onClickAction());

        findViewById(R.id.address_book_scan).setOnClickListener(v -> onScan());

        updateUI();
    }

    private void check() {
        if (TextUtils.isEmpty(mNameView.getText()) || TextUtils.isEmpty(mAddressView.getText())) {
            mActionView.setEnabled(false);
        } else {
            mActionView.setEnabled(true);
        }
    }

    private void updateUI() {
        mBook = getIntent().getParcelableExtra(ADDRESS_BOOK);
        mAccountType = getIntent().getIntExtra(ACCOUNT_TYPE, -1);
        mSettingAccountType = getIntent().getIntExtra(SETTING_ACCOUNT_TYPE, -1);
        if (mBook != null) {
            //编辑
            mActionView.setText(R.string.address_book_complete);
            mNetworkView.setText(getCoin(mBook.getCoinType()));

            mNameView.setText(mBook.getName());
            mAddressView.setText(mBook.getAddress());

            mDeleteView.setVisibility(View.VISIBLE);
            mDeleteView.setOnClickListener(v -> onDelete());
        } else if (mSettingAccountType != -1) {
            //从设置界面过来
            mNetworkView.setText(getCoin(mSettingAccountType));
            Drawable dra = getResources().getDrawable(R.drawable.address_book_network_drop);
            dra.setBounds(0, 0, dra.getIntrinsicWidth(), dra.getIntrinsicHeight());
            mNetworkView.setCompoundDrawables(null, null, dra, null);
            mNetworkView.setOnClickListener(v -> onSwitchNetwork());
        } else {
            //从转账界面过来
            mNetworkView.setText(getCoin(mAccountType));
        }
    }

    //*****************点击事件*****************
    //选择网络
    private void onSwitchNetwork() {
        if (mMenuPopWindow != null) {
            if (mMenuPopWindow.isShowing()) {
                mMenuPopWindow.dismiss();
            }
            mMenuPopWindow = null;
        }

        View contentView = View.inflate(getApplicationContext(), R.layout.import_token_menu_layout, null);
        mMenuPopWindow = new PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);

        //********QKC********
        ViewGroup qkc = contentView.findViewById(R.id.symbol_qkc);
        qkc.setOnClickListener((View v) -> {
            mMenuPopWindow.dismiss();
            mNetworkView.setText(R.string.qkc);
        });

        //***********ETH***********
        ViewGroup eth = contentView.findViewById(R.id.symbol_eth);
        eth.setOnClickListener((View v) -> {
            mMenuPopWindow.dismiss();
            mNetworkView.setText(R.string.eth);
        });

        //***********TRX***********
        ViewGroup trx = contentView.findViewById(R.id.symbol_trx);
        trx.setOnClickListener((View v) -> {
            mMenuPopWindow.dismiss();
            mNetworkView.setText(R.string.trx);
        });

        String text = mNetworkView.getText().toString();
        switch (text) {
            case "QKC":
                qkc.getChildAt(1).setVisibility(View.VISIBLE);
                eth.getChildAt(1).setVisibility(View.GONE);
                trx.getChildAt(1).setVisibility(View.GONE);
                break;
            case "ETH":
                qkc.getChildAt(1).setVisibility(View.GONE);
                eth.getChildAt(1).setVisibility(View.VISIBLE);
                trx.getChildAt(1).setVisibility(View.GONE);
                break;
            case "TRX":
                qkc.getChildAt(1).setVisibility(View.GONE);
                eth.getChildAt(1).setVisibility(View.GONE);
                trx.getChildAt(1).setVisibility(View.VISIBLE);
                break;
            default:
                qkc.getChildAt(1).setVisibility(View.GONE);
                eth.getChildAt(1).setVisibility(View.GONE);
                trx.getChildAt(1).setVisibility(View.GONE);
                break;
        }

        mMenuPopWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mMenuPopWindow.setTouchable(true);
        mMenuPopWindow.setTouchInterceptor((View v, MotionEvent event) -> false);
        showPopWindowPos(mNetworkView);
    }

    public void showPopWindowPos(final View anchorView) {
        final int windowPos[] = new int[2];
        final int anchorLoc[] = new int[2];
        // 获取锚点View在屏幕上的左上角坐标位置
        anchorView.getLocationOnScreen(anchorLoc);
        final int anchorHeight = anchorView.getHeight();
        // 判断需要向上弹出还是向下弹出显示
        windowPos[0] = anchorLoc[0];
        windowPos[1] = anchorLoc[1] + anchorHeight;
        mMenuPopWindow.showAtLocation(anchorView, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);
    }

    //添加地址簿
    private void onClickAction() {
        String address = mAddressView.getText().toString().trim();
        int accountType = getAccountType();
        switch (accountType) {
            case Constant.ACCOUNT_TYPE_ETH:
                if (!WalletUtils.isValidAddress(address)) {
                    MyToast.showSingleToastShort(this, getErrorText(accountType));
                    return;
                }
                break;
            case Constant.ACCOUNT_TYPE_QKC:
                if (!QWWalletUtils.isQKCValidAddress(address)) {
                    MyToast.showSingleToastShort(this, getErrorText(accountType));
                    return;
                }
                break;
            case Constant.ACCOUNT_TYPE_TRX:
                if (!TronWalletClient.isTronAddressValid(address)) {
                    MyToast.showSingleToastShort(this, getErrorText(accountType));
                    return;
                }
                break;
        }

        if (mBook != null) {
            modifyBook(address);
        } else {
            insertBook(accountType, address);
        }
    }

    private void insertBook(int accountType, String address) {
        QWAddressBook book = new QWAddressBook();
        book.setCoinType(accountType);
        book.setAddress(address);

        book.setName(mNameView.getText().toString().trim());
        switch (accountType) {
            case Constant.ACCOUNT_TYPE_ETH:
                book.setIcon(WalletIconUtils.getResourcesUri(getApplicationContext(), R.drawable.wallet_switch_eth_selected));
                break;
            case Constant.ACCOUNT_TYPE_QKC:
                book.setIcon(WalletIconUtils.getResourcesUri(getApplicationContext(), R.drawable.wallet_switch_qkc_selected));
                break;
            case Constant.ACCOUNT_TYPE_TRX:
                book.setIcon(WalletIconUtils.getResourcesUri(getApplicationContext(), R.drawable.wallet_switch_trx_selected));
                break;
        }


        QWAddressBookDao dao = new QWAddressBookDao(getApplication());
        dao.insert(book);
        MyToast.showSingleToastShort(this, R.string.add_book_success);
        finish();
    }

    private void modifyBook(String address) {
        mBook.setAddress(address);
        mBook.setName(mNameView.getText().toString().trim());

        QWAddressBookDao dao = new QWAddressBookDao(getApplication());
        dao.update(mBook);
        MyToast.showSingleToastShort(this, R.string.modify_book_success);

        finish();
    }

    private void onDelete() {
        QuarkSDKDialog dialog = new QuarkSDKDialog(this);
        dialog.setMessage(R.string.delete_book_tips);
        dialog.setNegativeBtn(R.string.cancel, v -> dialog.dismiss());
        dialog.setPositiveBtn(R.string.ok, v -> {
            QWAddressBookDao dao = new QWAddressBookDao(getApplication());
            dao.remove(mBook);
            MyToast.showSingleToastShort(this, R.string.delete_book_success);
            dialog.dismiss();

            finish();
        });
        dialog.show();
    }

    private void onScan() {
        if (ToolUtils.isFastDoubleClick()) {
            return;
        }

        final RxPermissions rxPermissions = new RxPermissions(this);
        if (!rxPermissions.isGranted(Manifest.permission.CAMERA)) {
            Disposable disposable = rxPermissions.request(Manifest.permission.CAMERA)
                    .subscribe(aBoolean -> {
                        if (aBoolean) {
                            startCamera();
                        } else {
                            String[] name = {Manifest.permission.CAMERA};
                            MyToast.showSingleToastLong(this, PermissionHelper.getPermissionToast(getApplicationContext(), name));
                        }
                    });
            return;
        }

        startCamera();
    }

    private void startCamera() {
        int accountType = getAccountType();
        CaptureActivity.startForResultActivity(this, accountType);
    }

    @Override
    public void finish() {
        setResult(Activity.RESULT_OK);
        super.finish();
    }

    private int getAccountType() {
        String type = mNetworkView.getText().toString().trim();
        switch (type) {
            case "TRX":
                return Constant.ACCOUNT_TYPE_TRX;
            case "QKC":
                return Constant.ACCOUNT_TYPE_QKC;
        }
        return Constant.ACCOUNT_TYPE_ETH;
    }

    private String getCoin(int type) {
        switch (type) {
            case Constant.ACCOUNT_TYPE_ETH:
                return "ETH";
            case Constant.ACCOUNT_TYPE_TRX:
                return "TRX";
        }
        return "QKC";
    }

    private String getErrorText(int accountType) {
        int name = R.string.import_wallet_qkc;
        switch (accountType) {
            case Constant.ACCOUNT_TYPE_ETH:
                name = R.string.import_wallet_eth;
                break;
            case Constant.ACCOUNT_TYPE_TRX:
                name = R.string.import_wallet_trx;
                break;
        }
        return String.format(getString(R.string.import_wallet_fail_watch), getString(name));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Constant.REQUEST_CODE_CAPTURE == requestCode && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String address = data.getStringExtra(Constant.WALLET_ADDRESS);
                mAddressView.setText(address);
            }
        }
    }
}
