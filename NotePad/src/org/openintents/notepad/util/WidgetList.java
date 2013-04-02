package org.openintents.notepad.util;

public class WidgetList {

	private String name;
	private int id;
	
	public WidgetList(String name,int id){
		this.name = name;
		this.id = id;
	}
	
	public int getNoteId(){
		return id;
	}
	
	public String getNote(){
		return name;
	}
}

