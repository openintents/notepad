/* 
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Original copyright:
 * Based on the Android SDK sample application NotePad.
 * Copyright (C) 2007 Google Inc.
 * Licensed under the Apache License, Version 2.0.
 */

package org.openintents.notepad.noteslist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.openintents.distribution.DistributionLibraryListActivity;
import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.intents.CryptoIntents;
import org.openintents.notepad.NoteEditor;
import org.openintents.notepad.NotePad;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.NotePadProvider;
import org.openintents.notepad.PreferenceActivity;
import org.openintents.notepad.PrivateNotePadIntents;
import org.openintents.notepad.R;
import org.openintents.notepad.crypto.EncryptActivity;
import org.openintents.notepad.dialog.DeleteConfirmationDialog;
import org.openintents.notepad.filename.DialogHostingActivity;
import org.openintents.notepad.intents.NotepadInternalIntents;
import org.openintents.notepad.util.FileUriUtils;
import org.openintents.notepad.util.SendNote;
import org.openintents.notepad.wrappers.WrapActionBar;
import org.openintents.util.MenuIntentOptionsWithIcons;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v2.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Displays a list of notes. Will display notes from the {@link Uri} provided in
 * the intent if there is one, otherwise defaults to displaying the contents of
 * the {@link NotePadProvider}
 */
public class NotesList extends DistributionLibraryListActivity implements ListView.OnScrollListener {
	private static final String TAG = "NotesList";
	private static final boolean debug = false;

	// Menu item ids
	private static final int MENU_ITEM_DELETE = Menu.FIRST;
	private static final int MENU_ITEM_INSERT = Menu.FIRST + 1;
	private static final int MENU_ITEM_SHARE = Menu.FIRST + 2;
	private static final int MENU_ITEM_ENCRYPT = Menu.FIRST + 5;
	private static final int MENU_ITEM_UNENCRYPT = Menu.FIRST + 6;
	private static final int MENU_ITEM_EDIT_TAGS = Menu.FIRST + 7;
	//private static final int MENU_ITEM_SAVE = Menu.FIRST + 8;
	private static final int MENU_OPEN = Menu.FIRST + 9;
	private static final int MENU_SETTINGS = Menu.FIRST + 10;
	private static final int MENU_SEARCH = Menu.FIRST + 11;
	private static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST BE LAST

	private static final String BUNDLE_LAST_FILTER = "last_filter";

	private static final String BUNDLE_CONTEXTMENUINFO_ID = "ctx_menu_id";
	private static final String BUNDLE_CONTEXTMENUINFO_POSITION = "ctx_menu_position";

	/**
	 * A group id for alternative menu items.
	 */
	private final static int CATEGORY_ALTERNATIVE_GLOBAL = 1;

	private static final int REQUEST_CODE_DECRYPT_TITLE = 3;
	//private static final int REQUEST_CODE_UNENCRYPT_NOTE = 4;
	private static final int REQUEST_CODE_OPEN = 5;

	private static final int DIALOG_TAGS = 1;
	private static final int DIALOG_GET_FROM_MARKET = 3;
	private static final int DIALOG_DELETE = 4;
	private static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST

	private final int DECRYPT_DELAY = 100;

	NotesListCursor mCursorUtils;
	NotesListCursorAdapter mAdapter;

	String mLastFilter;
	String mSelectedTag;

	private Handler mHandler = new Handler();

	private boolean mDecryptionFailed;
	private boolean mDecryptionSucceeded;
	
	private static boolean mActionBarAvailable;
	
	static {
		try {
			WrapActionBar.checkAvailable();
			mActionBarAvailable = true;
		} catch(Throwable t){
			mActionBarAvailable = false;
		}
	}

	AdapterView.AdapterContextMenuInfo mContextMenuInfo;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (debug) {
			Log.d(TAG, "onCreate() " + (savedInstanceState == null ? "(no bundle)" : "(with bundle)"));
		}

		mDistribution.setFirst(MENU_DISTRIBUTION_START, DIALOG_DISTRIBUTION_START);

		// Check whether EULA has been accepted
		// or information about new version can be presented.
		if (mDistribution.showEulaOrNewVersion()) {
			return;
		}

		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

		// If no data was given in the intent (because we were started
		// as a MAIN activity), then use our default content provider.
		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(Notes.CONTENT_URI);
		}
		
		//show loading
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		requestWindowFeature(Window.FEATURE_PROGRESS);

		// Inform the list we provide context menus for items
		setContentView(R.layout.noteslist);
		getListView().setOnCreateContextMenuListener(this);
		getListView().setEmptyView(findViewById(R.id.empty));
		getListView().setTextFilterEnabled(true);

		/*
		 * Button b = (Button) findViewById(R.id.add); b.setOnClickListener(new
		 * Button.OnClickListener() {
		 * 
		 * @Override public void onClick(View arg0) { insertNewNote(); }
		 * 
		 * });
		 */

		/*
		// Perform a managed query. The Activity will handle closing and
		// requerying the cursor
		// when needed.
		Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null,
				null, Notes.DEFAULT_SORT_ORDER);

		/*
		// Used to map notes entries from the database to views
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.noteslist_item, cursor, new String[] { Notes.TITLE },
				new int[] { android.R.id.text1 });
		 * /
		mAdapter = new NotesListCursorAdapter(this, cursor, getIntent());
		setListAdapter(mAdapter);
		 */

		getListView().setOnScrollListener(this);

		mLastFilter = null;

		if (savedInstanceState != null) {
			mLastFilter = savedInstanceState.getString(BUNDLE_LAST_FILTER);

			// Restore information for context menu, for opening "Tags" dialog.
			if (savedInstanceState.containsKey(BUNDLE_CONTEXTMENUINFO_ID)) {
				long id = savedInstanceState.getLong(BUNDLE_CONTEXTMENUINFO_ID);
				int position = savedInstanceState.getInt(BUNDLE_CONTEXTMENUINFO_POSITION);
				mContextMenuInfo = new AdapterView.AdapterContextMenuInfo(null, position, id);
			}
		}

		mCursorUtils = new NotesListCursor(this, getIntent());

		// Make sure mAdapter is created here already,
		// because onPrepareDialog for the Tags dialog may be called
		// before onResume() is called.
		checkAdapter();

		if (Intent.ACTION_CREATE_SHORTCUT.equals(intent.getAction())) {
			setTitle(R.string.title_pick_note_for_shortcut);
		}

		updateTagList();

		Cursor cur = mAdapter.runQueryOnBackgroundThread(null, null);

		cur.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				NotesList.this.updateTagList();
			}
		});

		Spinner s = (Spinner) findViewById(R.id.tagselection);
		s.setOnItemSelectedListener(
				new AdapterView.OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
						NotesList.this.updateQuery();
						decryptDelayed();
					}

					public void onNothingSelected(AdapterView<?> arg0) {

					}
				});

		mDecryptionFailed = false;
		mDecryptionSucceeded = false;

		//disable the progressbar when done.
		setProgressBarIndeterminateVisibility(false);
		setProgressBarVisibility(false);
	}

	protected void updateTagList() {

		List<String> taglist = new ArrayList<String>();

		Uri notesUri = getIntent().getData();

		Cursor managedCursor = getContentResolver().query(notesUri,
				new String[]{Notes.TAGS, Notes.ENCRYPTED},
				null,
				null,
				null);

		if (managedCursor.moveToFirst()) {
			do {
				String tags = managedCursor.getString(0);
				long encrypted = managedCursor.getLong(1);

				if (!TextUtils.isEmpty(tags)) {
					if (encrypted == 0) {
						for (String tag : tags.split(",")) {
							if (!taglist.contains(tag.trim())) {
								taglist.add(tag.trim());
							}
						}
					} else {
						// Currently: ignore encrypted tags.

						// TODO: store encrypted tags in an array,
						// decrypt them, and show them.
					}
				}
			} while (managedCursor.moveToNext());
		}
		Collections.sort(taglist, Collator.getInstance(Locale.getDefault()));
		taglist.add(0, getString(R.string.all_notes));
		Spinner s = (Spinner) findViewById(R.id.tagselection);
		ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, taglist);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);

		// Hide Spinner if there are no tags
		if (taglist.size() > 1) {
			s.setVisibility(View.VISIBLE);
		} else {
			s.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (debug) Log.d(TAG, "onResume()");

		NotesListCursor.mSuspendQueries = false;

		//mCursorUtils.registerContentObserver(mListContentObserver);
		//mCursorUtils.registerDataSetObserver(mListDatasetObserver);

		checkAdapter();

		if (!mDecryptionFailed) {
			decryptDelayed();
		} else {
			// Reset
			mDecryptionFailed = false;
		}

		if (mDecryptionSucceeded) {
			NotesListCursor.mLoggedIn = true;
			if (debug) Log.d(TAG, "onResume: logged in");
		}

		IntentFilter filter = new IntentFilter();
		filter.addAction(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
		registerReceiver(mBroadcastReceiver, filter);


		// getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}
	/*
	ContentObserver mListContentObserver = new ContentObserver(mHandler) {

		@Override
		public boolean deliverSelfNotifications() {
			Log.i(TAG, "NotesList:mListContentObserver: deliverSelfNotifications");
			return super.deliverSelfNotifications();
		}

		@Override
		public void onChange(boolean arg0) {
			Log.i(TAG, "NotesList:mListContentObserver: onChange");
			//mCursorListFilter.requery();
			updateTagList();

			super.onChange(arg0);
		}
	};

	DataSetObserver mListDatasetObserver = new DataSetObserver() {

		@Override
		public void onChanged() {
			Log.i(TAG, "mListDatasetObserver: onChanged");
			super.onChanged();
		}

		@Override
		public void onInvalidated() {
			Log.i(TAG, "mListDatasetObserver: onInvalidated");
			super.onInvalidated();
		}

	};
	 */
	private void checkAdapter() {
		if (mAdapter == null) {
			// Perform a managed query. The Activity will handle closing and
			// requerying the cursor
			// when needed.
			//Cursor cursor = getContentResolver().query(getIntent().getData(), NotesListCursorUtils.PROJECTION, null,
			//		null, Notes.DEFAULT_SORT_ORDER);

			Cursor cursor = mCursorUtils.query(null, null);

			/*
			// Used to map notes entries from the database to views
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
					R.layout.noteslist_item, cursor, new String[] { Notes.TITLE },
					new int[] { android.R.id.text1 });
			 */
			mAdapter = new NotesListCursorAdapter(this, cursor, mCursorUtils);
			setListAdapter(mAdapter);

			updateQuery();
		} else {
			mAdapter.getCursor().requery();
		}
	}

	protected void updateQuery() {
		if (debug) {
			Log.i(TAG, "Lastfilter: " + mLastFilter);
		}

		Spinner s = (Spinner) findViewById(R.id.tagselection);

		if ( s.getSelectedItemPosition() == 0 ) {
			mSelectedTag = null;
		} else {
			mSelectedTag = (String) s.getSelectedItem();
		}

		//if (mLastFilter != null || mSelectedTag != null) {
		Cursor cursor = mAdapter.runQueryOnBackgroundThread(mLastFilter, mSelectedTag);
		mAdapter.changeCursor(cursor);
		//}
}

	@Override
	protected void onPause() {
		super.onPause();

		mLastFilter = mCursorUtils.mCurrentFilter;


		// Deactivating the cursor leads to flickering whenever some
		// encrypted information is retrieved.
		// Cursor c = mAdapter.getCursor();
		//if (c != null) {
		//	c.deactivate();
		//}

		//mCursorUtils.unregisterDataSetObserver(mListDatasetObserver);
		//mCursorUtils.unregisterContentObserver(mListContentObserver);

		unregisterReceiver(mBroadcastReceiver);

		// After unregistering broadcastreceiver, the logged in state is not clear.
		NotesListCursor.mLoggedIn = false;
		if (debug) Log.d(TAG, "onPause: logged out");

		// No need wasting a lot of time doing queries when external applications change the
		// database - we requery in onResume anyway.
		NotesListCursor.mSuspendQueries = true;
		mDecryptionFailed = false;
		mDecryptionSucceeded = false;
	}



	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(BUNDLE_LAST_FILTER, mCursorUtils.mCurrentFilter);

		if (mContextMenuInfo != null) {
			outState.putLong(BUNDLE_CONTEXTMENUINFO_ID, mContextMenuInfo.id);
			outState.putInt(BUNDLE_CONTEXTMENUINFO_POSITION, mContextMenuInfo.position);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuItem insertItem = menu.add(0, MENU_ITEM_INSERT, 0, R.string.menu_insert);
		insertItem.setShortcut('1', 'i');
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Icon for holo theme
			insertItem.setIcon(R.drawable.ic_menu_add_note);
		} else {
			insertItem.setIcon(android.R.drawable.ic_menu_add);
		}
		//Show the delete icon when there is an actionbar
		if(mActionBarAvailable){
			WrapActionBar.showIfRoom(insertItem);
		}

		menu.add(0, MENU_SEARCH, 0, R.string.menu_search).setShortcut('2',
				's').setIcon(android.R.drawable.ic_menu_search);

		menu.add(0, MENU_OPEN, 0, R.string.menu_open_from_sdcard).setShortcut('3',
				'o').setIcon(R.drawable.ic_menu_folder);

		menu.add(0, MENU_SETTINGS, 0, R.string.settings).setIcon(
				android.R.drawable.ic_menu_preferences).setShortcut('9', 's');

		// Add distribution menu items last.
		mDistribution.onCreateOptionsMenu(menu);

		// Generate any additional actions that can be performed on the
		// overall list. In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		Intent intent = new Intent(null, getIntent().getData());
		if (debug) Log.i(TAG, "Building options menu for: " + intent.getDataString());
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		//menu
		//		.addIntentOptions(CATEGORY_ALTERNATIVE_GLOBAL, 0, 0,
		//				new ComponentName(this, NotesList.class), null, intent,
		//				0, null);

		// Workaround to add icons:
		MenuIntentOptionsWithIcons menu2 = new MenuIntentOptionsWithIcons(this, menu);
		menu2.addIntentOptions(CATEGORY_ALTERNATIVE_GLOBAL, 0, 0,
				new ComponentName(this, NotesList.class), null, intent, 0, null);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		final boolean haveItems = getListAdapter().getCount() > 0;

		// If there are any notes in the list (which implies that one of
		// them is selected), then we need to generate the actions that
		// can be performed on the current selection. This will be a combination
		// of our own specific actions along with any extensions that can be
		// found.
		if (haveItems) {
			// This is the selected item.
			Uri uri = ContentUris.withAppendedId(getIntent().getData(),
					getSelectedItemId());

			// Build menu... always starts with the EDIT action...
			Intent[] specifics = new Intent[1];
			specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
			MenuItem[] items = new MenuItem[1];

			// ... is followed by whatever other actions are available...
			Intent intent = new Intent(null, uri);
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			//menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0, null,
			//		specifics, intent, 0, items);

			// Workaround to add icons:
			MenuIntentOptionsWithIcons menu2 = new MenuIntentOptionsWithIcons(this, menu);
			menu2.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
					null, specifics, intent, 0, items);

			// Give a shortcut to the edit action.
			if (items[0] != null) {
				items[0].setShortcut('1', 'e');
			}
		} else {
			menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_ITEM_INSERT:
				insertNewNote();
				return true;
			case MENU_SEARCH:
				search();
				return true;
			case MENU_OPEN:
				openFromSdCard();
				return true;
			case MENU_SETTINGS:
				showNotesListSettings();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Launch activity to insert a new item.
	 */
	private void insertNewNote() {
		// Launch activity to insert a new item
		Intent i = new Intent(Intent.ACTION_INSERT, getIntent().getData());
		i.putExtra(NotepadInternalIntents.EXTRA_TAGS, mSelectedTag);
		startActivity(i);
	}

	private void search() {
		onSearchRequested();
	}

	private void openFromSdCard() {

		File sdcard = getSdCardPath();
		String directory = sdcard.getAbsolutePath();
		if (!directory.endsWith("/")) {
			directory += "/";
		}
		Uri uri = FileUriUtils.getUri(directory);

		Intent i = new Intent(this, DialogHostingActivity.class);
		i.putExtra(DialogHostingActivity.EXTRA_DIALOG_ID, DialogHostingActivity.DIALOG_ID_OPEN);
		i.setData(uri);
		startActivityForResult(i, REQUEST_CODE_OPEN);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			// For some reason the requested item isn't available, do nothing
			return;
		}

		Uri noteUri = ContentUris.withAppendedId(getIntent().getData(),
				info.id);

		long encrypted = cursor.getLong(NotesListCursor.COLUMN_INDEX_ENCRYPTED);

		// Setup the menu header
		menu.setHeaderTitle(cursor.getString(NotesListCursor.COLUMN_INDEX_TITLE));

		menu.add(0, MENU_ITEM_EDIT_TAGS, 0, R.string.menu_edit_tags);

		if (encrypted == 0) {

			// Add a menu item to send the note
			menu.add(0, MENU_ITEM_SHARE, 0, R.string.menu_share);

			// Added automatically through manifest:
			//menu.add(0, MENU_ITEM_SAVE, 0, R.string.menu_save_to_sdcard);

			menu.add(0, MENU_ITEM_ENCRYPT, 0, R.string.menu_encrypt);
		} else {
			menu.add(0, MENU_ITEM_UNENCRYPT, 0, R.string.menu_undo_encryption);
		}

		// Add a menu item to delete the note
		menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete);

		// Add additional items only if note is not encrypted:

		if (encrypted == 0) {
			Intent intent = new Intent(null, noteUri);
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
					new ComponentName(this, NotesList.class), null, intent, 0, null);
		}

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		try {
			mContextMenuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuInfo", e);
			return false;
		}

		switch (item.getItemId()) {
			case MENU_ITEM_DELETE: {
				showDialog(DIALOG_DELETE);

				//mAdapter.getCursor().requery();
				return true;
			}
			case MENU_ITEM_SHARE:
				sendNoteByEmail(mContextMenuInfo.id);
				return true;
			case MENU_ITEM_ENCRYPT:
				encryptNote(mContextMenuInfo.id, CryptoIntents.ACTION_ENCRYPT);
				return true;
			case MENU_ITEM_UNENCRYPT:
				encryptNote(mContextMenuInfo.id, CryptoIntents.ACTION_DECRYPT);
				return true;
			case MENU_ITEM_EDIT_TAGS:
				editTags();
				return true;
				//case MENU_ITEM_SAVE:
				//	saveToSdCard();
				//	return true;
		}
		return false;
	}

	private void sendNoteByEmail(long id) {
		// Obtain Uri for the context menu
		Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), id);
		// getContentResolver().(noteUri, null, null);

		Cursor c = getContentResolver().query(noteUri,
				new String[] { NotePad.Notes.TITLE, NotePad.Notes.NOTE }, null,
				null, PreferenceActivity.getSortOrderFromPrefs(this));

		String title = "";
		String content = getString(R.string.empty_note);
		if (c != null) {
			c.moveToFirst();
			title = c.getString(0);
			content = c.getString(1);
		}

		if (debug) Log.i(TAG, "Title to send: " + title);
		if (debug) Log.i(TAG, "Content to send: " + content);
		SendNote.sendNote(this, title, content);
	}

	/**
	 * Encrypt or unencrypt a note.
	 * 
	 * @param id
	 * @param action
	 */
	private void encryptNote(long id, String action) {
		// Obtain Uri for the context menu
		Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), id);
		// getContentResolver().(noteUri, null, null);

		Cursor c = getContentResolver().query(noteUri,
				new String[] { NotePad.Notes.TITLE, NotePad.Notes.NOTE, NotePad.Notes.TAGS, NotePad.Notes.ENCRYPTED }, null,
				null, PreferenceActivity.getSortOrderFromPrefs(this));

		String title = "";
		String text = getString(R.string.empty_note);
		String tags = "";
		int encrypted = 0;
		if (c != null) {
			c.moveToFirst();
			title = c.getString(0);
			text = c.getString(1);
			tags = c.getString(2);
			encrypted = c.getInt(3);
		}

		if (action.equals(CryptoIntents.ACTION_ENCRYPT) && encrypted != 0) {
			Toast.makeText(this,
					R.string.already_encrypted,
					Toast.LENGTH_SHORT).show();
			return;
		}

		if (action.equals(CryptoIntents.ACTION_DECRYPT) && encrypted == 0) {
			Toast.makeText(this,
					R.string.not_encrypted,
					Toast.LENGTH_SHORT).show();
			return;
		}

		Intent i = new Intent(this, EncryptActivity.class);
		i.putExtra(PrivateNotePadIntents.EXTRA_ACTION, action);
		i.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY, EncryptActivity.getCryptoStringArray(text, title, tags));
		i.putExtra(PrivateNotePadIntents.EXTRA_URI, noteUri.toString());
		startActivity(i);
	}

	private void editTags() {
		showDialog(DIALOG_TAGS);
	}


	private File getSdCardPath() {
		return android.os.Environment
				.getExternalStorageDirectory();
	}

	private void showNotesListSettings() {
		startActivity(new Intent(this, PreferenceActivity.class));
	}


	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
			int totalItemCount) {
	}


	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_IDLE:
				Log.i(TAG, "idle");
				mAdapter.mBusy = false;

				if (!NotesListCursor.mEncryptedStringList.isEmpty()) {
					String encryptedString = NotesListCursor.mEncryptedStringList.remove(0);
					Log.i(TAG, "Decrypt idle: " + encryptedString);
					decryptTitle(encryptedString);
				}
				/*
            int first = view.getFirstVisiblePosition();
            int count = view.getChildCount();
            for (int i=0; i<count; i++) {
                NotesListItemView t = (NotesListItemView)view.getChildAt(i);
            	String encryptedTitle = (String) t.getTag();
                if (encryptedTitle != null) {
                	// Retrieve decrypted title
                	decryptTitle(encryptedTitle);
                    t.setTag(null);

                	// decrypt one item at a time.
                	break;
                }
            }
				 */

				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				mAdapter.mBusy = true;
				break;
			case OnScrollListener.SCROLL_STATE_FLING:
				mAdapter.mBusy = true;
				break;
		}
	}

	public void decryptDelayed() {
		// Poll the next string that has not been decrypted yet.
		String encryptedString = NotesListCursor.getNextEncryptedString();
		if (encryptedString != null) {
			setProgressBarIndeterminateVisibility(true);
			decryptDelayed(encryptedString, DECRYPT_DELAY);
		} else if (!mDecryptionFailed && !mDecryptionSucceeded) {
			// If neither failed nor succeeded yet, we send a test intent.
			// This is to ensure that the service is still running
			// even if we may serve all decrypted strings from the cache.
			NotesListCursor nlc = (NotesListCursor) mAdapter.getCursor();
			if (nlc.mContainsEncryptedStrings) {
				// Of course only if there is at least one encrypted string.
				setProgressBarIndeterminateVisibility(true);
				decryptDelayed(null, 0);
			}
		} else {
			// Done with decryption
			setProgressBarIndeterminateVisibility(false);
		}
	}

	public void decryptDelayed(final String encryptedTitle, long delayMillis) {
		mHandler.postDelayed(new Runnable() {

			public void run() {
				decryptTitle(encryptedTitle);
			}

		}, delayMillis);
	}

	public void decryptTitle(String encryptedTitle) {

		Intent intent = new Intent();
		intent.setAction(CryptoIntents.ACTION_DECRYPT);
		if (encryptedTitle != null) {
			intent.putExtra(CryptoIntents.EXTRA_TEXT, encryptedTitle);
			intent.putExtra(PrivateNotePadIntents.EXTRA_ENCRYPTED_TEXT, encryptedTitle);
		}

		intent.putExtra(CryptoIntents.EXTRA_PROMPT, false);

		try {
			startActivityForResult(intent, REQUEST_CODE_DECRYPT_TITLE);
		} catch (ActivityNotFoundException e) {
			mDecryptionFailed = true;
			/*
			Toast.makeText(this,
					R.string.decryption_failed,
					Toast.LENGTH_SHORT).show();
			 */
			Log.e(TAG, "failed to invoke encrypt");
		}
	}


	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
			case DIALOG_TAGS:
				return new TagsDialog(this);
			case DIALOG_GET_FROM_MARKET:
				return new DownloadOIAppDialog(this,
						DownloadOIAppDialog.OI_SAFE);
			case DIALOG_DELETE:
				return new DeleteConfirmationDialog(this, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri noteUri = ContentUris.withAppendedId(getIntent().getData(),
								mContextMenuInfo.id);
						getContentResolver().delete(noteUri, null, null);
					}
				}).create();
		}
		return super.onCreateDialog(id);
	}


	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (debug) Log.d(TAG, "onPrepareDialog()");

		switch (id) {
			case DIALOG_TAGS:
				TagsDialog d = (TagsDialog) dialog;

				Uri uri = ContentUris.withAppendedId(getIntent().getData(), mContextMenuInfo.id);

				Cursor c = mAdapter.getCursor();
				c.moveToPosition(mContextMenuInfo.position);
				String tags = c.getString(NotesListCursor.COLUMN_INDEX_TAGS);
				long encrypted = c.getLong(NotesListCursor.COLUMN_INDEX_ENCRYPTED);

				d.setUri(uri);
				d.setTags(tags);
				d.setEncrypted(encrypted);

				String[] taglist = getTaglist(c);
				d.setTagList(taglist);

				break;
			case DIALOG_GET_FROM_MARKET:
				DownloadOIAppDialog.onPrepareDialog(this, dialog);
				break;
		}
	}

	/**
	 * Create list of tags.
	 * 
	 * Tags for notes can be comma-separated. Here we create a list of the unique tags.
	 * 
	 * @param c
	 * @return
	 */
	String[] getTaglist(Cursor c) {
		// Create a set of all tags (every tag should only appear once).
		HashSet<String> tagset = new HashSet<String>();
		c.moveToPosition(-1);
		while (c.moveToNext()) {
			String tags = c.getString(NotesListCursor.COLUMN_INDEX_TAGS);
			if (tags != null) {
				// Split several tags in a line, separated by comma
				String[] smalltaglist = tags.split(",");
				for (String tag : smalltaglist) {
					if (!tag.equals("")) {
						tagset.add(tag.trim());
					}
				}
			}
		}

		// Sort the list
		// 1. Convert HashSet to String list.
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(tagset);
		// 2. Sort the String list 
		Collections.sort(list);
		// 3. Convert it to String array
		return list.toArray(new String[0]);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		// First see if note is encrypted
		Cursor c = mAdapter.getCursor();
		c.moveToPosition(position);

		long encrypted = c.getLong(NotesListCursor.COLUMN_INDEX_ENCRYPTED);

		String encryptedTitle = c.getString(NotesListCursor.COLUMN_INDEX_TITLE_ENCRYPTED);
		// are we in decrypted mode?
		//Log.i(TAG, "Encrypted title: " + encryptedTitle);

		String title = c.getString(NotesListCursor.COLUMN_INDEX_TITLE);
		//Log.i(TAG, "title: " + title);

		if (encrypted != 0) {
			if (!TextUtils.isEmpty(encryptedTitle)) {
				// Try to decrypt first
				//Log.i(TAG, "Decrypt first");

				Intent intent = new Intent();
				intent.setAction(CryptoIntents.ACTION_DECRYPT);
				intent.putExtra(CryptoIntents.EXTRA_TEXT, encryptedTitle);
				intent.putExtra(PrivateNotePadIntents.EXTRA_ENCRYPTED_TEXT, encryptedTitle);

				intent.putExtra(CryptoIntents.EXTRA_PROMPT, true);

				try {
					startActivityForResult(intent, REQUEST_CODE_DECRYPT_TITLE);
				} catch (ActivityNotFoundException e) {
					mDecryptionFailed = true;

					/*Toast.makeText(this,
							R.string.decryption_failed,
							Toast.LENGTH_SHORT).show();*/
					showDialog(DIALOG_GET_FROM_MARKET);
					Log.e(TAG, "failed to invoke encrypt");
				}
				return;
			}
		}

		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			// The caller is waiting for us to return a note selected by
			// the user. The have clicked on one, so return it now.
			setResult(RESULT_OK, new Intent().setData(uri));
			finish ();
		} else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
			Intent data = new Intent(Intent.ACTION_VIEW);
			data.setData(uri);

			String useTitle = title;
			if (encrypted != 0) {
				// Small security risk: The title is shown.
				// But ok, if someone wants to set a shortcut, they can choose
				// a good title.
				useTitle = encryptedTitle;
			}

			Intent shortcut = new Intent(Intent.ACTION_CREATE_SHORTCUT);
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, data);
			Intent.ShortcutIconResource sir = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_notepad);
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, sir);


			setResult(RESULT_OK, shortcut);
			finish();
		} else {
			// Launch activity to view/edit the currently selected item
			startActivity(new Intent(Intent.ACTION_EDIT, uri));
		}
	}


	protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
		if (debug) Log.i(TAG, "Received requestCode " + requestCode + ", resultCode " + resultCode);
		switch(requestCode) {
			case REQUEST_CODE_DECRYPT_TITLE:
				if (resultCode == RESULT_OK && intent != null) {
					String decryptedText = intent.getStringExtra (CryptoIntents.EXTRA_TEXT);
					String encryptedText = intent.getStringExtra (PrivateNotePadIntents.EXTRA_ENCRYPTED_TEXT);

					if (encryptedText != null) {
						//Log.i(TAG, "Encrypted text is not passed properly.");
						//return;


						// Add decrypted text to hash:
						NotesListCursor.mEncryptedStringHashMap.put(encryptedText, decryptedText);

						if (debug) Log.i(TAG, "Decrypted: " + encryptedText + " -> " + decryptedText);
					}
					mDecryptionSucceeded = true;
					NotesListCursor.mLoggedIn = true;
					if (debug) Log.d(TAG, "onActivity: logged in");

					// decrypt the next string.

					decryptDelayed();


				} else {
					mDecryptionFailed = true;
					setProgressBarIndeterminateVisibility(false);
				}
				break;
			case REQUEST_CODE_OPEN:
				if (resultCode == RESULT_OK && intent != null) {
					// File name should be in Uri:
					File filename = FileUriUtils.getFile(intent.getData());

					if (filename.exists() && !filename.isDirectory()) {
						// Open file in note editor
						Intent i = new Intent(this, NoteEditor.class);
						i.setAction(Intent.ACTION_VIEW);
						i.setData(intent.getData());
						startActivity(i);
					} else {
						Toast.makeText(this, R.string.file_not_found,
								Toast.LENGTH_SHORT).show();
					}
				}
				break;

				/*
    	case REQUEST_CODE_UNENCRYPT_NOTE:
    		if (resultCode == RESULT_OK && data != null) {
    			String[] decryptedTextArray = data.getStringArrayExtra(CryptoIntents.EXTRA_TEXT_ARRAY);
    			String decryptedText = decryptedTextArray[0];
    			String decryptedTitle = decryptedTextArray[1];

    			String uristring = data.getStringExtra(NotePadIntents.EXTRA_URI);

    			Uri uri = null;
    			if (uristring != null) {
    				uri = Uri.parse(uristring);
    			} else {
        	    	Log.i(TAG, "Wrong extra uri");
    				Toast.makeText(this,
        					"Encrypted information incomplete",
        					Toast.LENGTH_SHORT).show();
    				return;
    			}

    			// Write this to content provider:

                ContentValues values = new ContentValues();
                values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
                values.put(Notes.TITLE, decryptedTitle);
                values.put(Notes.NOTE, decryptedText);
                values.put(Notes.ENCRYPTED, 0);

                //Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), id);
                Uri noteUri = getIntent().getData();

                getContentResolver().update(uri, values, null, null);

    		} else {
    	        setProgressBarIndeterminateVisibility(false);
    		}
    		break;
				 */
		}
	}

	private void saveFile(Uri uri, File file) {
		if (debug) Log.i(TAG, "Saving file: uri: " + uri + ", file: " + file);
		Cursor c = getContentResolver().query(uri, new String[] {Notes.ENCRYPTED, Notes.NOTE}, null, null, null);

		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			long encrypted = c.getLong(0);
			String note = c.getString(1);
			if (encrypted == 0) {
				// Save to file
				if (debug) Log.d(TAG, "Save unencrypted file.");
				writeToFile(file, note);
			} else {
				// decrypt first, then save to file

				if (debug) Log.d(TAG, "Save encrypted file.");
			}
		} else {
			Log.e(TAG, "Error saving file: Uri not valid: " + uri);
		}
	}

	void writeToFile(File file, String text) {
		try {
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(text);
			out.close();
			Toast.makeText(this, R.string.note_saved,
					Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Toast.makeText(this, R.string.error_writing_file,
					Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error writing file");
		}
	}

	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			if (debug) Log.i(TAG, "flush decrypted data");
			NotesListCursor.flushDecryptedStringHashMap();
			mAdapter.getCursor().requery();
		}

	};

	/*

	// Note: onKeyDown is never called, because the 
	//       list filter consumes the event before.

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
        	// Delete the currently selected item (if any).
        	Log.i(TAG, "Selected item: " + getSelectedItemId());

        	return true;
        }
        return false;
    }
	 */
}
