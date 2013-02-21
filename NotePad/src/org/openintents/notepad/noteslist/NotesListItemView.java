package org.openintents.notepad.noteslist;

import org.openintents.notepad.PreferenceActivity;
import org.openintents.notepad.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NotesListItemView extends LinearLayout {

	private static final String TAG = "NotesListItemView";

	Context mContext;

	private MarqueeTextView mTitle;
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

		mTitle = (MarqueeTextView) findViewById(R.id.title);
		mTags = (TextView) findViewById(R.id.info);
		mStatus = (ImageView) findViewById(R.id.status);
	}
	


	@Override
	public boolean hasFocus() {
		// TODO Auto-generated method stub
		if(PreferenceActivity.getMarqueeFromPrefs(mContext)==true) {
			mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
			mTitle.setMarquee(true);
		}
		else
		{
			mTitle.setEllipsize(TextUtils.TruncateAt.END);
			mTitle.setMarquee(false);
		}
		return super.hasFocus();
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



	public void setColor(int color) {
		Resources res = this.getResources();
		Drawable d = res.getDrawable(R.drawable.note_item_bg_yellow);
		switch(color) {
			case R.color.lightBabyBlue:
				d = res.getDrawable(R.drawable.note_item_bg_blue);
				break;
			case R.color.lightGray:
				d = res.getDrawable(R.drawable.note_item_bg_grey);
				break;
			case R.color.lightGreen:
				d = res.getDrawable(R.drawable.note_item_bg_green);
				break;
			case R.color.lightPink:
				d = res.getDrawable(R.drawable.note_item_bg_pink);
				break;
			default:
				break;
		}
		
		mTitle.setBackgroundDrawable(d);		
	}
}
