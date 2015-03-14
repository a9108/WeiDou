package data;

import java.util.LinkedList;

public class DoubanUser {
	public String uid;
	public String display;
	public String location;
	public String description;
	public LinkedList<MovieLog> movies;
	public DoubanUser(){}
	
	public DoubanUser(String s) {
		uid=s.substring(0, s.indexOf('\t'));s=s.substring(uid.length()+1, s.length());
		display=s.substring(0, s.indexOf('\t'));s=s.substring(display.length()+1, s.length());
		location=s.substring(0, s.indexOf('\t'));s=s.substring(location.length()+1, s.length());
		description=s;
		movies=new LinkedList<MovieLog>();
	}
	
	public void addMovie(MovieLog cur){
		movies.add(cur);
	}
	@Override
	public String toString() {
		return  uid+"\t"+display+"\t"+location+"\t"+description.replace("\t", " ").replace("\n", " ").replace("\r", " ");
	}
}
