package com.quarkonium.qpocket.model.permission;

import androidx.annotation.NonNull;

/**
 * Created by yk on 15-12-15.
 */
public interface OnActivityPermissionCallback {

    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);
}
