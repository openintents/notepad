/* 
 * Copyright (C) 2008 OpenIntents.org
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

package org.openintents.notepad;

import org.openintents.notepad.NotePad.Notes;
import org.openintents.notepad.noteslist.NotesList;
import org.openintents.notepad.wrappers.WrapActionBar;
import org.openintents.util.IntentUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.util.Linkify;
import android.view.MenuItem;

public class PreferenceActivity extends android.preference.PreferenceActivity {

	public static final String PREFS_MARQUEE = "marquee";
	public static final boolean PREFS_MARQUEE_DEFAULT = false;
	public static final String PREFS_SORTORDER = "sortorder";
	public static final String PREFS_SORTORDER_DEFAULT = "2";
	public static final String PREFS_FONTSIZE = "fontsize";
	public static final String PREFS_FONTSIZE_DEFAULT = "2";
	public static final String PREFS_MARKET_EXTENSIONS = "preference_market_extensions";
	public static final String PREFS_MARKET_THEMES = "preference_market_themes";
	public static final String PREFS_AUTOLINK = "autolink";
	public static final String PREFS_THEME_SET_FOR_ALL = "theme_set_for_all";
	public static final String PREFS_SCREEN_ADDONS = "preference_screen_addons";

	public static final String EXTRA_SHOW_GET_ADD_ONS = "show_get_add_ons";
	
	private static boolean mActionBarAvailable;
	
	static {
		try {
			WrapActionBar.checkAvailable();
			mActionBarAvailable = true;
		} catch(Throwable t){
			mActionBarAvailable = false;
		}
	}

	@Override
	protected void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		
		//enable the user to go back with the actionbar
		if(mActionBarAvailable){
			WrapActionBar bar = new WrapActionBar(this);
			bar.setDisplayHomeAsUpEnabled(true);
			
			//force to show the actionbar on version 14+
			if(Integer.valueOf(android.os.Build.VERSION.SDK) >= 14){
				bar.setHomeButtonEnabled(true);
			}
		}

		addPreferencesFromResource(R.xml.preferences);

		// Set enabled state of Market preference
		PreferenceScreen sp = (PreferenceScreen) findPreference(PREFS_MARKET_EXTENSIONS);
		sp.setEnabled(isMarketAvailable());
		sp = (PreferenceScreen) findPreference(PREFS_MARKET_THEMES);
		sp.setEnabled(isMarketAvailable());
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (getIntent() != null && getIntent().hasExtra(EXTRA_SHOW_GET_ADD_ONS)) {
			// Open License section directly:
			PreferenceScreen licensePrefScreen = (PreferenceScreen) getPreferenceScreen().findPreference(PREFS_SCREEN_ADDONS);
			setPreferenceScreen(licensePrefScreen);
		}
	}

	/**
	 * Returns the sort order for the notes list based on the user preferences.
	 * Performs error-checking.
	 * 
	 * @param context The context to grab the preferences from.
	 */
	static public String getSortOrderFromPrefs(Context context) {
		int sortOrder = 0;
		try {
			sortOrder = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context)
					.getString(PREFS_SORTORDER, PREFS_SORTORDER_DEFAULT));
		} catch (NumberFormatException e) {
			// Guess somebody messed with the preferences and put a string into this
			// field. We'll use the default value then.
		}

		if (sortOrder >= 0 && sortOrder < Notes.SORT_ORDERS.length)
		{
			return Notes.SORT_ORDERS[sortOrder];
		}

		// Value out of range - somebody messed with the preferences.
		return Notes.SORT_ORDERS[0];
	}

	public static int getFontSizeFromPrefs(Context context) {
		int size = Integer.parseInt(PreferenceManager
				.getDefaultSharedPreferences(context).getString(
						PREFS_FONTSIZE,
						PREFS_FONTSIZE_DEFAULT));
		return size;
	}

	public static boolean getMarqueeFromPrefs(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(PREFS_MARQUEE, PREFS_MARQUEE_DEFAULT);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle all of the possible menu actions.
		switch (item.getItemId()) {
			default:
				Intent intent = new Intent(this, NotesList.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Check whether Market is available.
	 * @return true if Market is available
	 */
	private boolean isMarketAvailable() {
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(getString(R.string.preference_market_extensions_link)));
		return IntentUtils.isIntentAvailable(this, i);
	}

	static int getAutoLinkFromPreference(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(PREFS_AUTOLINK, true) ? Linkify.ALL : 0;
	}

	public static boolean getThemeSetForAll(Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREFS_THEME_SET_FOR_ALL, false);
	}

	public static void setThemeSetForAll(Context context, boolean setForAll) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor ed = prefs.edit();
		ed.putBoolean(PREFS_THEME_SET_FOR_ALL, setForAll);
		ed.commit();
	}
}
