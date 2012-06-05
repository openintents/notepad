package org.openintents.notepad.filename;

import org.openintents.notepad.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

public class FilenameDialog extends AlertDialog implements OnClickListener {
	private static final String TAG = "FilenameDialog";

	private static final String BUNDLE_TAGS = "tags";

	protected static final int DIALOG_ID_NO_FILE_MANAGER_AVAILABLE = 2;

	Context mContext;

	EditText mEditText;

	public interface OnFilenamePickedListener {
		void onFilenamePicked(String filename);
	}

	OnFilenamePickedListener mListener;

	public FilenameDialog(Context context) {
		super(context);
		mContext = context;

		setTitle(context.getText(R.string.menu_edit_tags));
		setButton(context.getText(android.R.string.ok), this);
		setButton2(context.getText(android.R.string.cancel),
				(OnClickListener) null);
		setIcon(R.drawable.ic_launcher_folder_small);

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_filename, null);
		setView(view);

		mEditText = (EditText) view.findViewById(R.id.file_path);

		// SharedPreferences pm =
		// PreferenceManager.getDefaultSharedPreferences(context);
		// mEditText.setText(pm.getString(PREFERENCE_FILENAME,
		// DEFAULT_FILENAME));

		ImageButton buttonFileManager = (ImageButton) view
				.findViewById(R.id.file_manager);

		buttonFileManager.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				openFileManager();
			}
		});

	}

	public void setFilename(String filename) {
		mEditText.setText(filename);
	}

	public void setOnFilenamePickedListener(OnFilenamePickedListener listener) {
		mListener = listener;
	}

	public void onClick(DialogInterface dialog, int which) {
		if (which == BUTTON1) {
			openOrSave();
		}

	}

	void openOrSave() {
		if (mListener != null) {
			String filename = mEditText.getText().toString();
			mListener.onFilenamePicked(filename);
		}
	}

	private void openFileManager() {
		showNoFileManagerAvailableDialog();

		/*
		 * String fileName = mEditText.getText().toString();
		 * 
		 * Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
		 * intent.setData(Uri.parse("file://" + fileName));
		 */

		// intent.putExtra(FileManagerIntents.EXTRA_TITLE,
		// getString(RES_STRING_FILEMANAGER_TITLE));
		// intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
		// getString(RES_STRING_FILEMANAGER_BUTTON_TEXT));

		/*
		 * try { startActivityForResult(intent, REQUEST_CODE_PICK_FILE); } catch
		 * (ActivityNotFoundException e) {
		 * mContext.showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE); }
		 */
	}

	void showNoFileManagerAvailableDialog() {

		Intent i = new Intent(mContext, DialogHostingActivity.class);
		i.putExtra(DialogHostingActivity.EXTRA_DIALOG_ID,
				DialogHostingActivity.DIALOG_ID_NO_FILE_MANAGER_AVAILABLE);
		mContext.startActivity(i);
	}

	@Override
	public Bundle onSaveInstanceState() {
		Bundle state = super.onSaveInstanceState();
		state.putString(BUNDLE_TAGS, "");
		return state;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String tags = savedInstanceState.getString(BUNDLE_TAGS);
	}

}
