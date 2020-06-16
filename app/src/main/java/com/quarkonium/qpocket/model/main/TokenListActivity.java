package com.quarkonium.qpocket.model.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.quarkonium.qpocket.api.Constant;
import com.quarkonium.qpocket.api.db.dao.QWTokenDao;
import com.quarkonium.qpocket.api.db.dao.QWWalletTokenDao;
import com.quarkonium.qpocket.api.db.table.QWToken;
import com.quarkonium.qpocket.api.db.table.QWWallet;
import com.quarkonium.qpocket.api.db.table.QWWalletToken;
import com.quarkonium.qpocket.base.BaseActivity;
import com.quarkonium.qpocket.crypto.Keys;
import com.quarkonium.qpocket.model.main.viewmodel.MainWallerViewModel;
import com.quarkonium.qpocket.model.main.viewmodel.MainWalletViewModelFactory;
import com.quarkonium.qpocket.tron.TronWalletClient;
import com.quarkonium.qpocket.util.ConnectionUtil;
import com.quarkonium.qpocket.util.QWWalletUtils;
import com.quarkonium.qpocket.util.UiUtils;
import com.quarkonium.qpocket.view.MyToast;
import com.quarkonium.qpocket.view.SwipeMenuLayout;
import com.quarkonium.qpocket.R;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.quarkonium.qpocket.view.EmptyRecyclerView;
import com.quarkonium.qpocket.view.SwitchButton;
import com.xdandroid.materialprogressview.MaterialProgressView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class TokenListActivity extends BaseActivity {

    private class MyAddressTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mSearchListAdapter.setNewInstance(new ArrayList<>());
            if (TextUtils.isEmpty(s)) {
                mEditRemoveView.setVisibility(View.GONE);
            } else {
                mEditRemoveView.setVisibility(View.VISIBLE);
                search(s.toString().trim());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class TokenListAdapter extends BaseQuickAdapter<QWToken, BaseViewHolder> {

        TokenListAdapter(int layoutResId, @Nullable List<QWToken> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder holder, QWToken token) {
            SwipeMenuLayout layout = (SwipeMenuLayout) holder.itemView;
            View bgView = holder.getView(R.id.content);
            View line = holder.getView(R.id.line);
            int position = holder.getAdapterPosition();
            if (token.getIsAdd() == 0 || QWTokenDao.QKC_SYMBOL.equals(token.getSymbol())) {
                layout.setSwipeEnable(false);
                bgView.setBackgroundColor(Color.parseColor("#f7f7f7"));

                if (position == getItemCount() - 1) {
                    line.setVisibility(View.GONE);
                } else {
                    line.setVisibility(View.VISIBLE);
                }
            } else {
                layout.setSwipeEnable(true);
                bgView.setBackgroundColor(Color.WHITE);

                QWToken tokenNext = getItem(position + 1);
                if (tokenNext != null && (tokenNext.getIsAdd() == 0 || QWTokenDao.QKC_SYMBOL.equals(tokenNext.getSymbol()))) {
                    line.setVisibility(View.GONE);
                } else {
                    line.setVisibility(View.VISIBLE);
                }
            }

            ImageView icon = holder.getView(R.id.token_img);
            String path = token.getIconPath();
            if (!TextUtils.isEmpty(path)) {
                Glide.with(icon)
                        .asBitmap()
                        .load(path)
                        .into(icon);
            } else {
                Glide.with(icon)
                        .asBitmap()
                        .load(R.drawable.token_default_icon)
                        .into(icon);
            }

            String symbol = token.getSymbol();
            holder.setText(R.id.token_symbol, symbol.toUpperCase());

            String name = token.getName();
            holder.setText(R.id.token_name, name);

            String address = token.getAddress();
            address = QWWalletUtils.parseAddressTo8Show(address);
            holder.setText(R.id.token_address, address);

            SwitchButton toggleButton = holder.getView(R.id.token_toggle);
            if (token.isShow()) {
                toggleButton.setChecked(true);
            } else {
                toggleButton.setChecked(false);
            }
            toggleButton.setTag(token);
            toggleButton.setOnCheckedChangeListener(this::onToggleChanged);

            View delete = holder.getView(R.id.btnDelete);
            delete.setTag(token);
            delete.setOnClickListener(this::onDelete);
        }

        private void onToggleChanged(View view, boolean off) {
            QWToken token = (QWToken) view.getTag();
            if (off) {
                //打开
                openToken(token);
                token.setIsShow(1);
            } else {
                //关闭
                closeToken(token);
                token.setIsShow(0);
            }
            mIsChange = true;
        }

        private void onDelete(View view) {
            QWToken token = (QWToken) view.getTag();
            List<QWToken> list = getData();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                if (token.equals(list.get(i))) {
                    list.remove(i);
                    notifyItemRemoved(i);
                    if (i - 1 >= 0) {
                        notifyItemChanged(i - 1);
                    }
                    break;
                }
            }

            QWWalletTokenDao adjustTokenDao = new QWWalletTokenDao(getApplication());
            adjustTokenDao.delete(mWallet.getCurrentAddress(), token.getAddress());
            mMainWalletFragmentViewModel.deleteToken(token);
            mIsChange = true;
        }
    }

    private class TokenSearchListAdapter extends BaseQuickAdapter<QWToken, BaseViewHolder> {

        private ArrayList<String> mAddTokenList = new ArrayList<>();

        TokenSearchListAdapter(int layoutResId, @Nullable List<QWToken> data) {
            super(layoutResId, data);
        }

        @Override
        protected void convert(BaseViewHolder holder, QWToken token) {
            ImageView icon = holder.getView(R.id.token_img);
            String path = token.getIconPath();
            if (QWTokenDao.QKC_SYMBOL.equals(token.getSymbol().toLowerCase())) {
                Glide.with(icon)
                        .asBitmap()
                        .load(R.drawable.token_qkc_icon)
                        .into(icon);
            } else if (!TextUtils.isEmpty(path)) {
                Glide.with(icon)
                        .asBitmap()
                        .load(path)
                        .into(icon);
            } else {
                Glide.with(icon)
                        .asBitmap()
                        .load(R.drawable.token_default_icon)
                        .into(icon);
            }

            String symbol = token.getSymbol();
            holder.setText(R.id.token_symbol, symbol.toUpperCase());

            String name = token.getName();
            holder.setText(R.id.token_name, name);

            String address = token.getAddress();
            address = QWWalletUtils.parseAddressTo8Show(address);
            holder.setText(R.id.token_address, address);

            View selected = holder.getView(R.id.add_token_success);
            View addView = holder.getView(R.id.add_token_info);
            addView.setTag(token);
            addView.setOnClickListener(this::addToken);
            if (mAddTokenList.contains(token.getAddress().toLowerCase())) {
                addView.setVisibility(View.INVISIBLE);
                selected.setVisibility(View.VISIBLE);
            } else {
                addView.setVisibility(View.VISIBLE);
                selected.setVisibility(View.GONE);
            }
        }

        void setNewData(List<QWToken> data, List<String> selectedList) {
            mAddTokenList.clear();
            mAddTokenList.addAll(selectedList);
            super.setNewInstance(data);
        }

        private void addToken(View view) {
            QWToken token = (QWToken) view.getTag();
            mMainWalletFragmentViewModel.addToken(mWallet.getCurrentAddress(), token);
            UmengStatistics.openTokenClickCount(getApplicationContext(), token.getSymbol(), mWallet.getCurrentAddress());
        }

        private void addItem(String address) {
            mAddTokenList.add(address.toLowerCase());
            int size = getData().size();
            for (int i = 0; i < size; i++) {
                QWToken token = getData().get(i);
                if (address.toLowerCase().equals(token.getAddress().toLowerCase())) {
                    notifyItemChanged(i);
                    return;
                }
            }
        }
    }

    private EditText mFocusEditView;
    private EditText mSearchEditView;
    private View mEditRemoveView;
    private View mEditCancelView;
    private View mEditCancelWidthView;
    private View mEditLayout;
    private float mCancelViewWidth;
    private boolean mIsAnim;

    private View mSearchListLayout;
    private TokenSearchListAdapter mSearchListAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mTokenRecyclerView;
    private TokenListAdapter mTokenListAdapter;
    private View mSearchProgressView;
    private MaterialProgressView mAnimProgressView;

    private QWWallet mWallet;
    private boolean mIsChange;
    private String mTokenPositionAddress;

    @Inject
    public MainWalletViewModelFactory mMainWallerFragmentFactory;
    private MainWallerViewModel mMainWalletFragmentViewModel;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_token_list;
    }

    @Override
    public int getActivityTitle() {
        return R.string.token_list_title;
    }

    @Override
    protected void onInitialization(Bundle bundle) {
        mTopBarView.setTitle(R.string.token_list_title);
        mTopBarView.setRightImage(R.drawable.add_wallet);
        mTopBarView.setRightImageClickListener(v -> {
            onAddToken();
            UmengStatistics.topBarTokenListAddTokenClickCount(getApplicationContext(), QWWalletUtils.getCurrentWalletAddress(getApplicationContext()));
        });
        mEditLayout = findViewById(R.id.top_edit_layout);

        mSearchEditView = findViewById(R.id.token_list_search_edit);
        mSearchEditView.addTextChangedListener(new MyAddressTextWatcher());
        mSearchEditView.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (hasFocus) {
                openEdit();
            }
        });
        mSearchEditView.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (!TextUtils.isEmpty(mSearchEditView.getText())) {
                    search(mSearchEditView.getText().toString().trim());
                }
                return true;
            }
            return false;
        });

        mEditRemoveView = findViewById(R.id.search_edit_remove);
        mEditRemoveView.setOnClickListener(v -> mSearchEditView.setText(""));

        mFocusEditView = findViewById(R.id.edit_focus);
        mEditCancelView = findViewById(R.id.edit_cancel);
        mEditCancelView.setOnClickListener(v -> cancelEdit());
        mEditCancelWidthView = findViewById(R.id.edit_cancel_width_view);


        mSwipeRefreshLayout = findViewById(R.id.token_list_swipe);
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefresh);
        mTokenRecyclerView = findViewById(R.id.token_list_recycler_view);
        mTokenRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mTokenListAdapter = new TokenListAdapter(R.layout.holder_recycler_token_list_item, new ArrayList<>());
        mTokenRecyclerView.setAdapter(mTokenListAdapter);


        mSearchListAdapter = new TokenSearchListAdapter(R.layout.holder_recycler_token_search_item, new ArrayList<>());
        mSearchListLayout = findViewById(R.id.token_search_layout);
        mSearchListLayout.setOnTouchListener((View v, MotionEvent event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                UiUtils.hideKeyboard(getApplicationContext(), mSearchEditView);
            }
            return false;
        });
        EmptyRecyclerView searchListRecyclerView = findViewById(R.id.token_search_list_view);
        searchListRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        searchListRecyclerView.setAdapter(mSearchListAdapter);
        searchListRecyclerView.setOnTouchListener((View v, MotionEvent event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                UiUtils.hideKeyboard(getApplicationContext(), mSearchEditView);
            }
            return false;
        });
        View emptyView = findViewById(R.id.empty_view);
        searchListRecyclerView.setEmptyView(emptyView);


        mSearchProgressView = findViewById(R.id.token_search_progress);
        mAnimProgressView = findViewById(R.id.token_search_progress_anim);
        ViewCompat.setElevation(mSearchProgressView, UiUtils.dpToPixel(4));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mMainWalletFragmentViewModel = new ViewModelProvider(this, mMainWallerFragmentFactory)
                .get(MainWallerViewModel.class);
        mMainWalletFragmentViewModel.findDefaultWalletObserve().observe(this, this::findWalletSuccess);
        mMainWalletFragmentViewModel.tokenListObserver().observe(this, this::onTokenListSuccess);
        mMainWalletFragmentViewModel.searchObserve().observe(this, this::onSearchSuccess);
        mMainWalletFragmentViewModel.addTokenStatus().observe(this, this::onAddTokenSuccess);
        mMainWalletFragmentViewModel.findWallet();
    }


    private void findWalletSuccess(QWWallet wallet) {
        mWallet = wallet;
        mSwipeRefreshLayout.setRefreshing(true);
        mMainWalletFragmentViewModel.fetchTokenList(mWallet, Constant.TOKEN_LIST_TYPE_LOAD_NOT_NULL);
    }

    private void onTokenListSuccess(List<QWToken> list) {
        mSwipeRefreshLayout.setRefreshing(false);
        //10放在20前面
        ArrayList<QWToken> trc10List = new ArrayList<>();
        ArrayList<QWToken> trc20List = new ArrayList<>();
        for (QWToken bean : list) {
            if (bean.isNative() || TronWalletClient.isTronErc10TokenAddressValid(bean.getAddress())) {
                trc10List.add(bean);
            } else {
                trc20List.add(bean);
            }
        }
        trc10List.addAll(trc20List);
        mTokenListAdapter.setNewInstance(trc10List);

        if (!TextUtils.isEmpty(mTokenPositionAddress)) {
            if ("0".equals(mTokenPositionAddress)) {
                mTokenRecyclerView.scrollToPosition(0);
            } else {
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    if (TextUtils.equals(mTokenPositionAddress, list.get(i).getAddress())) {
                        mTokenRecyclerView.scrollToPosition(i);
                        break;
                    }
                }
            }
        }
        mTokenPositionAddress = "";
    }

    private void onSearchSuccess(List<QWToken> list) {
        List<String> stringList = new ArrayList<>();
        List<QWToken> localList = mTokenListAdapter.getData();
        for (QWToken qwToken : localList) {
            stringList.add(qwToken.getAddress().toLowerCase());
        }
        mSearchListAdapter.setNewData(list, stringList);
        mSearchProgressView.setVisibility(View.GONE);
        mAnimProgressView.setVisibility(View.GONE);
    }

    //向数据库添加数据
    private void openToken(QWToken token) {
        QWWalletTokenDao tokenDao = new QWWalletTokenDao(getApplication());
        QWWalletToken walletToken = new QWWalletToken();
        walletToken.setTokenAddress(Keys.toChecksumHDAddress(token.getAddress()));
        walletToken.setAccountAddress(mWallet.getCurrentAddress());
        tokenDao.insert(walletToken);
    }

    private void closeToken(QWToken token) {
        QWWalletTokenDao tokenDao = new QWWalletTokenDao(getApplication());
        tokenDao.closeToken(mWallet.getCurrentAddress(), token.getAddress());
    }

    private void onRefresh() {
        if (mWallet == null) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (!ConnectionUtil.isInternetConnection(getApplicationContext())) {
            MyToast.showSingleToastShort(this, R.string.network_error);
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        mMainWalletFragmentViewModel.fetchTokenList(mWallet, Constant.TOKEN_LIST_TYPE_LOAD_NET);
    }

    private void onAddToken() {
        if (mWallet == null) {
            return;
        }
        Intent intent = new Intent(this, AddTokenActivity.class);
        intent.putExtra(Constant.KEY_WALLET, mWallet);
        startActivityForResult(intent, Constant.REQUEST_CODE_ADD_TOKEN);
    }

    private void onAddTokenSuccess(String address) {
        mMainWalletFragmentViewModel.fetchTokenList(mWallet, Constant.TOKEN_LIST_TYPE_LOAD_DB);
        mSearchListAdapter.addItem(address);
        mIsChange = true;
    }

    @Override
    public void finish() {
        if (mIsChange) {
            setResult(Activity.RESULT_OK);
        }
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == Constant.REQUEST_CODE_ADD_TOKEN) {
            if (data != null) {
                mTokenPositionAddress = data.getStringExtra(Constant.KEY_TOKEN_ADDRESS);
            } else {
                mTokenPositionAddress = "0";
            }
            if (mWallet != null) {
                mMainWalletFragmentViewModel.fetchTokenList(mWallet, Constant.TOKEN_LIST_TYPE_LOAD_NET);
            }
            mIsChange = true;
        }
    }

    private void openEdit() {
        if (mSearchListLayout.getVisibility() == View.VISIBLE) {
            return;
        }
        if (mIsAnim) {
            mFocusEditView.requestFocus();
            return;
        }
        if (mCancelViewWidth == 0) {
            mCancelViewWidth = mEditCancelWidthView.getWidth();
        }
        mEditCancelView.setVisibility(View.INVISIBLE);
        mSearchListLayout.setVisibility(View.VISIBLE);

        float width = UiUtils.dpToPixel(20);
        float top = getResources().getDimension(R.dimen.dp_67);
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(250);
        animator.addUpdateListener((ValueAnimator animation) -> {
            float value = animation.getAnimatedFraction();
            mTopBarView.setAlpha(1 - value);

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mEditLayout.getLayoutParams();
            layoutParams.topMargin = (int) (top - top * value);
            mEditLayout.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mEditCancelView.getLayoutParams();
            lp.width = (int) (mCancelViewWidth * value) + (int) (width * (1 - value));
            mEditCancelView.setLayoutParams(lp);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mEditLayout.getLayoutParams();
                layoutParams.topMargin = 0;
                mEditLayout.setLayoutParams(layoutParams);

                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mEditCancelView.getLayoutParams();
                lp.width = (int) (mCancelViewWidth);
                mEditCancelView.setLayoutParams(lp);

                mEditCancelView.setVisibility(View.VISIBLE);
                mIsAnim = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                RelativeLayout.LayoutParams editLP = (RelativeLayout.LayoutParams) mSearchEditView.getLayoutParams();
                editLP.rightMargin = 0;
                mSearchEditView.setLayoutParams(editLP);
                mIsAnim = true;
            }
        });
        animator.start();
    }

    private void cancelEdit() {
        if (mIsAnim) {
            return;
        }
        mFocusEditView.requestFocus();
        mEditCancelView.setVisibility(View.INVISIBLE);
        mSearchEditView.setText("");

        UiUtils.hideKeyboard(getApplicationContext(), mSearchEditView);

        float width = UiUtils.dpToPixel(20);
        float top = getResources().getDimension(R.dimen.dp_67);
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(250);
        animator.addUpdateListener((ValueAnimator animation) -> {
            float value = animation.getAnimatedFraction();
            mTopBarView.setAlpha(value);

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mEditLayout.getLayoutParams();
            layoutParams.topMargin = (int) (top * value);
            mEditLayout.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mEditCancelView.getLayoutParams();
            lp.width = (int) (mCancelViewWidth - mCancelViewWidth * value) + (int) (width * value);
            mEditCancelView.setLayoutParams(lp);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mEditLayout.getLayoutParams();
                layoutParams.topMargin = (int) (top);
                mEditLayout.setLayoutParams(layoutParams);

                RelativeLayout.LayoutParams editLP = (RelativeLayout.LayoutParams) mSearchEditView.getLayoutParams();
                editLP.rightMargin = (int) width;
                mSearchEditView.setLayoutParams(editLP);

                mEditCancelView.setVisibility(View.GONE);
                mSearchListLayout.setVisibility(View.GONE);
                mIsAnim = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mIsAnim = true;
            }
        });
        animator.start();
    }

    private void search(String str) {
        if (mWallet == null) {
            return;
        }
        if (ConnectionUtil.isInternetConnection(getApplicationContext())) {
            mSearchProgressView.setVisibility(View.VISIBLE);
            mAnimProgressView.setVisibility(View.VISIBLE);
            mMainWalletFragmentViewModel.searchTokenList(mWallet.getCurrentAccount(), str);
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsAnim) {
            return;
        }
        if (mSearchListLayout.getVisibility() == View.VISIBLE) {
            cancelEdit();
            return;
        }
        super.onBackPressed();
    }
}
