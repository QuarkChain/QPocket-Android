package com.quarkonium.qpocket.view.listener;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

public class ActionModeCallbackInterceptor implements ActionMode.Callback {

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }
}
