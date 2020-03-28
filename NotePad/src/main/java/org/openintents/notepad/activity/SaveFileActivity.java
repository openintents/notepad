package org.openintents.notepad.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.openintents.notepad.NotePad;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.R;
import org.openintents.notepad.filename.DialogHostingActivity;
import org.openintents.notepad.intents.NotepadInternalIntents;
import org.openintents.notepad.util.FileUriUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SaveFileActivity extends Activity {
    private static final String TAG = "SaveFileActivity";

    private static final int REQUEST_CODE_SAVE = 1;
    private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 2;

    private static final int DIALOG_OVERWRITE_WARNING = 1;

    private static final String BUNDLE_SAVE_FILENAME = "save_filename";
    private static final String BUNDLE_SAVE_CONTENT = "save_content";

    File mSaveFilename;
    String mSaveContent;
    private Uri fileUriForSaving;

    public static void writeToFile(Context context, File file, String text) {
        try {
            FileWriter fstream = new FileWriter(file);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(text);
            out.close();
            Toast.makeText(context, R.string.note_saved, Toast.LENGTH_SHORT)
                    .show();
        } catch (IOException e) {
            Toast.makeText(
                    context, R.string.error_writing_file,
                    Toast.LENGTH_SHORT
            ).show();
            Log.e(TAG, "Error writing file");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // retrieve data from saved instance
            if (savedInstanceState.containsKey(BUNDLE_SAVE_FILENAME)) {
                mSaveFilename = new File(
                        savedInstanceState.getString(BUNDLE_SAVE_FILENAME)
                );
            }
            if (savedInstanceState.containsKey(BUNDLE_SAVE_CONTENT)) {
                mSaveContent = savedInstanceState
                        .getString(BUNDLE_SAVE_CONTENT);
            }
        } else {
            // start new activity
            final Intent intent = getIntent();
            final Uri uri = intent.getData();
            Uri fileUri;
            if (uri != null) {
                if (uri.getScheme().equals("file")) {
                    // Save file provided in extras
                    fileUri = uri;
                    mSaveContent = intent
                            .getStringExtra(NotepadInternalIntents.EXTRA_TEXT);
                } else {
                    // Save a note specified by the note URI
                    fileUri = getFilenameFromNoteTitle(uri);
                    mSaveContent = getNote(uri);
                }
                if (mSaveContent != null && fileUri != null) {
                    askForFilenameSDCard(fileUri);
                } else {
                    // Nothing to save
                    finish();
                }
            } else {
                Log.w(TAG, "Invalid URI");
                finish();
            }
        }

        // Default answer:
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSaveFilename != null) {
            outState.putString(
                    BUNDLE_SAVE_FILENAME,
                    mSaveFilename.getAbsolutePath()
            );
        }
        if (mSaveContent != null) {
            outState.putString(BUNDLE_SAVE_CONTENT, mSaveContent);
        }
    }

    private String getNote(Uri uri) {
        String note = null;

        Cursor c = getContentResolver().query(
                uri,
                new String[]{Notes.ENCRYPTED, Notes.NOTE}, null, null, null
        );

        if (c != null && c.moveToFirst()) {
            long encrypted = c.getLong(0);
            if (encrypted == 0) {
                note = c.getString(1);
            } else {
                // TODO: decrypt first, then save to file
                Log.d(TAG, "Save encrypted file not possible.");
            }
        } else {
            Log.e(TAG, "Error saving file: Uri not valid: " + uri);
        }

        if (c != null) {
            c.close();
        }

        return note;
    }

    private Uri getFilenameFromNoteTitle(Uri noteUri) {

        File sdcard = getSdCardPath();

        // Construct file name:
        Cursor c = getContentResolver().query(
                noteUri,
                new String[]{NotePad.Notes._ID, NotePad.Notes.TITLE}, null,
                null, null
        );
        String filename;
        if (c != null & c.moveToFirst()) {
            filename = c.getString(1) + ".txt";
        } else {
            Log.w(TAG, "Unvalid note URI");
            finish();
            return null;
        }
        if (c != null) {
            c.close();
        }

        // Avoid dangerous characters:
        filename = filename.replace("/", "");
        filename = filename.replace("\\", "");
        filename = filename.replace(":", "");
        filename = filename.replace("?", "");
        filename = filename.replace("*", "");
        return FileUriUtils.getUri(
                FileUriUtils
                        .getFile(sdcard, filename)
        );


    }

    private void askForFilenameSDCard(Uri fileUri) {

        Intent i = new Intent(this, DialogHostingActivity.class);
        i.putExtra(
                DialogHostingActivity.EXTRA_DIALOG_ID,
                DialogHostingActivity.DIALOG_ID_SAVE
        );
        i.setData(fileUri);
        startActivityForResult(i, REQUEST_CODE_SAVE);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        Log.i(
                TAG, "Received requestCode " + requestCode + ", resultCode "
                        + resultCode
        );
        switch (requestCode) {
            case REQUEST_CODE_SAVE:
                if (resultCode == RESULT_OK && intent != null) {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        fileUriForSaving = intent.getData();
                        saveNote(fileUriForSaving);
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
                        }
                    }

                } else {
                    // nothing to do.
                    finish();
                }
                break;

            default:
                // We should never reach here...
                finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveNote(fileUriForSaving);
                }
                return;
            }
        }
    }

    private void saveNote(Uri uri) {
        // File name should be in Uri:
        mSaveFilename = FileUriUtils.getFile(uri);

        if (mSaveFilename.exists()) {
            showDialog(DIALOG_OVERWRITE_WARNING);
        } else {
            writeToFileAndFinish();
        }
    }

    private void writeToFileAndFinish() {
        // save file
        writeToFile(this, mSaveFilename, mSaveContent);

        // Return the new file name
        Intent i = new Intent();
        Uri uri = FileUriUtils.getUri(mSaveFilename);
        i.setData(uri);
        setResult(RESULT_OK, i);
        finish();
    }

    private File getSdCardPath() {
        return android.os.Environment.getExternalStorageDirectory();
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
            case DIALOG_OVERWRITE_WARNING:
                return getOverwriteWarningDialog();
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        }
    }

    Dialog getOverwriteWarningDialog() {
        return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.warning_file_exists_title)
                .setMessage(R.string.warning_file_exists_message)
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Ok
                                writeToFileAndFinish();
                            }
                        }
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Cancel
                                finish();
                            }
                        }
                ).create();
    }
}
