/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modified by OpenIntents.org

package org.openintents.notepad.search;

import org.openintents.notepad.R;
import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.noteslist.NotesListCursor;
import org.openintents.notepad.noteslist.NotesListCursorAdapter;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SearchQueryResultsActivity extends ListActivity {
	NotesListCursor mCursorUtils;
	NotesListCursorAdapter mAdapter;
	String queryString;
	
	/**
	 * Called with the activity is first created.
	 * 
	 * After the typical activity setup code, we check to see if we were
	 * launched with the ACTION_SEARCH intent, and if so, we handle it.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// get and process search query here
		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			doSearchQuery(queryIntent, "onCreate()");
		} else if (Intent.ACTION_VIEW.equals(queryAction)) {
			showItem(queryIntent);
		} else {
			finish();
		}
	}

	/**
	 * Called when new intent is delivered.
	 * 
	 * This is where we check the incoming intent for a query string.
	 * 
	 * @param newIntent
	 *            The intent used to restart this activity
	 */
	@Override
	public void onNewIntent(final Intent newIntent) {
		super.onNewIntent(newIntent);

		// get and process search query here
		final Intent queryIntent = getIntent();
		final String queryAction = queryIntent.getAction();
		if (Intent.ACTION_SEARCH.equals(queryAction)) {
			doSearchQuery(queryIntent, "onNewIntent()");
		} else if (Intent.ACTION_VIEW.equals(queryAction)) {
			showItem(queryIntent);
		} else {
			finish();
		}
	}

	/**
	 * Generic search handler.
	 * 
	 * In a "real" application, you would use the query string to select results
	 * from your data source, and present a list of those results to the user.
	 */
	private void doSearchQuery(final Intent queryIntent, final String entryPoint) {

		// The search query is provided as an "extra" string in the query intent
		queryString = queryIntent.getStringExtra(SearchManager.QUERY);

		Intent i = new Intent();
		i.setData(Notes.CONTENT_URI);

		mCursorUtils = new NotesListCursor(this, i);
		Cursor cursor = mCursorUtils.query(null, null);

		cursor = FullTextSearch.getCursor(this, queryString);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_2, cursor, new String[] {
						SearchManager.SUGGEST_COLUMN_TEXT_1,
						SearchManager.SUGGEST_COLUMN_TEXT_2 }, new int[] {
						android.R.id.text1, android.R.id.text2 });

		if (cursor.getCount() <= 0) {
			// Nothing found.
			TextView t = new TextView(this);
			t.setText(getString(R.string.search_found_no_results, queryString));
			t.setPadding(5, 5, 5, 5);
			t.setTextSize(20);
			getListView().addHeaderView(t);

			setListAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, new String[] {}));
		} else {

			// setListAdapter(mAdapter);
			setListAdapter(adapter);
		}

	}

	private void showItem(Intent queryIntent) {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(queryIntent.getData());

		startActivity(i);
		finish();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if (id >= 0) {
			Uri uri = ContentUris.withAppendedId(Notes.CONTENT_URI, id);

			// Launch activity to view/edit the currently selected item
			startActivity(new Intent(Intent.ACTION_EDIT, uri).putExtra("SEARCH_STRING", queryString));
		}
	}

}
