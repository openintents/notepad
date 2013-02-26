package org.openintents.notepad.noteslist;

import org.openintents.notepad.R;
import org.openintents.notepad.NotePad.Notes;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class ConfigureWidget extends ListActivity{
	
	private int widgetId;
	NotesListCursor mCursorUtils;
	CursorAdapter adapter1;
	private String[] from;
	private int[] to;
	AppWidgetManager appWidgetManager;
	RemoteViews views;
	static String str;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure_widget);
		
		from = new String[]{ Notes._ID, Notes.TITLE };
		to = new int[]{R.id.id_row_notelist,R.id.title_row_notelist};
		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(Notes.CONTENT_URI);
		}
		Uri notesUri = getIntent().getData();
		Cursor managedCursor = getContentResolver().query(notesUri,
				from, null, null, null);
		startManagingCursor(managedCursor);
		adapter1 = new SimpleCursorAdapter(this,R.layout.config_widget_entry, managedCursor,from,to);
		setListAdapter(adapter1);
		
		setResult(RESULT_CANCELED);
		Bundle extras = getIntent().getExtras();
		
		if(extras != null){
			widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		
		appWidgetManager = AppWidgetManager.getInstance(this); 	
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Uri notesUri = getIntent().getData();
		Cursor managedCursor = getContentResolver().query(notesUri,
				null, null, null, null);
		
		managedCursor.moveToFirst();
		managedCursor.moveToPosition(position);
		String s = managedCursor.getString(9); 
		str = "STICKY NOTE:"+"\n"+s;
		views = new RemoteViews(this.getPackageName(),
				R.layout.widget_layout);
		views.setTextViewText(R.id.textViewWidget, str);
				
		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		Intent intent1 = new Intent(Intent.ACTION_EDIT, uri);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				intent1, 0);
		
	//	LayoutInflater inflator = getLayoutInflater();
	//	View ll = inflator.inflate(R.layout.widget_layout, null);
	//	TextView text = (TextView)ll.findViewById(R.id.textViewWidget);
	//	text.setMovementMethod(new ScrollingMovementMethod());
	//	Toast.makeText(this, text.getText(), Toast.LENGTH_SHORT).show();
			
		views.setOnClickPendingIntent(R.id.textViewWidget, pendingIntent);
		appWidgetManager.updateAppWidget(widgetId, views);
		
		//Store the value of the text in remote view and the id of menu item selected
		SharedPreferences sp = getSharedPreferences("Widget_Prefs",0);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("RemoteText"+id, str);
		editor.putInt("WidgetId"+id, widgetId);
		editor.commit();
	
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		setResult(RESULT_OK,resultValue);
		NotesList.hasWidget = true;
		finish();
	}

}
