package org.openintents.notepad.intents;

public class NotepadInternalIntents {

	/**
	 * Activity action: Save note to SD card.
	 * 
	 * If data contains a note URI, the file name is generated and the note is
	 * saved.
	 * 
	 * If data contains a file URI, the text from EXTRA_TEXT is saved.
	 * 
	 * In any case, the user is prompted for a file name, and warned if that
	 * file already exists.
	 * 
	 * <p>
	 * Constant Value: "org.openintents.notepad.action.SAVE_TO_SD_CARD"
	 * </p>
	 */
	public static final String ACTION_SAVE_TO_SD_CARD = "org.openintents.notepad.action.SAVE_TO_SD_CARD";

	/**
	 * The text currently selected, if used with
	 * CATEGORY_TEXT_SELECTION_ALTERNATIVE.
	 * 
	 * <p>
	 * Constant Value: "org.openintents.extra.TEXT"
	 * </p>
	 */
	public static final String EXTRA_TEXT = "org.openintents.extra.TEXT";

	/**
	 * The tags to be used for a newly created note.
	 * 
	 * <p>
	 * Constant Value: "org.openintents.extra.TAGS"
	 * </p>
	 */
	public static final String EXTRA_TAGS = "org.openintents.extra.TAGS";
}
