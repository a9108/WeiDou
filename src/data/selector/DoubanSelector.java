package data.selector;

import java.util.HashMap;
import java.util.LinkedList;

import data.Douban;
import data.DoubanUser;
import data.MovieLog;
import BasicOps.Config;
import BasicOps.FileOps;

public class DoubanSelector {
	public static void main(String[] args) {
		Config.load("config.txt");
		Douban douban=new Douban();
		douban.loadUser();
		
		System.out.println("Douban Loaded");
		
		LinkedList<String> users=FileOps.LoadFilebyLine(Config.getValue("SelectDir")+"users");
		System.out.println("User Loaded");
		LinkedList<String> movies=FileOps.LoadFilebyLine(Config.getValue("SelectDir")+"douban_movie");
		HashMap<Integer, Integer> moviemap=new HashMap<Integer, Integer>();
		for (String movie:movies){
			String[] sep=movie.split("\t");
			moviemap.put(Integer.valueOf(sep[1]), Integer.valueOf(sep[0]));
		}
		System.out.println("Movie Loaded");
		
		LinkedList<String> userloc=new LinkedList<String>();
		LinkedList<String> username=new LinkedList<String>();
		LinkedList<String> userdes=new LinkedList<String>();
		LinkedList<String> moviedata=new LinkedList<String>();
		for (String user:users){
			String[] sep=user.split("\t");
			String uid=sep[1];
			DoubanUser cur=douban.getUser(uid);
			username.add(uid+"\t"+cur.display);
			userloc.add(uid+"\t"+cur.location);
			userdes.add(uid+"\t"+cur.description);
			LinkedList<String> mfile=FileOps.LoadFilebyLine(Config.getValue("DataDir")+"douban_usermovie\\"+uid);
			for (String line:mfile){
				MovieLog log=new MovieLog(line);
				if (moviemap.containsKey(log.mid))
					moviedata.add(uid+"\t"+moviemap.get(log.mid)+"\t"+log.rate);
			}
		}
		
		FileOps.SaveFile(Config.getValue("SelectDir")+"douban_username",username);
		FileOps.SaveFile(Config.getValue("SelectDir")+"douban_uselocation",userloc);
		FileOps.SaveFile(Config.getValue("SelectDir")+"douban_usedescription",userdes);
		FileOps.SaveFile(Config.getValue("SelectDir")+"douban_userwatched",moviedata);
	}
}
