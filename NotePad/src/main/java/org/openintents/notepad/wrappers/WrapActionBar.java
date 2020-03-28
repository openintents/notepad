package org.openintents.notepad.wrappers;

import android.app.ActionBar;
import android.app.Activity;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;

public class WrapActionBar {
    static {
        try {
            Class.forName("android.app.ActionBar");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        try {
            Class.forName("androidx.appcompat.app.ActionBar");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private ActionBar mInstance;
    private androidx.appcompat.app.ActionBar mCompatInstance;

    public WrapActionBar(Activity a) {
        if (a instanceof AppCompatActivity)
            mCompatInstance = ((AppCompatActivity) a).getSupportActionBar();
        else
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
        if (mCompatInstance != null)
            mCompatInstance.setDisplayHomeAsUpEnabled(b);
        else if (mInstance != null)
            mInstance.setDisplayHomeAsUpEnabled(b);
    }

    public void setHomeButtonEnabled(boolean b) {
        if (mCompatInstance != null)
            mCompatInstance.setHomeButtonEnabled(b);
        else if (mInstance != null)
            mInstance.setHomeButtonEnabled(b);
    }
}
