set PATH=.;c:\cygwin\bin;%PATH%
bash androidxml2po.bash -e
mkdir translations_notepad
copy notepad* translations_notepad
tar -cvvzf translations_notepad.tgz translations_notepad