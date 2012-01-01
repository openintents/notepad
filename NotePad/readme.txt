 ****************************************************************************
 * Copyright (C) 2008-2011 OpenIntents.org                                  *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *      http://www.apache.org/licenses/LICENSE-2.0                          *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************

The OpenIntents Notepad is based on Google's open source 
sample application Notepad that is provided with the Android SDK. 

OI Notepad allows to create, edit, and delete notes.

To obtain the current release, visit
  http://www.openintents.org

---------------------------------------------------------
release 1.3
date: ?
- setting for marquee for long titles (issue 266, Google Code-in task by Aviral Dasgupta)
- added delete confirmation (issue 363, Google Code-in task by Shuhao)
- sort tags (issue 265, Google Code-in task by Shuhao)
- shortcut for adding notes (Google Code-in task by Żyła)
- share note directly from note editor (issue 417, Google Code-in task by Shuhao)
- save cursor and selection position when note is closed (Google Code-in task by Aviral Dasgupta)
- fixed pressing note icon in OI Shopping List crashes OI Notepad (issue 434, Google Code-in task by Neis)
- apply marquee setting immediately (issue 405, Google Code-in task by Daniel Huang)

---------------------------------------------------------
release: 1.2.3
date: 2011-05-28
- sort by creation date or modification date (patch by Maciek)
- support external notes (from OI Shopping List)

---------------------------------------------------------
release: 1.2.2
date: 2011-02-05
- new application icon for Android 2.0 or higher, with original design by VisualPharm.
- allow app installation on external storage (requires Android 2.2 or higher)
- support Android 2.3.
- translations into various languages.

---------------------------------------------------------
release: 1.2.1
date: 2010-02-22
- new font size "tiny" (for Droid users).

---------------------------------------------------------
release: 1.2.0
date: 2010-02-21
- add search system-wide search (has to be activated
  in home > settings > search > searchable items >
  OI Notepad).
- add full-text search within notepad.
- add filter for tags in list of notes.
- don't change modification time when only viewing note.
- new preference for font size.
- support external themes.
- set theme optionally for all lists.
- fix bugs with encrypted notes:
  revert menu and extensions like OI Insert date
- fix revert menu button logic
- translations: Finnish, Italian, Korean, Lao, Romanian, 
  Russian

---------------------------------------------------------
release: 1.1.3
date: 2009-11-11
- translations: Dutch, French, German, Spanish
- create shortcut for a note from Launcher
- save to SD card possible directly from note
- don't automatically import notes from SD card
  but edit in place.
- when saving to SD card, show a warning dialog
  before overwriting an existing file.
- keep cursor position (or selection) when
  changing screen orientation.
- when entering tags, show list of existing tags
- hide Update menu item if Android Market or
  aTrackDog are present.
- compatible with various screen sizes.
- add fast scroll bar.
- hide Update menu item if Market or
  aTrackDog are present.
- add support for MyBackup Pro.

---------------------------------------------------------
release: 1.1.1
date: 2009-05-16

- add support for CATEGORY_TEXT_SELECTION_ALTERNATIVE
  intents.

---------------------------------------------------------
release: 1.1.0
date: 2009-02-02

- upgrade Content Provider with new fields:
  tags, encrypted, theme
- support for encrypted notes through CryptoIntents
  (requires Android Password Safe or compatible)
- tags for notes.
- quickly filter notes by typing the first letters;
  searches through title and tags.
- open .txt files from SD card and save .txt files
  to SD card.
- prepare for permissions to access notes
  (but don't activate them yet).
- support for OI About.

---------------------------------------------------------
release: 1.0.2
date: 2008-12-10

- allow alternative menus that affect the whole list
  of notes. Allows support for ConvertCSV.

---------------------------------------------------------
release: 1.0.1
date: 2008-11-21

- removed Internet permission
- fix for lost note on screen lock
- revert twice to undo last revert
- broadcast changes to database so that extensions
  like VoiceNotes can listen.

---------------------------------------------------------
release: 1.0.0
date: 2008-10-29

- First public release on Android SDK 1.0.

Features: 
- Create, edit, delete notes.
- Send note.

Difference from the original Android SDK version:
- Fixed bug in connection with orientation change.
- Automatically pick title from first line of note.
  Drop title editor.