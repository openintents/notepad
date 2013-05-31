/* 
 * Copyright (C) 2007-2010 OpenIntents.org
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

package org.openintents.notepad.dialog;

import java.util.List;

import org.openintents.notepad.PreferenceActivity;
import org.openintents.notepad.R;
import org.openintents.notepad.theme.ThemeNotepad;
import org.openintents.notepad.theme.ThemeUtils;
import org.openintents.notepad.theme.ThemeUtils.ThemeInfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ThemeDialog extends AlertDialog implements OnClickListener,
		OnCancelListener, OnItemClickListener {
	private static final String TAG = "ThemeDialog";

	private static final String BUNDLE_THEME = "theme";

	Context mContext;
	ThemeDialogListener mListener;
	ListView mListView;
	CheckBox mCheckBox;
	List<ThemeInfo> mListInfo;

	public ThemeDialog(Context context) {
		super(context);
		mContext = context;
		init();
	}

	public ThemeDialog(Context context, ThemeDialogListener listener) {
		super(context);
		mContext = context;
		mListener = listener;
		init();
	}

	private void init() {
		setInverseBackgroundForced(true);

		LayoutInflater inflate = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		inflate = inflate.cloneInContext(new ContextThemeWrapper(mContext,
				android.R.style.Theme_Light));

		final View view = inflate.inflate(R.layout.dialog_theme_settings, null);

		setView(view);

		mListView = (ListView) view.findViewById(R.id.list1);
		mListView.setCacheColorHint(0);
		mListView.setItemsCanFocus(false);
		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		Button b = new Button(mContext);
		b.setText(R.string.get_more_themes);
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(mContext, PreferenceActivity.class);
				i.putExtra(PreferenceActivity.EXTRA_SHOW_GET_ADD_ONS, true);
				mContext.startActivity(i);

				pressCancel();
				dismiss();
			}
		});

		LinearLayout ll = new LinearLayout(mContext);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		ll.setPadding(20, 10, 20, 10);
		ll.addView(b, lp);
		ll.setGravity(Gravity.CENTER);
		mListView.addFooterView(ll);

		mCheckBox = (CheckBox) view.findViewById(R.id.check1);

		setTitle(R.string.theme_pick);

		setButton(Dialog.BUTTON_POSITIVE, mContext.getText(R.string.ok), this);
		setButton(Dialog.BUTTON_NEGATIVE, mContext.getText(R.string.cancel),
				this);
		setOnCancelListener(this);

		prepareDialog();
	}

	public void fillThemes() {
		mListInfo = ThemeUtils.getThemeInfos(mContext,
				ThemeNotepad.THEME_NOTEPAD);

		String[] s = new String[mListInfo.size()];
		int i = 0;
		for (ThemeInfo ti : mListInfo) {
			s[i] = ti.title;
			i++;
		}

		mListView.setAdapter(new ArrayAdapter<String>(new ContextThemeWrapper(
				mContext, android.R.style.Theme_Light),
				android.R.layout.simple_list_item_single_choice, s));

		mListView.setOnItemClickListener(this);
	}

	public void prepareDialog() {
		fillThemes();
		updateList();
		mCheckBox.setChecked(PreferenceActivity.getThemeSetForAll(mContext));
	}

	/**
	 * Set selection to currently used theme.
	 */
	private void updateList() {
		String theme = mListener.onLoadTheme();
		
		// Check special cases for backward compatibility:
		if ("1".equals(theme)) {
			theme = mContext.getResources().getResourceName(
					R.style.Theme_Notepad);
		} else if ("2".equals(theme)) {
			theme = mContext.getResources().getResourceName(
					R.style.Theme_Notepad_Monospaced);
		} else if ("3".equals(theme)) {
			theme = mContext.getResources().getResourceName(
					R.style.Theme_Notepad_Serif);
		}

		// Reset selection in case the current theme is not
		// in this list (for example got uninstalled).
		mListView.setItemChecked(-1, false);
		
		//Set the default theme listitem.
		mListView.setItemChecked(0, true);
		mListView.setSelection(0);

		int pos = 0;
		for (ThemeInfo ti : mListInfo) {
			if (ti.styleName.equals(theme)) {
				mListView.setItemChecked(pos, true);
				
				// Move list to show the selected item:
				mListView.setSelection(pos);
				break;
			}
			pos++;
		}
	}

	@Override
	public Bundle onSaveInstanceState() {
		Log.d(TAG, "onSaveInstanceState");

		Bundle b = super.onSaveInstanceState();
		String theme = getSelectedTheme();
		b.putString(BUNDLE_THEME, theme);
		return b;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		Log.d(TAG, "onRestore");

		String theme = getSelectedTheme();

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(BUNDLE_THEME)) {
				theme = savedInstanceState.getString(BUNDLE_THEME);

				Log.d(TAG, "onRestore theme " + theme);
			}
		}

		mListener.onSetTheme(theme);
	}

	public void onClick(DialogInterface dialog, int which) {
		if (which == BUTTON_POSITIVE) {
			pressOk();
		} else if (which == BUTTON_NEGATIVE) {
			pressCancel();
		}

	}

	@Override
	public void onCancel(DialogInterface arg0) {
		pressCancel();
	}

	public void pressOk() {

		/* User clicked Yes so do some stuff */
		String theme = getSelectedTheme();
		mListener.onSaveTheme(theme);
		mListener.onSetTheme(theme);

		boolean setForAllThemes = mCheckBox.isChecked();
		PreferenceActivity.setThemeSetForAll(mContext, setForAllThemes);
		if (setForAllThemes) {
			mListener.onSetThemeForAll(theme);
		}
	}

	private String getSelectedTheme() {
		int pos = mListView.getCheckedItemPosition();

		if (pos != ListView.INVALID_POSITION) {
			ThemeInfo ti = mListInfo.get(pos);
			return ti.styleName;
		} else {
			return null;
		}
	}

	public void pressCancel() {
		/* User clicked No so do some stuff */
		String theme = mListener.onLoadTheme();
		mListener.onSetTheme(theme);

		//Set the list to the default theme
		mListView.setItemChecked(0, true);
		mListView.setSelection(0);
		
		//Set the list item to the previously chosen theme.
		int pos = 0;
		for (ThemeInfo ti : mListInfo) {
			if (ti.styleName.equals(theme)) {
				mListView.setItemChecked(pos, true);
				mListView.setSelection(pos);
				break;
			}
			pos++;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		String theme = getSelectedTheme();

		if (theme != null) {
			mListener.onSetTheme(theme);
		}
	}

	public interface ThemeDialogListener {
		void onSetTheme(String theme);

		void onSetThemeForAll(String theme);

		String onLoadTheme();

		void onSaveTheme(String theme);
	}
}
