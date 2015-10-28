package org.openintents.notepad.noteslist;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

public class NotesListCursorAdapter extends CursorAdapter {
	private static final String TAG = "NotesListCursorAdapter";

	Context mContext;
	NotesListCursor mCursorUtils;

	/**
	 * Flag for slow list adapter.
	 */
	public boolean mBusy;

	public NotesListCursorAdapter(Context context, Cursor c,
			NotesListCursor cursorUtils) {
		super(context, c);
		mContext = context;
		mCursorUtils = cursorUtils;

		mBusy = false;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		NotesListItemView nliv = (NotesListItemView) view;

		String title = cursor.getString(NotesListCursor.COLUMN_INDEX_TITLE);
		String tags = cursor.getString(NotesListCursor.COLUMN_INDEX_TAGS);
		long encrypted = cursor.getLong(NotesListCursor.COLUMN_INDEX_ENCRYPTED);
		String titleEncrypted = cursor
				.getString(NotesListCursor.COLUMN_INDEX_TITLE_ENCRYPTED);
		String tagsEncrypted = cursor
				.getString(NotesListCursor.COLUMN_INDEX_TAGS_ENCRYPTED);

		nliv.setEncrypted(encrypted);

		nliv.setTitle(title);
		nliv.setTags(tags);
		nliv.mTitleEncrypted = titleEncrypted;
		nliv.mTagsEncrypted = tagsEncrypted;

		/*
		 * if (encrypted == 0) { // Not encrypted: nliv.setTitle(title);
		 * nliv.setTags(tags); // Null tag means the view has the correct data
		 * nliv.setTag(null); } else { // encrypted String decrypted =
		 * mTitleHashMap.get(title); if (decrypted != null) {
		 * nliv.setTitle(decrypted); nliv.setTags(tags); // Null tag means the
		 * view has the correct data nliv.setTag(null); } else {
		 * nliv.setTitle(mContext.getString(R.string.encrypted));
		 * nliv.setTags(tags); // Non-null tag means the view still needs to
		 * load it's data // Tag contains a pointer to a string with the
		 * encrypted title. nliv.setTag(title); } /* if (!mBusy) {
		 * nliv.setTitle("set"); nliv.setTitle("wow"); // Null tag means the
		 * view has the correct data nliv.setTag(null); } else {
		 * nliv.setTitle(mContext.getString(R.string.encrypted));
		 * nliv.setTags(tags); // Non-null tag means the view still needs to
		 * load it's data nliv.setTag(this); } / }
		 */
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return new NotesListItemView(context);
	}

	/*
	 * @Override public Filter getFilter() { Log.i(TAG, "Request filter");
	 * 
	 * return super.getFilter(); }
	 */

	/*
	 * @Override public CharSequence convertToString(Cursor cursor) { //return
	 * super.convertToString(cursor);
	 * 
	 * Log.i(TAG, "convertToString" + cursor.getPosition() + " / " +
	 * cursor.getCount());
	 * 
	 * return cursor.getString(NotesList.COLUMN_INDEX_TITLE); }
	 */

	public Cursor runQueryOnBackgroundThread(CharSequence constraint, String tag) {
		// Log.i(TAG, "runQueryOnBackgroundThread " + constraint + ", " +
		// mIntent.getData());

		/*
		 * Cursor cursor =
		 * mContext.getContentResolver().query(mIntent.getData(),
		 * NotesList.PROJECTION, "(" + Notes.TITLE + " like '" +
		 * constraint.toString() + "%' ) or (" + Notes.TITLE + " like '% " +
		 * constraint.toString() + "%' )", new String[] { },
		 * Notes.DEFAULT_SORT_ORDER);
		 */

		Cursor cursor = mCursorUtils.query(constraint, tag);

		return cursor;
	}

}
