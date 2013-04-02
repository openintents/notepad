package org.openintents.notepad.widget;

import java.util.ArrayList;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.R;
import org.openintents.notepad.util.WidgetList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

public class StackWidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}
}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private int mCount;
	private ArrayList<WidgetList> mWidgetItems = new ArrayList<WidgetList>();
	private Context mContext;
	private int mAppWidgetId;
	private Intent intent;
	private Cursor c;
	private Uri uri;

	public StackRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		this.intent = intent;
		Bundle b = this.intent.getExtras();
		uri = Uri.parse(b.getString("Uri"));
	}

	public void onCreate() {

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void onDestroy() {
		// In onDestroy() you should tear down anything that was setup for your
		// data source,
		// eg. cursors, connections, etc.
		mWidgetItems.clear();
	}

	public int getCount() {
		return mCount;
	}

	public RemoteViews getViewAt(int position) {
		// position will always range from 0 to getCount() - 1.

		// We construct a remote views item based on our widget item xml file,
		// and set the text based on the position.
		Log.d("Fuck atleast come here", "Ohh yeah");
		RemoteViews rv = new RemoteViews(mContext.getPackageName(),
				R.layout.stack_widgetitem);
		rv.setTextViewText(R.id.widget_item, mWidgetItems.get(position)
				.getNote());

		// Next, we set a pending-intent which will be used to open edit note.
		PendingIntent pi = PendingIntent.getActivity(
				mContext,
				0,
				new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(uri,
						mWidgetItems.get(position).getNoteId())), 0);
		rv.setOnClickPendingIntent(R.id.widget_item, pi);

		try {
			System.out.println("Loading view " + position);
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return rv;
	}

	public RemoteViews getLoadingView() {
		// You can create a custom loading view (for instance when getViewAt()
		// is slow.) If you
		// return null here, you will get the default loading view.
		return null;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public long getItemId(int position) {
		return position;
	}

	public boolean hasStableIds() {
		return true;
	}

	public void onDataSetChanged() {
	
		//This method is called after onCreate() method as well as when we call notifydatasetupdate().
		mWidgetItems.clear();
		c = mContext.getContentResolver().query(uri,
				new String[] { Notes._ID, Notes.NOTE }, null, null, null);
		mCount = c.getCount();
		c.moveToFirst();
		mCount = c.getCount();
		for (int i = 0; i < mCount; i++) {
			mWidgetItems.add(new WidgetList(c.getString(1), Integer.parseInt(c
					.getString(0))));
			c.moveToNext();
		}
		c.close();

	}
}