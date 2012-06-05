package org.openintents.notepad.box;

import org.openintents.intents.NotepadIntents;
import org.openintents.notepad.NoteEditor;

import android.content.Context;
import android.content.Intent;

import com.box.onecloud.android.BoxOneCloudReceiver;
import com.box.onecloud.android.OneCloudData;

/**
 * This example just illustrates how to handle Box requesting that your app edit a file. In this example, the file is simply renamed and then uploaded back to
 * Box.
 * 
 */
public class TextPlainBoxOneCloudReceiver extends BoxOneCloudReceiver {

    @Override
    public void onEditFileRequested(Context context, OneCloudData oneCloudData) {

        // Box has requested that a file be edited. Hand off to an activity to do this.
        Intent i = new Intent(context, NoteEditor.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(NotepadIntents.EXTRA_ONE_CLOUD, oneCloudData); // This is what we need to keep track of.
        i.putExtra(NotepadIntents.)
        context.startActivity(i);
    }

    @Override
    public void onCreateFileRequested(Context context, OneCloudData oneCloudData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onViewFileRequested(Context context, OneCloudData oneCloudData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLaunchRequested(Context context, OneCloudData oneCloudData) {
        // TODO Auto-generated method stub

    }
}
