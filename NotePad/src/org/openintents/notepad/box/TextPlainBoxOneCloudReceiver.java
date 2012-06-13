package org.openintents.notepad.box;

import org.openintents.intents.NotepadIntents;
import org.openintents.notepad.NoteEditor;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.noteslist.NotesList;

import android.content.Context;
import android.content.Intent;

import com.box.onecloud.android.BoxOneCloudReceiver;
import com.box.onecloud.android.OneCloudData;

/**
 * This example just illustrates how to handle Box requesting that your app edit
 * a file. In this example, the file is simply renamed and then uploaded back to
 * Box.
 * 
 */
public class TextPlainBoxOneCloudReceiver extends BoxOneCloudReceiver {

	@Override
	public void onEditFileRequested(Context context, OneCloudData oneCloudData) {

		// Box has requested that a file be edited. Hand off to an activity to
		// do this.
		Intent i = new Intent(context, NoteEditor.class);
		i.setAction(Intent.ACTION_EDIT);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(NotepadIntents.EXTRA_ONE_CLOUD, oneCloudData); // This is
																	// what we
																	// need to
																	// keep
																	// track of.
		context.startActivity(i);
	}

	@Override
	public void onCreateFileRequested(Context context, OneCloudData oneCloudData) {
		// Box has requested that a file be edited. Hand off to an activity to
		// do this.
		Intent i = new Intent(context, NoteEditor.class);
		i.setAction(Intent.ACTION_INSERT);
		i.setData(Notes.CONTENT_URI);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(NotepadIntents.EXTRA_ONE_CLOUD, oneCloudData); // This is
																	// what we
																	// need to
																	// keep
																	// track of.
		context.startActivity(i);

	}

	@Override
	public void onViewFileRequested(Context context, OneCloudData oneCloudData) {
		// Box has requested that a file be edited. Hand off to an activity to
		// do this.
		Intent i = new Intent(context, NoteEditor.class);
		i.setAction(Intent.ACTION_VIEW);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(NotepadIntents.EXTRA_ONE_CLOUD, oneCloudData); // This is
																	// what we
																	// need to
																	// keep
																	// track of.
		context.startActivity(i);

	}

	@Override
	public void onLaunchRequested(Context context, OneCloudData oneCloudData) {
		Intent i = new Intent(context, NotesList.class);
		context.startActivity(i);
	}
}
