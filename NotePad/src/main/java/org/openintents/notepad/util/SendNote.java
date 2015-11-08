package org.openintents.notepad.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.Toast;

import org.openintents.notepad.R;

public class SendNote {
    public static void sendNote(Activity from, String title, String content) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, title);
        i.putExtra(Intent.EXTRA_TEXT, content);

        try {
            from.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    from, R.string.share_not_available,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
