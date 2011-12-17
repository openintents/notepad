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

/**
 * Original copyright:
 * Based on the Android SDK sample application NotePad.
 * Copyright (C) 2007 Google Inc.
 * Licensed under the Apache License, Version 2.0.
 */

package org.openintents.notepad;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for NotePadProvider
 * 
 * @version 2009-01-12, 23:09 UTC
 */
public final class NotePad {
	public static final String AUTHORITY = "org.openintents.notepad";

	// This class cannot be instantiated
	private NotePad() {}

	/**
	 * Notes table
	 */
	public static final class Notes implements BaseColumns {
		// This class cannot be instantiated
		private Notes() {}

		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.openintents.notepad.note";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.openintents.notepad.note";

		/**
		 * The title of the note
		 * <P>Type: TEXT</P>
		 */
		public static final String TITLE = "title";

		/**
		 * The note itself
		 * <P>Type: TEXT</P>
		 */
		public static final String NOTE = "note";

		/**
		 * The timestamp for when the note was created
		 * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
		 */
		public static final String CREATED_DATE = "created";

		/**
		 * The timestamp for when the note was last modified
		 * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
		 */
		public static final String MODIFIED_DATE = "modified";

		/**
		 * Tags associated with a note.
		 * Multiple tags are separated by commas.
		 * <P>Type: TEXT</P>
		 * @since 1.1.0
		 */
		public static final String TAGS = "tags";

		/**
		 * Whether the note is encrypted.
		 * 0 = not encrypted. 1 = encrypted.
		 * <P>Type: INTEGER</P>
		 * @since 1.1.0
		 */
		public static final String ENCRYPTED = "encrypted";

		/**
		 * A theme URI.
		 * <P>Type: TEXT</P>
		 * @since 1.1.0
		 */
		public static final String THEME = "theme";
		
		/**
		 * The starting position of the selection in the note.
		 * <p>TYPE: INTEGER</p>
		 * @since 1.2.3
		 */
		public static final String SELECTION_START = "selection_start";
		
		/**
		 * The ending position of the selection in the note.
		 * <p>TYPE: INTEGER</p>
		 * @since 1.2.3
		 */
		public static final String SELECTION_END = "selection_end";
		
		/**
		 * The scroll position in the list expressed as scrollY/height
		 * TODO Implement.
		 * <p>TYPE: REAL</p>
		 * @since 1.2.3
		 */
		public static final String SCROLL_POSITION = "scroll_position";

		/**
		 * Support sort orders. The "sort order" in the preferences
		 * is an index into this array.
		 */
		public static final String[] SORT_ORDERS = {"title ASC", "title DESC", "modified DESC", "modified ASC", "created DESC", "created ASC"};
	}
}
