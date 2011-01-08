package org.openintents.notepad.noteslist;

import org.openintents.notepad.R;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NotesListItemView extends LinearLayout {
	
	private static final String TAG = "NotesListItemView";

	Context mContext;
	
	private TextView mTitle;
	private TextView mTags;
	private ImageView mStatus;
	
	protected String mTitleEncrypted;
	protected String mTagsEncrypted;
	
	
	public NotesListItemView(Context context) {
		super(context);
		mContext = context;

		// inflate rating
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		inflater.inflate(
				R.layout.noteslist_item, this, true);
		
		mTitle = (TextView) findViewById(R.id.title);
		mTags = (TextView) findViewById(R.id.info);
		mStatus = (ImageView) findViewById(R.id.status);
	}

	/**
	 * Convenience method to set the title of a NewsView
	 */
	public void setTitle(String title) {
		mTitle.setText(title);
	}
	
	public void setTags(String tags) {
		if (!TextUtils.isEmpty(tags)) {
			mTags.setVisibility(View.VISIBLE);
			mTags.setText(tags);
		} else {
			mTags.setVisibility(View.GONE);
		}
	}
	
	public void setEncrypted(long encrypted) {
		if (encrypted > 0) {
			mStatus.setImageResource(android.R.drawable.ic_lock_lock);
		} else {
			mStatus.setImageBitmap(null);
		}
	}
}
