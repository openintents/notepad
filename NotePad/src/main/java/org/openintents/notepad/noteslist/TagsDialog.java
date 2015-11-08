package org.openintents.notepad.noteslist;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import org.openintents.intents.CryptoIntents;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.PrivateNotePadIntents;
import org.openintents.notepad.R;
import org.openintents.notepad.crypto.EncryptActivity;

public class TagsDialog extends AlertDialog implements OnClickListener {
    private static final String TAG = "TagsDialog";

    private static final String BUNDLE_URI = "uri";
    private static final String BUNDLE_ENCRYPTED = "encrypted";
    private static final String BUNDLE_TAGLIST = "taglist";

    Context mContext;
    Uri mUri;
    long mEncrypted;

    MultiAutoCompleteTextView mTextView;
    String[] mTagList;

    /**
     * @param context      Parent.
     * @param theme        the theme to apply to this dialog
     * @param callBack     How parent is notified.
     * @param hourOfDay    The initial hour.
     * @param minute       The initial minute.
     * @param is24HourView Whether this is a 24 hour view, or AM/PM.
     */
    public TagsDialog(Context context) {
        super(context);
        mContext = context;

        setTitle(context.getText(R.string.menu_edit_tags));
        setButton(context.getText(android.R.string.ok), this);
        setButton2(
                context.getText(android.R.string.cancel),
                (OnClickListener) null
        );
        // setIcon(R.drawable.ic_menu_edit);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_edit_tags, null);
        setView(view);

        mTextView = (MultiAutoCompleteTextView) view.findViewById(R.id.edit);
        mTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mTextView.setThreshold(0);
        mTextView.setOnClickListener(
                new View.OnClickListener() {

                    public void onClick(View v) {
                        toggleTaglistPopup();
                    }

                }
        );
        String[] mTagList = new String[0];
        if (mTagList.length < 1) {
            mTextView.setHint(R.string.tags_hint);
        }
        /*
		 * Button b = (Button) view.findViewById(R.id.button1);
		 * b.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { toggleTaglistPopup(); } });
		 */
    }

    private void toggleTaglistPopup() {
        if (mTextView.isPopupShowing()) {
            mTextView.dismissDropDown();
        } else {
            mTextView.showDropDown();
        }
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public void setTagList(String[] taglist) {
        mTagList = taglist;

        if (taglist != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    mContext,
                    android.R.layout.simple_dropdown_item_1line, mTagList
            );
            mTextView.setAdapter(adapter);
        }
    }

    public void setTags(String tags) {
        mTextView.setText(tags);
    }

    public void setEncrypted(long encrypted) {
        mEncrypted = encrypted;
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON1) {
            saveTags();
        }

    }

    void saveTags() {
        if (mTextView == null) {
            Log.e(TAG, "mTextView is null.");
            return;
        }

        String tags = mTextView.getText().toString();

        tags = tags.trim();
        // Remove trailing ","
        if (tags.endsWith(",")) {
            tags = tags.substring(0, tags.length() - 1);
        }
        tags = tags.trim();

        if (mEncrypted == 0) {
            // Simply store the value
            ContentValues values = new ContentValues(2);
            values.put(Notes.MODIFIED_DATE, System.currentTimeMillis());
            values.put(Notes.TAGS, tags);

            mContext.getContentResolver().update(mUri, values, null, null);
            mContext.getContentResolver().notifyChange(mUri, null);
        } else {
            // Encrypt the tag

            Intent i = new Intent(mContext, EncryptActivity.class);
            i.putExtra(
                    PrivateNotePadIntents.EXTRA_ACTION,
                    CryptoIntents.ACTION_ENCRYPT
            );
            i.putExtra(
                    CryptoIntents.EXTRA_TEXT_ARRAY,
                    EncryptActivity.getCryptoStringArray(null, null, tags)
            );
            i.putExtra(PrivateNotePadIntents.EXTRA_URI, mUri.toString());
            mContext.startActivity(i);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putString(BUNDLE_URI, mUri.toString());
        state.putLong(BUNDLE_ENCRYPTED, mEncrypted);
        state.putStringArray(BUNDLE_TAGLIST, mTagList);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUri = Uri.parse(savedInstanceState.getString(BUNDLE_URI));
        mEncrypted = savedInstanceState.getLong(BUNDLE_ENCRYPTED);
        mTagList = savedInstanceState.getStringArray(BUNDLE_TAGLIST);
        setTagList(mTagList);
    }
}
