package data.selector;

import java.util.HashMap;
import java.util.LinkedList;

import data.Douban;
import data.DoubanUser;
import data.MovieLog;
import data.Weibo;
import BasicOps.Config;
import BasicOps.FileOps;

public class WeiboSelector {
	public static void main(String[] args) {
		Config.load("config.txt");
		Weibo weibo=new Weibo();
		weibo.loadUser();
		weibo.loadWeibo();
		
		System.out.println("Weibo Loaded");
		
		LinkedList<String> users=FileOps.LoadFilebyLine(Config.getValue("SelectDir")+"users");
		System.out.println("User Loaded");
		
		LinkedList<String> username=new LinkedList<String>();
		LinkedList<String> userweibo=new LinkedList<String>();
		
		for (String user:users){
			String[] sep=user.split("\t");
			String uid=sep[2];
			username.add(uid+"\t"+weibo.getUsername(uid));
			for (String w:weibo.getWeibo(uid))
				userweibo.add(uid+"\t"+w);
		}
		
		FileOps.SaveFile(Config.getValue("SelectDir")+"weibo_username",username);
		FileOps.SaveFile(Config.getValue("SelectDir")+"weibo_weibo",userweibo);
		
	}
}
