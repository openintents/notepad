package org.openintents.notepad.wrappers;

import android.app.ActionBar;
import android.app.Activity;
import android.view.MenuItem;

@SuppressLint("NewApi")
public class WrapActionBar {
    static {
        try {
            Class.forName("android.app.ActionBar");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private ActionBar mInstance;

    public WrapActionBar(Activity a) {
        mInstance = a.getActionBar();
    }

    /* calling here forces class initialization */
    public static void checkAvailable() {
    }

    // show an icon in the actionbar if there is room for it.
    public static void showIfRoom(MenuItem item) {
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    public void setDisplayHomeAsUpEnabled(boolean b) {
        mInstance.setDisplayHomeAsUpEnabled(b);
    }

    public void setHomeButtonEnabled(boolean b) {
        mInstance.setHomeButtonEnabled(b);
    }
}
