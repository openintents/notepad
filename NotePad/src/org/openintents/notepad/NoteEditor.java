/* 
 * Copyright (C) 2008-2010 OpenIntents.org
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

package org.openintents.notepad;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openintents.intents.CryptoIntents;
import org.openintents.intents.NotepadIntents;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.activity.SaveFileActivity;
import org.openintents.notepad.crypto.EncryptActivity;
import org.openintents.notepad.dialog.DeleteConfirmationDialog;
import org.openintents.notepad.dialog.ThemeDialog;
import org.openintents.notepad.dialog.ThemeDialog.ThemeDialogListener;
import org.openintents.notepad.intents.NotepadInternalIntents;
import org.openintents.notepad.noteslist.NotesList;
import org.openintents.notepad.theme.ThemeAttributes;
import org.openintents.notepad.theme.ThemeNotepad;
import org.openintents.notepad.theme.ThemeUtils;
import org.openintents.notepad.util.ExtractTitle;
import org.openintents.notepad.util.FileUriUtils;
import org.openintents.notepad.util.SendNote;
import org.openintents.notepad.wrappers.WrapActionBar;
import org.openintents.util.MenuIntentOptionsWithIcons;
import org.openintents.util.UpperCaseTransformationMethod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A generic activity for editing a note in a database. This can be used either
 * to simply view a note {@link Intent#ACTION_VIEW}, view and edit a note
 * {@link Intent#ACTION_EDIT}, or create a new note {@link Intent#ACTION_INSERT}
 * .
 */
public class NoteEditor extends Activity implements ThemeDialogListener {
	private static final String TAG = "NoteEditor";
	private static final boolean debug = false;

	/**
	 * Standard projection for the interesting columns of a normal note.
	 */
	private static final String[] PROJECTION = new String[] { Notes._ID, // 0
			Notes.NOTE, // 1
			Notes.TAGS, // 2
			Notes.ENCRYPTED, // 3
			Notes.THEME, // 4
			Notes.SELECTION_START, // 5
			Notes.SELECTION_END, // 6
			Notes.SCROLL_POSITION, // 7
			Notes.COLOR, // 7
	};
	/** The index of the note column */
	private static final int COLUMN_INDEX_ID = 0;
	private static final int COLUMN_INDEX_NOTE = 1;
	private static final int COLUMN_INDEX_TAGS = 2;
	private static final int COLUMN_INDEX_ENCRYPTED = 3;
	private static final int COLUMN_INDEX_THEME = 4;
	private static final int COLUMN_INDEX_SELECTION_START = 5;
	private static final int COLUMN_INDEX_SELECTION_END = 6;
	private static final int COLUMN_INDEX_SCROLL_POSITION = 7;
	private static final int COLUMN_INDEX_COLOR = 8;

	// This is our state data that is stored when freezing.
	private static final String BUNDLE_ORIGINAL_CONTENT = "original_content";
	private static final String BUNDLE_UNDO_REVERT = "undo_revert";
	private static final String BUNDLE_STATE = "state";
	private static final String BUNDLE_URI = "uri";
	private static final String BUNDLE_SELECTION_START = "selection_start";
	private static final String BUNDLE_SELECTION_STOP = "selection_stop";
	// private static final String BUNDLE_FILENAME = "filename";
	private static final String BUNDLE_FILE_CONTENT = "file_content";
	private static final String BUNDLE_APPLY_TEXT = "apply_text";
	private static final String BUNDLE_APPLY_TEXT_BEFORE = "apply_text_before";
	private static final String BUNDLE_APPLY_TEXT_AFTER = "apply_text_after";

	// Identifiers for our menu items.
	private static final int MENU_REVERT = Menu.FIRST;
	private static final int MENU_DISCARD = Menu.FIRST + 1;
	private static final int MENU_DELETE = Menu.FIRST + 2;
	private static final int MENU_ENCRYPT = Menu.FIRST + 3;
	private static final int MENU_UNENCRYPT = Menu.FIRST + 4;
	private static final int MENU_IMPORT = Menu.FIRST + 5;
	private static final int MENU_SAVE = Menu.FIRST + 6;
	private static final int MENU_SAVE_AS = Menu.FIRST + 7;
	private static final int MENU_THEME = Menu.FIRST + 8;
	private static final int MENU_SETTINGS = Menu.FIRST + 9;
	private static final int MENU_SEND = Menu.FIRST + 10;
	private static final int MENU_WORD_COUNT = Menu.FIRST + 11;
	private static final int MENU_COLOR = Menu.FIRST + 12;

	// private static final int REQUEST_CODE_ENCRYPT = 1;
	private static final int REQUEST_CODE_DECRYPT = 2;
	private static final int REQUEST_CODE_TEXT_SELECTION_ALTERNATIVE = 3;
	private static final int REQUEST_CODE_SAVE_AS = 4;

	// The different distinct states the activity can be run in.
	private static final int STATE_EDIT = 0;
	private static final int STATE_INSERT = 1;
	private static final int STATE_EDIT_NOTE_FROM_SDCARD = 2;
	private static final int STATE_EDIT_EXTERNAL_NOTE = 3;

	private static final int DIALOG_UNSAVED_CHANGES = 1;
	private static final int DIALOG_THEME = 2;
	private static final int DIALOG_DELETE = 3;

	private static final int GROUP_ID_TEXT_SELECTION_ALTERNATIVE = 1234; // some
																			// number
																			// that
																			// must
																			// not
																			// collide
																			// with
																			// others

	private int mState;
	private boolean mNoteOnly = false;
	private Uri mUri;
	private Cursor mCursor;
	private EditText mText;
	private String mOriginalContent;
	private String mUndoRevert;
	private int mSelectionStart;
	private int mSelectionStop;

	// If the following are not null, the result of
	// a text change (TEXT_SELECTION_ALTERNATIVE) still needs to be applied.
	private String mApplyText;
	private String mApplyTextBefore;
	private String mApplyTextAfter;

	// Whether this note is stored in encrypted format
	private long mEncrypted;
	private String mDecryptedText;

	private int mColor = -1;

	/**
	 * static string for hack. Only used for configuration changes.
	 */
	private static String sDecryptedText = null;
	private static int sSelectionStart = 0;
	private static int sSelectionStop = 0;

	private static final int BLUE = 1;
	private static final int GREEN = 2;
	private static final int GREY = 3;
	private static final int PINK = 4;
	private static final int YELLOW = 5;


	private String mFileContent;

	// private String mTags;

	private String mTheme;

	Typeface mCurrentTypeface = null;
	public String mTextTypeface;
	public float mTextSize;
	public boolean mTextUpperCaseFont;
	public int mTextColor;
	public int mBackgroundPadding;

	/**
	 * Which features are supported (which columns are available in the
	 * database)? Everything is supported by default.
	 */
	private boolean hasNoteColumn = true;
	private boolean hasTagsColumn = true;
	private boolean hasEncryptionColumn = true;
	private boolean hasColorColumn = true;
	private boolean hasThemeColumn = true;
	private boolean hasSelection_startColumn = true;
	private boolean hasSelection_endColumn = true;

	/**
	 * Lines mode: 0..no line. 2..show lines only where there is text (padding
	 * width). 3..show lines only where there is text (full width). 4..show
	 * lines for whole page (padding width). 5..show lines for whole page (full
	 * width).
	 */
	public static int mLinesMode;
	public static int mLinesColor;

	private static boolean mActionBarAvailable;

	static {
		try {
			WrapActionBar.checkAvailable();
			mActionBarAvailable = true;
		} catch (Throwable t) {
			mActionBarAvailable = false;
		}
	}

	/**
	 * A custom EditText that draws lines between each line of text that is
	 * displayed.
	 */
	public static class LinedEditText extends EditText {
		private Rect mRect;
		private Paint mPaint;

		// we need this constructor for LayoutInflater
		public LinedEditText(Context context, AttributeSet attrs) {
			super(context, attrs);

			mRect = new Rect();
			mPaint = new Paint();
			mPaint.setStyle(Paint.Style.STROKE);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			boolean fullWidth = (mLinesMode & 1) == 1;
			boolean textlines = (mLinesMode & 2) == 2;
			boolean pagelines = (mLinesMode & 4) == 4;
			if (textlines || pagelines) {
				mPaint.setColor(mLinesColor);

				int count = getLineCount();
				Rect r = mRect;
				Paint paint = mPaint;

				int height = getHeight();
				int line_height = getLineHeight();
				int page_size = height / line_height + 1;

				int baseline = 0;
				int left = 0;
				int right = 0;
				for (int i = 0; i < count; i++) {
					baseline = getLineBounds(i, r);
					left = r.left;
					right = r.right;
					if (fullWidth) {
						left = getLeft();
						right = getRight();
					}
					canvas.drawLine(left, baseline + 1, right, baseline + 1,
							paint);
				}
				if (pagelines) {
					// Fill the rest of the page with lines
					for (int i = count; i < page_size; i++) {
						baseline += line_height;
						canvas.drawLine(left, baseline + 1, right,
								baseline + 1, paint);
					}
				}
			}

			super.onDraw(canvas);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (debug)
			Log.d(TAG, "onCreate()");

		if (getIntent().getAction().equals(Intent.ACTION_CREATE_SHORTCUT)) {
			createShortcut();
			return;
		}

		if (savedInstanceState == null) {
			// sDecryptedText has no use for brand new activities
			sDecryptedText = null;
		}

		// Usually, sDecryptedText == null.
		mDecryptedText = sDecryptedText;
		if (sDecryptedText != null) {
			// we use the text right now,
			// so don't encrypt the text anymore.
			EncryptActivity.cancelEncrypt();

			if (EncryptActivity.getPendingEncryptActivities() == 0) {
				if (debug)
					Log.d(TAG, "sDecryptedText = null");
				// no more encrypt activies will be called
				sDecryptedText = null;
			}
		}

		mSelectionStart = 0;
		mSelectionStop = 0;

		// If an instance of this activity had previously stopped, we can
		// get the original text it started with.
		if (savedInstanceState != null) {
			mOriginalContent = savedInstanceState
					.getString(BUNDLE_ORIGINAL_CONTENT);
			mUndoRevert = savedInstanceState.getString(BUNDLE_UNDO_REVERT);
			mState = savedInstanceState.getInt(BUNDLE_STATE);
			mUri = Uri.parse(savedInstanceState.getString(BUNDLE_URI));
			mSelectionStart = savedInstanceState.getInt(BUNDLE_SELECTION_START);
			mSelectionStop = savedInstanceState.getInt(BUNDLE_SELECTION_STOP);
			mFileContent = savedInstanceState.getString(BUNDLE_FILE_CONTENT);
			if (mApplyText == null && mApplyTextBefore == null
					&& mApplyTextAfter == null) {
				// Only read values if they had not been set by
				// onActivityResult() yet:
				mApplyText = savedInstanceState.getString(BUNDLE_APPLY_TEXT);
				mApplyTextBefore = savedInstanceState
						.getString(BUNDLE_APPLY_TEXT_BEFORE);
				mApplyTextAfter = savedInstanceState
						.getString(BUNDLE_APPLY_TEXT_AFTER);
			}
		} else {
			// Do some setup based on the action being performed.
			final Intent intent = getIntent();

			final String action = intent.getAction();
			if (Intent.ACTION_EDIT.equals(action)
					|| Intent.ACTION_VIEW.equals(action)) {
				// Requested to edit: set that state, and the data being edited.
				mState = STATE_EDIT;
				mUri = intent.getData();

				if (mUri.getScheme().equals("file")) {
					mState = STATE_EDIT_NOTE_FROM_SDCARD;
					// Load the file into a new note.

					mFileContent = readFile(FileUriUtils.getFile(mUri));
				} else if (!mUri.getAuthority().equals(NotePad.AUTHORITY)) {
					// Note a notepad note. Treat slightly differently.
					// (E.g. a note from OI Shopping List)
					mState = STATE_EDIT_EXTERNAL_NOTE;
				}
				/*
				 * if (mUri.getScheme().equals("file")) { // Load the file into
				 * a new note.
				 * 
				 * mFilename = FileUriUtils.getFilename(mUri);
				 * 
				 * String text = readFile(FileUriUtils.getFile(mUri));
				 * 
				 * if (text == null) { Log.e(TAG, "Error reading file");
				 * finish(); return; }
				 * 
				 * 
				 * 
				 * // Let's check whether the exactly same note already exists
				 * or not: Cursor c =
				 * getContentResolver().query(Notes.CONTENT_URI, new String[]
				 * {Notes._ID}, Notes.NOTE + " = ?", new String[] {text}, null);
				 * if (c != null && c.getCount() > 0) { // Same note exists
				 * already: c.moveToFirst(); long id = c.getLong(0); mUri =
				 * ContentUris.withAppendedId(Notes.CONTENT_URI, id); } else {
				 * 
				 * // Add new note // Requested to insert: set that state, and
				 * create a new entry // in the container. mState =
				 * STATE_INSERT; ContentValues values = new ContentValues();
				 * values.put(Notes.NOTE, text); mUri =
				 * getContentResolver().insert(Notes.CONTENT_URI, values);
				 * intent.setAction(Intent.ACTION_EDIT); intent.setData(mUri);
				 * setIntent(intent);
				 * 
				 * // If we were unable to create a new note, then just finish
				 * // this activity. A RESULT_CANCELED will be sent back to the
				 * // original activity if they requested a result. if (mUri ==
				 * null) { Log.e(TAG, "Failed to insert new note into " +
				 * getIntent().getData()); finish(); return; }
				 * 
				 * // The new entry was created, so assume all will end well and
				 * // set the result to be returned. //setResult(RESULT_OK, (new
				 * Intent()).setAction(mUri.toString())); setResult(RESULT_OK,
				 * intent); }
				 * 
				 * }
				 */
			} else if (Intent.ACTION_INSERT.equals(action)
					|| Intent.ACTION_SEND.equals(action)) {
				// Use theme of most recently modified note:
				ContentValues values = new ContentValues(1);
				String theme = getMostRecentlyUsedTheme();
				values.put(Notes.THEME, theme);

				String tags = intent
						.getStringExtra(NotepadInternalIntents.EXTRA_TAGS);
				values.put(Notes.TAGS, tags);

				if (mText != null) {
					values.put(Notes.SELECTION_START, mText.getSelectionStart());
					values.put(Notes.SELECTION_END, mText.getSelectionEnd());
				}

				// Requested to insert: set that state, and create a new entry
				// in the container.
				mState = STATE_INSERT;
				/*
				 * intent.setAction(Intent.ACTION_EDIT); intent.setData(mUri);
				 * setIntent(intent);
				 */

				if (Intent.ACTION_SEND.equals(action)) {
					values.put(Notes.NOTE,
							getIntent().getStringExtra(Intent.EXTRA_TEXT));
					mUri = getContentResolver().insert(Notes.CONTENT_URI,
							values);
				} else {
					mUri = getContentResolver()
							.insert(intent.getData(), values);
				}

				// If we were unable to create a new note, then just finish
				// this activity. A RESULT_CANCELED will be sent back to the
				// original activity if they requested a result.
				if (mUri == null) {
					Log.e(TAG, "Failed to insert new note into "
							+ getIntent().getData());
					finish();
					return;
				}

				// The new entry was created, so assume all will end well and
				// set the result to be returned.
				// setResult(RESULT_OK, (new
				// Intent()).setAction(mUri.toString()));
				setResult(RESULT_OK, intent);

			} else {
				// Whoops, unknown action! Bail.
				Log.e(TAG, "Unknown action, exiting");
				finish();
				return;
			}
		}

		// setup actionbar
		if (mActionBarAvailable) {
			requestWindowFeature(Window.FEATURE_ACTION_BAR);
			WrapActionBar bar = new WrapActionBar(this);
			bar.setDisplayHomeAsUpEnabled(true);
			// force to show the actionbar on version 14+
			if (Integer.valueOf(android.os.Build.VERSION.SDK) >= 14) {
				bar.setHomeButtonEnabled(true);
			}
		} else {
			requestWindowFeature(Window.FEATURE_RIGHT_ICON);
		}

		// Set the layout for this activity. You can find it in
		// res/layout/note_editor.xml
		setContentView(R.layout.note_editor);

		// The text view for our note, identified by its ID in the XML file.
		mText = (EditText) findViewById(R.id.note);

		if (mState == STATE_EDIT_NOTE_FROM_SDCARD) {
			// We add a text watcher, so that the title can be updated
			// to indicate a small "*" if modified.
			mText.addTextChangedListener(mTextWatcherSdCard);
		}

		if (mState != STATE_EDIT_NOTE_FROM_SDCARD) {
			// Check if we load a note from notepad or from some external module
			if (mState == STATE_EDIT_EXTERNAL_NOTE) {
				// Get all the columns as we don't know which columns are
				// supported.
				mCursor = managedQuery(mUri, null, null, null, null);

				// Now check which columns are available
				List<String> columnNames = Arrays.asList(mCursor
						.getColumnNames());

				if (!columnNames.contains(Notes.NOTE)) {
					hasNoteColumn = false;
				}
				if (!columnNames.contains(Notes.TAGS)) {
					hasTagsColumn = false;
				}
				if (!columnNames.contains(Notes.ENCRYPTED)) {
					hasEncryptionColumn = false;
				}
				if (!columnNames.contains(Notes.THEME)) {
					hasThemeColumn = false;
				}
				if(!columnNames.contains(Notes.COLOR)){
					hasColorColumn = false;
				}
				if(!columnNames.contains(Notes.SELECTION_START)){
					hasSelection_startColumn = false;
				}
				if (!columnNames.contains(Notes.SELECTION_END)) {
					hasSelection_endColumn = false;
				}
			} else {
				// Get the note!
				mCursor = managedQuery(mUri, PROJECTION, null, null, null);

				// It's not an external note, so all the columns are available
				// in the database
			}
		} else {
			mCursor = null;
		}

		mText.addTextChangedListener(mTextWatcherCharCount);
	}

	/**
	 * Return intent data when invoked with
	 * action=android.intent.action.CREATE_SHORTCUT
	 */
	private void createShortcut() {
		Intent intent = new Intent(Intent.ACTION_INSERT, Notes.CONTENT_URI,
				getApplicationContext(), NoteEditor.class);

		Intent result = new Intent();
		result.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		result.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				ShortcutIconResource.fromContext(getApplicationContext(),
						R.drawable.ic_launcher_notepad));
		result.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				getString(R.string.new_note));

		setResult(RESULT_OK, result);

		finish();
	}

	/**
	 * Returns most recently used theme, or null.
	 * 
	 * @return
	 */
	private String getMostRecentlyUsedTheme() {
		String theme = null;
		Cursor c = getContentResolver().query(Notes.CONTENT_URI,
				new String[] { Notes.THEME }, null, null,
				Notes.MODIFIED_DATE + " DESC");
		if (c != null && c.moveToFirst()) {
			theme = c.getString(0);
		}
		c.close();
		return theme;
	}

	private TextWatcher mTextWatcherSdCard = new TextWatcher() {
		public void afterTextChanged(Editable s) {
			// if (debug) Log.d(TAG, "after");
			mFileContent = s.toString();
			updateTitleSdCard();
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// if (debug) Log.d(TAG, "before");
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// if (debug) Log.d(TAG, "on");
		}

	};

	private TextWatcher mTextWatcherCharCount = new TextWatcher() {
		public void afterTextChanged(Editable s) {
			updateCharCount();
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
	};

	public String readFile(File file) {

		FileInputStream fis = null;
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		StringBuffer sb = new StringBuffer();

		try {
			fis = new FileInputStream(file);

			// Here BufferedInputStream is added for fast reading.
			bis = new BufferedInputStream(fis);
			dis = new DataInputStream(bis);

			// dis.available() returns 0 if the file does not have more lines.
			while (dis.available() != 0) {

				// this statement reads the line from the file and print it to
				// the console.
				sb.append(dis.readLine());
				if (dis.available() != 0) {
					sb.append("\n");
				}
			}

			// dispose all the resources after using them.
			fis.close();
			bis.close();
			dis.close();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found", e);
			Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT)
					.show();
			return null;
		} catch (IOException e) {
			Log.e(TAG, "File not found", e);
			Toast.makeText(this, R.string.error_reading_file,
					Toast.LENGTH_SHORT).show();
			return null;
		}

		return sb.toString();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (debug)
			Log.d(TAG, "onResume");

		if (debug)
			Log.d(TAG, "mDecrypted: " + mDecryptedText);

		// Set auto-link on or off, based on the current setting.
		int autoLink = PreferenceActivity.getAutoLinkFromPreference(this);

		mText.setAutoLinkMask(autoLink);

		mEncrypted = 0;

		if (mState == STATE_EDIT || mState == STATE_INSERT
				|| mState == STATE_EDIT_EXTERNAL_NOTE) {
			getNoteFromContentProvider();
		} else if (mState == STATE_EDIT_NOTE_FROM_SDCARD) {
			getNoteFromFile();
		}

		if (mEncrypted == 0 || mDecryptedText != null) {
			applyInsertText();
		}

		// Make sure that we don't use the link movement method.
		// Instead, we need a blend between the arrow key movement (for regular
		// navigation) and
		// the link movement (so the user can click on links).
		mText.setMovementMethod(new ArrowKeyMovementMethod() {
			public boolean onTouchEvent(TextView widget, Spannable buffer,
					MotionEvent event) {
				// This block is copied and pasted from LinkMovementMethod's
				// onTouchEvent (without the part that actually changes the
				// selection).
				int action = event.getAction();

				if (action == MotionEvent.ACTION_UP) {
					int x = (int) event.getX();
					int y = (int) event.getY();

					x -= widget.getTotalPaddingLeft();
					y -= widget.getTotalPaddingTop();

					x += widget.getScrollX();
					y += widget.getScrollY();

					Layout layout = widget.getLayout();
					int line = layout.getLineForVertical(y);
					int off = layout.getOffsetForHorizontal(line, x);

					ClickableSpan[] link = buffer.getSpans(off, off,
							ClickableSpan.class);

					if (link.length != 0) {
						link[0].onClick(widget);
						return true;
					}
				}

				return super.onTouchEvent(widget, buffer, event);
			}
		});

		setTheme(loadTheme());
	}

	private void getNoteFromContentProvider() {
		// If we didn't have any trouble retrieving the data, it is now
		// time to get at the stuff.
		if (mCursor != null && mCursor.requery() && mCursor.moveToFirst()) {

			// Modify our overall title depending on the mode we are running in.
			if (mState == STATE_EDIT || mState == STATE_EDIT_EXTERNAL_NOTE) {
				setTitle(getText(R.string.title_edit));
			} else if (mState == STATE_INSERT) {
				setTitle(getText(R.string.title_create));
			}

			// This always has to be available
			long id = mCursor.getLong(mCursor.getColumnIndex(Notes._ID));
			String note = "";

			if (mState == STATE_EDIT_EXTERNAL_NOTE) {
				// Check if the other columns are available

				// Note
				if (hasNoteColumn) {
					note = mCursor
							.getString(mCursor.getColumnIndex(Notes.NOTE));
				} else {
					note = "";
				}

				// Encrypted
				mEncrypted = isNoteUnencrypted() ? 0 : 1;

				// Theme
				if (hasThemeColumn) {
					mTheme = mCursor.getString(mCursor
							.getColumnIndex(Notes.THEME));
				} else {
					note = "";
				}

				// Selection start
				if (hasSelection_startColumn) {
					mSelectionStart = mCursor.getInt(mCursor
							.getColumnIndex(Notes.SELECTION_START));
				} else {
					mSelectionStart = 0;
				}

				// Selection end
				if (hasSelection_endColumn) {
					mSelectionStop = mCursor.getInt(mCursor
							.getColumnIndex(Notes.SELECTION_END));
				} else {
					mSelectionStop = 0;
				}
			} else {
				// We know for sure all the columns are available
				note = mCursor.getString(COLUMN_INDEX_NOTE);
				mEncrypted = mCursor.getLong(COLUMN_INDEX_ENCRYPTED);
				mTheme = mCursor.getString(COLUMN_INDEX_THEME);
				mSelectionStart = mCursor.getInt(COLUMN_INDEX_SELECTION_START);
				mSelectionStop = mCursor.getInt(COLUMN_INDEX_SELECTION_END);
				mColor = mCursor.getInt(COLUMN_INDEX_COLOR);
			}

			if (mEncrypted == 0) {
				// Not encrypted

				// This is a little tricky: we may be resumed after previously
				// being
				// paused/stopped. We want to put the new text in the text view,
				// but leave the user where they were (retain the cursor
				// position
				// etc). This version of setText does that for us.
				if (!note.equals(mText.getText().toString())) {
					mText.setTextKeepState(note);
					// keep state does not work, so we have to do it manually:
					mText.setSelection(mSelectionStart, mSelectionStop);
				}
			} else {
				if (mDecryptedText != null) {
					// Text had already been decrypted, use that:
					if (debug)
						Log.d(TAG, "set decrypted text as mText: "
								+ mDecryptedText);
					mText.setTextKeepState(mDecryptedText);
					// keep state does not work, so we have to do it manually:
					mText.setSelection(mSelectionStart, mSelectionStop);

					if (!mActionBarAvailable) {
						setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON,
								android.R.drawable.ic_lock_idle_lock);
					}
				} else {
					// Decrypt note
					if (debug)
						Log.d(TAG, "Decrypt note: " + note);

					// Overwrite mText because it may contain unencrypted note
					// from savedInstanceState.
					// mText.setText(R.string.encrypted);

					Intent i = new Intent();
					i.setAction(CryptoIntents.ACTION_DECRYPT);
					i.putExtra(CryptoIntents.EXTRA_TEXT, note);
					i.putExtra(PrivateNotePadIntents.EXTRA_ID, id);

					try {
						startActivityForResult(i, REQUEST_CODE_DECRYPT);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(this, R.string.decryption_failed,
								Toast.LENGTH_SHORT).show();
						Log.e(TAG, "failed to invoke decrypt");
					}
				}
			}

			// If we hadn't previously retrieved the original text, do so
			// now. This allows the user to revert their changes.
			if (mOriginalContent == null) {
				mOriginalContent = note;
			}

		} else {
			setTitle(getText(R.string.error_title));
			mText.setText(getText(R.string.error_message));
		}
	}

	private void getNoteFromFile() {
		if (debug)
			Log.d(TAG, "file: " + mFileContent);

		mText.setTextKeepState(mFileContent);
		// keep state does not work, so we have to do it manually:
		try {
			mText.setSelection(mSelectionStart, mSelectionStop);
		} catch (IndexOutOfBoundsException e) {
			// Then let's not adjust the selection.
		}

		// If we hadn't previously retrieved the original text, do so
		// now. This allows the user to revert their changes.
		if (mOriginalContent == null) {
			mOriginalContent = mFileContent;
		}

		updateTitleSdCard();
	}

	private void updateTitleSdCard() {
		String modified = "";
		if (mOriginalContent != null && !mOriginalContent.equals(mFileContent)) {
			modified = "* ";
		}
		String filename = FileUriUtils.getFilename(mUri);
		setTitle(modified + filename);
		// setTitle(getString(R.string.title_edit_file, modified + filename));
		// setFeatureDrawableResource(Window.FEATURE_RIGHT_ICON,
		// android.R.drawable.ic_menu_save);
	}

	private void updateCharCount() {
		boolean charCountVisible = false;
		String currentTitle = getTitle().toString();
		if (currentTitle.startsWith("[")) {
			charCountVisible = true;
		}
		if (PreferenceActivity.getCharCountEnabledFromPrefs(this)) {
			if (charCountVisible) {
				setTitle("[" + mText.length() + "]"
						+ currentTitle.substring(currentTitle.indexOf(" ")));
			} else {
				setTitle("[" + mText.length() + "] " + currentTitle);
			}
		} else {
			if (charCountVisible) {
				setTitle(currentTitle.substring(currentTitle.indexOf(" ")));
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (debug)
			Log.d(TAG, "onSaveInstanceState");
		// if (debug) Log.d(TAG, "file content: " + mFileContent);

		// Save away the original text, so we still have it if the activity
		// needs to be killed while paused.
		mSelectionStart = mText.getSelectionStart();
		mSelectionStop = mText.getSelectionEnd();
		mFileContent = mText.getText().toString();

		if (debug)
			Log.d(TAG, "Selection " + mSelectionStart + " - " + mSelectionStop
					+ " for text : " + mFileContent);

		outState.putString(BUNDLE_ORIGINAL_CONTENT, mOriginalContent);
		outState.putString(BUNDLE_UNDO_REVERT, mUndoRevert);
		outState.putInt(BUNDLE_STATE, mState);
		outState.putString(BUNDLE_URI, mUri.toString());
		outState.putInt(BUNDLE_SELECTION_START, mSelectionStart);
		outState.putInt(BUNDLE_SELECTION_STOP, mSelectionStop);
		outState.putString(BUNDLE_FILE_CONTENT, mFileContent);
		outState.putString(BUNDLE_APPLY_TEXT, mApplyText);
		outState.putString(BUNDLE_APPLY_TEXT_BEFORE, mApplyTextBefore);
		outState.putString(BUNDLE_APPLY_TEXT_AFTER, mApplyTextAfter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (debug)
			Log.d(TAG, "onPause");

		mText.setAutoLinkMask(0);

		// The user is going somewhere else, so make sure their current
		// changes are safely saved away in the provider. We don't need
		// to do this if only editing.
		if (mCursor != null) {

			mCursor.moveToFirst();

			if (isNoteUnencrypted()) {
				String text = mText.getText().toString();
				int length = text.length();

				// If this activity is finished, and there is no text, then we
				// do something a little special: simply delete the note entry.
				// Note that we do this both for editing and inserting... it
				// would be reasonable to only do it when inserting.
				if (isFinishing() && (length == 0) && !mNoteOnly) {
					setResult(RESULT_CANCELED);
					deleteNote();

					// Get out updates into the provider.
				} else {
					ContentValues values = new ContentValues();

					// This stuff is only done when working with a full-fledged
					// note.
					if (!mNoteOnly) {
						String oldText = "";
						Cursor cursor = getContentResolver().query(mUri,
								new String[] { "note" }, null, null, null);
						if (cursor.moveToFirst()) {
							oldText = cursor.getString(0);
						}
						if (!oldText.equals(text)) {
							// Bump the modification time to now.
							values.put(Notes.MODIFIED_DATE,
									System.currentTimeMillis());
						}

						String title;
						if (PreferenceActivity.getMarqueeFromPrefs(this) == false) {
							title = ExtractTitle.extractTitle(text);
						} else {
							title = text;
						}
						values.put(Notes.TITLE, title);
					}

					// Write our text back into the provider.
					if (hasNoteColumn) {
						values.put(Notes.NOTE, text);
					}
					if (hasThemeColumn) {
						values.put(Notes.THEME, mTheme);
					}
					if(hasColorColumn){
						values.put(Notes.COLOR, mColor);
					}
					if(hasSelection_startColumn){
						values.put(Notes.SELECTION_START, mText.getSelectionStart());
					}
					if(hasSelection_endColumn){
						values.put(Notes.SELECTION_END, mText.getSelectionEnd());
					}

					// Commit all of our changes to persistent storage. When the
					// update completes
					// the content provider will notify the cursor of the
					// change, which will
					// cause the UI to be updated.
					getContentResolver().update(mUri, values, null, null);
				}
			} else {
				// encrypted note: First encrypt and store encrypted note:

				// Save current theme:
				ContentValues values = new ContentValues();

				if (hasThemeColumn) {
					values.put(Notes.THEME, mTheme);
				}
				if(hasColorColumn){
					values.put(Notes.COLOR, mColor);
				}

				getContentResolver().update(mUri, values, null, null);

				if (mDecryptedText != null) {
					// Decrypted had been decrypted.
					// We take the current version from 'text' and encrypt it.

					encryptNote(false);

					// Remove displayed note.
					// mText.setText(R.string.encrypted);
				}
			}
		}
	}

	/**
	 * Encrypt the current note.
	 * 
	 * @param text
	 */
	private void encryptNote(boolean encryptTags) {
		String text = mText.getText().toString();
		String title;
		if (PreferenceActivity.getMarqueeFromPrefs(this) == false) {
			title = ExtractTitle.extractTitle(text);
		} else {
			title = text;
		}
		String tags = getTags();
		// Log.i(TAG, "encrypt tags: " + tags);

		boolean isNoteEncrypted = !isNoteUnencrypted();

		if (!encryptTags) {
			tags = null;
		}

		if (debug)
			Log.d(TAG, "encrypt note: " + text);

		if (EncryptActivity.getPendingEncryptActivities() == 0) {
			Intent i = new Intent(this, EncryptActivity.class);
			i.putExtra(PrivateNotePadIntents.EXTRA_ACTION,
					CryptoIntents.ACTION_ENCRYPT);
			i.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY,
					EncryptActivity.getCryptoStringArray(text, title, tags));
			i.putExtra(PrivateNotePadIntents.EXTRA_URI, mUri.toString());
			if (text.equals(mOriginalContent) && isNoteEncrypted) {
				// No need to encrypt, content was not modified.
				i.putExtra(PrivateNotePadIntents.EXTRA_CONTENT_UNCHANGED, true);
			}
			startActivity(i);

			// Remove knowledge of the decrypted note.
			// If encryption fails because one has been locked out, (another)
			// user
			// should not be able to see note again from cache.
			if (debug)
				Log.d(TAG, "using static decrypted text: " + text);
			sDecryptedText = text;
			if (isNoteEncrypted) {
				// Already encrypted
				mDecryptedText = null;
				mText.setText(R.string.encrypted);
			} else {
				// not yet encrypted, but we want to encrypt.
				// Leave mText until note is really encrypted
				// (in case password is not entered or OI Safw not installed)
			}
			EncryptActivity.confirmEncryptActivityCalled();
		} else {
			// encrypt already called
			if (debug)
				Log.d(TAG, "encrypt already called");

		}

	}

	public static void deleteStaticDecryptedText() {
		if (debug)
			Log.d(TAG, "deleting decrypted text: " + sDecryptedText);
		sDecryptedText = null;
	}

	/**
	 * Unencrypt the current note.
	 * 
	 * @param text
	 */
	private void unencryptNote() {
		String text = mText.getText().toString();
		String title = ExtractTitle.extractTitle(text);
		String tags = getTags();
		// Log.i(TAG, "unencrypt tags: " + tags);

		ContentValues values = new ContentValues();
		values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
		values.put(Notes.TITLE, title);
		values.put(Notes.NOTE, text);
		values.put(Notes.ENCRYPTED, 0);

		getContentResolver().update(mUri, values, null, null);
		mCursor.requery();

		if (!mActionBarAvailable) {
			setFeatureDrawable(Window.FEATURE_RIGHT_ICON, null);
		}

		// Small trick: Tags have not been converted properly yet. Let's do it
		// now:
		Intent i = new Intent(this, EncryptActivity.class);
		i.putExtra(PrivateNotePadIntents.EXTRA_ACTION,
				CryptoIntents.ACTION_DECRYPT);
		i.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY,
				EncryptActivity.getCryptoStringArray(null, null, tags));
		i.putExtra(PrivateNotePadIntents.EXTRA_URI, mUri.toString());
		startActivity(i);
	}

	private String getTags() {
		String tags;

		// Check if there is a tags column in the database
		int index;
		if ((index = mCursor.getColumnIndex(Notes.TAGS)) != -1) {
			tags = mCursor.getString(index);
		} else {
			tags = "";
		}

		if (!TextUtils.isEmpty(tags)) {
			return tags;
		} else {
			return "";
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Build the menus that are shown when editing.

		// if (!mOriginalContent.equals(mText.getText().toString())) {

		menu.add(0, MENU_REVERT, 0, R.string.menu_revert).setShortcut('0', 'r')
				.setIcon(android.R.drawable.ic_menu_revert);
		// }

		menu.add(1, MENU_ENCRYPT, 0, R.string.menu_encrypt)
				.setShortcut('1', 'e').setIcon(android.R.drawable.ic_lock_lock); // TODO:
																					// better
																					// icon

		menu.add(1, MENU_UNENCRYPT, 0, R.string.menu_undo_encryption)
				.setShortcut('1', 'e').setIcon(android.R.drawable.ic_lock_lock); // TODO:
																					// better
																					// icon

		MenuItem item = menu.add(1, MENU_DELETE, 0, R.string.menu_delete);
		item.setIcon(android.R.drawable.ic_menu_delete);

		menu.add(2, MENU_IMPORT, 0, R.string.menu_import).setShortcut('1', 'i')
				.setIcon(android.R.drawable.ic_menu_add);

		menu.add(2, MENU_SAVE, 0, R.string.menu_save).setShortcut('2', 's')
				.setIcon(android.R.drawable.ic_menu_save);

		menu.add(2, MENU_SAVE_AS, 0, R.string.menu_save_as)
				.setShortcut('3', 'w').setIcon(android.R.drawable.ic_menu_save);

		menu.add(3, MENU_THEME, 0, R.string.menu_theme)
				.setIcon(android.R.drawable.ic_menu_manage)
				.setShortcut('4', 't');

		menu.add(3, MENU_SETTINGS, 0, R.string.settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setShortcut('9', 'p');

		item = menu.add(4, MENU_SEND, 0, R.string.menu_share);
		item.setIcon(android.R.drawable.ic_menu_share);
		if (mActionBarAvailable) {
			WrapActionBar.showIfRoom(item);
		}

		item = menu.add(4, MENU_COLOR, 0, R.string.menu_color);
		item.setIcon( R.drawable.notes_btn_changecolors );
		if(mActionBarAvailable){
			WrapActionBar.showIfRoom(item);
		}

		menu.add(5, MENU_WORD_COUNT, 0, R.string.menu_word_count);

		/*
		 * if (mState == STATE_EDIT) {
		 * 
		 * menu.add(0, REVERT_ID, 0, R.string.menu_revert) .setShortcut('0',
		 * 'r') .setIcon(android.R.drawable.ic_menu_revert);
		 * 
		 * if (!mNoteOnly) { menu.add(1, DELETE_ID, 0, R.string.menu_delete)
		 * .setShortcut('1', 'd') .setIcon(android.R.drawable.ic_menu_delete); }
		 * 
		 * // Build the menus that are shown when inserting. } else {
		 * menu.add(1, DISCARD_ID, 0, R.string.menu_discard) .setShortcut('0',
		 * 'd') .setIcon(android.R.drawable.ic_menu_delete); }
		 */

		// If we are working on a full note, then append to the
		// menu items for any other activities that can do stuff with it
		// as well. This does a query on the system for any activities that
		// implement the ALTERNATIVE_ACTION for our data, adding a menu item
		// for each one that is found.
		if (!mNoteOnly) {
			// We use mUri instead of getIntent().getData() in the
			// following line, because mUri may have changed when inserting
			// a new note.
			Intent intent = new Intent(null, mUri);
			intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			// menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
			// new ComponentName(this, NoteEditor.class), null, intent, 0,
			// null);

			// Workaround to add icons:
			MenuIntentOptionsWithIcons menu2 = new MenuIntentOptionsWithIcons(
					this, menu);
			menu2.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
					new ComponentName(this, NoteEditor.class), null, intent, 0,
					null);

			// Add menu items for category CATEGORY_TEXT_SELECTION_ALTERNATIVE
			intent = new Intent(); // Don't pass data for this intent
			intent.addCategory(NotepadIntents.CATEGORY_TEXT_SELECTION_ALTERNATIVE);
			intent.setType("text/plain");
			// Workaround to add icons:
			menu2.addIntentOptions(GROUP_ID_TEXT_SELECTION_ALTERNATIVE, 0, 0,
					new ComponentName(this, NoteEditor.class), null, intent, 0,
					null);

		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Show "revert" menu item only if content has changed.
		boolean contentChanged = !mOriginalContent.equals(mText.getText()
				.toString());

		boolean isNoteUnencrypted = isNoteUnencrypted();

		// Show comands on the URI only if the note is not encrypted
		menu.setGroupVisible(Menu.CATEGORY_ALTERNATIVE, isNoteUnencrypted);

		if (mState == STATE_EDIT_NOTE_FROM_SDCARD) {
			// Menus for editing from SD card
			menu.setGroupVisible(0, false);
			menu.setGroupVisible(1, false);
			menu.setGroupVisible(2, true);
			menu.findItem(MENU_SAVE).setEnabled(contentChanged);
		} else if (mState == STATE_EDIT_EXTERNAL_NOTE) {
			// Menus for external notes, e.g. from OI Shopping List.
			// In this case, don't show encryption/decryption.
			menu.setGroupVisible(0, contentChanged || mUndoRevert != null);
			menu.setGroupVisible(1, true);
			menu.setGroupVisible(2, false);

			menu.findItem(MENU_ENCRYPT).setVisible(false);
			menu.findItem(MENU_UNENCRYPT).setVisible(false);
		} else {
			// Menus for internal notes
			menu.setGroupVisible(0, contentChanged || mUndoRevert != null);
			menu.setGroupVisible(1, true);
			menu.setGroupVisible(2, false);

			menu.findItem(MENU_ENCRYPT).setVisible(isNoteUnencrypted);
			menu.findItem(MENU_UNENCRYPT).setVisible(!isNoteUnencrypted);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	private boolean isNoteUnencrypted() {
		long encrypted = 0;
		if (mCursor != null && mCursor.moveToFirst()) {
			// Check if the column Notes.ENCRYPTED exists
			if (hasEncryptionColumn) {
				encrypted = mCursor.getInt(mCursor
						.getColumnIndex(Notes.ENCRYPTED));
			} else {
				encrypted = 0;
			}
		}
		boolean isNoteUnencrypted = (encrypted == 0);
		return isNoteUnencrypted;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, NotesList.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			break;
		case MENU_DELETE:
			deleteNoteWithConfirm();
			break;
		case MENU_DISCARD:
			revertNote();
			break;
		case MENU_REVERT:
			revertNote();
			break;
		case MENU_ENCRYPT:
			encryptNote(true);
			break;
		case MENU_UNENCRYPT:
			unencryptNote();
			break;
		case MENU_IMPORT:
			importNote();
			break;
		case MENU_SAVE:
			saveNote();
			break;
		case MENU_SAVE_AS:
			saveAsNote();
			break;
		case MENU_THEME:
			setThemeSettings();
			return true;
		case MENU_SETTINGS:
			showNotesListSettings();
			return true;
		case MENU_SEND:
			shareNote();
			return true;
		case MENU_COLOR:
			setColor();
			return true;
		case MENU_WORD_COUNT:
			showWordCount();
			break;
		}
		if (item.getGroupId() == GROUP_ID_TEXT_SELECTION_ALTERNATIVE) {
			// Process manually:
			// We pass the current selection along with the intent
			startTextSelectionActivity(item.getIntent());

			// Consume event
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setColor() {
		LinearLayout color = (LinearLayout) findViewById(R.id.editor_color);
		ImageView color_yellow = (ImageView) findViewById(R.id.editor_color_yellow);
		ImageView color_pink = (ImageView) findViewById(R.id.editor_color_pink);
		ImageView color_blue = (ImageView) findViewById(R.id.editor_color_blue);
		ImageView color_green = (ImageView) findViewById(R.id.editor_color_green);
		ImageView color_gray = (ImageView) findViewById(R.id.editor_color_gray);

		color_yellow.setOnClickListener(colorListener);
		color_pink.setOnClickListener(colorListener);
		color_blue.setOnClickListener(colorListener);
		color_green.setOnClickListener(colorListener);
		color_gray.setOnClickListener(colorListener);

		color.setVisibility(View.VISIBLE);
	}

	OnClickListener colorListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.editor_color_yellow:
				setNoteColor(YELLOW);
				break;
			case R.id.editor_color_pink:
				setNoteColor(PINK);
				break;
			case R.id.editor_color_blue:
				setNoteColor(BLUE);
				break;
			case R.id.editor_color_green:
				setNoteColor(GREEN);
				break;
			case R.id.editor_color_gray:
				setNoteColor(GREY);
				break;
			default:
				break;
			}
		}
	};

	private void shareNote() {
		String content = mText.getText().toString();
		String title = ExtractTitle.extractTitle(content);
		SendNote.sendNote(this, title, content);
	}

	protected void setNoteColor(int color) {
		int id = (int)R.color.lightYellow;
		Resources res = getResources();
		LinearLayout c = (LinearLayout) findViewById(R.id.editor_color);
		c.setVisibility(View.GONE);

		switch (color) {
			case BLUE:
				id = (int)R.color.lightBabyBlue;
				break;
			case GREEN:
				id = (int)R.color.lightGreen;
				break;
			case PINK:
				id = (int)R.color.lightPink;
				break;
			case GREY:
				id = (int)R.color.lightGray;
				break;
			default:
				break;
		}

		mText.setBackgroundDrawable(res.getDrawable(id));

		if (color == mColor)
			return;
		mColor = color;
		ContentValues values = new ContentValues();
		values.put(Notes.COLOR, mColor);
		getContentResolver().update(mUri, values, null, null);
	}

	private void deleteNoteWithConfirm() {
		showDialog(DIALOG_DELETE);
	}

	/**
	 * Modifies an activity to pass along the currently selected text.
	 * 
	 * @param intent
	 */
	private void startTextSelectionActivity(Intent intent) {
		Intent newIntent = new Intent(intent);

		String text = mText.getText().toString();
		int start = mText.getSelectionStart();
		int end = mText.getSelectionEnd();

		// if (debug) Log.i(TAG, "len: " + text.length() + ", start: " + start +
		// ", end: " + end);
		if (end < start) {
			int swap = end;
			end = start;
			start = swap;
		}

		newIntent.putExtra(NotepadIntents.EXTRA_TEXT,
				text.substring(start, end));
		newIntent.putExtra(NotepadIntents.EXTRA_TEXT_BEFORE_SELECTION,
				text.substring(0, start));
		newIntent.putExtra(NotepadIntents.EXTRA_TEXT_AFTER_SELECTION,
				text.substring(end));

		startActivityForResult(newIntent,
				REQUEST_CODE_TEXT_SELECTION_ALTERNATIVE);
	}

	/**
	 * Reverts to the original text, or undoes revert.
	 */
	private final void revertNote() {
		if (mCursor != null) {
			String tmp = mText.getText().toString();
			mText.setAutoLinkMask(0);
			if (!tmp.equals(mOriginalContent)) {
				// revert to original content
				mText.setTextKeepState(mOriginalContent);
				mUndoRevert = tmp;
			} else if (mUndoRevert != null) {
				// revert to original content
				mText.setTextKeepState(mUndoRevert);
				mUndoRevert = null;
			}
			int autolink = PreferenceActivity.getAutoLinkFromPreference(this);
			mText.setAutoLinkMask(autolink);
		}
		// mCursor.requery();
		// setResult(RESULT_CANCELED);
		// finish();
	}

	/**
	 * Take care of deleting a note. Simply deletes the entry.
	 */
	private final void deleteNote() {
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
			getContentResolver().delete(mUri, null, null);
			mText.setText("");
		}
	}

	/*
	 * private final void discardNote() { //if (mCursor != null) { //
	 * mCursor.close(); // mCursor = null; // getContentResolver().delete(mUri,
	 * null, null); // mText.setText(""); //} mOriginalContent =
	 * mText.getText().toString(); mText.setText(""); }
	 */

	private void applyInsertText() {
		if (mApplyTextBefore != null || mApplyText != null
				|| mApplyTextAfter != null) {
			// Need to apply insert text from previous
			// TEXT_SELECTION_ALTERNATIVE

			insertAtPoint(mApplyTextBefore, mApplyText, mApplyTextAfter);

			// Only apply once:
			mApplyTextBefore = null;
			mApplyText = null;
			mApplyTextAfter = null;
		}
	}

	/**
	 * Insert textToInsert at current position. Optionally, if textBefore or
	 * textAfter are non-null, replace the text before or after the current
	 * selection.
	 * 
	 * @author isaac
	 * @author Peli
	 */
	private void insertAtPoint(String textBefore, String textToInsert,
			String textAfter) {
		String originalText = mText.getText().toString();
		int startPos = mText.getSelectionStart();
		int endPos = mText.getSelectionEnd();
		if (mDecryptedText != null) {
			// Treat encrypted text:
			originalText = mDecryptedText;
			startPos = mSelectionStart;
			endPos = mSelectionStop;
		}
		int newStartPos = startPos;
		int newEndPos = endPos;
		ContentValues values = new ContentValues();
		String newNote = "";
		StringBuffer sb = new StringBuffer();
		if (textBefore != null) {
			sb.append(textBefore);
			newStartPos = textBefore.length();
		} else {
			sb.append(originalText.substring(0, startPos));
		}
		if (textToInsert != null) {
			sb.append(textToInsert);
			newEndPos = newStartPos + textToInsert.length();
		} else {
			String text = originalText.substring(startPos, endPos);
			sb.append(text);
			newEndPos = newStartPos + text.length();
		}
		if (textAfter != null) {
			sb.append(textAfter);
		} else {
			sb.append(originalText.substring(endPos));
		}
		newNote = sb.toString();

		if (mState == STATE_EDIT_NOTE_FROM_SDCARD) {
			mFileContent = newNote;
			mSelectionStart = newStartPos;
			mSelectionStop = newEndPos;
		} else if (mDecryptedText != null) {
			mDecryptedText = newNote;
		} else {
			// This stuff is only done when working with a full-fledged note.
			if (!mNoteOnly) {
				// Bump the modification time to now.
				values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
				String title;
				if (PreferenceActivity.getMarqueeFromPrefs(this) == false) {
					title = ExtractTitle.extractTitle(newNote);
				} else {
					title = newNote;
				}
				values.put(Notes.TITLE, title);
			}
			// Write our text back into the provider.
			values.put(Notes.NOTE, newNote);
			// Commit all of our changes to persistent storage. When the update
			// completes
			// the content provider will notify the cursor of the change, which
			// will
			// cause the UI to be updated.
			getContentResolver().update(mUri, values, null, null);
		}

		// ijones: notification doesn't seem to trigger for some reason :(
		mText.setTextKeepState(newNote);
		// Adjust cursor position according to new length:
		mText.setSelection(newStartPos, newEndPos);
	}

	private void importNote() {
		// Load the file into a new note.

		mFileContent = mText.getText().toString();

		Uri newUri = null;

		// Let's check whether the exactly same note already exists or not:
		Cursor c = getContentResolver().query(Notes.CONTENT_URI,
				new String[] { Notes._ID }, Notes.NOTE + " = ?",
				new String[] { mFileContent }, null);
		if (c != null && c.moveToFirst()) {
			// Same note exists already:
			long id = c.getLong(0);
			newUri = ContentUris.withAppendedId(Notes.CONTENT_URI, id);
		} else {

			// Add new note
			// Requested to insert: set that state, and create a new entry
			// in the container.
			// mState = STATE_INSERT;
			ContentValues values = new ContentValues();
			values.put(Notes.NOTE, mFileContent);
			values.put(Notes.THEME, mTheme);
			newUri = getContentResolver().insert(Notes.CONTENT_URI, values);

			// If we were unable to create a new note, then just finish
			// this activity. A RESULT_CANCELED will be sent back to the
			// original activity if they requested a result.
			if (newUri == null) {
				Log.e(TAG, "Failed to insert new note.");
				finish();
				return;
			}

			// The new entry was created, so assume all will end well and
			// set the result to be returned.
			// setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
			// setResult(RESULT_OK, intent);
		}

		// Start a new editor:
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_EDIT);
		intent.setData(newUri);
		intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		setIntent(intent);
		startActivity(intent);

		// and finish this editor
		finish();

	}

	private void saveNote() {
		mFileContent = mText.getText().toString();

		File file = FileUriUtils.getFile(mUri);
		SaveFileActivity.writeToFile(this, file, mFileContent);

		mOriginalContent = mFileContent;
	}

	/**
	 * Show the "Save as" dialog.
	 */
	private void saveAsNote() {
		mFileContent = mText.getText().toString();

		Intent intent = new Intent();
		intent.setAction(NotepadInternalIntents.ACTION_SAVE_TO_SD_CARD);
		intent.setData(mUri);
		intent.putExtra(NotepadInternalIntents.EXTRA_TEXT, mFileContent);

		startActivityForResult(intent, REQUEST_CODE_SAVE_AS);
	}

	void setThemeSettings() {
		showDialog(DIALOG_THEME);
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_UNSAVED_CHANGES:
			return getUnsavedChangesWarningDialog();

		case DIALOG_THEME:
			return new ThemeDialog(this, this);

		case DIALOG_DELETE:
			return new DeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface arg0, int arg1) {
							deleteNote();
							finish();
						}
					}).create();
		}
		return null;
	}

	public String onLoadTheme() {
		return loadTheme();
	}

	public void onSaveTheme(String theme) {
		saveTheme(theme);
	}

	public void onSetTheme(String theme) {
		setTheme(theme);
	}

	public void onSetThemeForAll(String theme) {
		setThemeForAll(this, theme);
	}

	/**
	 * Set theme for all lists.
	 * 
	 * @param context
	 * @param theme
	 */
	public static void setThemeForAll(Context context, String theme) {
		ContentValues values = new ContentValues();
		values.put(Notes.THEME, theme);
		context.getContentResolver().update(Notes.CONTENT_URI, values, null,
				null);
	}

	/**
	 * Loads the theme settings for the currently selected theme.
	 * 
	 * Up to version 1.2.1, only one of 3 hardcoded themes are available. These
	 * are stored in 'skin_background' as '1', '2', or '3'.
	 * 
	 * Starting in 1.2.2, also themes of other packages are allowed.
	 * 
	 * @return
	 */
	public String loadTheme() {
		return mTheme;
		/*
		 * if (mCursor != null && mCursor.moveToFirst()) { // mCursorListFilter
		 * has been set to correct position // by calling getSelectedListId(),
		 * // so we can read out further elements: String skinBackground =
		 * mCursor .getString(COLUMN_INDEX_THEME);
		 * 
		 * return skinBackground; } else { return null; }
		 */
	}

	public void saveTheme(String theme) {
		mTheme = theme;
		/*
		 * // Save theme only for content Uris with NotePad authority. // Don't
		 * save anything for file:// uri. if (mUri != null &&
		 * mUri.getAuthority().equals(NotePad.AUTHORITY)) { ContentValues values
		 * = new ContentValues(); values.put(Notes.THEME, theme);
		 * getContentResolver().update(mUri, values, null, null); }
		 */
	}

	/**
	 * Set theme according to Id.
	 * 
	 * @param themeId
	 */
	void setTheme(String themeName) {
		int size = PreferenceActivity.getFontSizeFromPrefs(this);

		// New styles:
		boolean themeFound = setRemoteStyle(themeName, size);

		if (!themeFound) {
			// Some error occured, let's use default style:
			setLocalStyle(R.style.Theme_Notepad, size);
		}

		applyTheme();
	}

	private void setLocalStyle(int styleResId, int size) {
		String styleName = getResources().getResourceName(styleResId);

		boolean themefound = setRemoteStyle(styleName, size);

		if (!themefound) {
			// Actually this should never happen.
			Log.e(TAG, "Local theme not found: " + styleName);
		}
	}

	private boolean setRemoteStyle(String styleName, int size) {
		if (TextUtils.isEmpty(styleName)) {
			if (debug)
				Log.e(TAG, "Empty style name: " + styleName);
			return false;
		}

		PackageManager pm = getPackageManager();

		String packageName = ThemeUtils.getPackageNameFromStyle(styleName);

		if (packageName == null) {
			Log.e(TAG, "Invalid style name: " + styleName);
			return false;
		}

		Context c = null;
		try {
			c = createPackageContext(packageName, 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Package for style not found: " + packageName + ", "
					+ styleName);
			return false;
		}

		Resources res = c.getResources();

		int themeid = res.getIdentifier(styleName, null, null);
		if (debug)
			Log.d(TAG, "Retrieving theme: " + styleName + ", " + themeid);

		if (themeid == 0) {
			Log.e(TAG, "Theme name not found: " + styleName);
			return false;
		}

		try {
			ThemeAttributes ta = new ThemeAttributes(c, packageName, themeid);

			mTextTypeface = ta.getString(ThemeNotepad.textTypeface);
			if (debug)
				Log.d(TAG, "textTypeface: " + mTextTypeface);

			mCurrentTypeface = null;

			// Look for special cases:
			if ("monospace".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.MONOSPACE,
						Typeface.NORMAL);
			} else if ("sans".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.SANS_SERIF,
						Typeface.NORMAL);
			} else if ("serif".equals(mTextTypeface)) {
				mCurrentTypeface = Typeface.create(Typeface.SERIF,
						Typeface.NORMAL);
			} else if (!TextUtils.isEmpty(mTextTypeface)) {

				try {
					if (debug)
						Log.d(TAG, "Reading typeface: package: " + packageName
								+ ", typeface: " + mTextTypeface);
					Resources remoteRes = pm
							.getResourcesForApplication(packageName);
					mCurrentTypeface = Typeface.createFromAsset(
							remoteRes.getAssets(), mTextTypeface);
					if (debug)
						Log.d(TAG, "Result: " + mCurrentTypeface);
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Package not found for Typeface", e);
				}
			}

			mTextUpperCaseFont = ta.getBoolean(ThemeNotepad.textUpperCaseFont,
					false);

			mTextColor = ta.getColor(ThemeNotepad.textColor,
					android.R.color.white);

			if (debug) {
				Log.d(TAG, "textColor: " + mTextColor);
			}

			if (size == 0) {
				mTextSize = getTextSizeTiny(ta);
			} else if (size == 1) {
				mTextSize = getTextSizeSmall(ta);
			} else if (size == 2) {
				mTextSize = getTextSizeMedium(ta);
			} else {
				mTextSize = getTextSizeLarge(ta);
			}
			if (debug)
				Log.d(TAG, "textSize: " + mTextSize);

			if (mText != null) {
				mBackgroundPadding = ta.getDimensionPixelOffset(
						ThemeNotepad.backgroundPadding, -1);
				int backgroundPaddingLeft = ta.getDimensionPixelOffset(
						ThemeNotepad.backgroundPaddingLeft, mBackgroundPadding);
				int backgroundPaddingTop = ta.getDimensionPixelOffset(
						ThemeNotepad.backgroundPaddingTop, mBackgroundPadding);
				int backgroundPaddingRight = ta
						.getDimensionPixelOffset(
								ThemeNotepad.backgroundPaddingRight,
								mBackgroundPadding);
				int backgroundPaddingBottom = ta.getDimensionPixelOffset(
						ThemeNotepad.backgroundPaddingBottom,
						mBackgroundPadding);

				if (debug) {
					Log.d(TAG, "Padding: " + mBackgroundPadding + "; "
							+ backgroundPaddingLeft + "; "
							+ backgroundPaddingTop + "; "
							+ backgroundPaddingRight + "; "
							+ backgroundPaddingBottom + "; ");
				}

				try {
					Resources remoteRes = pm
							.getResourcesForApplication(packageName);
					int resid = ta.getResourceId(ThemeNotepad.background, 0);
					if (resid != 0) {
						Drawable d = remoteRes.getDrawable(resid);
						mText.setBackgroundDrawable(d);
					} else {
						// remove background
						mText.setBackgroundResource(0);
						setNoteColor(mColor);
					}
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Package not found for Theme background.", e);
				} catch (Resources.NotFoundException e) {
					Log.e(TAG, "Resource not found for Theme background.", e);
				}

				// Apply padding
				if (mBackgroundPadding >= 0 || backgroundPaddingLeft >= 0
						|| backgroundPaddingTop >= 0
						|| backgroundPaddingRight >= 0
						|| backgroundPaddingBottom >= 0) {
					mText.setPadding(backgroundPaddingLeft,
							backgroundPaddingTop, backgroundPaddingRight,
							backgroundPaddingBottom);
				} else {
					// 9-patches do the padding automatically
					// todo clear padding
				}
			}

			mLinesMode = ta.getInteger(ThemeNotepad.lineMode, 2);
			mLinesColor = ta.getColor(ThemeNotepad.lineColor, 0xFF000080);

			if (debug)
				Log.d(TAG, "line color: " + mLinesColor);

			return true;

		} catch (UnsupportedOperationException e) {
			// This exception is thrown e.g. if one attempts
			// to read an integer attribute as dimension.
			Log.e(TAG, "UnsupportedOperationException", e);
			return false;
		} catch (NumberFormatException e) {
			// This exception is thrown e.g. if one attempts
			// to read a string as integer.
			Log.e(TAG, "NumberFormatException", e);
			return false;
		}
	}

	private float getTextSizeTiny(ThemeAttributes ta) {
		float size = ta.getDimensionPixelOffset(ThemeNotepad.textSizeTiny, -1);
		if (size == -1) {
			// Try to obtain from small:
			size = (12f / 18f) * getTextSizeSmall(ta);
		}
		return size;
	}

	private float getTextSizeSmall(ThemeAttributes ta) {
		float size = ta.getDimensionPixelOffset(ThemeNotepad.textSizeSmall, -1);
		if (size == -1) {
			// Try to obtain from small:
			size = (18f / 23f) * getTextSizeMedium(ta);
		}
		return size;
	}

	private float getTextSizeMedium(ThemeAttributes ta) {
		final float scale = getResources().getDisplayMetrics().scaledDensity;
		float size = ta.getDimensionPixelOffset(ThemeNotepad.textSizeMedium,
				(int) (23 * scale + 0.5f));
		return size;
	}

	private float getTextSizeLarge(ThemeAttributes ta) {
		float size = ta.getDimensionPixelOffset(ThemeNotepad.textSizeLarge, -1);
		if (size == -1) {
			// Try to obtain from small:
			size = (28f / 23f) * getTextSizeMedium(ta);
		}
		return size;
	}

	private void applyTheme() {
		mText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
		mText.setTypeface(mCurrentTypeface);
		mText.setTextColor(mTextColor);

		if (mTextUpperCaseFont) {
			// Turn off autolinkmask, because it is not compatible with
			// transformationmethod.
			mText.setAutoLinkMask(0);

			mText.setTransformationMethod(UpperCaseTransformationMethod
					.getInstance());
		} else {
			mText.setTransformationMethod(null);

			// Set auto-link on or off, based on the current setting.
			int autoLink = PreferenceActivity.getAutoLinkFromPreference(this);

			mText.setAutoLinkMask(autoLink);
		}

		mText.invalidate();
	}

	private void showNotesListSettings() {
		startActivity(new Intent(this, PreferenceActivity.class));
	}

	private void showWordCount() {
		String text = mText.getText().toString();
		int number_of_words = text.split("\\s+").length;
		if (TextUtils.isEmpty(text)) {
			// if text is empty, number_of_words is set to 1,
			// so in this case we set it manually
			number_of_words = 0;
		}
		AlertDialog.Builder wordCountAlert = new AlertDialog.Builder(this);
		wordCountAlert.setMessage(getResources().getQuantityString(
				R.plurals.word_count, number_of_words, number_of_words));
		wordCountAlert.setTitle(R.string.menu_word_count);
		wordCountAlert.setPositiveButton(R.string.ok, null);
		wordCountAlert.setCancelable(false);
		wordCountAlert.create().show();
	}

	Dialog getUnsavedChangesWarningDialog() {
		return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.warning_unsaved_changes_title)
				.setMessage(R.string.warning_unsaved_changes_message)
				.setPositiveButton(R.string.button_save,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Save
								saveNote();
								finish();
							}
						})
				.setNeutralButton(R.string.button_dont_save,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Don't save
								finish();
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Cancel
							}
						}).create();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mState == STATE_EDIT_NOTE_FROM_SDCARD) {
				mFileContent = mText.getText().toString();
				if (!mFileContent.equals(mOriginalContent)) {
					// Show a dialog
					showDialog(DIALOG_UNSAVED_CHANGES);
					return true;
				}
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (debug)
			Log.d(TAG, "onActivityResult: Received requestCode " + requestCode
					+ ", resultCode " + resultCode);
		switch (requestCode) {
		case REQUEST_CODE_DECRYPT:
			if (resultCode == RESULT_OK && data != null) {
				String decryptedText = data
						.getStringExtra(CryptoIntents.EXTRA_TEXT);
				long id = data.getLongExtra(PrivateNotePadIntents.EXTRA_ID, -1);

				// TODO: Check that id corresponds to current intent.

				if (id == -1) {
					Log.e(TAG, "Wrong extra id");
					Toast.makeText(this, "Decrypted information incomplete",
							Toast.LENGTH_SHORT).show();

					finish();
					return;
				}

				if (debug)
					Log.d(TAG, "decrypted text received: " + decryptedText);
				mDecryptedText = decryptedText;
				mOriginalContent = decryptedText;

			} else {
				Toast.makeText(this, R.string.decryption_failed,
						Toast.LENGTH_SHORT).show();
				Log.e(TAG, "decryption failed");

				finish();
			}
			break;
		case REQUEST_CODE_TEXT_SELECTION_ALTERNATIVE:
			if (resultCode == RESULT_OK && data != null) {
				// Insert result at current cursor position:
				mApplyText = data.getStringExtra(NotepadIntents.EXTRA_TEXT);
				mApplyTextBefore = data
						.getStringExtra(NotepadIntents.EXTRA_TEXT_BEFORE_SELECTION);
				mApplyTextAfter = data
						.getStringExtra(NotepadIntents.EXTRA_TEXT_AFTER_SELECTION);

				// Text is actually inserted in onResume() - see
				// applyInsertText()
			}
			break;
		case REQUEST_CODE_SAVE_AS:
			if (resultCode == RESULT_OK && data != null) {
				// Set the new file name
				mUri = data.getData();
				if (debug)
					Log.d(TAG, "original: " + mOriginalContent + ", file: "
							+ mFileContent);
				mOriginalContent = mFileContent;

				updateTitleSdCard();
			}
		}
	}
}
