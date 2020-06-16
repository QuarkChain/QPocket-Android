package com.quarkonium.qpocket.model.permission;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Created by yk on 15-12-15.
 */
public class SimplePerCallback implements OnPermissionCallback {

    private WeakReference<Context> mContext;

    public SimplePerCallback(Context ctx) {
        mContext = new WeakReference<>(ctx);
    }

    @Override
    public void onPermissionGranted(String[] permissionName) {
    }

    @Override
    public void onPermissionDeclined(String[] permissionName) {
    }

    @Override
    public void onPermissionPreGranted(String[] permissionName) {
    }

    @Override
    public void onPermissionNeedExplanation(String[] permissionName) {
    }

    public void onPermissionAlwaysDeclined(boolean isFromExp, String[] permissionName) {
        onPermissionAlwaysDeclined(permissionName);
    }

    /**
     * 当用户awalys拒绝权限的时候，屏蔽第一次拒绝用这个。
     */
    public void onPermissionAlwaysDeclined(String[] permissionName) {

    }

    public boolean request() {
        throw new RuntimeException();
    }

    public boolean request(boolean status) {
        throw new RuntimeException();
    }

    protected boolean getPermissionStatus() {
        throw new RuntimeException();
    }

    public Context getContext() {
        return mContext.get();
    }
}
