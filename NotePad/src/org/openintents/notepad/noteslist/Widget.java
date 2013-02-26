package org.openintents.notepad.noteslist;

import org.openintents.notepad.R;
import org.openintents.notepad.intents.NotepadInternalIntents;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		// Everything is updated in note_edit
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if (intent.getAction().equals(NotepadInternalIntents.NOTE_EDITED)
				|| intent.getAction().equals(
						AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				String uriId = extras.getString("ID");
				String note = extras.getString("Note");
				AppWidgetManager appWidgetManager = AppWidgetManager
						.getInstance(context);
				RemoteViews views = new RemoteViews(context.getPackageName(),
						R.layout.widget_layout);
				// Ids and the text are stored in shared preferences
				SharedPreferences sp = context.getSharedPreferences(
						"Widget_Prefs", 0);
				int widgetId = sp.getInt("WidgetId" + uriId, 0);
				views.setTextViewText(R.id.textViewWidget, "STICKY NOTE:\n"
						+ note);
				appWidgetManager.updateAppWidget(widgetId, views);
				SharedPreferences.Editor editor = sp.edit();
				editor.putString("RemoteText" + uriId, note);
				editor.commit();
			}
		}
	}

}
