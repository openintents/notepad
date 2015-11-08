package org.openintents.notepad.noteslist;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openintents.notepad.PreferenceActivity;
import org.openintents.notepad.R;

public class NotesListItemView extends LinearLayout {

    private static final String TAG = "NotesListItemView";
    protected String mTitleEncrypted;
    protected String mTagsEncrypted;
    Context mContext;
    private MarqueeTextView mTitle;
    private TextView mTags;
    private ImageView mStatus;

    public NotesListItemView(Context context) {
        super(context);
        mContext = context;

        // inflate rating
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.noteslist_item, this, true);

        mTitle = (MarqueeTextView) findViewById(R.id.title);
        mTags = (TextView) findViewById(R.id.info);
        mStatus = (ImageView) findViewById(R.id.status);
    }

    @Override
    public boolean hasFocus() {
        // TODO Auto-generated method stub
        if (PreferenceActivity.getMarqueeFromPrefs(mContext) == true) {
            mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            mTitle.setMarquee(true);
        } else {
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
}
