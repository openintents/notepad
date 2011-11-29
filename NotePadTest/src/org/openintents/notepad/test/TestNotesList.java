/*
 * This allows for the testing of OI Notepad. Both Notepad and NotepadTest should
 * be imported.
 * 
 * It is assumed that English is the language being used and OI File manager
 * is NOT installed. Marquee setting should also be active.
 * 
 * There should be at least one note with one tag already present so that the
 * tag selection spinner is displayed.
 * 
 * On the NotepadTest project, select Run As --> Run As Android JUnit Test
 * 
 * @author Gautam Gupta
 * 
 */
package org.openintents.notepad.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.jayway.android.robotium.solo.Solo;

public class TestNotesList extends InstrumentationTestCase {
	//ActivityInstrumentationTestCase2<NotesList> {

	// Text should be of more than one line so that it would be truncated if
	// marquee is active. Do not include "..."
	private static String test_text = "Note to self: OpenIntents is awesome!";

	// At least one tag
	private static final String[] test_tags = {"OpenIntents", "awesome things", "Android"};

	private Solo solo;
	private Activity activity;
	private Random random = new Random();

	/**
	 * Method to join array elements of type string
	 * @param inputArray Array which contains strings
	 * @param glueString String between each array element
	 * @return String containing all array elements separated by glue string
	 */
	public static String implodeArray(String[] inputArray, String glueString) {
		/** Output variable */
		String output = "";

		if (inputArray.length > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(inputArray[0]);

			for (int i=1; i<inputArray.length; i++) {
				sb.append(glueString);
				sb.append(inputArray[i]);
			}

			output = sb.toString();
		}

		return output;
	}

	/**
	 * Add a note
	 * We should be in the NotesList activity, we'd be back there at the end
	 * @param method Would be appended to the text
	 * @param assertCreation Assert that the note was created?
	 * @return Returns the new test text string
	 */
	public String addNote(String method, Boolean assertCreation) {
		solo.assertCurrentActivity("Expected NotesList activity", "NotesList");
		String new_test_text = test_text + " " + method;

		// Add the note
		solo.clickOnMenuItem("Add note");
		solo.assertCurrentActivity("Expected NoteEditor activity", "NoteEditor");
		solo.enterText(0, new_test_text);

		// Check if it exists
		solo.goBack();
		solo.assertCurrentActivity("Expected NotesList activity", "NotesList");
		if (assertCreation == true) {
			assertTrue(solo.searchText(new_test_text));
		}

		return new_test_text;
	}

	/**
	 * Remove a note
	 * We should be in the NotesList activity, we'd be back there at the end
	 * @param noteTitle Note title
	 * @param assertRemoval Assert that the note was removed?
	 */
	public void removeNote(String noteTitle, Boolean assertCreation) {
		solo.assertCurrentActivity("Expected NotesList activity", "NotesList");

		// Delete the item
		solo.clickLongOnText(noteTitle);
		solo.clickOnText("Delete");

		// Make sure user is asked before this and delete
		assertTrue(solo.searchText("Are you sure"));
		solo.clickOnText("OK");

		// Verify that our note is not there if asked to
		if (assertCreation == true) {
			assertFalse(solo.searchText(noteTitle));
		}
	}

	/**
	 * Add tags to a note
	 * We should be in the NotesList activity, we'd be back there at the end
	 * 
	 * There should be at least one note with one tag already present
	 * so that the tag selection spinner is displayed.
	 * @param noteTitle Note title
	 */
	public void addTags(String noteTitle) {
		solo.assertCurrentActivity("Expected NotesList activity", "NotesList");

		// Long press and add tags
		solo.clickLongOnTextAndPress(noteTitle, 0);
		solo.enterText(0, implodeArray(test_tags, ", "));
		solo.clickOnButton("OK");

		// Check that they were was added
		assertTrue(solo.searchText(test_tags[random.nextInt(test_tags.length)]));
	}

	/**
	 * Remove all the notes made by the tests
	 */
	public void cleanUp() {
		while (solo.searchText(test_text)) {
			removeNote(test_text, false);
		}
	}

	public TestNotesList() {
		super();
	}

	protected void setUp() throws Exception {
		super.setUp();

		// Unfortunately, extending ActivityInstrumentationTestCase2
		// does not work for NotesList, so we launch the activity
		// manually:
		Intent i = new Intent();
		i.setAction("android.intent.action.MAIN");
		i.setClassName("org.openintents.notepad",
				"org.openintents.notepad.noteslist.NotesList");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		activity = getInstrumentation().startActivitySync(i);

		this.solo = new Solo(getInstrumentation(), activity);
	}

	protected void tearDown() throws Exception {
		// Remove the test notes
		cleanUp();

		try {
			this.solo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		super.tearDown();
	}

	/**
	 * Test the marquee setting
	 */
	public void test1_SettingMarquee() {
		solo.assertCurrentActivity("Expected NotesList activity", "NotesList");

		// Navigate to preferences
		solo.clickOnMenuItem("Settings");
		solo.assertCurrentActivity("Expected PreferenceActivity activity", "PreferenceActivity");

		// Uncheck the checkbox if it's not checked
		if (solo.isCheckBoxChecked(0) == true) {
			solo.clickOnCheckBox(0);
		}
		solo.goBack();

		// Add the note
		String marquee_test_text = addNote("Marquee", false);

		// Assert that full text isn't displayed as marquee isn't active
		assertFalse(solo.searchText(marquee_test_text));

		// Navigate to preferences
		solo.clickOnMenuItem("Settings");
		solo.assertCurrentActivity("Expected PreferenceActivity activity", "PreferenceActivity");

		// Check the checkbox if it's not checked
		if (solo.isCheckBoxChecked(0) == false) {
			solo.clickOnCheckBox(0);
		}

		solo.goBack();
		solo.assertCurrentActivity("Expected NotesList activity", "NotesList");

		// Assert that full text is displayed as marquee is active
		// But first go to the note and come back to actually activate marquee
		solo.clickOnText(marquee_test_text.substring(0, 20));
		solo.goBack();
		assertTrue(solo.searchText(marquee_test_text));
	}

	/**
	 * Add a new note
	 */
	public void test2_NoteAdd() {
		String add_test_text = addNote("Add", true);
		assertTrue(solo.searchText(add_test_text));
	}

	/**
	 * Edit a note, save it, check.
	 */
	public void test3_NoteEdit() {
		// Add the note
		String edit_test_text = addNote("Edit", true);

		// Edit the text
		solo.clickOnText(edit_test_text);
		solo.assertCurrentActivity("Expected NoteEditor activity", "NoteEditor");

		// Generate a random string, set it and then save it
		String new_edit_test_text = " " + random.nextInt(10000);
		solo.enterText(0, new_edit_test_text);
		solo.goBack();
		solo.clickOnText(edit_test_text);
		assertTrue(solo.searchText(edit_test_text + new_edit_test_text));
		solo.goBack();
	}

	/**
	 * Edit a note, revert it, check.
	 */
	public void test4_NoteRevert() {
		// Add the note
		String revert_edit_test_text = addNote("RevEdit", true);

		// Edit the text
		solo.clickOnText(revert_edit_test_text);
		solo.assertCurrentActivity("Expected NoteEditor activity", "NoteEditor");

		// Generate a random string, set it and then revert it
		String new_revert_edit_test_text = " " + random.nextInt(10000);
		solo.enterText(0, new_revert_edit_test_text);
		solo.clickOnMenuItem("Revert");
		solo.goBack();

		// Test that it was actually not saved
		solo.clickOnText(revert_edit_test_text);
		assertFalse(solo.searchText(new_revert_edit_test_text));
		assertTrue(solo.searchText(revert_edit_test_text));
		solo.goBack();
	}

	/**
	 * Try saving the note to the SD Card and opening it
	 * 
	 * OI Filemanager should not be installed.
	 * If the file already exists, the file is overwritten.
	 */
	public void test5_NoteSDCard() {
		// Add the note
		String sdcard_test_text = addNote("SDCard", true);
		solo.clickOnText(sdcard_test_text);

		// Try saving the file to SD Card
		solo.clickOnMenuItem("Save to SD card");
		assertTrue(solo.searchText("Save to SD card"));
		String file_path = android.os.Environment.getExternalStorageDirectory().toString();
		if (!file_path.endsWith("/")) {
			file_path += "/";
		}
		file_path += "OINotepadTest" + random.nextInt(10000) + ".txt";
		solo.clearEditText(1);
		solo.enterText(1, file_path);
		solo.clickOnText("OK");

		// Press ok if a file with that name already exists
		if (solo.searchText("File exists already")) {
			solo.clickOnText("OK");
		}
		
		solo.goBack();

		// Check if the file was written on the sdcard
		try{
			File f = new File(file_path); 
			FileInputStream fileIS = new FileInputStream(f);
			BufferedReader buf = new BufferedReader(new InputStreamReader(fileIS));
			String readString = new String();
			String file_content = new String();
			//just reading each line and pass it on the debugger
			while((readString = buf.readLine())!= null){
				file_content = file_content.concat(readString);
			}
			assertEquals(file_content, sdcard_test_text);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	/**
	 * Add some tags to the note
	 */
	public void test6_TagsAdd() {
		// Add the note and tags
		addTags(addNote("Tags", true));
	}

	/**
	 * Filter the results by a tag that we added
	 * and verify that our note is there.
	 */
	public void test7_TagsFilter() {
		// Add the note and tags
		String filter_tags_test_text = addNote("FilterTags", true);
		addTags(filter_tags_test_text);

		// Let the spinner populate
		solo.clickOnText(filter_tags_test_text);
		solo.goBack();

		// Click the spinner and select a random value from test_tags array
		solo.clickLongOnText("All notes");
		solo.clickOnText(test_tags[random.nextInt(test_tags.length)]);
		assertTrue(solo.searchText(test_text));
	}

	/**
	 * Search for the note
	 */
	public void test8_NoteSearch() {
		// Add the note
		String search_test_text = addNote("Search", true);

		// Open search
		solo.clickOnMenuItem("Search");
		assertTrue(solo.searchText("OI Notepad search"));

		// Enter a part of the string in the search box
		solo.enterText(0, search_test_text.substring(2, 6));
		solo.clickOnButton(0);

		// Make sure our note is there
		assertTrue(solo.searchText(search_test_text));
		solo.goBack();
	}

	/**
	 * Remove the note
	 */
	public void test9_NoteRemove() {
		// Add & remove a note
		removeNote(addNote("Remove", true), true);
	}

}
