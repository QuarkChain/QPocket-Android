package com.quarkonium.qpocket.model.permission;

/**
 * Created by yk on 15-12-15.
 */
public interface OnPermissionCallback {

    void onPermissionGranted(String[] permissionName);

    void onPermissionDeclined(String[] permissionName);

    void onPermissionPreGranted(String[] permissionsName);

    void onPermissionNeedExplanation(String[] permissionName);

    void onPermissionAlwaysDeclined(boolean isFromExp, String[] permissionName);
}
