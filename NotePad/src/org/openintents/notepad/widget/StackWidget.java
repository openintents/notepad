package org.openintents.notepad.widget;

import org.openintents.notepad.R;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.intents.NotepadInternalIntents;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.sax.StartElementListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RemoteViews;
import android.widget.Toast;

public class StackWidget extends AppWidgetProvider {

	public static final String EXTRA_ITEM = "org.openintents.noteslist.EXTRA_ITEM";

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);

		if (intent.getAction().equals(
				AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				int[] appWidgetIds = extras
						.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				if (intent.getData() == null) {
					intent.setData(Notes.CONTENT_URI);
				}
				Uri uri = intent.getData();
				for (int i = 0; i < appWidgetIds.length; i++) {
					int appWidgetId = appWidgetIds[i];

					Intent i1 = new Intent(context, StackWidgetService.class);
					i1.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
							appWidgetIds[i]);
					i1.putExtra("Uri", uri.toString());
					RemoteViews rv = new RemoteViews(context.getPackageName(),
							R.layout.stack_widget);
					rv.setRemoteAdapter(appWidgetIds[i], R.id.stackView1, i1);

					rv.setEmptyView(R.id.stackView1, R.id.textViewStack);
					appWidgetManager.notifyAppWidgetViewDataChanged(
							appWidgetIds[i], R.id.widget_item);
					appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
				}
			}
		} else if (intent.getAction().equals(
				NotepadInternalIntents.NOTE_ADD_DEL)) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				int[] appWidgetIds = extras
						.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
				for (int i = 0; i < appWidgetIds.length; i++) {
					appWidgetManager.notifyAppWidgetViewDataChanged(
							appWidgetIds[i], R.id.stackView1);
				}
			}
		}
	}
}