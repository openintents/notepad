package org.openintents.notepad.noteslist;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.PreferenceActivity;
import org.openintents.notepad.R;
import org.openintents.notepad.util.OpenMatrixCursor;

public class NotesListCursor extends OpenMatrixCursor {

    private static final String TAG = "NotesListCursor";
    private static final boolean debug = false;

    static final String TITLE_DECRYPTED = "title_decrypted";
    static final String TAGS_DECRYPTED = "tags_decrypted";

    /**
     * The columns we are interested in from the database
     */
    protected static final String[] PROJECTION_DB = new String[]{Notes._ID, // 0
            Notes.TITLE, // 1
            Notes.TAGS, // 2
            Notes.ENCRYPTED // 3
    };

    /**
     * This cursors' columns
     */
    public static final String[] PROJECTION = new String[]{Notes._ID, // 0
            Notes.TITLE, // 1
            Notes.TAGS, // 2
            Notes.ENCRYPTED, // 3
            TITLE_DECRYPTED, // 4
            TAGS_DECRYPTED // 5
    };

    public static final int COLUMN_INDEX_ID = 0;
    /**
     * The index of the title column
     */
    public static final int COLUMN_INDEX_TITLE = 1;
    public static final int COLUMN_INDEX_TAGS = 2;
    public static final int COLUMN_INDEX_ENCRYPTED = 3;
    /**
     * Contains the encrypted title if it has not been decrypted yet
     */
    public static final int COLUMN_INDEX_TITLE_ENCRYPTED = 4;
    public static final int COLUMN_INDEX_TAGS_ENCRYPTED = 5;

    static boolean mLoggedIn = false;

    // If true, we will not requery if a change occurs.
    static boolean mSuspendQueries = false;

    Context mContext;
    Intent mIntent;
    // OpenMatrixCursor mCursor;

    /**
     * A database cursor that corresponds to the encrypted data of the current
     * cursor (that contains also decrypted information).
     */
    Cursor mDbCursor;

    public String mCurrentFilter;
    public String mSelectedTag;

    /**
     * Map encrypted titles to decrypted ones.
     */
    public static HashMap<String, String> mEncryptedStringHashMap = new HashMap<String, String>();

    /**
     * List containing all encrypted strings. These are decrypted one at a time
     * while idle. The list is synchronized because background threads may add
     * items to it.
     */
    public static List<String> mEncryptedStringList = Collections
            .synchronizedList(new LinkedList<String>());

    public boolean mContainsEncryptedStrings;

    public NotesListCursor(Context context, Intent intent) {
        super(PROJECTION);
        mContext = context;
        mIntent = intent;
        mCurrentFilter = null;
        mContainsEncryptedStrings = false;

    }

    // TODO: Replace new Handler() by mHandler from NotesList somehow.
    ContentObserver mContentObserver = new ContentObserver(new Handler()) {

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (debug) {
                Log.d(TAG, "NoteListCursor changed: " + selfChange);
            }

            if (!mSuspendQueries) {
                if (debug) {
                    Log.d(TAG, "NoteListCursor requery()");
                }
                requery();
            }
        }

    };

    @Override
    public boolean requery() {
        runQuery(mCurrentFilter, mSelectedTag);

        return super.requery();
    }

    /**
     * Return a new cursor with decrypted information.
     *
     * @param constraint
     */
    public Cursor query(CharSequence constraint, String tag) {
        NotesListCursor cursor = new NotesListCursor(mContext, mIntent);
        cursor.runQuery(constraint, tag);
        return cursor;
    }

    /**
     * Return a query with decrypted information on the current cursor.
     *
     * @param constraint
     */
    private void runQuery(CharSequence constraint, String tag) {

        // We have to query all items and return a new object, because notes may
        // be encrypted.

        if (constraint != null) {
            mCurrentFilter = constraint.toString();
        } else {
            mCurrentFilter = null;
        }
        if (tag != null) {
            mSelectedTag = tag;
        } else {
            mSelectedTag = null;
        }

        if (mDbCursor != null) {
            mDbCursor.unregisterContentObserver(mContentObserver);
            mDbCursor.close();
            mDbCursor = null;
        }
        mDbCursor = mContext.getContentResolver().query(
                mIntent.getData(),
                PROJECTION_DB, null, null,
                PreferenceActivity.getSortOrderFromPrefs(mContext)
        );

        // Register content observer
        mDbCursor.registerContentObserver(mContentObserver);

        if (debug) {
            Log.d(TAG, "Cursor count: " + mDbCursor.getCount());
        }

        // mCursor = new OpenMatrixCursor(PROJECTION, dbcursor.getCount());

        reset();
        mContainsEncryptedStrings = false;

        String encryptedlabel = mContext.getString(R.string.encrypted);

        mDbCursor.moveToPosition(-1);
        while (mDbCursor.moveToNext()) {
            long id = mDbCursor.getLong(COLUMN_INDEX_ID);
            String title = mDbCursor.getString(COLUMN_INDEX_TITLE);
            String tags = mDbCursor.getString(COLUMN_INDEX_TAGS);
            long encrypted = mDbCursor.getLong(COLUMN_INDEX_ENCRYPTED);
            String titleEncrypted = "";
            String tagsEncrypted = "";

            // Skip encrypted notes in filter.
            boolean skipEncrypted = false;

            if (encrypted > 0) {
                // get decrypted title:
                String titleDecrypted = mEncryptedStringHashMap.get(title);

                if (titleDecrypted != null) {
                    if (debug) {
                        Log.d(TAG, "got title: " + titleDecrypted);
                    }
                    title = titleDecrypted;
                } else {
                    if (debug) {
                        Log.d(TAG, "decrypt title later.");
                    }
                    // decrypt later
                    addForEncryption(title);

                    // Set encrypt title
                    titleEncrypted = title;
                    title = encryptedlabel;

                    skipEncrypted = true;
                }

                if (tags != null) {
                    String tagsDecrypted = mEncryptedStringHashMap.get(tags);
                    if (tagsDecrypted != null) {
                        if (debug) {
                            Log.d(TAG, "got tags: " + tagsDecrypted);
                        }
                        tags = tagsDecrypted;
                    } else {
                        if (debug) {
                            Log.d(TAG, "decrypt tags later.");
                        }
                        // decrypt later
                        addForEncryption(tags);

                        // Set encrypt title
                        tagsEncrypted = tags;
                        tags = "";

                        skipEncrypted = true;
                    }
                }

                if (!mLoggedIn) {
                    if (debug) {
                        Log.d(TAG, "not logged in.");
                    }
                    // suppress all decrypted output
                    title = encryptedlabel;
                    tags = "";
                }
            }

            boolean addrow = false;

            if (TextUtils.isEmpty(mCurrentFilter)
                    && TextUtils.isEmpty(mSelectedTag)) {
                // Add all rows if there is no filter.
                addrow = true;
            } else if (skipEncrypted) {
                if (debug) {
                    Log.d(TAG, "skipEncrypted)");
                }
                addrow = false;
            } else {
                // test the filter

                // Build search string from title and tags.
                StringBuilder sb = new StringBuilder();
                sb.append(" ");
                sb.append(title.toUpperCase());
                if (!TextUtils.isEmpty(tags)) {
                    sb.append(" ");
                    String spacetags = tags.replace(",", " ");
                    sb.append(spacetags.toUpperCase());
                }
                String searchstring = sb.toString();

                List<String> tagList = new ArrayList<String>();
                if (!TextUtils.isEmpty(tags)) {
                    for (String tagString : tags.split(",")) {
                        if (tagString.trim().length() != 0) {
                            tagList.add(tagString.trim());
                        }
                    }
                }

                if (TextUtils.isEmpty(mCurrentFilter)) {
                    addrow = tagList.contains(mSelectedTag.trim());
                } else if (TextUtils.isEmpty(mSelectedTag)) {
                    addrow = searchstring.contains(
                            " "
                                    + mCurrentFilter.toUpperCase()
                    );
                } else {
                    addrow = searchstring.contains(
                            " "
                                    + mCurrentFilter.toUpperCase()
                    )
                            && tagList.contains(mSelectedTag.trim());
                }

                if (!addrow && encrypted != 0) {
                    // Set the following so that list is updated even if
                    // encrypted notes are not shown yet.
                    mContainsEncryptedStrings = true;
                }
            }

            if (addrow) {
                if (tags == null) {
                    tags = "";
                }

                if (encrypted != 0) {
                    mContainsEncryptedStrings = true;
                }

                Object[] row = new Object[]{id, title, tags, encrypted,
                        titleEncrypted, tagsEncrypted};
                addRow(row);
            }
        }
    }

    public static void flushDecryptedStringHashMap() {
        mEncryptedStringHashMap = new HashMap<String, String>();
        mLoggedIn = false;
    }

    public static void addForEncryption(String encryptedString) {
        // Check whether it does not already exist:
        if (!mEncryptedStringList.contains(encryptedString)) {
            mEncryptedStringList.add(encryptedString);
        }
    }

    public static String getNextEncryptedString() {
        if (!NotesListCursor.mEncryptedStringList.isEmpty()) {
            String encryptedString = NotesListCursor.mEncryptedStringList
                    .remove(0);
            return encryptedString;
        } else {
            return null;
        }
    }

    @Override
    public void close() {
        if (debug) {
            Log.d(TAG, "Close NotesListCursor");
        }
        super.close();
    }

    @Override
    public void deactivate() {
        if (debug) {
            Log.d(TAG, "Deactivate NotesListCursor");
        }
        if (mDbCursor != null) {
            mDbCursor.deactivate();
        }
        super.deactivate();
    }

    @Override
    protected void finalize() {
        if (debug) {
            Log.d(TAG, "Finalize NotesListCursor");
        }

        if (mDbCursor != null) {
            mDbCursor.unregisterContentObserver(mContentObserver);
            // mDbCursor.close();
            mDbCursor.deactivate();
            mDbCursor.close();
            mDbCursor = null;
        }

        super.finalize();
    }

}
