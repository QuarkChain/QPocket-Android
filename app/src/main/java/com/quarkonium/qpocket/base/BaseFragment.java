package com.quarkonium.qpocket.base;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orhanobut.logger.Logger;
import com.quarkonium.qpocket.statistic.UmengStatistics;
import com.tendcloud.tenddata.TCAgent;


public abstract class BaseFragment extends Fragment {

    protected abstract int getLayoutResource();

    public abstract int getFragmentTitle();

    protected abstract void onInitView(Bundle savedInstanceState, View rootView);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getLayoutResource() != 0) {
            rootView = inflater.inflate(getLayoutResource(), null);
        } else {
            rootView = super.onCreateView(inflater, container, savedInstanceState);
        }
        this.onInitView(savedInstanceState, rootView);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d("name (%s.java:0)", getClass().getSimpleName());
        mContext = getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(mFragmentTitle)) {
            TCAgent.onPageStart(requireContext(), mFragmentTitle);
            UmengStatistics.onPageStart(mFragmentTitle);
        } else {
            String title = getTitleString();
            if (!TextUtils.isEmpty(title)) {
                TCAgent.onPageStart(requireContext(), title);
                UmengStatistics.onPageStart(title);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!TextUtils.isEmpty(mFragmentTitle)) {
            TCAgent.onPageEnd(requireContext(), mFragmentTitle);
            UmengStatistics.onPageEnd(mFragmentTitle);
        } else {
            String title = getTitleString();
            if (!TextUtils.isEmpty(title)) {
                TCAgent.onPageEnd(requireContext(), title);
                UmengStatistics.onPageEnd(title);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext = null;
    }

    private String getTitleString() {
        return getFragmentTitle() == 0 ? "" : getResources().getString(getFragmentTitle());
    }

    public void setFragmentTitle(String title) {
        mFragmentTitle = title;
    }

    protected View rootView;
    protected Context mContext = null;//context
    private String mFragmentTitle;
}
