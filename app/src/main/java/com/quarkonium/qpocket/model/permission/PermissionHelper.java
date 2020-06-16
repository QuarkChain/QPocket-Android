package com.quarkonium.qpocket.model.permission;


import android.Manifest;
import android.content.Context;

import com.quarkonium.qpocket.R;

public class PermissionHelper {


    public static final String[] PERMISSION_STORAGE = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final String[] PERMISSION_CAMERA = new String[]{Manifest.permission.CAMERA};
    public static final String[] PERMISSION_LOCATION = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    public static final String[] PERMISSION_READ_PHONE_STATE = new String[]{Manifest.permission.READ_PHONE_STATE};

    private static boolean hasPermission(String[] permissions, String[] localPermissions) {
        for (String permission : permissions) {
            for (String localPermission : localPermissions) {
                if (permission.equals(localPermission)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 是否包含相机权限
     */
    private static boolean hasCameraPermission(String[] permissions) {
        return hasPermission(permissions, PERMISSION_CAMERA);
    }

    /**
     * 是否包存储空间机权限
     */
    private static boolean hasStoragePermission(String[] permissions) {
        return hasPermission(permissions, PERMISSION_STORAGE);
    }

    /**
     * 是否有Location权限
     */
    private static boolean hasLocationPermission(String[] permissions) {
        return hasPermission(permissions, PERMISSION_LOCATION);
    }

    private static String getPermissionRes(Context context, String[] permissions) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean hasAdd = false;
        if (hasCameraPermission(permissions)) {
            hasAdd = true;
            stringBuilder.append(context.getResources().getString(R.string.permission_camera));
        }
        if (hasStoragePermission(permissions)) {
            if (hasAdd) {
                stringBuilder.append("、");
            }
            stringBuilder.append(context.getResources().getString(R.string.permission_storage));
        }
        if (hasLocationPermission(permissions)) {
            if (hasAdd) {
                stringBuilder.append("、");
            }
            stringBuilder.append(context.getResources().getString(R.string.permission_location));
        }
        return stringBuilder.toString();
    }

    public static String getPermissionToast(Context context, String[] permissions) {
        String stringStart = context.getResources().getString(R.string.permission_declined);
        return String.format(stringStart, getPermissionRes(context, permissions));
    }
}
